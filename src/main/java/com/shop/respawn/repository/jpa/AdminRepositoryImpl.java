package com.shop.respawn.repository.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.domain.Role;
import com.shop.respawn.dto.query.UserQueryDto;
import org.springframework.stereotype.Repository;

import static com.shop.respawn.domain.QAdmin.*;

@Repository
public class AdminRepositoryImpl implements AdminRepositoryCustom {

    private final BaseRepositoryImpl base; // 생성자 주입 권장

    // 생성자에서 주입 받는 형태 권장
    public AdminRepositoryImpl(JPAQueryFactory queryFactory) {
        this.base = new BaseRepositoryImpl(queryFactory);
    }

    @Override
    public Role findUserDtoRoleByUsername(String username) {
        return base.findUserDtoRoleByUsername(admin, admin.role, admin.username, username);
    }

    @Override
    public UserQueryDto findUserDtoByUsername(String username) {
        return base.findUserDtoByUsername(
                admin, admin.id, admin.name, admin.role, admin.username, username
        );
    }
}
