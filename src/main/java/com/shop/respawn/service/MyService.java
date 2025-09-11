package com.shop.respawn.service;

import com.shop.respawn.domain.*;
import com.shop.respawn.dto.AddressDto;
import com.shop.respawn.dto.NoticeDto;
import com.shop.respawn.dto.Payment.PaymentDto;
import com.shop.respawn.dto.coupon.CouponUsageStatusDto;
import com.shop.respawn.dto.order.OrderRequestDto;
import com.shop.respawn.dto.user.UserDto;
import com.shop.respawn.repository.jpa.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MyService {

    private final AdminRepository adminRepository;
    private final OrderRepository orderRepository;

    private final NoticeService noticeService;
    private final UserService userService;
    private final OrderService orderService;
    private final AddressService addressService;
    private final PaymentService paymentService;
    private final CartService cartService;
    private final CouponService couponService;

    private final BCryptPasswordEncoder encoder;
    private final EntityManager em;

    public void initData() {

        // 사용자 세팅
        initUser();

        // 사용자 주소 세팅
        initAddress();

        // 주문 세팅
        initOrder();

        // 공지사항 세팅
        initNotice();
    }

    // 사용자 세팅
    private void initUser() {
        UserDto buyerUserDto1 = new UserDto("buyer", "강지원", "kkjjww1122", "kjw741147", "kkjjww1122@naver.com", "01024466832", Role.ROLE_USER, Grade.BASIC);
        userService.join(buyerUserDto1);

        UserDto buyerUserDto2 = new UserDto("buyer", "test", "testUser", "testPassword", "test@test.com", "01012345678", Role.ROLE_USER, Grade.BASIC);
        userService.join(buyerUserDto2);

        UserDto sellerUserDto1 = new UserDto("seller", "강지원", "a", "Fanatec", 1231212345L, "a", "jiwon426@naver.com", "01023456789", Role.ROLE_SELLER);
        userService.join(sellerUserDto1);

        UserDto sellerUserDto2 = new UserDto("seller", "유예슬", "b", "Creative", 1230946578L, "b", "yeseul308@naver.com", "01098765432", Role.ROLE_SELLER);
        userService.join(sellerUserDto2);

        UserDto sellerUserDto3 = new UserDto("seller", "로지텍", "c", "Logitech", 9876543211L, "c", "logitech9876@gmail.com", "01055430909", Role.ROLE_SELLER);
        userService.join(sellerUserDto3);

        Admin admin = Admin.createAdmin("관리자", "admin", encoder.encode("adminPw"), Role.ROLE_ADMIN);
        adminRepository.save(admin);
        em.persist(admin);

        em.flush();
        em.clear();
    }

    // 주소 세팅
    private void initAddress() {
        AddressDto addressDto1 = new AddressDto(1L, "기본주소", "강지원", "06234", "서울시 강남구 선릉로 123", "101동 1204호", "010-9876-5432", "063-533-6832", true);
        AddressDto addressDto2 = new AddressDto(2L, "너네집", "김철수", "06234", "서울시 강남구 선릉로 123", "101동 1204호", "010-9876-5432", "063-533-6832", false);

        addressService.createAddress(1L, addressDto1);
        addressService.createAddress(1L, addressDto2);
    }

    // 주문 세팅
    private void initOrder() {

        Long orderId;
        Long buyerId = 1L;

        //== 상품페이지에서 바로 구매 5건 시작 ==//

        // 상품 세팅
        List<String> items1 = new ArrayList<>();
        items1.add("68ad9f8fe64e3ad0932e8526");
        items1.add("688f73f0d28a62e2908e88a2");
        items1.add("6882312ce8223dd3d36c5db8");
        items1.add("68823033e8223dd3d36c5db6");
        items1.add("68823189e8223dd3d36c5db9");

        orderId = 1L;
        for (String item : items1) {
            long count = (long) (Math.random() * 2 + 1);
            long address = (long) (Math.random() * 2 + 1);

            // 상품 구매 시작
            orderService.createTemporaryOrder(buyerId, item, count);

            // OrderRequestDto에 주소 세팅
            OrderRequestDto orderRequestDto1 = new OrderRequestDto();
            orderRequestDto1.setAddressId(address);

            // 상품 구매 완료하기
            orderService.completeSelectedOrder(buyerId, orderId, orderRequestDto1);

            // 페이먼트 정보 저장
            pay(orderId, 0L);

            orderId++;
        }

        //== 상품페이지에서 바로 구매 5건 종료 ==//

        //== 장바구니 상품 구매 1건 시작==//

        orderId = 6L;

        // 상품 세팅
        List<String> items2 = new ArrayList<>();
        items2.add("688f1ec09458c96f5cecfffa");
        items2.add("68ad9ff9e64e3ad0932e8530");

        // 장바구니에 상품 등록
        for (String item : items2) {
            cartService.addItemToCart(buyerId, item, 1L);
        }

        // 장바구니에서 구매할 상품과 주소 OrderRequestDto에 세팅
        OrderRequestDto orderRequestDto2 = new OrderRequestDto();
        List<Long> cartItemIds = new ArrayList<>();
        cartItemIds.add(1L);
        cartItemIds.add(2L);
        orderRequestDto2.setCartItemIds(cartItemIds);
        orderRequestDto2.setAddressId(1L);

        // 장바구니에 있는 상품 OrderItem에 옮기고 주문페이지 세팅
        orderService.prepareOrderSelectedFromCart(buyerId, orderRequestDto2);

        // 쿠폰 조회
        List<CouponUsageStatusDto> coupon = couponService.getCouponsUsageStatusByBuyerAndOrder(buyerId, orderId);
        String couponCode = coupon.getFirst().getCoupon().getCode();

        // 쿠폰 사용 후 orderRequestDto에 쿠폰 코드 세팅
        couponService.checkApplicableForOrder(buyerId, orderId, couponCode);
        orderRequestDto2.setCouponCode(couponCode);

        // 포인트 사용
        Long usePointAmount = 15000L;
        orderService.applyPoints(buyerId, orderId, usePointAmount);

        // 상품 구매 완료하기
        orderService.completeSelectedOrder(buyerId, orderId, orderRequestDto2);

        // 페이먼트 정보 저장
        pay(orderId, usePointAmount);
    }

    // 공지사항 저장
    private void initNotice() {
        NoticeDto noticeDto = new NoticeDto("소셜로그인에 관한 공지사항(1)","소셜로그인은 전화번호 추가가 필요합니다.",NoticeType.ACCOUNT);
        noticeService.CreateNotice(1L, noticeDto);
        NoticeDto noticeDto1 = new NoticeDto("정기점검에 관한 공지사항","정기점검을 실시할 예정입니다..",NoticeType.OPERATIONS);
        noticeService.CreateNotice(1L, noticeDto1);
        NoticeDto noticeDto2 = new NoticeDto("주문에 관한 공지사항","주문 후 주문완료 페이지를 확인해주세요.",NoticeType.ORDER);
        noticeService.CreateNotice(1L, noticeDto2);
        NoticeDto noticeDto3 = new NoticeDto("배송에 관한 공지사항","배송이 지연될 수 있습니다.",NoticeType.SHIPPING);
        noticeService.CreateNotice(1L, noticeDto3);
        NoticeDto noticeDto4 = new NoticeDto("소셜로그인에 관한 공지사항(2)","소셜로그인은 이름 추가가 필요합니다.",NoticeType.ACCOUNT);
        noticeService.CreateNotice(1L, noticeDto4);
        NoticeDto noticeDto5 = new NoticeDto("소셜로그인에 관한 공지사항(3)","소셜로그인은 정보 추가가 필요할 수 있습니다.",NoticeType.ACCOUNT);
        noticeService.CreateNotice(1L, noticeDto5);
    }

    // Payment에 정보 저장 (간단화한 버전)
    public void pay(Long orderId, Long usePointAmount) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));

        Long amount = order.getTotalAmount();
        String name = order.getOrderName();
        String status = "paid";
        String paymentMethod = "card";
        String pgProvider = getProvider();
        String cardName = getCard();

        PaymentDto paymentDto = PaymentDto.builder()
                .impUid("imp_" + (long) (Math.random() * 100000000000L))
                .amount(amount)
                .status(status)
                .name(name)
                .buyerId(order.getBuyer().getId())  // buyerId 추가
                .orderId(orderId)  // orderId 추가
                .paymentMethod(paymentMethod)
                .usePointAmount(usePointAmount)
                .pgProvider(pgProvider)
                .cardName(cardName)
                .build();

        // 결제 성공 로직
        paymentService.savePayment(paymentDto);
    }

    // 4개의 Card 중 랜덤 선택
    private static String getCard() {
        String paymentMethod;
        int temp = (int) (Math.random() * 4);
        if(temp == 0) paymentMethod = "신한카드";
        else if(temp == 1) paymentMethod = "국민카드";
        else if(temp == 2) paymentMethod = "우리카드";
        else paymentMethod = "현대카드";
        return paymentMethod;
    }

    // 4개의 Provider 중 랜덤 선택
    private static String getProvider() {
        String provider;
        int temp = (int) (Math.random() * 4);
        if(temp == 0) provider = "kakaopay";
        else if(temp == 1) provider = "tosspay";
        else if(temp == 2) provider = "smilepay";
        else provider = "html5_inicis";
        return provider;
    }

}
