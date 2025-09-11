package com.shop.respawn.dto.refund;

import com.shop.respawn.domain.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RefundRequest {

    // 주문 정보
    private Long orderItemId;
    private String itemName;
    private Long orderPrice;
    private Long count;
    private LocalDateTime orderDate;
    private String imageUrl;
    private RefundStatus refundStatus;

    // 관련 정보
    private BuyerInfo buyerInfo;
    private AddressInfo addressInfo;
    private RefundInfo refundInfo;

    // 생성자
    public RefundRequest(Order order, OrderItem orderItem, Item item, BuyerInfo buyerInfo, AddressInfo addressInfo, RefundInfo refundInfo) {
        this.orderItemId = orderItem.getId();
        this.itemName = item.getName();
        this.orderPrice = orderItem.getOrderPrice();
        this.orderDate =  order.getOrderDate();
        this.count = orderItem.getCount();
        this.imageUrl = item.getImageUrl();
        this.refundStatus = orderItem.getRefundStatus();

        this.buyerInfo = buyerInfo;
        this.addressInfo = addressInfo;
        this.refundInfo = refundInfo;
    }

    @Data
    public static class BuyerInfo {
        // 구매자 정보
        private String name;
        private String phoneNumber;
        private String email;

        public BuyerInfo(Buyer buyer) {
            this.name = buyer.getName();
            this.phoneNumber = buyer.getPhoneNumber();
            this.email = buyer.getEmail();
        }
    }

    @Data
    public static class AddressInfo {
        // 배송지 정보
        private String recipient;
        private String zoneCode;
        private String baseAddress;
        private String detailAddress;
        private String phone;
        private String subPhone;

        public AddressInfo(Address address) {
            this.recipient = address.getRecipient();
            this.zoneCode = address.getZoneCode();
            this.baseAddress = address.getBaseAddress();
            this.detailAddress = address.getDetailAddress();
            this.phone = address.getPhone();
            this.subPhone = address.getSubPhone();
        }
    }

    @Data
    public static class RefundInfo {
        // 환불 정보
        private String refundReason;
        private String refundDetail;
        private LocalDateTime requestedAt;

        public RefundInfo(com.shop.respawn.domain.RefundRequest refundRequest) {
            this.refundReason = refundRequest.getRefundReason();
            this.refundDetail = refundRequest.getRefundDetail();
            this.requestedAt = refundRequest.getRequestedAt();
        }
    }
}
