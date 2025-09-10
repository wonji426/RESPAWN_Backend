package com.shop.respawn.domain;

public interface User {

    Long getId();

    String getEmail();

    String getPhoneNumber();

    void updateEmail(String email);

    void updatePhoneNumber(String phoneNumber);

    void updateName(String name);

}
