package com.atguigu.tingshu.album.client.impl;


import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrackInfoDegradeFeignClient implements TrackInfoFeignClient {

    @Override
    public Result<List<TrackInfo>> findPaidTrackInfoList(Long trackId, Integer trackCount) {
        System.out.println("调用失败！");
        return null;
    }

    @Override
    public Result<TrackInfo> getTrackInfo(Long trackId) {
        System.out.println("调用失败！");
        return null;
    }
}
