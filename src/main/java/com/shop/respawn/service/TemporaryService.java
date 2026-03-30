package com.shop.respawn.service;

import com.shop.respawn.domain.Temporary;
import com.shop.respawn.dto.TemporaryDto;
import com.shop.respawn.repository.jpa.TemporaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TemporaryService {

    private final TemporaryRepository temporaryRepository;

    public Temporary saveTemporaryData(TemporaryDto request) {
        Temporary temporary = new Temporary();
        temporary.setOrderId(request.getOrderId());
        temporary.setAddressId(request.getAddressId());
        temporary.setCouponCode(request.getCouponCode() != null ? request.getCouponCode() : "");
        temporary.setUsedPointAmount(request.getUsePointAmount() != null ? request.getUsePointAmount() : 0L);

        // DB에 INSERT (이 순간 temporary_id가 자동 생성됨)
        return temporaryRepository.save(temporary);
    }

    @Transactional(readOnly = true)
    public Temporary getTemporaryData(Long temporaryId) {
        return temporaryRepository.findById(temporaryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 임시 결제 정보를 찾을 수 없습니다. ID: " + temporaryId));
    }
}