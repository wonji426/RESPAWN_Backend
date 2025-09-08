package com.shop.respawn.dto.query;

import com.shop.respawn.domain.Grade;
import com.shop.respawn.domain.Role;
import lombok.Data;

@Data
public class UserQueryDto {

    private Long id;
    private String name;
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private String provider;
    private String providerId;
    private Role role;
    private Grade grade;

    public UserQueryDto(Long id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public UserQueryDto(Long id, String username, Grade grade) {
        this.id = id;
        this.username = username;
        this.grade = grade;
    }

}