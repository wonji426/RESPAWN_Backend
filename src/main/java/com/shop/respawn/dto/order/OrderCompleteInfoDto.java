package com.shop.respawn.dto.order;

import com.shop.respawn.domain.*;
import com.shop.respawn.dto.coupon.CouponDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class OrderCompleteInfoDto {

    private Long orderId;                                   // 주문번호
    private String name;                                    // 구매자 이름
    private String phoneNumber;                             // 구매자 전화번호
    private String email;                                   // 구매자 이메일

    private String orderName;                               // 주문명
    private String pgOrderId;                               // 결제주문명
    private String paymentStatus;                           // 결제 상태
    private LocalDateTime orderDate;                        //주문일

    private List<OrderCompleteItemDto> orderItems;          // 주문 상품 상세 DTO
    private List<OrderCompleteDeliveryDto> deliveryInfo;    // 배송 상세 정보 DTO
    private CouponDTO couponInfo;                            // 사용한 쿠폰 정보 DTO
    private PaymentInfoDto paymentInfo;                     // 결제 정보 DTO
    private PointBenefitDto pointBenefit;                   // 포인트 적립 DTO

    private Long originalAmount;                            // 원래 금액 (할인/적립 전) [itemTotalPrice + deliveryFee]
    private Long deliveryFee;                               // 배송비
    private Long itemTotalPrice;                            // 아이템 총 가격
    private Long usedPointAmount;                           // 사용한 포인트
    private Long usedCouponAmount;                          // 사용한 쿠폰 금액
    private Long totalAmount;                               // 결제한 가격

    public static OrderCompleteInfoDto from(Order order,
                                            List<OrderCompleteItemDto> items,
                                            List<OrderCompleteDeliveryDto> deliveries,
                                            Coupon coupon,
                                            Payment payment,
                                            PointLedger savedLedger) {

        return OrderCompleteInfoDto.builder()
                .orderId(order.getId())
                .name(order.getBuyer().getName())
                .phoneNumber(order.getBuyer().getPhoneNumber())
                .email(order.getBuyer().getEmail())
                .orderName(order.getOrderName())
                .pgOrderId(order.getPgOrderId())
                .paymentStatus(order.getPaymentStatus())
                .orderDate(order.getOrderDate())
                .orderItems(items)
                .deliveryInfo(deliveries)
                .couponInfo(CouponDTO.fromEntity(coupon))
                .paymentInfo(PaymentInfoDto.from(payment))
                .pointBenefit(PointBenefitDto.from(savedLedger))
                .originalAmount(order.getOriginalAmount())
                .itemTotalPrice(order.getOriginalAmount() - order.getDeliveryFee())
                .deliveryFee(order.getDeliveryFee())
                .usedPointAmount(order.getUsedPointAmount())
                .usedCouponAmount(order.getUsedCouponAmount())
                .totalAmount(order.getTotalAmount())
                .build();
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class OrderCompleteItemDto {
        private Long orderItemId;
        private String itemId;
        private String itemName;
        private String imageUrl;
        private Long orderPrice;
        private Long count;
        private Long totalPrice;

        public static OrderCompleteItemDto from(OrderItem orderItem, Item item) {
            return OrderCompleteItemDto.builder()
                    .orderItemId(orderItem.getId())
                    .itemId(item.getId())
                    .itemName(item.getName())
                    .imageUrl(item.getImageUrl())
                    .orderPrice(orderItem.getOrderPrice())
                    .count(orderItem.getCount())
                    .totalPrice(orderItem.getOrderPrice() * orderItem.getCount())
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class OrderCompleteDeliveryDto {
        private Long orderItemId;
        private String itemId;
        private DeliveryStatus status;
        private AddressDto address;

        public static OrderCompleteDeliveryDto from(OrderItem orderItem) {
            Delivery delivery = orderItem.getDelivery();
            return OrderCompleteDeliveryDto.builder()
                    .orderItemId(orderItem.getId())
                    .itemId(orderItem.getItemId())
                    .status(delivery != null ? delivery.getStatus() : null)
                    .address(AddressDto.from(delivery != null ? delivery.getAddress() : null))
                    .build();
        }

        @Data
        @Builder
        @AllArgsConstructor
        public static class AddressDto {
            private Long id;
            private String addressName;
            private String recipient;
            private String zoneCode;
            private String baseAddress;
            private String detailAddress;
            private String phone;
            private String subPhone;

            public static AddressDto from(Address address) {
                if (address == null) return null;
                return AddressDto.builder()
                        .id(address.getId())
                        .addressName(address.getAddressName())
                        .recipient(address.getRecipient())
                        .zoneCode(address.getZoneCode())
                        .baseAddress(address.getBaseAddress())
                        .detailAddress(address.getDetailAddress())
                        .phone(address.getPhone())
                        .subPhone(address.getSubPhone())
                        .build();
            }
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class PaymentInfoDto {
        private String cardName;
        private String paymentMethod;
        private String pgProvider;

        public static PaymentInfoDto from(Payment payment) {
            if (payment == null) return null;
            return new PaymentInfoDto(
                    payment.getCardName(),
                    payment.getPaymentMethod(),
                    payment.getPgProvider()
            );
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class PointBenefitDto {
        private String benefitName;
        private Long savedAmount;
        private LocalDateTime expiryAt;

        public static PointBenefitDto from(PointLedger ledger) {
            if (ledger == null) return null;
            return new PointBenefitDto(
                    ledger.getReason(),
                    ledger.getAmount(),
                    ledger.getExpiryAt()
            );
        }
    }
}
