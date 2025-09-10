package com.shop.respawn.repository.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.respawn.dto.NoticeSummaryDto;
import com.shop.respawn.dto.QNoticeSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    public Page<NoticeSummaryDto> findNoticeSummaries(Pageable pageable) {
        List<NoticeSummaryDto> content = queryFactory
                .select(new QNoticeSummaryDto(
                        notice.id,
                        notice.title,
                        notice.noticeType,
                        notice.createdAt
                ))
                .from(notice)
                .orderBy(notice.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(notice.count())
                .from(notice)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

}
