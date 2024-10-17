package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import kotlin.collections.ArrayDeque;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private BaseAttributeMapper baseAttributeMapper;

    @Autowired
    private BaseAttributeValueMapper baseAttributeValueMapper;

    @Override
    public List<JSONObject> getBaseCategoryList() {
        // 创建要返回的集合数据
        List<JSONObject> result = new ArrayDeque<>();
        // 获取所有的分类信息
        List<BaseCategoryView> categoryViewList = baseCategoryViewMapper.selectList(null);
        // 根据 1 级分类 id 将信息分组
        Map<Long, List<BaseCategoryView>> map1 = categoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : map1.entrySet()) {
            // 获取 1 级分类 id
            Long category1Id = entry1.getKey();
            // 获取当前 1 级分类 id 对应的所有数据
            List<BaseCategoryView> category1ViewList = entry1.getValue();
            // 声明 1 级分类对象
            JSONObject category1 = new JSONObject();
            category1.put("categoryId", category1Id);
            category1.put("categoryName", category1ViewList.get(0).getCategory1Name());
            // 将该 1 级分类下的所有数据按照 2 级 id 分组
            Map<Long, List<BaseCategoryView>> map2 = category1ViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            // 将 2 级分类数据封装为一个列表
            ArrayList<JSONObject> category2Child = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : map2.entrySet()) {
                // 获取 2 级分类 id
                Long category2Id = entry2.getKey();
                // 获取当前 2 级分类 id 对应的所有数据
                List<BaseCategoryView> category2ViewList = entry2.getValue();
                // 声明 2 级分类对象
                JSONObject category2 = new JSONObject();
                category2.put("categoryId", category2Id);
                category2.put("categoryName", category2ViewList.get(0).getCategory2Name());
                // 封装所有的 3 级分类数据
                List<JSONObject> category3Child = category2ViewList.stream().map(baseCategoryView -> {
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId", baseCategoryView.getCategory3Id());
                    category3.put("categoryName", baseCategoryView.getCategory3Name());
                    return category3;
                }).collect(Collectors.toList());
                category2.put("categoryChild", category3Child);
                // 将该 2 级分类数据加入集合
                category2Child.add(category2);
            }
            category1.put("categoryChild", category2Child);
            result.add(category1);
        }
        return result;
    }

    @Override
    public List<BaseAttribute> findAttribute(Long category1Id) {
        // 根据一级分类 id 获取属性标签数据
        return baseAttributeMapper.selectAttribute(category1Id);
    }
}
