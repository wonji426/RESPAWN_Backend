package com.shop.respawn.controller;

import com.shop.respawn.domain.Temporary;
import com.shop.respawn.dto.TemporaryDto;
import com.shop.respawn.service.TemporaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/temporary")
@RequiredArgsConstructor
public class TemporaryController {

    private final TemporaryService temporaryService;

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveTemporary(@RequestBody TemporaryDto request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Temporary savedTemp = temporaryService.saveTemporaryData(request);

            response.put("success", true);
            response.put("temporaryId", savedTemp.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("임시 결제 데이터 저장 실패", e);
            response.put("success", false);
            response.put("message", "결제 준비 중 오류가 발생했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTemporary(@PathVariable("id") Long temporaryId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Service를 통해 DB에서 데이터 조회
            Temporary temporary = temporaryService.getTemporaryData(temporaryId);

            // 2. 응답용 DTO에 데이터 세팅 (보안 및 구조 깔끔함을 위해)
            TemporaryDto responseDto = new TemporaryDto();
            responseDto.setOrderId(temporary.getOrderId());
            responseDto.setAddressId(temporary.getAddressId());
            responseDto.setCouponCode(temporary.getCouponCode());
            responseDto.setUsePointAmount(temporary.getUsedPointAmount());
            // 주의: amount는 Temporary 엔티티에 없으므로 세팅 불가

            // 3. 성공 응답
            response.put("success", true);
            response.put("data", responseDto);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("임시 결제 데이터 조회 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("서버 내부 오류 발생", e);
            response.put("success", false);
            response.put("message", "데이터를 불러오는 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

}
