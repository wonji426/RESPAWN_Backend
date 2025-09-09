package com.shop.respawn.repository;

import com.shop.respawn.dto.NoticeSummaryDto;

import java.util.List;

public interface NoticeRepositoryCustom {

    void incrementViewCount(Long id);

    List<NoticeSummaryDto> findNoticeSummaries();
}
