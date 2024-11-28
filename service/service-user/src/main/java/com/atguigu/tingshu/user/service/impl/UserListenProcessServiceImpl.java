package com.atguigu.tingshu.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private KafkaService kafkaService;

    @Override
    public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {
        // 从 MongoDB 中获取: 数据库、集合、条件
        Query query = Query.query(Criteria.where("trackId").is(trackId));
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        if (null != userListenProcess) {
            return userListenProcess.getBreakSecond();
        }
        return new BigDecimal("0");
    }

    @Override
    public void updateListenProcess(UserListenProcessVo userListenProcessVo, Long userId) {
        // MongoDB 中是否已有相应用户相应声音的数据
        Query query = Query.query(Criteria.where("trackId").is(userListenProcessVo.getTrackId()));
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        if (null != userListenProcess) {
            userListenProcess.setUpdateTime(new Date());
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
        } else {
            userListenProcess = new UserListenProcess();
            userListenProcess.setId(ObjectId.get().toString());
            userListenProcess.setUserId(userId);
            userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
            userListenProcess.setTrackId(userListenProcessVo.getTrackId());
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setUpdateTime(new Date());
            userListenProcess.setIsShow(1);
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
        }
        mongoTemplate.save(userListenProcess, collectionName);

        // 防止频繁更新声音的播放量，同一用户，在 1 天之内无论播放多少次都只算一次 redis setnx key value 当 key 不存在时生效
        String key = userId + "_" + userListenProcessVo.getTrackId();
        // 具有原子性! set key value ex 84600
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, 1, 1, TimeUnit.DAYS);
        if (result) {
            // 第一次执行，播放量 + 1
            // 更新声音、专辑播放量 kafka 异步 解耦
            TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
            trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
            trackStatMqVo.setTrackId(userListenProcessVo.getTrackId());
            trackStatMqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
            trackStatMqVo.setCount(1);
            trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-", ""));

            // 发送消息
            kafkaService.sendMsg(KafkaConstant.QUEUE_TRACK_STAT_UPDATE, JSON.toJSONString(trackStatMqVo));
        }
    }

    @Override
    public Map<String, Object> getLatelyTrack(Long userId) {
        // 根据用户 id 获取播放记录并按照更新时间进行降序排序，取第一条记录
        UserListenProcess userListenProcess = mongoTemplate.findOne(Query.query(Criteria.where("userId").is(userId)).with(Sort.by(Sort.Direction.DESC, "updateTime")).limit(1), UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        // 创建 map
        if (null == userListenProcess) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("albumId", userListenProcess.getAlbumId());
        map.put("trackId", userListenProcess.getTrackId());

        return map;
    }
}
