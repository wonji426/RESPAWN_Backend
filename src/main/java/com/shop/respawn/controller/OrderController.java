package com.shop.respawn.controller;

import com.shop.respawn.domain.RefundStatus;
import com.shop.respawn.dto.PageResponse;
import com.shop.respawn.dto.order.*;
import com.shop.respawn.dto.refund.RefundRequest;
import com.shop.respawn.dto.refund.RefundResponse;
import com.shop.respawn.dto.user.SellerOrderDetailDto;
import com.shop.respawn.dto.user.SellerOrderDto;
import com.shop.respawn.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.shop.respawn.domain.RefundStatus.*;
import static com.shop.respawn.util.AuthenticationUtil.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 장바구니 선택 상품 주문
     */
    @PostMapping("/cart")
    public ResponseEntity<Map<String, Object>> orderSelectedFromCart(
            Authentication authentication,
            @RequestBody @Valid OrderRequestDto orderRequest
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            Long orderId = orderService.prepareOrderSelectedFromCart(buyerId, orderRequest);

            return ResponseEntity.ok(Map.of(
                    "message", "선택한 상품의 주문이 성공적으로 생성되었습니다.",
                    "orderId", orderId
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 상품페이지에서 상품 주문
     */
    @PostMapping("/prepare")
    public ResponseEntity<Map<String, Object>> prepareOrder(
            Authentication authentication,
            @RequestBody OrderRequestDto orderRequest
    ) {
        try {
            // 로그인된 사용자 ID 가져오기
            Long buyerId = getUserIdFromAuthentication(authentication);

            // 주문 생성 서비스 호출 (itemId, count, buyerId 전달)
            Long orderId = orderService.createTemporaryOrder(buyerId, orderRequest.getItemId(), orderRequest.getCount());

            // 결과 응답
            return ResponseEntity.ok(Map.of("orderId", orderId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 선택된 상품 주문 완료 처리
     */
    @PostMapping("/{orderId}/complete")
    public ResponseEntity<Map<String, Object>> completeSelectedOrder(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestBody @Valid OrderRequestDto orderRequest
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            orderService.completeSelectedOrder(buyerId, orderId, orderRequest);

            return ResponseEntity.ok(Map.of(
                    "message", "선택된 상품의 주문이 성공적으로 완료되었습니다."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 주문 완료 상세 페이지
     */
    @GetMapping("/{orderId}/complete-info")
    public ResponseEntity<?> getOrderCompleteInfo(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            OrderCompleteInfoDto response = orderService.getOrderCompleteInfo(orderId, buyerId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 임시 주문 상세 조회 (주문 페이지용)
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailsDto> getOrderDetails(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            OrderDetailsDto orderDetails = orderService.getOrderDetails(orderId, buyerId);
            return ResponseEntity.ok(orderDetails);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 로그인한 구매자의 주문 내역 조회
     */
    @GetMapping("/history")
    public ResponseEntity<PageResponse<OrderHistoryDto>> getOrderHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long buyerId = getUserIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderHistoryDto> orderHistory = orderService.getOrderHistory(buyerId, pageable);
        return ResponseEntity.ok(PageResponse.from(orderHistory));
    }

    /**
     * 로그인한 구매자의 주문 내역 단건 조회
     */
    @GetMapping("/history/{orderId}")
    public ResponseEntity<OrderHistoryDto> getOrderDetail(
            Authentication authentication,
            @PathVariable Long orderId) {
        // 로그인 사용자 아이디
        Long buyerId = getUserIdFromAuthentication(authentication);
        OrderHistoryDto order = orderService.getOrderDetail(orderId, buyerId);
        return ResponseEntity.ok(order);
    }

    /**
     * 현재 사용자의 모든 임시 주문 삭제 (TEMPORARY 상태인 주문들을 일괄 삭제)
     */
    @DeleteMapping("/temporary")
    public ResponseEntity<Map<String, Object>> deleteAllTemporaryOrders(Authentication authentication) {
        try {
            // 로그인 사용자 아이디
            Long buyerId = getUserIdFromAuthentication(authentication);
            long deletedCount = orderService.deleteAllTemporaryOrders(buyerId);

            return ResponseEntity.ok(Map.of(
                    "message", "임시 주문이 성공적으로 삭제되었습니다.",
                    "deletedCount", deletedCount
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 현재 사용자의 최근 주문 조회
     */
    @GetMapping("/latest")
    public ResponseEntity<OrderHistoryDto> getLatestOrder(Authentication authentication) {

        Long buyerId = getUserIdFromAuthentication(authentication);

        OrderHistoryDto latestOrder = orderService.getLatestOrderByBuyerId(buyerId);

        if (latestOrder == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(latestOrder);
    }

    @GetMapping("/history/recent-month")
    public ResponseEntity<PageResponse<OrderHistoryDto>> getRecentMonthOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderHistoryDto> orders = orderService.getRecentMonthOrders(buyerId, pageable);
            return ResponseEntity.ok(PageResponse.from(orders));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(PageResponse.error(e.getMessage()));
        }
    }

    /**
     * 환불 가능한 목록 조회
     */
    @GetMapping("/refundable-items")
    public ResponseEntity<?> getRefundableItems(Authentication authentication) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            List<OrderHistoryDto> refundableItems = orderService.getRefundableOrderItems(buyerId);
            return ResponseEntity.ok(refundableItems);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 사용자의 환불 요청
     */
    @PostMapping("/{orderId}/items/{orderItemId}/refund")
    public ResponseEntity<?> requestRefund(
            Authentication authentication,
            @PathVariable Long orderId,
            @PathVariable Long orderItemId,
            @RequestBody OrderRefundRequestDto refundDto
    ) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            orderService.requestRefund(orderId, orderItemId, buyerId, refundDto.getReason(), refundDto.getDetail());
            return ResponseEntity.ok(Map.of("message", "해당 아이템의 환불 요청이 완료되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 요청한 환불 목록 보기
     */
    @GetMapping("/refund-requests")
    public ResponseEntity<PageResponse<OrderHistoryDto>> getRefundRequests(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long buyerId = getUserIdFromAuthentication(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderHistoryDto> refundRequests = orderService.getRefundRequestedItems(buyerId, pageable);
            return ResponseEntity.ok(PageResponse.from(refundRequests));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(PageResponse.error(e.getMessage()));
        }
    }

    /**
     * 환불 요청 판매자 확인
     */
    @GetMapping("/seller/refund-requests")
    public ResponseEntity<PageResponse<RefundRequest>> getRefundRequestsOfSeller(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long sellerId = getUserIdFromAuthentication(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<RefundRequest> result = orderService.getRefundRequestsByStatus(sellerId, RefundStatus.REQUESTED, pageable);
            return ResponseEntity.ok(PageResponse.from(result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(PageResponse.error(e.getMessage()));
        }

    }

    /**
     * 판매자 환불 요청 완료 처리
     */
    @PostMapping("/seller/refund-requests/{orderItemId}/complete")
    public ResponseEntity<?> completeRefund(
            Authentication  authentication,
            @PathVariable Long orderItemId) {

        try {
            Long sellerId = getUserIdFromAuthentication(authentication);
            RefundResponse RefundResponse = orderService.completeRefund(orderItemId, sellerId);
            return ResponseEntity.ok(RefundResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 판매자 환불 요청 완료 조회
     */
    @GetMapping("/seller/refund-completed")
    public ResponseEntity<?> getCompletedRefunds(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long sellerId = getUserIdFromAuthentication(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<RefundRequest> completedRefunds = orderService.getRefundRequestsByStatus(sellerId, REFUNDED, pageable);
            return ResponseEntity.ok(PageResponse.from(completedRefunds));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(PageResponse.error(e.getMessage()));
        }
    }

    /**
     * 판매자의 item의 주문 기록 조회
     */
    @GetMapping("/seller/orders")
    public ResponseEntity<List<SellerOrderDto>> getSellerOrders(Authentication authentication) {
        try {
            Long sellerId = getUserIdFromAuthentication(authentication);
            List<SellerOrderDto> orders  = orderService.getSellerOrders(sellerId);
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 판매자의 item의 주문 상세 조회
     */
    @GetMapping("/seller/orders/{orderItemId}")
    public ResponseEntity<SellerOrderDetailDto> getSellerOrderDetail(
            Authentication authentication,
            @PathVariable Long orderItemId
    ) {
        try {
            Long sellerId = getUserIdFromAuthentication(authentication);
            SellerOrderDetailDto dto = orderService.getSellerOrderDetail(sellerId, orderItemId);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 배송 완료 처리 메서드
     */
    @PostMapping("/seller/order-items/{orderItemId}/complete-delivery")
    public ResponseEntity<?> completeDelivery(
            Authentication authentication,
            @PathVariable Long orderItemId
    ) {
        try {
            Long sellerId = getUserIdFromAuthentication(authentication);
            orderService.completeDelivery(orderItemId, sellerId);
            return ResponseEntity.ok(Map.of(
                    "message", "배송이 성공적으로 완료 처리되었습니다.",
                    "orderItemId", orderItemId
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
