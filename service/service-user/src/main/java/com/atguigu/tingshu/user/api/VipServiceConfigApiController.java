package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.service.VipServiceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "VIP服务配置管理接口")
@RestController
@RequestMapping("api/user/vipServiceConfig")
@SuppressWarnings({"unchecked", "rawtypes"})
public class VipServiceConfigApiController {

    @Autowired
    private VipServiceConfigService vipServiceConfigService;

    @Operation(summary = "获取 VIP 服务配置信息")
    @GetMapping("/findAll")
    public Result findAllVipServiceConfig() {
        List<VipServiceConfig> list = vipServiceConfigService.list();
        return Result.ok(list);
    }

    /**
     * 根据 vipServiceConfigId 获取 VIP 服务配置信息
     *
     * @param vipServiceConfigId
     * @return
     */
    @Operation(summary = "根据 vipServiceConfigId 获取 VIP 服务配置信息")
    @GetMapping("/getVipServiceConfig/{vipServiceConfigId}")
    Result<VipServiceConfig> getVipServiceConfig(@PathVariable("vipServiceConfigId") Long vipServiceConfigId) {
        VipServiceConfig vipServiceConfig = vipServiceConfigService.getById(vipServiceConfigId);
        return Result.ok(vipServiceConfig);
    }
}

