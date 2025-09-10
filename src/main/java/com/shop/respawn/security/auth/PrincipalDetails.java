package com.shop.respawn.security.auth;

import com.shop.respawn.domain.Admin;
import com.shop.respawn.domain.Buyer;
import com.shop.respawn.domain.Seller;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

@Data
public class PrincipalDetails implements UserDetails, OAuth2User {

    private Buyer buyer;
    private Seller seller;
    private Admin admin;
    private Map<String, Object> attributes;

    // 일반 로그인
    public PrincipalDetails(Buyer buyer) {
        this.buyer = buyer;
    }

    public PrincipalDetails(Seller seller) {
        this.seller = seller;
    }

    public PrincipalDetails(Admin admin) {
        this.admin = admin;
    }

    // OAuth 로그인
    public PrincipalDetails(Buyer buyer, Map<String, Object> attributes) {
        this.buyer = buyer;
        this.attributes = attributes;
    }

    // 해당 유저의 권한을 리턴하는 곳
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (buyer != null) {
            return List.of(new SimpleGrantedAuthority(buyer.getRole().name()));
        } else if (seller != null) {
            return List.of(new SimpleGrantedAuthority(seller.getRole().name()));
        } else if (admin != null) {
            return List.of(new SimpleGrantedAuthority(admin.getRole().name()));
        }
        return List.of();
    }

    @Override
    public String getPassword() {
        if (buyer != null) {
            return buyer.getPassword();
        } else if (seller != null) {
            return seller.getPassword();
        }  else if (admin != null) {
            return admin.getPassword();
        }
        return null;
    }

    public Long getUserId() {
        if (buyer != null) {
            return buyer.getId();
        } else if (seller != null) {
            return seller.getId();
        } else if (admin != null) {
            return admin.getId();
        }
        return null;
    }

    @Override
    public String getUsername() {
        if (buyer != null) {
            return buyer.getUsername();
        } else if (seller != null) {
            return seller.getUsername();
        } else if (admin != null) {
            return admin.getUsername();
        }
        return null;
    }

    @Override
    public String getName() {
        if (buyer != null) {
            return buyer.getName();
        } else if (seller != null) {
            return seller.getName();
        } else if (admin != null) {
            return admin.getName();
        }
        return null;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean isAccountNonExpired() {
        if (buyer != null && buyer.getAccountStatus() != null) {
            return buyer.getAccountStatus().isAccountNonExpired();
        } else if (seller != null && seller.getAccountStatus() != null) {
            return seller.getAccountStatus().isAccountNonExpired();
        }
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (buyer != null && buyer.getAccountStatus() != null) {
            return buyer.getAccountStatus().isAccountNonLocked();
        } else if (seller != null && seller.getAccountStatus() != null) {
            return seller.getAccountStatus().isAccountNonLocked();
        }
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        if (buyer != null && buyer.getAccountStatus() != null) {
            return buyer.getAccountStatus().isEnabled();
        } else if (seller != null && seller.getAccountStatus() != null) {
            return seller.getAccountStatus().isEnabled();
        }
        return true;
    }
}
