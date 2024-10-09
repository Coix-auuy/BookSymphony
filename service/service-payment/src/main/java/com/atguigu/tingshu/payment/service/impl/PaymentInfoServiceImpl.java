package com.bigwharf.tingshu.payment.service.impl;

import com.bigwharf.tingshu.model.payment.PaymentInfo;
import com.bigwharf.tingshu.payment.mapper.PaymentInfoMapper;
import com.bigwharf.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

}
