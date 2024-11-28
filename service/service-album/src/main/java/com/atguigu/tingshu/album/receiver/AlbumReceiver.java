package com.atguigu.tingshu.album.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.mysql.cj.util.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Author HeZx
 * Time 2024/11/27 17:40
 * Version 1.0
 * Description:
 */
@Component
public class AlbumReceiver {
    @Autowired
    private AlbumInfoService albumInfoService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 更新声音、专辑播放量
     * @param consumerRecord
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_TRACK_STAT_UPDATE)
    public void trackStatUpdate(ConsumerRecord<String, String> consumerRecord) {
        // 获取发送的消息
        String strJson = consumerRecord.value();
        if (!StringUtils.isNullOrEmpty(strJson)) {
            // 将 Json 字符串转换为对象
            TrackStatMqVo trackStatMqVo = JSON.parseObject(strJson, TrackStatMqVo.class);
            // 业务编号：去重使用
            String businessNo = trackStatMqVo.getBusinessNo();
            Boolean result = redisTemplate.opsForValue().setIfAbsent(businessNo, 1, 1, TimeUnit.DAYS);
            if(Boolean.TRUE.equals(result)) { // 第一次执行
                // 更新
                try {
                    albumInfoService.trackStatUpdate(trackStatMqVo);
                } catch (Exception e) {
                    // 如果有异常删除原有的 key
                    redisTemplate.delete(businessNo);
                    throw new RuntimeException(e);
                }
            }

        }

    }
}
