package com.shop.respawn.repository.jpa;

import com.shop.respawn.dto.NoticeSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NoticeRepositoryCustom {

    void incrementViewCount(Long id);

    List<NoticeSummaryDto> findNoticeSummaries();

    Page<NoticeSummaryDto> findNoticeSummaries(Pageable pageable);
}
