package com.shop.respawn.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {

    ROLE_USER,
    ROLE_SELLER,
    ROLE_ADMIN
}