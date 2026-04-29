package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Temporary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemporaryRepository extends JpaRepository<Temporary, Long> {
    // 나중에 콜백으로 돌아왔을 때 DB에서 찾기 위한 메서드
    // (만약 merchant_uid 필드를 엔티티에 추가한다면 findByMerchantUid 도 추가해야 합니다)
}
