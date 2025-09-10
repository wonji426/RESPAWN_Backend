package com.shop.respawn.service;

import com.shop.respawn.domain.Address;
import com.shop.respawn.domain.Buyer;
import com.shop.respawn.dto.AddressDto;
import com.shop.respawn.repository.jpa.AddressRepository;
import com.shop.respawn.repository.jpa.BuyerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final BuyerRepository buyerRepository;

    /**
     * 새 주소를 생성하고 구매자에게 연결
     */
    public AddressDto createAddress(Long buyerId, AddressDto addressDto) {
        // 구매자 조회
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구매자입니다."));

        // 기본 주소로 설정하는 경우, 기존 기본 주소를 해제
        if (addressDto.isBasic()) {
            updateExistingBasicAddress(buyer);
        }

        // 주소 생성 (정적 팩토리 메서드 사용)
        Address address = Address.createAddress(
                buyer,
                addressDto.getAddressName(),
                addressDto.getRecipient(),
                addressDto.getZoneCode(),
                addressDto.getBaseAddress(),
                addressDto.getDetailAddress(),
                addressDto.getPhone(),
                addressDto.getSubPhone(),
                addressDto.isBasic()
        );

        // 주소 저장
        Address saved = addressRepository.save(address);
        return new AddressDto(saved);
    }

    /**
     * 구매자의 모든 주소 조회 (기본 주소 우선, 최신 순)
     */
    @Transactional(readOnly = true)
    public List<AddressDto> getAddressesByBuyer(Long buyerId) {
        // 구매자 조회
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구매자입니다."));

        // 주소 목록 조회 (기본 주소 우선, 최신 순)
        List<Address> addresses = addressRepository.findByBuyerOrderByBasicDescIdDesc(buyer);

        // Address 엔티티를 AddressDto로 변환
        return addresses.stream()
                .map(this::convertToDto)
                .toList();
    }

    /**
     * 구매자의 기본 주소 조회
     */
    @Transactional(readOnly = true)
    public AddressDto getBasicAddress(Long buyerId) {
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구매자입니다."));

        Address basicAddress = addressRepository.findByBuyerAndBasicTrue(buyer)
                .orElseThrow(()->new RuntimeException("기본 주소가 설정되지 않았습니다."));

        return new AddressDto(basicAddress);
    }

    /**
     * 기존 기본 주소를 일반 주소로 변경
     */
    private void updateExistingBasicAddress(Buyer buyer) {
        addressRepository.findByBuyerAndBasicTrue(buyer)
                .ifPresent(address -> address.changeBasicStatus(false));
    }

    /**
     * 주소 삭제
     */
    public void deleteAddress(Long buyerId, Long addressId) {
        // 구매자 조회
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구매자입니다."));

        // 주소 조회 및 권한 체크
        Address address = addressRepository.findByIdAndBuyer(addressId, buyer)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주소이거나 삭제 권한이 없습니다."));

        // 기본 주소 삭제 시 처리
        if (address.isBasic()) {
            // 다른 주소들 중에서 가장 최근 주소를 기본 주소로 설정
            List<Address> otherAddresses = addressRepository.findByBuyerOrderByBasicDescIdDesc(buyer)
                    .stream()
                    .filter(addr -> !addr.getId().equals(addressId))
                    .toList();

            if (!otherAddresses.isEmpty()) {
                otherAddresses.getFirst().changeBasicStatus(true);
            }
        }

        // 주소 삭제
        addressRepository.delete(address);
    }

    /**
     * 주소 정보 수정
     */
    public AddressDto updateAddress(Long buyerId, Long addressId, AddressDto addressDto) {
        // 구매자 조회
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구매자입니다."));

        // 주소 조회 및 권한 체크
        Address address = addressRepository.findByIdAndBuyer(addressId, buyer)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주소이거나 수정 권한이 없습니다."));

        // 기본 주소로 변경하는 경우 기존 기본 주소 해제
        if (addressDto.isBasic() && !address.isBasic()) {
            updateExistingBasicAddress(buyer);
        }

        // 주소 정보 업데이트
        address.updateAddressInfo(
                addressDto.getAddressName(),
                addressDto.getRecipient(),
                addressDto.getZoneCode(),
                addressDto.getBaseAddress(),
                addressDto.getDetailAddress(),
                addressDto.getPhone(),
                addressDto.getSubPhone()
        );

        // 기본 주소 상태 변경
        address.changeBasicStatus(addressDto.isBasic());

        return convertToDto(address);
    }

    /**
     * Address 엔티티를 AddressDto로 변환하는 헬퍼 메서드
     */
    private AddressDto convertToDto(Address address) {
        return new AddressDto(
                address.getId(),
                address.getAddressName(),
                address.getRecipient(),
                address.getZoneCode(),
                address.getBaseAddress(),
                address.getDetailAddress(),
                address.getPhone(),
                address.getSubPhone(),
                address.isBasic()
        );
    }

}
