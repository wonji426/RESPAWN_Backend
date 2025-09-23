package com.shop.respawn.repository.jpa;

import com.shop.respawn.dto.notice.NoticeSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeRepositoryCustom {

    void incrementViewCount(Long id);

    Page<NoticeSummaryDto> findNoticeSummaries(Pageable pageable);
}
