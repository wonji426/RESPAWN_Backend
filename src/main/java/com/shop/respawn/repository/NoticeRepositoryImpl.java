package com.shop.respawn.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.dto.NoticeSummaryDto;
import com.shop.respawn.dto.QNoticeSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.shop.respawn.domain.QNotice.notice;

@Repository
@RequiredArgsConstructor
@Transactional
public class NoticeRepositoryImpl implements NoticeRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public void incrementViewCount(Long id) {
        queryFactory
                .update(notice)
                .set(notice.viewCount, notice.viewCount.add(1))
                .where(notice.id.eq(id))
                .execute();
    }

    @Override
    public List<NoticeSummaryDto> findNoticeSummaries() {
        return queryFactory
                .select(new QNoticeSummaryDto(
                        notice.title,
                        notice.noticeType,
                        notice.createdAt
                ))
                .from(notice)
                .fetch();
    }

}
