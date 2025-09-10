package com.shop.respawn.service;

import com.shop.respawn.domain.Buyer;
import com.shop.respawn.domain.Order;
import com.shop.respawn.domain.OrderStatus;
import com.shop.respawn.dto.Payment.PaymentDto;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.OrderRepository;
import com.shop.respawn.repository.jpa.PaymentRepository;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.PrepareData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import com.siot.IamportRestClient.response.Prepare;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;

@Slf4j
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IamportClient iamportClient;
    private final BuyerRepository buyerRepository;
    private final OrderRepository orderRepository;
    private final LedgerPointService ledgerPointService;

    public PaymentService(PaymentRepository paymentRepository,
                          BuyerRepository buyerRepository,
                          OrderRepository orderRepository,
                          LedgerPointService ledgerPointService,
                          @Value("${imp.api.key}") String impKey,
                          @Value("${imp.api.secretkey}") String impSecret) {
        this.buyerRepository = buyerRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.ledgerPointService = ledgerPointService;
        this.iamportClient = new IamportClient(impKey, impSecret);
    }

    // 결제 검증
    public PaymentDto verifyPayment(String impUid, Long buyerId, Long orderId) throws IamportResponseException, IOException {
        IamportResponse<Payment> iamportResponse = iamportClient.paymentByImpUid(impUid);
        Long paidAmount = iamportResponse.getResponse().getAmount().longValue();

        // 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        System.out.println("order.getTotalAmount() = " + order.getTotalAmount());
        System.out.println("paidAmount = " + paidAmount);
        // 결제 금액 검증 (배송비 포함된 totalAmount와 비교)
        if (!paidAmount.equals(order.getTotalAmount())) {
            throw new RuntimeException(
                    "결제 금액 불일치: PG=" + paidAmount + ", 서버계산=" + order.getTotalAmount()
            );
        }

        Long amount = iamportResponse.getResponse().getAmount().longValue();
        String name = iamportResponse.getResponse().getName();
        String status = iamportResponse.getResponse().getStatus();
        String paymentMethod = iamportResponse.getResponse().getPayMethod();
        String pgProvider = iamportResponse.getResponse().getPgProvider();
        String cardName = iamportResponse.getResponse().getCardName();

        PaymentDto paymentDto = PaymentDto.builder()
                .impUid(impUid)
                .amount(amount)
                .status(status)
                .name(name)
                .buyerId(buyerId)  // buyerId 추가
                .orderId(orderId)  // orderId 추가
                .paymentMethod(paymentMethod)
                .pgProvider(pgProvider)
                .cardName(cardName)
                .build();

        if ("paid".equals(status)) {
            // 결제 성공 로직
            savePayment(paymentDto);
        } else {
            paymentDto.setStatus("결제 오류입니다. 다시 시도해주세요.");
        }
        return paymentDto;
    }

    // 사전 검증 (결제 금액 위변조 방지)
    public void preparePayment(String merchantUid, Long orderId)
            throws IamportResponseException, IOException {

        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        // 2. totalAmount 가져오기 (배송비 포함된 금액)
        BigDecimal amount = BigDecimal.valueOf(order.getTotalAmount());

        // 3. 아임포트 사전검증 요청
        PrepareData prepareData = new PrepareData(merchantUid, amount);
        IamportResponse<Prepare> response = iamportClient.postPrepare(prepareData);

        if (response.getCode() != 0) {
            throw new RuntimeException("사전 검증 실패: " + response.getMessage());
        }
    }
    /**
     * 결제 정보를 데이터베이스에 저장
     */
    private void savePayment(PaymentDto paymentDto) {
        // Buyer 조회
        Buyer buyer = buyerRepository.findById(paymentDto.getBuyerId())
                .orElseThrow(() -> new RuntimeException("구매자를 찾을 수 없습니다: " + paymentDto.getBuyerId()));

        // Order 조회
        Order order = orderRepository.findById(paymentDto.getOrderId())
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + paymentDto.getOrderId()));
        com.shop.respawn.domain.Payment payment = com.shop.respawn.domain.Payment.builder()
                .impUid(paymentDto.getImpUid())
                .paymentMethod(paymentDto.getPaymentMethod())
                .pgProvider(paymentDto.getPgProvider())
                .cardName(paymentDto.getCardName())
                .amount(paymentDto.getAmount())
                .status(paymentDto.getStatus())
                .name(paymentDto.getName())
                .buyer(buyer)  // Buyer 엔티티 설정
                .order(order)  // Order 엔티티 설정
                .build();
        paymentRepository.save(payment);

        if (order.getUsedPointAmount() != null && order.getUsedPointAmount() > 0) {
            ledgerPointService.usePoints(buyer.getId(),
                    order.getUsedPointAmount(),
                    order.getId(),
                    "주문 포인트 사용",
                    "user");
        }

        // 주문의 결제 상태도 업데이트
        order.setPaymentStatus("SUCCESS");
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

}
