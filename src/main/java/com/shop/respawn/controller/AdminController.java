package com.shop.respawn.controller;

import com.shop.respawn.dto.user.BuyerListDto;
import com.shop.respawn.dto.PageResponse;
import com.shop.respawn.dto.user.SellerListDto;
import com.shop.respawn.dto.user.UserSummaryDto;
import com.shop.respawn.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@Secured("ROLE_ADMIN")
public class AdminController {

    private final AdminService adminService;

    /**
     * 사용자 계정 만료 처리
     */
    @PostMapping("/{userType}/{userId}/expire")
    public ResponseEntity<Map<String, Object>> expire(@PathVariable String userType,
                                                      @PathVariable Long userId) {
        adminService.expireUserById(userType, userId); // 만료(비활성: isAccountNonExpired=false 상태 유도)
        return ResponseEntity.ok(Map.of(
                "userType", userType,
                "userId", userId,
                "message", "계정을 만료 처리했습니다."
        ));
    }

    /**
     * 사용자 계정 만료 해제
     */
    @PostMapping("/{userType}/{userId}/unexpire")
    public ResponseEntity<Map<String, Object>> unexpire(@PathVariable String userType,
                                                        @PathVariable Long userId) {
        adminService.unexpireUserById(userType, userId); // 만료 해제(유효: isAccountNonExpired=true 상태 유도)
        return ResponseEntity.ok(Map.of(
                "userType", userType,
                "userId", userId,
                "message", "계정 만료를 해제했습니다."
        ));
    }

    /**
     * 사용자 계정 활성화
     */
    @PostMapping("/{userType}/{userId}/enable")
    public ResponseEntity<Map<String, Object>> enable(@PathVariable String userType,
                                                      @PathVariable Long userId) {
        adminService.enableUserById(userType, userId);
        boolean enabled = adminService.isEnabledById(userType, userId);
        return ResponseEntity.ok(Map.of(
                "userType", userType,
                "userId", userId,
                "enabled", enabled,
                "message", "계정이 활성화되었습니다."
        ));
    }

    /**
     * 사용자 계정 비활성화(정지)
     */
    @PostMapping("/{userType}/{userId}/disable")
    public ResponseEntity<Map<String, Object>> disable(@PathVariable String userType,
                                                       @PathVariable Long userId) {
        adminService.disableUserById(userType, userId);
        boolean enabled = adminService.isEnabledById(userType, userId);
        return ResponseEntity.ok(Map.of(
                "userType", userType,
                "userId", userId,
                "enabled", enabled,
                "message", "계정이 정지되었습니다."
        ));
    }

    /**
     * 사용자 계정 정지 해제
     */
    @PostMapping("/{userType}/{userId}/unlock")
    public ResponseEntity<Map<String, Object>> unlock(@PathVariable String userType,
                                                      @PathVariable Long userId) {
        adminService.unlockById(userType, userId);
        return ResponseEntity.ok(Map.of(
                "userType", userType,
                "userId", userId,
                "message", "계정 잠금이 해제되었습니다."
        ));
    }

    /**
     * 사용자 계정 상태 조회
     */
    @GetMapping("/{userType}/{userId}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String userType,
                                                      @PathVariable Long userId) {
        boolean enabled = adminService.isEnabledById(userType, userId);
        return ResponseEntity.ok(Map.of(
                "userType", userType,
                "userId", userId,
                "enabled", enabled
        ));
    }

    // ----- 구매자 조회 -----
    @GetMapping("/buyers/paged")
    public ResponseEntity<PageResponse<BuyerListDto>> getBuyersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "username") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "name") String field,
            @RequestParam(required = false) String dateRange
    ) {
        Page<BuyerListDto> buyersPaged = adminService.findBuyersPaged(page, size, sort, dir, keyword, field, dateRange);
        return ResponseEntity.ok(PageResponse.from(buyersPaged));
    }

    // ----- 판매자 조회 -----
    @GetMapping("/sellers/paged")
    public ResponseEntity<PageResponse<SellerListDto>> getSellersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "username") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "name") String field,
            @RequestParam(required = false) String dateRange
    ) {
        Page<SellerListDto> sellersPaged = adminService.findSellersPaged(page, size, sort, dir, keyword, field, dateRange);
        return ResponseEntity.ok(PageResponse.from(sellersPaged));
    }

    @GetMapping("/{userType}/{userId}/summary")
    public ResponseEntity<UserSummaryDto> getUserSummary(
            @PathVariable String userType,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(adminService.findUserSummaryById(userType, userId));
    }
}
