package com.shop.respawn.controller;

import com.shop.respawn.dto.AddressDto;
import com.shop.respawn.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.shop.respawn.util.SessionUtil.getUserIdFromAuthentication;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 로그인한 사용자의 주소 저장
     */
    @PostMapping("/add")
    public ResponseEntity<?> createAddress(
            Authentication authentication,
            @RequestBody AddressDto addressDto
    ) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        AddressDto savedAddress = addressService.createAddress(buyerId, addressDto);
        return ResponseEntity.ok(savedAddress);
    }

    /**
     * 로그인한 사용자의 모든 주소 조회 (기본 주소 우선, 최신 순)
     */
    @GetMapping
    public ResponseEntity<List<AddressDto>> getMyAddresses(Authentication authentication) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        List<AddressDto> addresses = addressService.getAddressesByBuyer(buyerId);
        return ResponseEntity.ok(addresses);
    }

    /**
     * 로그인한 사용자의 기본 주소 조회
     */
    @GetMapping("/basic")
    public ResponseEntity<AddressDto> getMyBasicAddress(Authentication authentication) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            AddressDto basicAddress = addressService.getBasicAddress(buyerId);
            return ResponseEntity.ok(basicAddress);
        } catch (IllegalStateException e) {
            // 기본 주소가 없는 경우
            return ResponseEntity.noContent().build(); // 204 No Content
        }
    }

    /**
     * 주소 정보 수정
     */
    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDto> updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @RequestBody AddressDto addressDto
    ) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        AddressDto updatedAddress = addressService.updateAddress(buyerId, addressId, addressDto);
        return ResponseEntity.ok(updatedAddress);
    }

    /**
     * 주소 삭제
     */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<String> deleteAddress(
            Authentication authentication,
            @PathVariable Long addressId
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            addressService.deleteAddress(buyerId, addressId);
            return ResponseEntity.ok("주소 삭제 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
