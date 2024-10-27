package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    // 重新注入换成主机的对象，不能使用系统自带的对象
    @Autowired
    private WxMaService wxMaService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private KafkaService kafkaService;

    /**
     * 微信小程序登录
     *
     * @param code
     * @return
     */
    @Override
    // @Transactional(rollbackFor = Exception.class) 只能处理自己的服务
    public Map<String, Object> wxLogin(String code) {
        // 通过 code 获取 openId (每个微信用户对应一个唯一的 openId)
        String openid = null;
        try {
            openid = wxMaService.jsCode2SessionInfo(code).getOpenid();
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
        // 通过 openId 查询用户是否注册过
        UserInfo userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getWxOpenId, openid));
        if (null == userInfo) {
            // 用户未注册，注册用户
            userInfo = new UserInfo();
            userInfo.setWxOpenId(openid);
            userInfo.setNickname("听友" + System.currentTimeMillis());
            //  赋值用户头像图片
            userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            // 保存数据
            userInfoMapper.insert(userInfo);
            // 初始化用户的账户信息 跨服务远程调用 --> 同步操作。
            // 异步：能够提升效率，实现解耦 --> kafka
            // kafka 发送消息需要传递的消息：1. 消息主题 topic，2. 消息内容 --> 由消费者的需求决定，即要实现功能需要的数据
            UserAccount userAccount = new UserAccount();
            kafkaService.sendMsg(KafkaConstant.QUEUE_USER_REGISTER, userInfo.getId().toString());
        }
        // 不为空，注册过，返回 token，并将数据放入缓存
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        // 声明一个 key 来记录
        String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
        // 这里可以直接放入 userInfo 是因为在公共类 RedisConfig 中设置了序列化器
        redisTemplate.opsForValue().set(loginKey, userInfo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
        HashMap<String, Object> map = new HashMap<>();
        map.put("token", token);
        return map;
    }

    @Override
    public UserInfoVo getUserInfo(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (null == userInfo) {
            return null;
        }
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo, userInfoVo);
        return userInfoVo;
    }

    @Override
    public void updateUser(UserInfoVo userInfoVo) {
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(userInfoVo, userInfo);
        this.updateById(userInfo);
    }
}
