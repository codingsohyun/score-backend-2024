package com.score.backend.controllers;

import com.score.backend.models.Group;
import com.score.backend.models.dtos.GroupCreateDto;
import com.score.backend.models.dtos.GroupDto;
import com.score.backend.services.GroupRankingService;
import com.score.backend.services.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.NoSuchElementException;

@Tag(name = "Group", description = "그룹 정보 관리를 위한 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final GroupRankingService groupRankingService;

    @Operation(summary = "그룹 생성", description = "새로운 그룹을 생성하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 생성이 완료되었습니다."),
            @ApiResponse(responseCode = "400", description = "Bad Request")
    })
    @PostMapping("/create")
    public ResponseEntity<String> createGroup(@Valid @RequestBody GroupCreateDto groupCreateDto, @RequestParam Long adminId) {
        try {
            groupCreateDto.isValid();  // 추가 검증
            groupService.createGroup(groupCreateDto, adminId);
            return ResponseEntity.ok("그룹 생성이 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "내(모든) 그룹 목록 조회", description = "내(모든) 그룹의 목록을 조회하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "모든 그룹 조회가 완료되었습니다.")
    })
    @GetMapping("/all")
    public ResponseEntity<List<GroupDto>> getAllGroups() {
        List<GroupDto> groups = groupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }

    @Operation(summary = "그룹 탈퇴", description = "사용자가 그룹을 탈퇴하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 탈퇴가 완료되었습니다."),
            @ApiResponse(responseCode = "400", description = "Bad Request")
    })
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<String> leaveGroup(@PathVariable Long groupId, @RequestParam Long userId) {
        try {
            groupService.leaveGroup(groupId, userId);
            return ResponseEntity.ok("그룹 탈퇴가 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "그룹 정보 수정", description = "그룹 정보를 수정하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 정보 수정이 완료되었습니다."),
            @ApiResponse(responseCode = "400", description = "Bad Request")
    })
    @PutMapping("/{groupId}/update")
    public ResponseEntity<String> updateGroup(@PathVariable Long groupId,
                                              @Valid @RequestBody GroupCreateDto groupCreateDto,
                                              @RequestParam Long adminId) {
        try {
            groupService.updateGroup(groupId, groupCreateDto, adminId);
            return ResponseEntity.ok("그룹 정보 update 완료!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "그룹 멤버 강퇴", description = "방장이 그룹에서 멤버를 강퇴하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "멤버 강퇴가 완료되었습니다."),
            @ApiResponse(responseCode = "400", description = "Bad Request")
    })
    @DeleteMapping("/{groupId}/removeMember")
    public ResponseEntity<String> removeMemberFromGroup(@PathVariable Long groupId, @RequestParam Long userId) {
        try {
            groupService.removeMember(groupId, userId);
            return ResponseEntity.ok("멤버 강퇴가 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "그룹 랭킹 조회", description = "지난 주 그룹 내 개인 랭킹을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 내 유저들을 랭킹순으로 정렬하여 응답"),
            @ApiResponse(responseCode = "409", description = "지난 주 신규 생성되어 랭킹이 산정되지 않는 그룹에 대한 조회 요청"),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없습니다.")
    })
    @GetMapping("/{groupId}/ranking")
    public ResponseEntity<?> getGroupRanking(@PathVariable Long groupId) {
        Group group = groupService.findById(groupId);

        // 현재 날짜 기준으로 직전 주 월요일 날짜
        LocalDate mondayLastWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        // 해당 그룹이 생성된 날짜
        LocalDate createdDate = group.getCreatedAt().toLocalDate();

        if (!createdDate.isBefore(mondayLastWeek)) {
            return ResponseEntity.status(409).body("신규 그룹은 이번주부터 랭킹이 산정돼요.");
        }
        try {
            return ResponseEntity.ok(groupRankingService.getWeeklyGroupRanking(groupId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
