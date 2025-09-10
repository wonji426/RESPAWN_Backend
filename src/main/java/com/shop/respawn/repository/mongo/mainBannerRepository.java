package com.shop.respawn.repository.mongo;

import com.shop.respawn.domain.MainBanner;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface mainBannerRepository extends MongoRepository<MainBanner, String> {

    List<MainBanner> findByTitle(String title);

}
