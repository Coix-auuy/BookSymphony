package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.config.ThreadPoolExecutorConfig;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class ItemServiceImpl implements ItemService {
    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;
    @Autowired
    private CategoryFeignClient categoryFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private UserInfoFeignClient userInfoFeignClient;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public Map<String, Object> getItem(Long albumId) {
        // 判断布隆过滤器中是否存在专辑 id
        // 保存专辑 id 到布隆过滤器
        HashMap<String, Object> resultMap = new HashMap<>();
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);

        if (!bloomFilter.contains(albumId)) {
            // 不包含，返回空数据，保存了缓存和数据库
            return resultMap;
        }

        // 远程调用获取所需数据
        // 获取统计数据
        CompletableFuture<Void> albumStatCompletableFuture = CompletableFuture.runAsync(() -> {
            Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStatVo(albumId);
            Assert.notNull(albumStatVoResult, "专辑统计结果集为空");
            AlbumStatVo albumStatVo = albumStatVoResult.getData();
            Assert.notNull(albumStatVo, "专辑统计信息为空");
            resultMap.put("albumStatVo", albumStatVo);
        }, threadPoolExecutor);
        // 获取专辑数据
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            Assert.notNull(albumInfoResult, "专辑结果集为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑信息为空");
            // 数据存储
            resultMap.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolExecutor);

        // 获取分类数据
        CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<BaseCategoryView> baseCategoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
            Assert.notNull(baseCategoryViewResult, "分类信息结果集为空");
            BaseCategoryView baseCategoryView = baseCategoryViewResult.getData();
            Assert.notNull(baseCategoryView, "分类信息为空");
            resultMap.put("baseCategoryView", baseCategoryView);
        }, threadPoolExecutor);
        // 获取主播数据
        CompletableFuture<Void> userInfoCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(albumInfo.getUserId());
            Assert.notNull(userInfoVoResult, "用户结果集为空");
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            Assert.notNull(userInfoVo, "用户信息为空");
            resultMap.put("announcer", userInfoVo);
        }, threadPoolExecutor);
        // 多任务组合
        CompletableFuture.allOf(albumStatCompletableFuture, albumInfoCompletableFuture, categoryCompletableFuture, userInfoCompletableFuture).join();

        return resultMap;
    }
}
