package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.*;
import com.atguigu.tingshu.user.factory.StrategyFactory;
import com.atguigu.tingshu.user.factory.impl.PaymentStrategyFactory;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.strategy.PaymentStrategy;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.joda.time.LocalDateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;
    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;
    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;
    @Autowired
    private UserVipServiceMapper userVipServiceMapper;
    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;
    @Autowired
    private StrategyFactory strategyFactory;

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
    public Map<Long, Integer> userIsPaidTrack(Long albumId, List<Long> tarckIdList, Long userId) {
        // 查看用户是否购买过专辑
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getAlbumId, albumId).eq(UserPaidAlbum::getUserId, userId));
        Map<Long, Integer> map;
        if (null != userPaidAlbum) {
            map = tarckIdList.stream().collect(Collectors.toMap(trackId -> trackId, trackId -> 1));
        } else {
            // 判断用户是否购买过当前专辑下的声音
            LambdaQueryWrapper<UserPaidTrack> userPaidTrackQueryWrapper = new LambdaQueryWrapper<>();
            userPaidTrackQueryWrapper.eq(UserPaidTrack::getUserId, userId).eq(UserPaidTrack::getAlbumId, albumId).in(UserPaidTrack::getTrackId, tarckIdList);
            List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(userPaidTrackQueryWrapper);
            map = tarckIdList.stream().collect(Collectors.toMap(trackId -> trackId, trackId -> 0));
            if (CollectionUtils.isNotEmpty(userPaidTrackList)) {
                for (UserPaidTrack userPaidTrack : userPaidTrackList) {
                    if (map.containsKey(userPaidTrack.getTrackId())) {
                        map.put(userPaidTrack.getTrackId(), 1);
                    }
                }
            }
        }
        return map;
    }

    @Override
    public void updateUser(UserInfoVo userInfoVo) {
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(userInfoVo, userInfo);
        this.updateById(userInfo);
    }

    @Override
    public Boolean isPaidAlbum(Long albumId, Long userId) {
        // 查看用户是否购买过专辑
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getAlbumId, albumId).eq(UserPaidAlbum::getUserId, userId));
        return null != userPaidAlbum;
    }

    @Override
    public List<Long> findUserPaidTrackList(Long albumId, Long userId) {
        LambdaQueryWrapper<UserPaidTrack> userPaidTrackQueryWrapper = new LambdaQueryWrapper<>();
        userPaidTrackQueryWrapper.eq(UserPaidTrack::getAlbumId, albumId).eq(UserPaidTrack::getUserId, userId);
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(userPaidTrackQueryWrapper);
        List<Long> TrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
        return TrackIdList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userPayRecord(UserPaidRecordVo userPaidRecordVo) {
        // 调用工厂实现类
        PaymentStrategy strategy = strategyFactory.getStrategy(userPaidRecordVo.getItemType());
        // 调用策略方法
        strategy.processPayment(userPaidRecordVo);


        // // 判断用户购买类型
        // if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(userPaidRecordVo.getItemType())) {
        //     // 专辑
        //     // 先查询这个用户记录表，是否已经存储数据了，防止重复消费
        //     UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo()));
        //
        //     if (null != userPaidAlbum) {
        //         return;
        //     }
        //     userPaidAlbum = new UserPaidAlbum();
        //     userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
        //     userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
        //     userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
        //     userPaidAlbumMapper.insert(userPaidAlbum);
        //
        // } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(userPaidRecordVo.getItemType())) {
        //     // 声音
        //
        //     Long count = userPaidTrackMapper.selectCount(new LambdaQueryWrapper<UserPaidTrack>().eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo()));
        //     if (count != 0) {
        //         return;
        //     }
        //     // 保存数据
        //     // 远程调用获取专辑 Id
        //     Result<TrackInfo> trackInfoResult = trackInfoFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0));
        //     Assert.notNull(trackInfoResult, "声音信息结果集为空");
        //     TrackInfo trackInfo = trackInfoResult.getData();
        //     Assert.notNull(trackInfo, "声音信息为空");
        //     userPaidRecordVo.getItemIdList().forEach(trackId -> {
        //         UserPaidTrack userPaidTrack = new UserPaidTrack();
        //         userPaidTrack.setUserId(userPaidRecordVo.getUserId());
        //         userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
        //         userPaidTrack.setAlbumId(trackInfo.getAlbumId());
        //         userPaidTrack.setTrackId(trackId);
        //         userPaidTrackMapper.insert(userPaidTrack);
        //     });
        //
        //
        // } else if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(userPaidRecordVo.getItemType())) {
        //     // VIP
        //     UserVipService userVipService = userVipServiceMapper.selectOne(new LambdaQueryWrapper<UserVipService>().eq(UserVipService::getOrderNo, userPaidRecordVo.getOrderNo()));
        //
        //     if (null != userVipService) {
        //         return;
        //     }
        //     // 查询用户购买的 vip 时间
        //     VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(userPaidRecordVo.getItemIdList().get(0));
        //     Integer serviceMonth = vipServiceConfig.getServiceMonth();
        //     // 保存数据
        //     userVipService = new UserVipService();
        //     userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
        //     userVipService.setUserId(userPaidRecordVo.getUserId());
        //     userVipService.setStartTime(new Date());
        //     // 过期时间
        //     // 第一次购买：当前时间 + 购买月份；续费：没过期 = 当前过期时间 + 续费时间，过期了 = 当前时间 + 购买月份
        //     // 查询购买记录
        //     UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
        //     Date expireTime;
        //     if (null != userInfo && userInfo.getIsVip() == 1 && userInfo.getVipExpireTime().after(new Date())) {
        //         // 续费
        //         LocalDateTime localDate = new LocalDateTime(userInfo.getVipExpireTime());
        //         expireTime = localDate.plusMonths(serviceMonth).toDate();
        //     } else {
        //         expireTime = new LocalDateTime().plusMonths(serviceMonth).toDate();
        //     }
        //     userVipService.setExpireTime(expireTime);
        //     // int i = 1/0;
        //     userVipServiceMapper.insert(userVipService);
        //     // 修改用户状态 普通用户 --> VIP
        //     userInfo.setIsVip(1);
        //     userInfo.setVipExpireTime(expireTime);
        //     userInfoMapper.updateById(userInfo);
        // }

    }

    @Override
    public void updateVipExpireStatus() {
        // 1. 查询哪些是 VIP 失效的用户并将 is_vip 设置为 0
        userInfoMapper.updateVipExpireStatus();
    }
}
