package com.atguigu.tingshu.album.client.impl;


import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlbumInfoDegradeFeignClient implements AlbumInfoFeignClient {


    @Override
    public Result<AlbumInfo> getAlbumInfo(Long albumId) {
        // 创建专辑对象
        AlbumInfo albumInfo = new AlbumInfo();
        albumInfo.setAlbumTitle("测试对象");
        albumInfo.setAlbumIntro("专辑走丢了...");
        return Result.ok(albumInfo);

    }

    @Override
    public Result<List<AlbumAttributeValue>> findAlbumAttributeValue(Long albumId) {
        System.out.println("调用失败");
        return null;
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        System.out.println("调用失败");
        return null;
    }
}
