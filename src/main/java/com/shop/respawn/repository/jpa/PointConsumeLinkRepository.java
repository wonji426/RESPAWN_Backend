package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.PointConsumeLink;
import com.shop.respawn.domain.PointLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointConsumeLinkRepository extends JpaRepository<PointConsumeLink, Long> {

    List<PointConsumeLink> findByUseLedger(PointLedger useLedger);
    List<PointConsumeLink> findBySaveLedger(PointLedger saveLedger);
}
