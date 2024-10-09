package com.bigwharf.tingshu.payment.service.impl;

import com.bigwharf.tingshu.payment.service.PaymentInfoService;
import com.bigwharf.tingshu.payment.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

	@Autowired
	private PaymentInfoService paymentInfoService;

}
