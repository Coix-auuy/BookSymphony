package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Override
    public TrackMediaInfoVo getMediaInfo(String mediaFileId) {
        // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
        // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
        // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
        Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
        // 实例化要请求产品的client对象,clientProfile是可选的
        VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
        // 实例化一个请求对象,每个接口都会对应一个request对象
        DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
        String[] fileIds1 = {mediaFileId};
        req.setFileIds(fileIds1);
        // 可选
        req.setSubAppId(vodConstantProperties.getAppId());
        // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
        DescribeMediaInfosResponse resp = null;
        try {
            resp = client.DescribeMediaInfos(req);
            // 创建对象
            TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();

            MediaInfo mediaInfo = resp.getMediaInfoSet()[0];
            // 给 trackMediaInfoVo 赋值
            trackMediaInfoVo.setType(mediaInfo.getBasicInfo().getType());
            trackMediaInfoVo.setDuration(mediaInfo.getMetaData().getDuration());
            trackMediaInfoVo.setMediaUrl(mediaInfo.getBasicInfo().getMediaUrl());
            trackMediaInfoVo.setSize(mediaInfo.getMetaData().getSize());
            return trackMediaInfoVo;
        } catch (TencentCloudSDKException e) {
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
            throw new RuntimeException(e);
        }

    }

    @Override
    public void removeMedia(String mediaFileId) {
        // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
        // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
        // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
        Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());

        // 实例化要请求产品的client对象,clientProfile是可选的
        VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
        // 实例化一个请求对象,每个接口都会对应一个request对象
        DeleteMediaRequest req = new DeleteMediaRequest();
        req.setFileId(mediaFileId);
        req.setSubAppId(vodConstantProperties.getAppId());
        // 返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
        DeleteMediaResponse resp = null;
        try {
            resp = client.DeleteMedia(req);
        } catch (TencentCloudSDKException e) {
            throw new RuntimeException(e);
        }
        // 输出json格式的字符串回包
        System.out.println(AbstractModel.toJsonString(resp));
    }
}
