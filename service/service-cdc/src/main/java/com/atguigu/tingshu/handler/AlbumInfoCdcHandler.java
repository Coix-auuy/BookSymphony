package com.atguigu.tingshu.handler;

import com.atguigu.tingshu.model.CDCEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

/**
 * Author HeZx
 * Time 2024/12/10 10:49
 * Version 1.0
 * Description:
 */
@Slf4j
@Component
@CanalTable("album_info")
public class AlbumInfoCdcHandler implements EntryHandler<CDCEntity> {
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void insert(CDCEntity cdcEntity) {
        // EntryHandler.super.insert(cdcEntity);
    }

    @Override
    public void update(CDCEntity before, CDCEntity after) {
        log.info("监听到数据修改，ID: {}", after.getId());
        // 主要任务就是删除缓存数据
        String key = "album:[" + after.getId() + "]";
        redisTemplate.delete(key);
    }

    @Override
    public void delete(CDCEntity cdcEntity) {
        log.info("监听到数据删除，ID:{}", cdcEntity.getId());
        String key = "album:[" + cdcEntity.getId() + "]";
        redisTemplate.delete(key);
    }
}
