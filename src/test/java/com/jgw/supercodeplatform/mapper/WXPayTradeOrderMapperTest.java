package com.jgw.supercodeplatform.mapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jgw.supercodeplatform.SuperCodeMarketingApplication;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingMembersWinRecordMapper;
import com.jgw.supercodeplatform.marketing.dao.weixin.WXPayTradeOrderMapper;
import com.jgw.supercodeplatform.marketing.pojo.MarketingMembersWinRecord;
import com.jgw.supercodeplatform.marketing.pojo.pay.WXPayTradeOrder;

@RunWith(SpringJUnit4ClassRunner.class) // SpringJUnit支持，由此引入Spring-Test框架支持！
@SpringBootTest(classes = SuperCodeMarketingApplication.class) // 指定我们SpringBoot工程的Application启动类
public class WXPayTradeOrderMapperTest {
@Autowired
private WXPayTradeOrderMapper dao;
@Autowired
private MarketingMembersWinRecordMapper mWinRecordMapper;

	@Test
	public void contextLoads() {
		WXPayTradeOrder w=new WXPayTradeOrder();
		w.setAmount(11);
		w.setOpenId("ssss");
		w.setErrCodeDes("签名错误");
		w.setErrCode("errorcode");
		w.setTradeStatus((byte)1);
		w.setReturnMsg("returnmsg");
		w.setReturnCode("returncode");
		w.setResultCode("resultcode");
		w.setPartnerTradeNo("12458774");
		dao.insert(w);
	}

	@Test
	public void selectByNo() {
		WXPayTradeOrder w=dao.selectByTradeNo("12458774");
		System.out.println(w);
	}
	
	@Test
	public void insert() {
		MarketingMembersWinRecord record=new MarketingMembersWinRecord();
		record.setActivityId(99L);
		record.setMobile("15421");
		mWinRecordMapper.addWinRecord(record);
	}
}
