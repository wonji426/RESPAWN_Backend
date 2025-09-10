package com.shop.respawn.service;

import com.shop.respawn.domain.AdminMemo;
import com.shop.respawn.dto.adminMemo.AdminMemoResponse;
import com.shop.respawn.dto.adminMemo.AdminMemoUpsertRequest;
import com.shop.respawn.repository.mongo.AdminMemoRepository;
import com.shop.respawn.repository.jpa.BuyerRepository;
import com.shop.respawn.repository.jpa.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemoService {

    private final AdminMemoRepository memoRepository;
    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;

    // 사용자당 1개만: upsert
    @Transactional
    public AdminMemoResponse upsert(AdminMemoUpsertRequest request) {
        String userType = normalize(request.getUserType());
        Long userId = request.getUserId();

        validateUser(userType, userId);

        AdminMemo memo = memoRepository.findByUserTypeAndUserId(userType, userId)
                .orElseGet(() -> AdminMemo.builder()
                        .userType(userType)
                        .userId(userId)
                        .build());

        memo.setContent(request.getContent());

        return AdminMemoResponse.from(memoRepository.save(memo));
    }

    public AdminMemoResponse getByUser(String userType, Long userId) {
        AdminMemo memo = memoRepository.findByUserTypeAndUserId(normalize(userType), userId)
                .orElseThrow(() -> new RuntimeException("해당 사용자에 대한 메모가 없습니다."));
        return AdminMemoResponse.from(memo);
    }

    private void validateUser(String userType, Long userId) {
        switch (userType) {
            case "buyer" -> buyerRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("구매자 없음: " + userId));
            case "seller" -> sellerRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("판매자 없음: " + userId));
            default -> throw new IllegalArgumentException("userType은 buyer 또는 seller여야 합니다.");
        }
    }

    private String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }
}
