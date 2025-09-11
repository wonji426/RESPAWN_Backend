package com.shop.respawn.dto.user;

import com.shop.respawn.domain.Grade;
import com.shop.respawn.domain.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto {

    private String userType;

    private String name;

    private String username;

    private String password;

    private String company;

    private Long companyNumber;

    private String email;

    private String phoneNumber;

    private String provider;

    private String providerId;

    private Role role;

    private Grade grade;

    public UserDto(String userType, String name, String username, String password, String email, String phoneNumber, Role role, Grade grade) {
        this.userType = userType;
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.grade = grade;
    }

    public UserDto(String name, String username, String email, String phoneNumber, String provider, Role role, Grade grade) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.provider = provider;
        this.role = role;
        this.grade = grade;
    }

    public UserDto(String name, String username, String email, String phoneNumber, Role role) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    public UserDto(String userType, String name, String username, String company, Long companyNumber, String password, String email, String phoneNumber, Role role) {
        this.userType = userType;
        this.name = name;
        this.username = username;
        this.password = password;
        this.company = company;
        this.companyNumber = companyNumber;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }
}
