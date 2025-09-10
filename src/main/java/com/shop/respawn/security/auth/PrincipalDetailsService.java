package com.shop.respawn.security.auth;

import com.shop.respawn.domain.Admin;
import com.shop.respawn.domain.Buyer;
import com.shop.respawn.domain.Role;
import com.shop.respawn.domain.Seller;
import com.shop.respawn.repository.jpa.AdminRepository;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Setter
public class PrincipalDetailsService implements UserDetailsService {

    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;
    private final AdminRepository adminRepository;

    // 시큐리티 session(내부 Authentication(내부 UserDetails))
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1) 우선 가벼운 DTO로 Role 탐색 (최대 1~3회 중복 없이 순차 시도)
        Role dto = buyerRepository.findUserDtoRoleByUsername(username);
        if (dto == null) dto = sellerRepository.findUserDtoRoleByUsername(username);
        if (dto == null) dto = adminRepository.findUserDtoRoleByUsername(username);
        if (dto == null) throw new UsernameNotFoundException(username + " : 사용자를 찾을 수 없습니다");

        // 2) 확인된 Role에 따라 대상 테이블만 실 엔티티 1회 조회
        switch (dto) {
            case ROLE_USER -> {
                Buyer buyer = buyerRepository.findByUsername(username);
                buyer.getAccountStatus().isAccountNonExpired(); // 만료 갱신 트리거
                buyerRepository.save(buyer);
                return new PrincipalDetails(buyer);
            }
            case ROLE_SELLER -> {
                Seller seller = sellerRepository.findByUsername(username);
                seller.getAccountStatus().isAccountNonExpired();
                sellerRepository.save(seller);
                return new PrincipalDetails(seller);
            }
            case ROLE_ADMIN -> {
                Admin admin = adminRepository.findByUsername(username);
                adminRepository.save(admin);
                return new PrincipalDetails(admin);
            }
            default -> throw new UsernameNotFoundException("알 수 없는 권한의 사용자입니다");
        }
    }
}
