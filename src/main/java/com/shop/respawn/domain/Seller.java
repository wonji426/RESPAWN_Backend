package com.shop.respawn.domain;

import com.shop.respawn.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class Seller extends BaseTimeEntity implements User {

    @Id @GeneratedValue
    @Column(name = "seller_id")
    private Long id;

    private String name;

    @Column(unique = true)
    private String username;

    private String company;

    private Long companyNumber;

    private String password;

    private String email;

    private String phoneNumber;

    @Enumerated(STRING)
    private Role role;

    // 계정 상태 필드 추가
    @Embedded
    private AccountStatus accountStatus = new AccountStatus();

    //정적 팩토리 메서드
    public static Seller createSeller(String name, String username, String company, Long companyNumber, String password, String email, String phoneNumber, Role role) {
        Seller seller = Seller.builder()
                .name(name)
                .username(username)
                .company(company)
                .companyNumber(companyNumber)
                .password(password)
                .email(email)
                .phoneNumber(phoneNumber)
                .role(role)
                .accountStatus(new AccountStatus(true)) // 가입시 1년 만료일 자동 할당
                .build();
        if (seller.accountStatus.getLastPasswordChangedAt() == null) {
            seller.accountStatus.setLastPasswordChangedAt(LocalDateTime.now());
        }
        return seller;
    }

    //연관 관계 편의 메서드
    public void renewExpiryDate() {
        if (this.accountStatus != null) {
            this.accountStatus.setAccountExpiryDate(LocalDateTime.now().plusYears(1));
        }
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }
    public void updateName(String newName) {
        this.name = newName;
    }
    public void updateEmail(String newEmail) {
        this.email = newEmail;
    }
    public void updatePhoneNumber(String newPhoneNumber) {
        this.phoneNumber = newPhoneNumber;
    }

    @PrePersist
    public void prePersist() {
        if (this.accountStatus != null && this.accountStatus.getLastPasswordChangedAt() == null) {
            this.accountStatus.setLastPasswordChangedAt(LocalDateTime.now());
        }
    }
}
