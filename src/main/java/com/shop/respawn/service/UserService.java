package com.shop.respawn.service;

import com.shop.respawn.domain.*;
import com.shop.respawn.dto.findInfo.FindInfoRequest;
import com.shop.respawn.dto.findInfo.FindInfoResponse;
import com.shop.respawn.dto.findInfo.ResetPasswordRequest;
import com.shop.respawn.dto.query.UserQueryDto;
import com.shop.respawn.dto.user.*;
import com.shop.respawn.email.EmailService;
import com.shop.respawn.repository.jpa.AdminRepository;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.SellerRepository;
import com.shop.respawn.repository.UserRepositoryCustom;
import com.shop.respawn.security.auth.PrincipalDetails;
import com.shop.respawn.sms.SmsService;
import com.shop.respawn.util.RedisUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static com.shop.respawn.util.MaskingUtil.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;
    private final AdminRepository adminRepository;

    private final EmailService emailService;
    private final SmsService smsService;
    private final LedgerPointService ledgerPointService;
    private final CouponService couponService;

    private final BCryptPasswordEncoder encoder;
    private final RedisUtil redisUtil;
    enum Channel { EMAIL, PHONE }

    /**
     * 회원가입
     */
    public void join(UserDto userDto){
        String userType = userDto.getUserType();
        String name = userDto.getName();
        String username = userDto.getUsername();
        String company = userDto.getCompany();
        Long companyNumber = userDto.getCompanyNumber();
        String password = encoder.encode(userDto.getPassword());
        String email = userDto.getEmail();
        String phoneNumber = userDto.getPhoneNumber();

        if(userType.equals("buyer")){
            Buyer buyer = Buyer.createBuyer(name, username, password, email, phoneNumber, "local", Role.ROLE_USER, Grade.BASIC);
            buyer.renewExpiryDate();
            buyerRepository.save(buyer);
            couponService.newBuyerCoupon(buyer.getId());
        } else if (userType.equals("seller")){
            Seller seller = Seller.createSeller(name, username, company, companyNumber, password, email, phoneNumber, Role.ROLE_SELLER);
            seller.renewExpiryDate();
            sellerRepository.save(seller);
        }

    }

    /**
     * loginOK 시 로그인유저의 데이터
     */
    public LoginOkResponse getUserData(String username, String authorities) {

        LoginOkResponse userData = getSimpleUserData(username, authorities);

        boolean due = isPasswordChangeDue(username);
        boolean snoozed = isSnoozed(username);
        return new LoginOkResponse(userData.getName(), username, authorities, userData.getRole(), due, snoozed, userData.getUserId());
    }

    public LoginOkResponse getBringMe(String username, String authorities) {

        LoginOkResponse userData = getSimpleUserData(username, authorities);
        return new LoginOkResponse(userData.getName(), username, authorities, userData.getRole(), userData.getUserId());
    }

    @NotNull
    private LoginOkResponse getSimpleUserData(String username, String authorities) {
        String name = null;
        Long userId = null;
        Role role = null;

        switch (authorities) {
            case "[ROLE_USER]" -> {
                UserQueryDto buyer = buyerRepository.findUserDtoByUsername(username);
                if (buyer != null) {
                    name = buyer.getName();
                    userId = buyer.getId();
                    role = buyer.getRole();
                }
            }
            case "[ROLE_SELLER]" -> {
                UserQueryDto seller = sellerRepository.findUserDtoByUsername(username);
                if (seller != null) {
                    name = seller.getName();
                    userId = seller.getId();
                    role = seller.getRole();
                }
            }
            case "[ROLE_ADMIN]" -> {
                UserQueryDto admin = adminRepository.findUserDtoByUsername(username);
                if (admin != null) {
                    name = admin.getName();
                    userId = admin.getId();
                    role = admin.getRole();
                }
            }
        }
        return new LoginOkResponse(name, role, userId);
    }

    public LoginOkResponse bringMe(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("인증 필요");
        }
        String username = authentication.getName();
        String authorities = authentication.getAuthorities() != null
                ? authentication.getAuthorities().toString()
                : "[]";
        LoginOkResponse data = getBringMe(username, authorities);
        if (data == null) {
            throw new RuntimeException("세션 정보가 유효하지 않습니다.");
        }
        return data;
    }

    public UserDto getUserInfo(String username, String authorities) {
        // User 먼저 조회
        switch (authorities) {
            case "[ROLE_USER]" -> {
                Buyer buyer = getBuyerInfo(username);
                if (buyer != null) {
                    buyer.renewExpiryDate();
                    buyerRepository.save(buyer);
                    return new UserDto(buyer.getName(), buyer.getUsername(), buyer.getEmail(), buyer.getPhoneNumber(), buyer.getProvider(), buyer.getRole(), buyer.getGrade());
                }
            }
            case "[ROLE_SELLER]" -> {
                Seller seller = getSellerInfo(username);
                if (seller != null) {
                    seller.renewExpiryDate();
                    sellerRepository.save(seller);
                    return new UserDto(seller.getName(), seller.getUsername(), seller.getEmail(), seller.getPhoneNumber(), seller.getRole()
                    );
                }
            }
        }
        throw new UsernameNotFoundException("구매자/판매자 정보가 없습니다");
    }

    /**
     * 비밀번호 확인
     */
    public boolean isMatchPassword(String username, String authorities, String inputPassword) {

        String encodedPassword = null;
        switch (authorities) {
            case "[ROLE_USER]" -> {
                Buyer buyer = buyerRepository.findByUsername(username);
                encodedPassword = buyer.getPassword();
            }
            case "[ROLE_SELLER]" -> {
                Seller seller = sellerRepository.findByUsername(username);
                encodedPassword = seller.getPassword();
            }
        }
        return encoder.matches(inputPassword, encodedPassword);
    }

    public void updatePhoneNumber(String username, String newPhoneNumber) {
        Buyer buyer = buyerRepository.findOptionalByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        buyer.updatePhoneNumber(newPhoneNumber);
        // userRepository.save(user); // 트랜잭션 내 변경감지로 자동 업데이트 됩니다.
    }

    public boolean changePassword(String username, String authorities, String currentPassword, String newPassword) {
        switch (authorities) {
            case "[ROLE_USER]" -> {
                Buyer buyer = buyerRepository.findByUsername(username);
                if (!encoder.matches(currentPassword, buyer.getPassword())) {
                    return false; // 현재 비밀번호 불일치
                }
                buyer.updatePassword(encoder.encode(newPassword));
                // 변경감지(트랜잭션 내)로 save 호출 불필요
                if (buyer.getAccountStatus() != null) {
                    buyer.getAccountStatus().markPasswordChangedNow();
                }
                return true;
            }
            case "[ROLE_SELLER]" -> {
                Seller seller = sellerRepository.findByUsername(username);
                if (!encoder.matches(currentPassword, seller.getPassword())) {
                    return false;
                }
                seller.updatePassword(encoder.encode(newPassword));
                if (seller.getAccountStatus() != null) {
                    seller.getAccountStatus().markPasswordChangedNow();
                }
                return true;
            }
        }
        throw new RuntimeException("사용자를 찾을 수 없습니다.");
    }

    public void resetPasswordByToken(String username, String newPassword) {
        Buyer buyer = buyerRepository.findByUsername(username);
        if (buyer != null) {
            buyer.updatePassword(encoder.encode(newPassword));
            if (buyer.getAccountStatus() != null) {
                buyer.getAccountStatus().markPasswordChangedNow();
            }
            return;
        }
        Seller seller = sellerRepository.findByUsername(username);
        if (seller != null) {
            seller.updatePassword(encoder.encode(newPassword));
            if (seller.getAccountStatus() != null) {
                seller.getAccountStatus().markPasswordChangedNow();
            }
            return;
        }
        throw new RuntimeException("사용자를 찾을 수 없습니다.");
    }

    public boolean isPasswordChangeDue(String username) {
        final int months = 3;
        LocalDateTime now = LocalDateTime.now();

        // 1) Buyer에서 단일 컬럼 조회
        Optional<LocalDateTime> buyerChangedAtOpt =
                buyerRepository.findLastPasswordChangedAtByUsername(username);

        if (buyerChangedAtOpt.isPresent()) {
            LocalDateTime due = buyerChangedAtOpt.get().plusMonths(months);
            return !due.isAfter(now);
        }

        // 2) Seller에서 단일 컬럼 조회
        Optional<LocalDateTime> sellerChangedAtOpt =
                sellerRepository.findLastPasswordChangedAtByUsername(username);

        if (sellerChangedAtOpt.isPresent()) {
            LocalDateTime due = sellerChangedAtOpt.get().plusMonths(months);
            return !due.isAfter(now);
        }
        return false;
    }

    private String snoozeKey(String username) {
        return "pwd-reminder-snooze:" + username;
    }

    // 7일간 리마인드 억제
    public void snoozePasswordReminder(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof PrincipalDetails principal)) {
            throw new RuntimeException("인증 필요");
        }
        long snoozeSeconds = 604_800L;
        redisUtil.setDataExpire(snoozeKey(principal.getUsername()), "1", snoozeSeconds);
    }

    public boolean isSnoozed(String username) {
        String v = redisUtil.getData(snoozeKey(username));
        return v != null;
    }

    public Buyer getBuyerInfo(String username){
        return buyerRepository.findByUsername(username);
    }

    public Seller getSellerInfo(String username){
        return sellerRepository.findByUsername(username);
    }

    /**
     * 이메일로 실제 username 찾기
     */
    public String getRealUsernameByNameAndEmail(String userType, String name, String email) {
        return findUsernameByNameAndEmail(userType, name, email);
    }

    private String findUsernameByNameAndEmail(String userType, String name, String email) {
        if (userType.equals("buyer")) {
            Buyer buyer = buyerRepository.findByNameAndEmail(name, email);
            if (buyer != null) return buyer.getUsername();
        } else if (userType.equals("seller")) {
            Seller seller = sellerRepository.findByNameAndEmail(name, email);
            if (seller != null) return seller.getUsername();
        }
        throw new RuntimeException("해당 이름과 이메일로 가입된 사용자가 없습니다.");
    }

    /**
     * 전화번호로 실제 username 찾기
     */
    public String getRealUsernameByNameAndPhone(String userType, String name, String phone) {
        return findUsernameByNameAndPhone(userType, name, phone);
    }

    private String findUsernameByNameAndPhone(String userType, String name, String phone) {
        if (userType.equals("buyer")) {
            Buyer buyer = buyerRepository.findByNameAndPhoneNumber(name, phone);
            if (buyer != null) return buyer.getUsername();
        } else if (userType.equals("seller")) {
            Seller seller = sellerRepository.findByNameAndPhoneNumber(name, phone);
            if (seller != null) return seller.getUsername();
        }
        throw new RuntimeException("해당 이름과 전화번호로 가입된 사용자가 없습니다.");
    }

    /**
     * 2단계 - 이메일로 실제 아이디 전송
     */
    public void sendRealUsernameByEmail(String userType, String name, String email) {
        String realUsername = findUsernameByNameAndEmail(userType, name, email);
        String message = "회원님의 아이디는 [" + realUsername + "] 입니다.";
        emailService.sendEmailUsernameAsync(email, message);
    }

    /**
     * 2단계 - 휴대폰으로 실제 아이디 전송
     */
    public void sendRealUsernameByPhone(String userType, String name, String phoneNumber) {
        String realUsername = findUsernameByNameAndPhone(userType, name, phoneNumber);
        String username = "회원님의 아이디는 [" + realUsername + "] 입니다.";
        smsService.sendUsernameMessage(phoneNumber, username);
    }

    public String findPhoneNumberByNameAndEmail(String name, String email) {
        Buyer buyer = buyerRepository.findByNameAndEmail(name, email);
        if (buyer != null) {
            return buyer.getPhoneNumber();
        }
        Seller seller = sellerRepository.findByNameAndEmail(name, email);
        if (seller != null) {
            return seller.getPhoneNumber();
        }
        throw new RuntimeException("해당 이름과 이메일로 가입된 사용자가 없습니다.");
    }

    public String findEmailByNameAndPhone(String name, String phoneNumber) {
        Buyer buyer = buyerRepository.findByNameAndPhoneNumber(name, phoneNumber);
        if (buyer != null) {
            return buyer.getEmail();
        }

        Seller seller = sellerRepository.findByNameAndPhoneNumber(name, phoneNumber);
        if (seller != null) {
            return seller.getEmail();
        }

        throw new RuntimeException("해당 이름과 전화번호로 가입된 사용자가 없습니다.");
    }

    public boolean sendPasswordResetLinkByEmail(String username, String name, String email) {
        Buyer buyer = buyerRepository.findByUsernameAndNameAndEmail(username, name, email);
        Seller seller = sellerRepository.findByUsernameAndNameAndEmail(username, name, email);

        if (buyer == null && seller == null) {
            return false; // 일치하는 유저 없음
        }

        // 임시 토큰 생성 (10분 만료)
        String token = UUID.randomUUID().toString();
        String resetLink = "http://localhost:3000/reset-password?token=" + token;

        // Redis나 DB에 토큰 저장
        redisUtil.setDataExpire("reset-token:" + token, username, 10 * 60L);

        // 이메일 발송
        emailService.sendPasswordResetLink(email, resetLink);
        return true;
    }

    public boolean sendPasswordResetLinkByPhone(String username, String name, String phoneNumber) {
        Buyer buyer = buyerRepository.findByUsernameAndNameAndPhoneNumber(username, name, phoneNumber);
        Seller seller = sellerRepository.findByUsernameAndNameAndPhoneNumber(username, name, phoneNumber);

        if (buyer == null && seller == null) {
            return false;
        }

        String token = UUID.randomUUID().toString();
        String resetLink = "http://localhost:3000/reset-password?token=" + token;

        redisUtil.setDataExpire("reset-token:" + token, username, 30 * 60L);

        smsService.sendPasswordResetLink(phoneNumber, resetLink);
        return true;
    }

    public void storeUsernameToken(String token, String username) {
        // 예: 10분(600초) 만료시간 설정
        redisUtil.setDataExpire("find-id-token:" + token, username, 600L);
    }

    public String getUsernameByToken(String token) {
        return redisUtil.getData("find-id-token:" + token);
    }

    public void deleteUsernameToken(String token) {
        redisUtil.deleteData("find-id-token:" + token);
    }

    public boolean verifyUsernameNameEmail(String username, String name, String email) {
        // 이름과 이메일이 실제 유저 정보와 일치하는지 검증
        Buyer buyer = buyerRepository.findByUsername(username);
        if (buyer != null) {
            return buyer.getName().equals(name) && buyer.getEmail().equals(email);
        }
        Seller seller = sellerRepository.findByUsername(username);
        if (seller != null) {
            return seller.getName().equals(name) && seller.getEmail().equals(email);
        }
        return false;
    }

    public boolean verifyUsernameNamePhone(String username, String name, String phoneNumber) {
        Buyer buyer = buyerRepository.findByUsername(username);
        if (buyer != null) {
            return buyer.getName().equals(name) && buyer.getPhoneNumber().equals(phoneNumber);
        }
        Seller seller = sellerRepository.findByUsername(username);
        if (seller != null) {
            return seller.getName().equals(name) && seller.getPhoneNumber().equals(phoneNumber);
        }
        return false;
    }

    public Long getUserIdByUsername(String username) {
        Buyer buyer = buyerRepository.findByUsername(username);
        if (buyer != null) {
            return buyer.getId();
        }
        Seller seller = sellerRepository.findByUsername(username);
        if (seller != null) {
            return seller.getId();
        }
        throw new RuntimeException("해당 아이디의 사용자를 찾을 수 없습니다.");
    }

    /**
     * 아이디 중복확인
     */
    public boolean checkUsernameDuplicate(String username) {
        return buyerRepository.existsByUsername(username) || sellerRepository.existsByUsername(username);
    }

    /**
     * 전화번호 중복확인
     */
    public boolean checkPhoneNumberDuplicate(String phoneNumber) {
        return buyerRepository.existsByPhoneNumber(phoneNumber) || sellerRepository.existsByPhoneNumber(phoneNumber);
    }

    /**
     * 이메일 중복확인
     */
    public boolean checkEmailDuplicate(String email) {
        return buyerRepository.existsByEmail(email) || sellerRepository.existsByEmail(email);
    }

    public FindInfoResponse findId(FindInfoRequest findInfoRequest) {
        String userType = findInfoRequest.getUserType();
        String name = findInfoRequest.getName();
        String email = findInfoRequest.getEmail();
        String phoneNumber = findInfoRequest.getPhoneNumber();

        String realUsername;
        String maskedUsername;
        String maskedEmail;
        String maskedPhone;
        Long userId;

        try {
            if (phoneNumber == null && email != null) {
                realUsername = getRealUsernameByNameAndEmail(userType, name, email); // 실제 아이디 조회 메서드 새로 만듦
                maskedUsername = maskMiddleFourChars(realUsername);
                String findPhoneNumber = findPhoneNumberByNameAndEmail(name, email);
                if (findPhoneNumber == null) throw new RuntimeException();

                maskedEmail = maskEmail(email);
                maskedPhone = maskPhoneNumber(findPhoneNumber);
                userId = getUserIdByUsername(realUsername);
            } else if (email == null && phoneNumber != null) {
                realUsername = getRealUsernameByNameAndPhone(userType, name, phoneNumber);
                maskedUsername = maskMiddleFourChars(realUsername);
                String findEmail = findEmailByNameAndPhone(name, phoneNumber);
                if (findEmail == null) throw new RuntimeException();

                maskedPhone = maskPhoneNumber(phoneNumber);
                maskedEmail = maskEmail(findEmail);
                userId = getUserIdByUsername(realUsername);
            } else {
                throw new IllegalArgumentException("이메일 또는 전화번호 중 하나만 입력하세요.");
            }
        } catch (Exception e) {
            throw new NoSuchElementException("일치하는 계정을 찾을 수 없습니다.");
        }

        // 임시 토큰 생성 및 레디스 저장
        String token = UUID.randomUUID().toString();
        storeUsernameToken(token, realUsername); // userService에서 Redis에 저장하는 메서드 호출

        return new FindInfoResponse(maskedUsername, maskedEmail, maskedPhone, token, userId);
    }

    public String processSendId(FindInfoRequest findInfoRequest) {
        String userType = findInfoRequest.getUserType();
        String token = findInfoRequest.getToken();
        Long userId = findInfoRequest.getUserId();
        String type = findInfoRequest.getType();

        final Channel channel;
        try {
            channel = Channel.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("type은 'email' 또는 'phone' 이어야 합니다.");
        }

        // 1. 필수값 검증
        if (token == null || userId == null) {
            throw new IllegalArgumentException("필수 정보를 입력하세요.");
        }

        // 2. Redis 토큰으로 username 조회
        String realUsername = getUsernameByToken(token);
        if (realUsername == null) {
            throw new SecurityException("유효하지 않거나 만료된 토큰입니다.");
        }

        // 3. DB에서 Buyer 또는 Seller 조회
        String name = null, email = null, phoneNumber = null;

        if (userType.equals("buyer")) {
            Buyer buyer = buyerRepository.findById(userId).orElse(null);
            if (buyer != null) {
                name = buyer.getName();
                email = buyer.getEmail();
                phoneNumber = buyer.getPhoneNumber();
            }
        } else if (userType.equals("seller")) {
            Seller seller = sellerRepository.findById(userId).orElse(null);
            if (seller != null) {
                name = seller.getName();
                email = seller.getEmail();
                phoneNumber = seller.getPhoneNumber();
            }
        } else {
            throw new EntityNotFoundException("해당 회원을 찾을 수 없습니다.");
        }

        // 4. 이메일 or 휴대폰으로 아이디 전송
        switch (channel) {
            case EMAIL:
                if (!verifyUsernameNameEmail(realUsername, name, email))
                    throw new SecurityException("입력 정보와 토큰이 일치하지 않습니다.");
                sendRealUsernameByEmail(userType, name, email);
                break;
            case PHONE:
                if (!verifyUsernameNamePhone(realUsername, name, phoneNumber))
                    throw new SecurityException("입력 정보와 토큰이 일치하지 않습니다.");
                sendRealUsernameByPhone(userType, name, phoneNumber);
                break;
        }

        // 5. 사용 후 토큰 삭제
        deleteUsernameToken(token);

        return "아이디가 " + (channel == Channel.EMAIL ? "이메일" : "휴대폰") + "으로 전송되었습니다.";
    }

    public FindInfoResponse findPasswordAccount(FindInfoRequest findInfoRequest) {
        String username = findInfoRequest.getUsername();
        String name = findInfoRequest.getName();
        String email = findInfoRequest.getEmail();
        String phoneNumber = findInfoRequest.getPhoneNumber();

        if (phoneNumber == null && email != null) {
            // 이메일 정보로 조회
            String findPhoneNumber = findPhoneNumberByNameAndEmail(name, email);
            if (findPhoneNumber == null) {
                throw new RuntimeException("일치 계정 없음");
            }
            return new FindInfoResponse(
                    maskEmail(email),
                    maskPhoneNumber(findPhoneNumber),
                    getUserIdByUsername(username)
            );
        } else if (email == null && phoneNumber != null) {
            // 전화번호 정보로 조회
            String findEmail = findEmailByNameAndPhone(name, phoneNumber);
            if (findEmail == null) {
                throw new RuntimeException("일치 계정 없음");
            }
            return new FindInfoResponse(
                    maskEmail(findEmail),
                    maskPhoneNumber(phoneNumber),
                    getUserIdByUsername(username)
            );
        } else {
            throw new IllegalArgumentException("이메일 또는 전화번호 중 하나만 입력해야 합니다.");
        }
    }

    public String sendPasswordReset(FindInfoRequest findInfoRequest) {
        String userType = findInfoRequest.getUserType();
        Long userId = findInfoRequest.getUserId();
        String type = findInfoRequest.getType();

        if (userId == null || type == null || userType == null) {
            throw new IllegalArgumentException("필수 정보를 입력하세요.");
        }

        // DB에서 회원 조회
        String username = null;
        String name = null;
        String email = null;
        String phoneNumber = null;

        if (userType.equals("buyer")) {
            Buyer buyer = buyerRepository.findById(userId).orElse(null);
            if (buyer != null) {
                username = buyer.getUsername();
                name = buyer.getName();
                email = buyer.getEmail();
                phoneNumber = buyer.getPhoneNumber();
            }
        } else if (userType.equals("seller")) {
            Seller seller = sellerRepository.findById(userId).orElse(null);
            if (seller != null) {
                username = seller.getUsername();
                name = seller.getName();
                email = seller.getEmail();
                phoneNumber = seller.getPhoneNumber();
            }
        } else {
            throw new RuntimeException("해당 회원을 찾을 수 없습니다.");
        }

        // type에 따라 발송 처리
        if ("email".equalsIgnoreCase(type)) {
            boolean result = sendPasswordResetLinkByEmail(username, name, email);
            if (result) {
                return "비밀번호 재설정 링크가 이메일로 전송되었습니다.";
            }
        } else if ("phone".equalsIgnoreCase(type)) {
            boolean result = sendPasswordResetLinkByPhone(username, name, phoneNumber);
            if (result) {
                return "비밀번호 재설정 링크가 휴대폰으로 전송되었습니다.";
            }
        } else {
            throw new IllegalArgumentException("type은 'email' 또는 'phone' 이어야 합니다.");
        }

        return "비밀번호 재설정 요청이 처리되었습니다."; // 기본 응답 (실패 케이스 대비)
    }

    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        String token = resetPasswordRequest.getToken();
        String newPassword = resetPasswordRequest.getNewPassword();

        if (token == null || newPassword == null) {
            throw new IllegalArgumentException("토큰과 새 비밀번호를 모두 입력해야 합니다.");
        }

        // Redis에서 토큰 확인
        String username = redisUtil.getData("reset-token:" + token);
        if (username == null) {
            throw new RuntimeException("토큰이 유효하지 않거나 만료되었습니다.");
        }

        // 비밀번호 강제 변경 (기존 비밀번호 검증 없음)
        resetPasswordByToken(username, newPassword);

        // 사용 완료된 토큰 삭제
        redisUtil.deleteData("reset-token:" + token);
    }

    public MeResponse getMyStatus(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof PrincipalDetails principal)) {
            return new MeResponse(false, null, null);
        }
        String username = principal.getUsername();
        boolean due = isPasswordChangeDue(username);
        boolean snoozed = isSnoozed(username);

        return new MeResponse(true, due, snoozed);
    }

    @Transactional
    public void updateProfile(Authentication authentication, ProfileUpdateRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof PrincipalDetails principal)) {
            throw new RuntimeException("인증 필요");
        }
        String authorities = authentication.getAuthorities() != null
                ? authentication.getAuthorities().toString()
                : "[]";
        String username = principal.getUsername();

        switch (authorities) {
            case "[ROLE_USER]" -> {
                Buyer buyer = buyerRepository.findByUsername(username);
                if (buyer == null) throw new RuntimeException("구매자 정보가 없습니다.");
                updateUserInfo(buyer, request, buyerRepository, sellerRepository);
            }

            case "[ROLE_SELLER]" -> {
                Seller seller = sellerRepository.findByUsername(username);
                if (seller == null) throw new RuntimeException("판매자 정보가 없습니다.");
                updateUserInfo(seller, request, sellerRepository, buyerRepository);
            }

            default -> throw new RuntimeException("지원하지 않는 역할입니다.");
        }
    }

    public <T extends User> void updateUserInfo(T user, ProfileUpdateRequest request,
                                                UserRepositoryCustom<T> userRepo,
                                                UserRepositoryCustom<? extends User> otherRepo) {
        Long userId = user.getId();

        if (request.getEmail() != null) {
            boolean dup = userRepo.existsByEmailAndIdNot(request.getEmail(), userId)
                    || otherRepo.existsByEmail(request.getEmail());
            if (dup) throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
            user.updateEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null) {
            boolean dup = userRepo.existsByPhoneNumberAndIdNot(request.getPhoneNumber(), userId)
                    || otherRepo.existsByPhoneNumber(request.getPhoneNumber());
            if (dup) throw new IllegalArgumentException("이미 사용 중인 전화번호입니다.");
            user.updatePhoneNumber(request.getPhoneNumber());
        }

        if (request.getName() != null) {
            user.updateName(request.getName());
        }
    }

    public MyPageMiniResponse getMyPageMini(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("인증 필요");
        }
        Long buyerId = buyerRepository.findOnlyBuyerIdByUsername(authentication.getName()); // 또는 username으로 buyerId 조회
        Long active = buyerRepository.findActivePoint(buyerId);
        Long couponCnt = buyerRepository.countUsableCoupons(buyerId, LocalDateTime.now());
        var grade = buyerRepository.findBuyerGrade(buyerId);
        long expirePoint = ledgerPointService.expireBuyer(buyerId);

        return new MyPageMiniResponse(active, couponCnt, grade, expirePoint);
    }

}
