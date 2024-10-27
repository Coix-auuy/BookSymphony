package com.atguigu.tingshu.user.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Author HeZx
 * Time 2024/10/20 10:50
 * Version 1.0
 * Description:
 */
@Configuration
public class WeChatMpConfig {
    @Autowired
    private WechatAccountConfig wechatAccountConfig;

    @Bean
    public WxMaService wxMaService() {
        // 创建 WxMaConfig 对象
        WxMaDefaultConfigImpl wxMaDefaultConfigImpl = new WxMaDefaultConfigImpl();
        wxMaDefaultConfigImpl.setAppid(wechatAccountConfig.getAppId());
        wxMaDefaultConfigImpl.setSecret(wechatAccountConfig.getAppSecret());

        WxMaServiceImpl wxMaService = new WxMaServiceImpl();
        wxMaService.setWxMaConfig(wxMaDefaultConfigImpl);
        return wxMaService;
    }
}
