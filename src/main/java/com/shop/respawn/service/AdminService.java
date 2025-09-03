package com.shop.respawn.service;

import com.shop.respawn.domain.Buyer;
import com.shop.respawn.domain.Seller;
import com.shop.respawn.dto.user.BuyerListDto;
import com.shop.respawn.dto.user.SellerListDto;
import com.shop.respawn.dto.user.UserSummaryDto;
import com.shop.respawn.repository.BuyerRepository;
import com.shop.respawn.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;
    private static final LocalDate MIN_DATE = LocalDate.of(1970, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);
    // 드라이버 친화적인 일 마감 시각(밀리초 정밀도)
    private static final LocalTime DAY_END = LocalTime.of(23, 59, 59, 999_000_000);

    public void expireUserById(String userType, Long userId) {
        switch (userType.toLowerCase()) {
            case "buyer" -> {
                Buyer buyer = buyerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("구매자 없음: " + userId));
                if (buyer.getAccountStatus() != null) {
                    // 현재 시각보다 과거로 설정하여 만료 상태가 되게 함
                    buyer.getAccountStatus().setAccountExpiryDate(LocalDateTime.now().minusSeconds(1));
                }
            }
            case "seller" -> {
                Seller seller = sellerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("판매자 없음: " + userId));
                if (seller.getAccountStatus() != null) {
                    seller.getAccountStatus().setAccountExpiryDate(LocalDateTime.now().minusSeconds(1));
                }
            }
            default -> throw new IllegalArgumentException("userType은 buyer 또는 seller여야 합니다.");
        }
    }

    public void unexpireUserById(String userType, Long userId) {
        switch (userType.toLowerCase()) {
            case "buyer" -> {
                Buyer buyer = buyerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("구매자 없음: " + userId));
                if (buyer.getAccountStatus() != null) {
                    buyer.renewExpiryDate();
                }
            }
            case "seller" -> {
                Seller seller = sellerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("판매자 없음: " + userId));
                if (seller.getAccountStatus() != null) {
                    seller.renewExpiryDate();
                }
            }
            default -> throw new IllegalArgumentException("userType은 buyer 또는 seller여야 합니다.");
        }
    }

    public void enableUserById(String userType, Long userId) {
        switch (userType.toLowerCase()) {
            case "buyer" -> {
                Buyer buyer = buyerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("구매자 없음: " + userId));
                if (buyer.getAccountStatus() != null) {
                    buyer.getAccountStatus().setEnabled(true);
                }
            }
            case "seller" -> {
                Seller seller = sellerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("판매자 없음: " + userId));
                if (seller.getAccountStatus() != null) {
                    seller.getAccountStatus().setEnabled(true);
                }
            }
            default -> throw new IllegalArgumentException("userType은 buyer 또는 seller여야 합니다.");
        }
    }

    public void disableUserById(String userType, Long userId) {
        switch (userType.toLowerCase()) {
            case "buyer" -> {
                Buyer buyer = buyerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("구매자 없음: " + userId));
                if (buyer.getAccountStatus() != null) {
                    buyer.getAccountStatus().setEnabled(false);
                }
            }
            case "seller" -> {
                Seller seller = sellerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("판매자 없음: " + userId));
                if (seller.getAccountStatus() != null) {
                    seller.getAccountStatus().setEnabled(false);
                }
            }
            default -> throw new IllegalArgumentException("userType은 buyer 또는 seller여야 합니다.");
        }
    }

    public void unlockById(String userType, Long userId) {
        switch (userType.toLowerCase()) {
            case "buyer" -> {
                Buyer buyer = buyerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("구매자 없음: " + userId));
                if (buyer.getAccountStatus() != null) {
                    buyer.getAccountStatus().resetFailedLoginAttempts();
                }
            }
            case "seller" -> {
                Seller seller = sellerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("판매자 없음: " + userId));
                if (seller.getAccountStatus() != null) {
                    seller.getAccountStatus().resetFailedLoginAttempts();
                }
            }
            default -> throw new IllegalArgumentException("userType은 buyer 또는 seller여야 합니다.");
        }
    }

    public boolean isEnabledById(String userType, Long userId) {
        return switch (userType.toLowerCase()) {
            case "buyer" -> buyerRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("구매자 없음: " + userId))
                    .getAccountStatus().isEnabled();
            case "seller" -> sellerRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("판매자 없음: " + userId))
                    .getAccountStatus().isEnabled();
            default -> throw new IllegalArgumentException("userType은 buyer 또는 seller여야 합니다.");
        };
    }

    // -------- 구매자 조회 --------
    @Transactional(readOnly = true)
    public Page<BuyerListDto> findBuyersPaged(int page, int size, String sort, String dir, String keyword, String field, String dateRange) {
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortOrDefault(sort)));

        LocalDateTime[] range = parseDateRange(dateRange);
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        Page<Buyer> buyers;
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (start != null && end != null) {
            if (!hasKeyword) {
                buyers = buyerRepository.findByCreatedAtBetween(start, end, pageable);
            } else {
                switch (normalizeField(field)) {
                    case "name" -> buyers = buyerRepository.findByNameContainingIgnoreCaseAndCreatedAtBetween(keyword, start, end, pageable);
                    case "username" -> buyers = buyerRepository.findByUsernameContainingIgnoreCaseAndCreatedAtBetween(keyword, start, end, pageable);
                    case "email" -> buyers = buyerRepository.findByEmailContainingIgnoreCaseAndCreatedAtBetween(keyword, start, end, pageable);
                    case "phonenumber" -> buyers = buyerRepository.findByPhoneNumberContainingAndCreatedAtBetween(keyword, start, end, pageable);
                    default -> buyers = buyerRepository.findByNameContainingIgnoreCaseAndCreatedAtBetween(keyword, start, end, pageable);
                }
            }
        } else {
            // 날짜 범위가 없으면 기존 로직
            if (!hasKeyword) {
                buyers = buyerRepository.findAll(pageable);
            } else {
                switch (normalizeField(field)) {
                    case "name" -> buyers = buyerRepository.findByNameContainingIgnoreCase(keyword, pageable);
                    case "username" -> buyers = buyerRepository.findByUsernameContainingIgnoreCase(keyword, pageable);
                    case "email" -> buyers = buyerRepository.findByEmailContainingIgnoreCase(keyword, pageable);
                    case "phonenumber" -> buyers = buyerRepository.findByPhoneNumberContaining(keyword, pageable);
                    default -> buyers = buyerRepository.findByNameContainingIgnoreCase(keyword, pageable);
                }
            }
        }
        return buyers.map(BuyerListDto::from);
    }

    // -------- 판매자 조회 --------
    @Transactional(readOnly = true)
    public Page<SellerListDto> findSellersPaged(int page, int size, String sort, String dir, String keyword, String field, String dateRange) {
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortOrDefault(sort)));

        LocalDateTime[] range = parseDateRange(dateRange);
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        Page<Seller> sellers;
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (start != null && end != null) {
            if (!hasKeyword) {
                sellers = sellerRepository.findByCreatedAtBetween(start, end, pageable);
            } else {
                switch (normalizeField(field)) {
                    case "name" -> sellers = sellerRepository.findByNameContainingIgnoreCaseAndCreatedAtBetween(keyword, start, end, pageable);
                    case "username" -> sellers = sellerRepository.findByUsernameContainingIgnoreCaseAndCreatedAtBetween(keyword, start, end, pageable);
                    case "email" -> sellers = sellerRepository.findByEmailContainingIgnoreCaseAndCreatedAtBetween(keyword, start, end, pageable);
                    case "phonenumber" -> sellers = sellerRepository.findByPhoneNumberContainingAndCreatedAtBetween(keyword, start, end, pageable);
                    default -> sellers = sellerRepository.findByNameContainingIgnoreCaseAndCreatedAtBetween(keyword, start, end, pageable);
                }
            }
        } else {
            if (!hasKeyword) {
                sellers = sellerRepository.findAll(pageable);
            } else {
                switch (normalizeField(field)) {
                    case "name" -> sellers = sellerRepository.findByNameContainingIgnoreCase(keyword, pageable);
                    case "username" -> sellers = sellerRepository.findByUsernameContainingIgnoreCase(keyword, pageable);
                    case "email" -> sellers = sellerRepository.findByEmailContainingIgnoreCase(keyword, pageable);
                    case "phonenumber" -> sellers = sellerRepository.findByPhoneNumberContaining(keyword, pageable);
                    default -> sellers = sellerRepository.findByNameContainingIgnoreCase(keyword, pageable);
                }
            }
        }
        return sellers.map(SellerListDto::from);
    }

    private LocalDateTime[] parseDateRange(String dateRange) {
        if (dateRange == null) return new LocalDateTime[]{null, null};

        String raw = dateRange.trim();
        if (raw.isEmpty()) return new LocalDateTime[]{null, null};

        String[] parts = raw.split("~", -1); // 빈 오른쪽도 보존
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");

        String left = (parts.length >= 1 && parts[0] != null) ? parts[0].trim() : null;
        String right = (parts.length >= 2 && parts[1] != null) ? parts[1].trim() : null;

        LocalDateTime start = null;
        LocalDateTime end = null;

        try {
            if (left != null && !left.isEmpty()) {
                LocalDate d = LocalDate.parse(left, fmt);
                start = d.atStartOfDay();
            }
            if (right != null && !right.isEmpty()) {
                LocalDate d = LocalDate.parse(right, fmt);
                end = d.atTime(LocalTime.MAX);
            }

             if (start == null && end != null) start = MIN_DATE.atStartOfDay();
             if (start != null && end == null) end =  MAX_DATE.atTime(DAY_END);

            if (start != null && start.isAfter(end)) {
                LocalDateTime tmp = start; start = end; end = tmp;
            }
        } catch (Exception e) {
            // 형식 오류 시 범위 미적용
            return new LocalDateTime[]{null, null};
        }

        // 둘 다 없는 경우 미적용
        if (start == null) return new LocalDateTime[]{null, null};
        return new LocalDateTime[]{start, end};
    }

    private String normalizeField(String field) {
        return field == null ? "name" : field.replaceAll("\\s+", "").toLowerCase();
    }

    @Transactional(readOnly = true)
    public UserSummaryDto findUserSummaryById(String userType, Long userId) {
        return switch (userType.toLowerCase()) {
            case "buyer" -> {
                Buyer buyer = buyerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("구매자 없음: " + userId));
                yield UserSummaryDto.fromBuyer(buyer);
            }
            case "seller" -> {
                Seller seller = sellerRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("판매자 없음: " + userId));
                yield UserSummaryDto.fromSeller(seller);
            }
            default -> throw new IllegalArgumentException("userType은 buyer 또는 seller여야 합니다.");
        };
    }

    private String sortOrDefault(String sort) {
        if (sort == null || sort.isBlank()) return "username";
        return switch (sort.toLowerCase()) {
            case "username", "name", "id", "phonenumber", "email", "createdat", "grade", "company" -> sort;
            default -> "username";
        };
    }
}
