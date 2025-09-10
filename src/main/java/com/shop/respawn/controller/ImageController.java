package com.shop.respawn.controller;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.shop.respawn.domain.MainBanner;
import com.shop.respawn.repository.mongo.mainBannerRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ImageController {

    private final GridFsTemplate gridFsTemplate;
    private final mainBannerRepository mainBannerRepository;

    @GetMapping("/mainBanner/upload")
    public String createForm(Model model) {
        model.addAttribute("mainBannerForm", new MainBannerForm());
        return "mainBanner/mainBannerForm";
    }

    @PostMapping("/mainBanner/upload")
    public String uploadImage(
            @RequestParam("file") MultipartFile file,
            @ModelAttribute MainBannerForm mainBannerForm, Model model) throws IOException {

        ObjectId fileId = gridFsTemplate.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType()
        );

        MainBanner mainBanner = new MainBanner();
        mainBanner.setImageFileId(fileId.toString());
        mainBanner.setTitle(mainBannerForm.getTitle()); // 입력받은 타이틀 저장
        mainBannerRepository.save(mainBanner);

        // 업로드 성공 메시지 전달
        model.addAttribute("successMessage", "업로드에 성공하였습니다.");

        return "home";
    }

    @GetMapping("/mainBanner/view")
    public String showMainBanners(Model model) {
        List<MainBanner> banners = mainBannerRepository.findAll();
        model.addAttribute("banners", banners);
        return "mainBanner/mainBannerView";
    }

    @GetMapping("/mainBanner/image/{id}")
    public void getImage(@PathVariable String id, HttpServletResponse response) throws IOException {
        GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(id)));
        GridFsResource resource = gridFsTemplate.getResource(file);
        assert file.getMetadata() != null;
        response.setContentType(file.getMetadata().get("_contentType").toString());
        StreamUtils.copy(resource.getInputStream(), response.getOutputStream());
    }

    @GetMapping("/mainBanner")
    public String mainBanner(@RequestParam(required = false) String titleInput, Model model) {
        model.addAttribute("titleInput", titleInput);
        return "/mainBanner/mainBannerView"; // 위 HTML 파일명
    }

    @GetMapping("/mainBanner/image/title/{title}")
    public void getImageByTitle(@PathVariable String title, HttpServletResponse response) throws IOException {
        List<MainBanner> banners = mainBannerRepository.findByTitle(title);

        if (!banners.isEmpty()) {
            MainBanner banner = banners.getFirst(); // 첫 번째만 사용하거나, 원하는 방식으로 선택
            String fileId = banner.getImageFileId();
            GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)));
            GridFsResource resource = gridFsTemplate.getResource(file);
            response.setContentType(resource.getContentType());
            StreamUtils.copy(resource.getInputStream(), response.getOutputStream());
            return;
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}
