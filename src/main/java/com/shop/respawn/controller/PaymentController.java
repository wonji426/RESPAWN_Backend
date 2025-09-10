package com.shop.respawn.controller;

import com.shop.respawn.dto.Payment.PaymentDto;
import com.shop.respawn.dto.Payment.PrepareRequest;
import com.shop.respawn.dto.Payment.VerifyRequest;
import com.shop.respawn.service.OrderService;
import com.shop.respawn.service.PaymentService;
import com.siot.IamportRestClient.exception.IamportResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    // 사전 검증 API
    @PostMapping("/prepare")
    public ResponseEntity<String> preparePayment(@RequestBody PrepareRequest request) {
        try {
            paymentService.preparePayment(request.getMerchantUid(), request.getOrderId());
            return ResponseEntity.ok("사전 검증 완료");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("사전 검증 실패: " + e.getMessage());
        }
    }

    // 결제 검증 API
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody VerifyRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 입력 데이터 검증
            if (request.getImpUid() == null || request.getImpUid().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "impUid가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getOrderId() == null) {
                response.put("success", false);
                response.put("message", "orderId가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("결제 검증 요청 - impUid: {}, merchantUid: {}, orderId: {}",
                    request.getImpUid(), request.getMerchantUid(), request.getOrderId());

            // Order를 통해 buyerId 조회
            Long buyerId = getBuyerIdFromOrder(request.getOrderId());

            PaymentDto result = paymentService.verifyPayment(request.getImpUid(), buyerId, request.getOrderId(), request.getUsePointAmount());

            response.put("success", true);
            response.put("data", result);
            response.put("message", "결제 검증 완료");

            return ResponseEntity.ok(response);

        } catch (IamportResponseException e) {
            log.error("아임포트 API 오류: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "아임포트 서버 오류: " + e.getMessage());
            response.put("errorCode", "IAMPORT_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (IOException e) {
            log.error("네트워크 오류: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "네트워크 연결 오류");
            response.put("errorCode", "NETWORK_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("결제 검증 중 오류 발생", e);
            response.put("success", false);
            response.put("message", "결제 검증 실패: " + e.getMessage());
            response.put("errorCode", "VERIFICATION_ERROR");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 주문 ID로 구매자 ID 조회 (OrderService 사용)
     */
    private Long getBuyerIdFromOrder(Long orderId) {
        try {
            return orderService.getBuyerIdByOrderId(orderId);
        } catch (Exception e) {
            log.error("주문에서 구매자 ID 조회 실패 - orderId: {}", orderId, e);
            throw new RuntimeException("주문 정보를 조회할 수 없습니다: " + e.getMessage());
        }
    }
}
