package com.datakhaos.visual.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.common.model.R;
import com.datakhaos.visual.entity.PortraitTag;
import com.datakhaos.visual.entity.PortraitTagCategory;
import com.datakhaos.visual.entity.PortraitUserTag;
import com.datakhaos.visual.service.PortraitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户画像管理 API（分类 / 标签 / 用户标签值 / 画像查询 / 统计）
 */
@RestController
@RequestMapping("/api/visual/portrait")
@RequiredArgsConstructor
public class PortraitController {

    private final PortraitService portraitService;

    /* ==================== 标签分类 ==================== */
    @GetMapping("/category/list")
    public R<List<PortraitTagCategory>> listCategories() {
        return R.ok(portraitService.listCategories());
    }

    @PostMapping("/category")
    public R<String> createCategory(@RequestBody PortraitTagCategory c) {
        return R.ok(portraitService.createCategory(c));
    }

    @PutMapping("/category/{id}")
    public R<Void> updateCategory(@PathVariable String id, @RequestBody PortraitTagCategory c) {
        portraitService.updateCategory(id, c);
        return R.ok();
    }

    @DeleteMapping("/category/{id}")
    public R<Void> deleteCategory(@PathVariable String id) {
        portraitService.deleteCategory(id);
        return R.ok();
    }

    /* ==================== 标签定义 ==================== */
    @GetMapping("/tag/page")
    public R<Page<PortraitTag>> pageTags(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String keyword) {
        return R.ok(portraitService.pageTags(new Page<>(current, size), categoryId, keyword));
    }

    @GetMapping("/tag/list")
    public R<List<PortraitTag>> listTags(@RequestParam(required = false) String categoryId) {
        return R.ok(portraitService.listTags(categoryId));
    }

    @GetMapping("/tag/{id}")
    public R<PortraitTag> getTag(@PathVariable String id) {
        return R.ok(portraitService.getTag(id));
    }

    @PostMapping("/tag")
    public R<String> createTag(@RequestBody PortraitTag tag) {
        return R.ok(portraitService.createTag(tag));
    }

    @PutMapping("/tag/{id}")
    public R<Void> updateTag(@PathVariable String id, @RequestBody PortraitTag tag) {
        portraitService.updateTag(id, tag);
        return R.ok();
    }

    @DeleteMapping("/tag/{id}")
    public R<Void> deleteTag(@PathVariable String id) {
        portraitService.deleteTag(id);
        return R.ok();
    }

    /* ==================== 用户标签值 ==================== */
    @PostMapping("/user-tag")
    public R<Void> upsertUserTag(@RequestBody PortraitUserTag ut) {
        portraitService.upsertUserTag(ut);
        return R.ok();
    }

    @DeleteMapping("/user-tag/{id}")
    public R<Void> deleteUserTag(@PathVariable String id) {
        portraitService.deleteUserTag(id);
        return R.ok();
    }

    /** 查询某个用户的画像标签 */
    @GetMapping("/user/{userKey}")
    public R<List<PortraitUserTag>> userTags(@PathVariable String userKey) {
        return R.ok(portraitService.userTags(userKey));
    }

    /* ==================== 画像统计 ==================== */
    @GetMapping("/tag/distribution")
    public R<List<Map<String, Object>>> tagDistribution(@RequestParam String tagId) {
        return R.ok(portraitService.tagDistribution(tagId));
    }
}