package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value = "/api/album/category")
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryApiController {

    @Autowired
    private BaseCategoryService baseCategoryService;

    @Operation(tags = "查询所有分类数据")
    @GetMapping("/getBaseCategoryList")
    public Result getBaseCategoryList() {
        // 调用服务层的查询分类方法
        List<JSONObject> categoryList = baseCategoryService.getBaseCategoryList();
        // 将数据返回给页面
        return Result.ok(categoryList);
    }

    @Operation(tags = "根据一级分类 id 获取属性标签数据")
    @GetMapping("/findAttribute/{category1Id}")
    public Result findAttribute(@PathVariable Long category1Id) {
        List<BaseAttribute> result = baseCategoryService.findAttribute(category1Id);
        return Result.ok(result);
    }
}

