package com.score.backend.services;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.score.backend.models.User;
import com.score.backend.models.dtos.FcmMessageRequest;
import com.score.backend.models.dtos.FcmNotificationResponse;
import com.score.backend.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final UserService userService;
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final NotificationRepository notificationRepository;

    private String generateRedisKey(Long senderId, Long receiverId) {
        return String.format("user:%d:notified:%d", senderId, receiverId);
    }
    // 알림 전송 가능 여부 확인(오늘 sender가 receiver에게 바통 찌르기를 한 기록이 없으면 true 리턴)
    public boolean canSendNotification(Long senderId, Long receiverId) {
        String key = generateRedisKey(senderId, receiverId);
        return Boolean.FALSE.equals(stringRedisTemplate.hasKey(key));
    }

    public com.score.backend.models.Notification findById(Long id) {
        return notificationRepository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    public Page<FcmNotificationResponse> findAllByUserId(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, 25, Sort.by(Sort.Order.desc("createdAt")));
        return new FcmNotificationResponse().toDto(notificationRepository.findByAgentId(userId, pageable));
    }

    public void getToken(Long userId, String token) {
        User user = userService.findUserById(userId).orElseThrow(
                () -> new NoSuchElementException("User not found")
        );
        user.setFcmToken(token);
    }

    @Transactional(readOnly = true)
    public String sendMessage(FcmMessageRequest request)  throws FirebaseMessagingException {
        return FirebaseMessaging.getInstance().send(Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getBody())
                        .build())
                .setToken(userService.findUserById(request.getUserId()).orElseThrow(
                        () -> new NoSuchElementException("User not found")
                ).getFcmToken())  // 대상 디바이스의 등록 토큰
                .build());
    }

    public void saveNotification(FcmMessageRequest request) {
        notificationRepository.save(new com.score.backend.models.Notification(userService.findUserById(request.getUserId()).orElseThrow(
                () -> new NoSuchElementException("User not found")
        ), request.getTitle(), request.getBody()));
    }

    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
