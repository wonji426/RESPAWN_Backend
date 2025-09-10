package com.shop.respawn.domain;

import com.shop.respawn.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.shop.respawn.domain.Grade.*;
import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class Buyer extends BaseTimeEntity implements User {

    @Id @GeneratedValue
    @Column(name = "buyer_id")
    private Long id;

    private String name;

    @Column(unique = true)
    private String username;

    private String password;

    private String email;

    private String phoneNumber;

    private String provider;

    private String providerId;

    @Enumerated(STRING)
    private Role role;

    @Enumerated(STRING)
    private Grade grade = BASIC;

    @Builder.Default
    @OneToMany(mappedBy = "buyer")
    private List<PointLedger> pointLedgers = new ArrayList<>();

    // 계정 상태 필드 추가
    @Embedded
    @Builder.Default
    private AccountStatus accountStatus = new AccountStatus();

    @Builder.Default
    @OneToMany(mappedBy = "buyer")
    private List<Address> addresses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "buyer")
    private List<Cart> cart = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "buyer")
    private List<Order> orders = new ArrayList<>();

    private Buyer(String name, String username, String password, String email, String phoneNumber, String provider, Role role, Grade grade) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.provider = provider;
        this.role = role;
        this.grade = grade;
    }

    //정적 팩토리 메서드
    public static Buyer createBuyer(String name, String username, String password, String email, String phoneNumber, String provider, Role role, Grade grade) {
        Buyer buyer = Buyer.builder()
                .name(name)
                .username(username)
                .password(password)
                .email(email)
                .phoneNumber(phoneNumber)
                .provider(provider)
                .role(role)
                .grade(grade)
                .accountStatus(new AccountStatus(true)) // 가입시 1년 만료일 자동 할당
                .build();
        if (buyer.accountStatus.getLastPasswordChangedAt() == null) {
            buyer.accountStatus.setLastPasswordChangedAt(LocalDateTime.now());
        }
        return buyer;
    }

    public void updatePhoneNumber(String newPhoneNumber) {
        this.phoneNumber = newPhoneNumber;
    }
    public void updateName(String newName) {
        this.name = newName;
    }
    public void updateEmail(String newEmail) {
        this.email = newEmail;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    // 연관관계 편의 메서드
    /**
     * 주소를 추가하는 편의 메서드
     * 양방향 연관관계를 안전하게 설정합니다
     */
    public void addAddress(Address address) {
        this.addresses.add(address);
        address.setBuyer(this); // 패키지 레벨 세터 사용
    }

    public void renewExpiryDate() {
        if (this.accountStatus != null) {
            this.accountStatus.setAccountExpiryDate(LocalDateTime.now().plusYears(1));
        }
    }

    // 맴버십 변경
    public void updateGrade(Grade grade) {
        this.grade = grade;
    }

    // initData 용도
    public static Buyer createBuyerWithInitLists(String name, String username, String password, String email, String phoneNumber, Role role, Grade grade) {
        Buyer buyer = new Buyer(name, username, password, email, phoneNumber, "local", role, grade);
        buyer.orders = new ArrayList<>();
        buyer.addresses = new ArrayList<>();
        buyer.cart = new ArrayList<>();
        return buyer;
    }

    @PrePersist
    public void prePersist() {
        if (this.accountStatus == null) {
            this.accountStatus = new AccountStatus(true);
        }
        if (this.accountStatus.getLastPasswordChangedAt() == null) {
            this.accountStatus.setLastPasswordChangedAt(LocalDateTime.now());
        }
    }
}
