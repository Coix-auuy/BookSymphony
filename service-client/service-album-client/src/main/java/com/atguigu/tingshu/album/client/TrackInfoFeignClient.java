package com.atguigu.tingshu.album.client;

import com.atguigu.tingshu.album.client.impl.TrackInfoDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import feign.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-album", fallback = TrackInfoDegradeFeignClient.class)
public interface TrackInfoFeignClient {


    /**
     * 获取下单付费声音列表
     * @param trackId
     * @param trackCount
     * @return
     */
    @GetMapping("api/album/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
    Result<List<TrackInfo>> findPaidTrackInfoList(@PathVariable Long trackId, @PathVariable Integer trackCount);

    /**
     * 根据 trackId 获取声音信息
     * @param trackId
     * @return
     */
    @GetMapping("api/album/trackInfo/getTrackInfo/{trackId}")
    Result<TrackInfo> getTrackInfo(@PathVariable("trackId") Long trackId);
}