package com.shop.respawn.repository.jpa;

import com.shop.respawn.domain.Address;
import com.shop.respawn.domain.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByBuyerAndBasicTrue(Buyer buyer);

    // 구매자의 모든 주소 조회
    List<Address> findByBuyerOrderByBasicDescIdDesc(Buyer buyer);

    Optional<Address> findByIdAndBuyer(Long addressId, Buyer buyer);
}
