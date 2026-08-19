package com.datakhaos.visual.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.visual.entity.PortraitTag;
import com.datakhaos.visual.entity.PortraitTagCategory;
import com.datakhaos.visual.entity.PortraitUserTag;
import com.datakhaos.visual.mapper.PortraitTagCategoryMapper;
import com.datakhaos.visual.mapper.PortraitTagMapper;
import com.datakhaos.visual.mapper.PortraitUserTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户画像服务：标签分类 / 标签定义 / 用户标签值 / 画像查询 / 标签分布统计
 */
@Service
@RequiredArgsConstructor
public class PortraitService {

    private final PortraitTagCategoryMapper categoryMapper;
    private final PortraitTagMapper tagMapper;
    private final PortraitUserTagMapper userTagMapper;

    /* ==================== 标签分类 ==================== */

    public List<PortraitTagCategory> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<PortraitTagCategory>()
                .orderByAsc(PortraitTagCategory::getSortOrder)
                .orderByAsc(PortraitTagCategory::getCreateTime));
    }

    @Transactional
    public String createCategory(PortraitTagCategory c) {
        c.setId(UUID.randomUUID().toString().replace("-", ""));
        if (c.getSortOrder() == null) c.setSortOrder(0);
        if (c.getStatus() == null) c.setStatus(1);
        fillTs(c);
        categoryMapper.insert(c);
        return c.getId();
    }

    @Transactional
    public void updateCategory(String id, PortraitTagCategory c) {
        PortraitTagCategory e = categoryMapper.selectById(id);
        if (e == null) throw new RuntimeException("标签分类不存在: " + id);
        if (StringUtils.hasText(c.getName())) e.setName(c.getName());
        if (StringUtils.hasText(c.getCode())) e.setCode(c.getCode());
        if (c.getSortOrder() != null) e.setSortOrder(c.getSortOrder());
        if (c.getStatus() != null) e.setStatus(c.getStatus());
        e.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(e);
    }

    @Transactional
    public void deleteCategory(String id) {
        categoryMapper.deleteById(id);
        // 级联删除该分类下的标签及其用户标签值
        List<PortraitTag> tags = tagMapper.selectList(new LambdaQueryWrapper<PortraitTag>().eq(PortraitTag::getCategoryId, id));
        for (PortraitTag t : tags) {
            userTagMapper.delete(new LambdaQueryWrapper<PortraitUserTag>().eq(PortraitUserTag::getTagId, t.getId()));
            tagMapper.deleteById(t.getId());
        }
    }

    /* ==================== 标签定义 ==================== */

    public Page<PortraitTag> pageTags(Page<PortraitTag> page, String categoryId, String keyword) {
        return tagMapper.selectPage(page, new LambdaQueryWrapper<PortraitTag>()
                .eq(StringUtils.hasText(categoryId), PortraitTag::getCategoryId, categoryId)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(PortraitTag::getName, keyword).or().like(PortraitTag::getCode, keyword))
                .orderByDesc(PortraitTag::getUpdateTime));
    }

    public List<PortraitTag> listTags(String categoryId) {
        return tagMapper.selectList(new LambdaQueryWrapper<PortraitTag>()
                .eq(StringUtils.hasText(categoryId), PortraitTag::getCategoryId, categoryId)
                .orderByDesc(PortraitTag::getUpdateTime));
    }

    public PortraitTag getTag(String id) {
        return tagMapper.selectById(id);
    }

    @Transactional
    public String createTag(PortraitTag t) {
        t.setId(UUID.randomUUID().toString().replace("-", ""));
        if (t.getTagType() == null) t.setTagType("STR");
        if (t.getStatus() == null) t.setStatus(1);
        fillTs(t);
        tagMapper.insert(t);
        return t.getId();
    }

    @Transactional
    public void updateTag(String id, PortraitTag t) {
        PortraitTag e = tagMapper.selectById(id);
        if (e == null) throw new RuntimeException("标签不存在: " + id);
        if (StringUtils.hasText(t.getName())) e.setName(t.getName());
        if (StringUtils.hasText(t.getCode())) e.setCode(t.getCode());
        if (StringUtils.hasText(t.getTagType())) e.setTagType(t.getTagType());
        if (t.getUnit() != null) e.setUnit(t.getUnit());
        if (t.getEnumOptions() != null) e.setEnumOptions(t.getEnumOptions());
        if (t.getDescription() != null) e.setDescription(t.getDescription());
        if (t.getStatus() != null) e.setStatus(t.getStatus());
        if (StringUtils.hasText(t.getCategoryId())) e.setCategoryId(t.getCategoryId());
        e.setUpdateTime(LocalDateTime.now());
        tagMapper.updateById(e);
    }

    @Transactional
    public void deleteTag(String id) {
        userTagMapper.delete(new LambdaQueryWrapper<PortraitUserTag>().eq(PortraitUserTag::getTagId, id));
        tagMapper.deleteById(id);
    }

    /* ==================== 用户标签值 ==================== */

    @Transactional
    public void upsertUserTag(PortraitUserTag ut) {
        if (!StringUtils.hasText(ut.getUserKey())) throw new RuntimeException("用户标识不能为空");
        if (!StringUtils.hasText(ut.getTagId())) throw new RuntimeException("标签不能为空");
        PortraitUserTag exist = userTagMapper.selectOne(new LambdaQueryWrapper<PortraitUserTag>()
                .eq(PortraitUserTag::getUserKey, ut.getUserKey())
                .eq(PortraitUserTag::getTagId, ut.getTagId()));
        if (exist != null) {
            exist.setTagValue(ut.getTagValue());
            exist.setUserName(ut.getUserName());
            if (ut.getTagTime() != null) exist.setTagTime(ut.getTagTime());
            exist.setUpdateTime(LocalDateTime.now());
            userTagMapper.updateById(exist);
        } else {
            ut.setId(UUID.randomUUID().toString().replace("-", ""));
            if (ut.getTagTime() == null) ut.setTagTime(LocalDateTime.now());
            ut.setCreateTime(LocalDateTime.now());
            ut.setUpdateTime(LocalDateTime.now());
            userTagMapper.insert(ut);
        }
    }

    @Transactional
    public void deleteUserTag(String id) {
        userTagMapper.deleteById(id);
    }

    /** 查询某个用户的全部标签值 */
    public List<PortraitUserTag> userTags(String userKey) {
        return userTagMapper.selectList(new LambdaQueryWrapper<PortraitUserTag>()
                .eq(PortraitUserTag::getUserKey, userKey)
                .orderByDesc(PortraitUserTag::getUpdateTime));
    }

    /** 按标签分布统计（各取值对应的用户数） */
    public List<Map<String, Object>> tagDistribution(String tagId) {
        List<PortraitUserTag> rows = userTagMapper.selectList(new LambdaQueryWrapper<PortraitUserTag>()
                .eq(PortraitUserTag::getTagId, tagId)
                .select(PortraitUserTag::getTagValue, PortraitUserTag::getUserKey));
        Map<String, Integer> counter = new LinkedHashMap<>();
        int total = 0;
        for (PortraitUserTag r : rows) {
            String v = r.getTagValue() == null || r.getTagValue().isEmpty() ? "未知" : r.getTagValue();
            counter.merge(v, 1, Integer::sum);
            total++;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counter.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", e.getKey());
            item.put("count", e.getValue());
            item.put("ratio", total == 0 ? 0 : (double) Math.round(e.getValue() * 1000.0 / total) / 10.0);
            result.add(item);
        }
        return result;
    }

    private void fillTs(PortraitTagCategory e) {
        e.setCreateTime(LocalDateTime.now());
        e.setUpdateTime(LocalDateTime.now());
    }

    private void fillTs(PortraitTag e) {
        e.setCreateTime(LocalDateTime.now());
        e.setUpdateTime(LocalDateTime.now());
    }
}