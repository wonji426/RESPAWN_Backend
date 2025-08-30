package com.shop.respawn.controller;

import com.shop.respawn.dto.findInfo.ChangePasswordRequest;
import com.shop.respawn.dto.findInfo.ResetPasswordRequest;
import com.shop.respawn.exception.ApiMessage;
import com.shop.respawn.exception.CommonResponse;
import com.shop.respawn.dto.findInfo.FindInfoRequest;
import com.shop.respawn.dto.findInfo.FindInfoResponse;
import com.shop.respawn.dto.user.*;
import com.shop.respawn.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

import static com.shop.respawn.exception.status_code.ErrorStatus.*;
import static com.shop.respawn.exception.status_code.SuccessStatus.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원 가입
     */
    @PostMapping("/join")
    public ResponseEntity<Void> join(@RequestBody UserDto userDto) {
        userService.join(userDto);
        return ResponseEntity.ok().build();
    }

    /**
     * session 동기화/초기화
     */
    @GetMapping("/bring-me")
    public ResponseEntity<LoginOkResponse> bringMe(Authentication authentication) {
        LoginOkResponse data = userService.bringMe(authentication);
        if (data == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(data);
    }

    /**
     * 로그인 완료 처리
     */
    @GetMapping("/loginOk")
    public ResponseEntity<LoginOkResponse> loginOk(Authentication authentication,
                                                   HttpSession session) {
        LoginOkResponse userData = userService.getUserData(authentication.getName(),
                authentication.getAuthorities().toString());
        session.setAttribute("userId", userData.getUserId());
        return ResponseEntity.ok(userData);
    }

    /**
     * 로그아웃
     */
    @GetMapping("/logoutOk")
    public ResponseEntity<?> logoutOk() {
        return ResponseEntity.ok().build();
    }

    /**
     * 관리자 정보 조회
     */
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminPage() {
        return ResponseEntity.ok().build();
    }

    /**
     * 일반 유저 정보 조회
     */
    @GetMapping("/user")
    public ResponseEntity<CommonResponse<UserDto>> getUserPage(Authentication authentication) {
        try {
            UserDto userInfo = userService.getUserInfo(
                    authentication.getName(), authentication.getAuthorities().toString());
            return ResponseEntity.ok(CommonResponse.of(_OK, userInfo));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.onFailure(_USER_UNAUTHORIZED, null));
        }
    }

    @GetMapping("/myPage/summary")
    public ResponseEntity<CommonResponse<MyPageMiniResponse>> summary(Authentication authentication) {
        return ResponseEntity.ok(CommonResponse.of(_OK, userService.getMyPageMini(authentication)));
    }

    /**
     * 마이페이지에서 비밀번호가 일치하는지 검사하는 메서드
     */
    @PostMapping("/myPage/checkPassword")
    public ResponseEntity<CommonResponse<?>> checkPassword(Authentication authentication,
                                                 @Valid @RequestBody PasswordRequest request) {
        if(userService.isMatchPassword(authentication.getName(), authentication.getAuthorities().toString(), request.getPassword())) {
            return ResponseEntity.ok(CommonResponse.ok(_PASSWORD_CHECKED));
        } else {
            return ResponseEntity.badRequest().body(CommonResponse.onFailure(_PASSWORD_MISMATCH, null));
        }
    }

    /**
     * 전화번호 수정 엔드포인트
     */
    @PutMapping("/myPage/setPhoneNumber")
    public ResponseEntity<ApiMessage> updatePhoneNumber(Authentication authentication,
                                                  @Valid @RequestBody PhoneNumberRequest request) {
        userService.updatePhoneNumber(authentication.getName(), request.getPhoneNumber());
        return ResponseEntity.ok(ApiMessage.of(_PHONE_NUMBER_CHANGED));
    }

    /**
     * 비밀번호 변경 엔드포인트
     */
    @PutMapping("/myPage/setPassword")
    public ResponseEntity<ApiMessage> changePassword(Authentication authentication,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        if (userService.changePassword(authentication.getName(), authentication.getAuthorities().toString(),
                request.getCurrentPassword(), request.getNewPassword())) {
            return ResponseEntity.ok(ApiMessage.of(_PASSWORD_CHANGED));
        } else {
            return ResponseEntity.badRequest().body(ApiMessage.of(_PASSWORD_MISMATCH));
        }
    }

    /**
     * 이름 + 이메일 or 전화번호로 마스킹된 아이디 찾기
     */
    @PostMapping("/find-id")
    public ResponseEntity<FindInfoResponse> findId(@RequestBody FindInfoRequest findInfoRequest) {
        try {
            FindInfoResponse response = userService.findId(findInfoRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new FindInfoResponse(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new FindInfoResponse(e.getMessage()));
        }
    }

    /**
     * 이메일 or 전화번호로 실제 아이디 전송 컨트롤러
     */
    @PostMapping("/find-id/send")
    public ResponseEntity<ApiMessage> sendId(@RequestBody FindInfoRequest findInfoRequest) {
        try {
            String message = userService.processSendId(findInfoRequest);
            return ResponseEntity.ok(ApiMessage.of(message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiMessage.of(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiMessage.of(e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiMessage.of(e.getMessage()));
        }
    }

    /**
     * 이메일 or 전화번호 사용 비밀번호 찾을 계정 조회 컨트롤러
     */
    @PostMapping("/find-password")
    public ResponseEntity<FindInfoResponse> findPassword(@RequestBody FindInfoRequest findInfoRequest) {
        try {
            FindInfoResponse response = userService.findPasswordAccount(findInfoRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new FindInfoResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new FindInfoResponse(e.getMessage()));
        }
    }

    /**
     * 이메일 or 전화번호로 비밀번호 재설정 페이지 발송 컨트롤러
     */
    @PostMapping("/find-password/send")
    public ResponseEntity<FindInfoResponse> sendPassword(@RequestBody FindInfoRequest findInfoRequest) {
        try {
            String message = userService.sendPasswordReset(findInfoRequest);
            return ResponseEntity.ok(new FindInfoResponse(message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new FindInfoResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new FindInfoResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new FindInfoResponse(e.getMessage()));
        }
    }

    /**
     * 비밀번호 재설정 페이지 컨트롤러
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessage> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        try {
            userService.resetPassword(resetPasswordRequest);
            return ResponseEntity.ok(ApiMessage.of("비밀번호가 성공적으로 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiMessage.of(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiMessage.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiMessage.of(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        MeResponse body = userService.getMyStatus(authentication);
        return ResponseEntity.ok(body);
    }

    // 사용자가 "나중에" 클릭 시 호출
    @PostMapping("/password-change/snooze")
    public ResponseEntity<ApiMessage> snooze(Authentication authentication) {
        try {
            // 7일(604,800초) 억제
            userService.snoozePasswordReminder(authentication);
            return ResponseEntity.ok(ApiMessage.of(_NO_CONTENT));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiMessage.of(e.getMessage()));
        }
    }

    @PostMapping("/profile/update")
    public ResponseEntity<?> updateProfile(Authentication authentication,
                                           @RequestBody ProfileUpdateRequest request) {
        // 최소 1개 필드 제공 검증
        if (request.getName() == null && request.getPhoneNumber() == null && request.getEmail() == null) {
            return ResponseEntity.badRequest().body(ApiMessage.of("최소 1개 이상의 필드를 입력하세요."));
        }

        userService.updateProfile(authentication, request);
        return ResponseEntity.ok(ApiMessage.of("NO_CONTENT","프로필이 업데이트되었습니다."));
    }

    /**
     * username 중복 체크
     */
    @GetMapping("signup/username/{username}")
    public Boolean checkUsernameDuplicate(@PathVariable String username) {
        return userService.checkUsernameDuplicate(username);
    }

    /**
     * phoneNumber 중복 체크
     */
    @GetMapping("signup/phoneNumber/{phoneNumber}")
    public Boolean checkPhoneNumberDuplicate(@PathVariable String phoneNumber) {
        return userService.checkPhoneNumberDuplicate(phoneNumber);
    }

    /**
     * email 중복 체크
     */
    @GetMapping("signup/email/{email}")
    public Boolean checkEmailDuplicate(@PathVariable String email) {
        return userService.checkEmailDuplicate(email);
    }

}
