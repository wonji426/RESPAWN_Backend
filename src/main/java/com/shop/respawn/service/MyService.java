package com.shop.respawn.service;

import com.shop.respawn.domain.*;
import com.shop.respawn.dto.AddressDto;
import com.shop.respawn.dto.NoticeDto;
import com.shop.respawn.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MyService {

    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;
    private final AdminRepository adminRepository;
    private final AddressService addressService;
    private final OrderItemRepository orderItemRepository;
    private final CouponRepository couponRepository;
    private final BCryptPasswordEncoder encoder;
    private final LedgerPointService ledgerPointService;
    private final NoticeService noticeService;

    private final EntityManager em;

    public void initData() {

        Buyer buyer = Buyer.createBuyerWithInitLists("강지원", "kkjjww1122", encoder.encode("kjw741147"), "kkjjww1122@naver.com", "01024466832", Role.ROLE_USER, Grade.BASIC);
        buyer.renewExpiryDate();
        buyerRepository.save(buyer);
        em.persist(buyer);

        Buyer buyer1 = Buyer.createBuyerWithInitLists("test", "testUser", encoder.encode("testPassword"), "test@test.com", "01012345678", Role.ROLE_USER, Grade.BASIC);
        buyer1.renewExpiryDate();
        buyerRepository.save(buyer1);
        em.persist(buyer1);

        Seller seller1 = Seller.createSeller("강지원", "b", "Fanatec", 1231212345L, encoder.encode("b"), "jiwon426@naver.com", "01023456789", Role.ROLE_SELLER);
        seller1.renewExpiryDate();
        sellerRepository.save(seller1);
        em.persist(seller1);

        Seller seller2 = Seller.createSeller("유예슬", "c", "Creative", 1230946578L, encoder.encode("c"), "jiwon426@naver.com", "01098765432", Role.ROLE_SELLER);
        seller2.renewExpiryDate();
        sellerRepository.save(seller2);
        em.persist(seller2);

        Seller seller3 = Seller.createSeller("로지텍", "d", "Logitech", 9876543211L, encoder.encode("d"), "logitech9876@gmail.com", "01055430909", Role.ROLE_SELLER);
        seller3.renewExpiryDate();
        sellerRepository.save(seller3);
        em.persist(seller3);

        Admin admin = Admin.createAdmin("관리자", "admin", encoder.encode("adminPw"), Role.ROLE_ADMIN);
        adminRepository.save(admin);
        em.persist(admin);

        em.flush();
        em.clear();

        AddressDto addressDto1 = new AddressDto(1L, "기본주소", "강지원", "06234", "서울시 강남구 선릉로 123", "101동 1204호", "010-9876-5432", "063-533-6832", true);
        AddressDto addressDto2 = new AddressDto(2L, "너네집", "김철수", "06234", "서울시 강남구 선릉로 123", "101동 1204호", "010-9876-5432", "063-533-6832", false);

        addressService.createAddress(1L, addressDto1);
        addressService.createAddress(1L, addressDto2);

        Coupon coupon1 = Coupon.createCoupon(buyer, "신규 가입 축하 쿠폰(10,000원)", 10_000L, LocalDateTime.now().plusDays(30));
        Coupon coupon2 = Coupon.createCoupon(buyer, "신규 가입 축하 쿠폰(5,000원)", 5_000L, LocalDateTime.now().plusDays(30));
        couponRepository.save(coupon1);
        couponRepository.save(coupon2);
        em.persist(coupon1);
        em.persist(coupon2);

        ledgerPointService.savePoints(buyer.getId(),
                20000L,
                LocalDateTime.now().plusYears(1),
                1L,
                "결제 포인트 적립",
                "system");

        // 주문 데이터 생성 예시
        Item item1 = new Item();
        item1.setId("68822f86e8223dd3d36c5db5");
        Item item2 = new Item();
        item2.setId("688f1ec09458c96f5cecfffa");
        Item item3 = new Item();
        item3.setId("6882343ae8223dd3d36c5dbe");

        // 날짜 예시 (오늘부터 6일 전까지)
        LocalDateTime baseDate = LocalDateTime.now();

        Address defaultAddress = em.find(Address.class, 1L);

        for (int i = 0; i < 6; i++) {
            LocalDateTime orderDate = baseDate.minusDays(i);

            Order order = new Order();
            order.setBuyer(buyer);
            order.setOrderDate(orderDate);
            order.setStatus(OrderStatus.PAID);

            Delivery delivery = new Delivery();
            delivery.setAddress(defaultAddress);
            delivery.setStatus(DeliveryStatus.DELIVERED);

            if (i % 3 == 0) {
                OrderItem oi2 = OrderItem.createOrderItem(item2, 20L, 2L);
                order.addOrderItem(oi2);
                delivery.setOrderItem(oi2);
                oi2.setDelivery(delivery);
            } else {
                OrderItem oi3 = OrderItem.createOrderItem(item3, 50L, 1L);
                order.addOrderItem(oi3);
                delivery.setOrderItem(oi3);
                oi3.setDelivery(delivery);
            }

            Long totalAmount = order.calculateTotalAmount();
            order.setTotalAmount(totalAmount);

            order.setOrderName(order.generateOrderName());
            order.setPgOrderId("ORDER_" + System.currentTimeMillis() + "_" + i);
            order.setPaymentStatus("SUCCESS");

            em.persist(delivery);
            em.persist(order);

            OrderItem findOrderItem = orderItemRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("OrderItem이 존재하지 않습니다."));

            findOrderItem.getDelivery().setStatus(DeliveryStatus.DELIVERED);
        }
        NoticeDto noticeDto = new NoticeDto("소셜로그인에 관한 공지사항(1)","소셜로그인은 전화번호 추가가 필요합니다.",NoticeType.ACCOUNT);
        noticeService.CreateNotice(admin.getId(), noticeDto);
        NoticeDto noticeDto1 = new NoticeDto("정기점검에 관한 공지사항","정기점검을 실시할 예정입니다..",NoticeType.OPERATIONS);
        noticeService.CreateNotice(admin.getId(), noticeDto1);
        NoticeDto noticeDto2 = new NoticeDto("주문에 관한 공지사항","주문 후 주문완료 페이지를 확인해주세요.",NoticeType.ORDER);
        noticeService.CreateNotice(admin.getId(), noticeDto2);
        NoticeDto noticeDto3 = new NoticeDto("배송에 관한 공지사항","배송이 지연될 수 있습니다.",NoticeType.SHIPPING);
        noticeService.CreateNotice(admin.getId(), noticeDto3);
        NoticeDto noticeDto4 = new NoticeDto("소셜로그인에 관한 공지사항(2)","소셜로그인은 이름 추가가 필요합니다.",NoticeType.ACCOUNT);
        noticeService.CreateNotice(admin.getId(), noticeDto4);
        NoticeDto noticeDto5 = new NoticeDto("소셜로그인에 관한 공지사항(3)","소셜로그인은 정보 추가가 필요할 수 있습니다.",NoticeType.ACCOUNT);
        noticeService.CreateNotice(admin.getId(), noticeDto5);
    }

}
