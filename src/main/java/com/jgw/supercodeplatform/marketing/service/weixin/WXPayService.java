package com.jgw.supercodeplatform.marketing.service.weixin;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.asyntask.WXPayAsynTask;
import com.jgw.supercodeplatform.marketing.constants.WechatConstants;
import com.jgw.supercodeplatform.marketing.dao.weixin.MarketingWxMerchantsMapper;
import com.jgw.supercodeplatform.marketing.dao.weixin.WXPayTradeOrderMapper;
import com.jgw.supercodeplatform.marketing.mybatisplusdao.MarketingWxMerchantsExtMapper;
import com.jgw.supercodeplatform.marketing.pojo.MarketingWxMerchants;
import com.jgw.supercodeplatform.marketing.pojo.MarketingWxMerchantsExt;
import com.jgw.supercodeplatform.marketing.pojo.pay.WXPayTradeOrder;
import com.jgw.supercodeplatform.marketing.weixinpay.WXPay;
import com.jgw.supercodeplatform.marketing.weixinpay.WXPayConstants.SignType;
import com.jgw.supercodeplatform.marketing.weixinpay.WXPayMarketingConfig;
import com.jgw.supercodeplatform.marketing.weixinpay.WXPayUtil;
import com.jgw.supercodeplatform.marketing.weixinpay.requestparam.OrganizationPayRequestParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.util.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class WXPayService {
     @Autowired
    private MarketingWxMerchantsMapper mWxMerchantsMapper;
    
    @Value("${weixin.certificate.path}")
    private String certificatePath;
    
    private static ExecutorService exec=Executors.newFixedThreadPool(20);
	@Autowired
	private WXPayTradeOrderMapper wXPayTradeOrderMapper;

	@Autowired
	private MarketingWxMerchantsExtMapper marketingWxMerchantsExtMapper;
	/**
     * ?????????????????????
     * @param openid
     * @param spbill_create_ip
     * @param amount
     * @param organizationId
     * @throws Exception
     */
	public void qiyePay(String  openid,String  spbill_create_ip,int amount,String  partner_trade_no, String organizationId) throws Exception {
		if (StringUtils.isBlank(openid) || StringUtils.isBlank(spbill_create_ip)|| StringUtils.isBlank(partner_trade_no)|| StringUtils.isBlank(organizationId)) {
			throw new SuperCodeException("????????????????????????????????????,openid="+openid+",spbill_create_ip="+spbill_create_ip+",partner_trade_no="+partner_trade_no+spbill_create_ip+",organizationId="+organizationId, 500);
		}
		MarketingWxMerchants mWxMerchants=mWxMerchantsMapper.get(organizationId);
		if (null==mWxMerchants) {
			throw new SuperCodeException("????????????"+organizationId+"????????????????????????", 500);
		}
		if (mWxMerchants.getMerchantType() == 1) {
			if (mWxMerchants.getJgwId() != null) {
				mWxMerchants = mWxMerchantsMapper.getJgw(mWxMerchants.getJgwId());
			} else {
				mWxMerchants = mWxMerchantsMapper.getDefaultJgw();
			}
		} else if (StringUtils.isBlank(mWxMerchants.getCertificateAddress())) {
			throw new SuperCodeException("????????????"+organizationId+"???????????????????????????", 500);
		}
		String mechid=mWxMerchants.getMchid();
		String mechappid=mWxMerchants.getMchAppid();
		if (StringUtils.isBlank(mechid) || StringUtils.isBlank(mechappid)) {
			throw new SuperCodeException("???????????????????????????????????????????????????mechid="+mechid+",mechappid="+mechappid, 500);
		}
		String certificatePassword = mWxMerchants.getCertificatePassword();
		String key=mWxMerchants.getMerchantKey();
		//???????????????
		WXPayMarketingConfig config=new WXPayMarketingConfig();
		config.setAppId(mechappid);
		config.setKey(key);
		config.setMchId(mechid);
		if (StringUtils.isBlank(certificatePassword)) {
			config.setCertificatePassword(mWxMerchants.getMchid());
		} else {
			config.setCertificatePassword(certificatePassword);
		}
		String wholePath=certificatePath+File.separator+organizationId+File.separator+mWxMerchants.getCertificateAddress();
		log.info("????????????????????????????????????????????????"+wholePath);
		config.setCertificatePath(wholePath);
		//????????????????????????
		OrganizationPayRequestParam oRequestParam=new OrganizationPayRequestParam();
		oRequestParam.setAmount(amount);
		oRequestParam.setMch_appid(mechappid);
		oRequestParam.setOpenid(openid);
		oRequestParam.setNonce_str(WXPayUtil.generateNonceStr());
		oRequestParam.setPartner_trade_no(partner_trade_no);
		oRequestParam.setSpbill_create_ip(spbill_create_ip);
		oRequestParam.setMchid(mechid);
		//??????????????????????????????map
		Map<String, String> signMap=generateMap(oRequestParam);

		//???????????????sign
		String sign=WXPayUtil.generateSignature(signMap, key, SignType.MD5);
		signMap.put("sign", sign);

		WXPay wxPay=new WXPay(config);
		exec.submit(new WXPayAsynTask(wxPay, WechatConstants.ORGANIZATION_PAY_CHANGE_Suffix_URL, signMap, 1000, 5000));
	}

	/**
	 * ??????????????????
	 * @param openid
	 * @param spbill_create_ip
	 * @param amount
	 * @param partner_trade_no
	 * @param organizationId
	 * @throws Exception
	 */
	public void qiyePayAsycPlatform(String  openid,String  spbill_create_ip,int amount,String  partner_trade_no, String organizationId) throws Exception {
		if (StringUtils.isBlank(openid) || StringUtils.isBlank(spbill_create_ip)|| StringUtils.isBlank(partner_trade_no)|| StringUtils.isBlank(organizationId)) {
			log.info("????????????????????????????????????,openid=" + openid + ",spbill_create_ip=" + spbill_create_ip + ",partner_trade_no=" + partner_trade_no + spbill_create_ip + ",organizationId=" + organizationId);
			return;
		}
		MarketingWxMerchants mWxMerchants=mWxMerchantsMapper.get(organizationId);
		if (null==mWxMerchants) {
			log.info("????????????"+organizationId+"????????????????????????");
			return;
		}
		if (mWxMerchants.getMerchantType() == 1) {
			if (mWxMerchants.getJgwId() != null) {
				mWxMerchants = mWxMerchantsMapper.getJgw(mWxMerchants.getJgwId());
			} else {
				mWxMerchants = mWxMerchantsMapper.getDefaultJgw();
			}
		} else if (StringUtils.isBlank(mWxMerchants.getCertificateAddress())) {
			log.info("????????????"+organizationId+"???????????????????????????");
			return;
		}
		String mechid=mWxMerchants.getMchid();
		String mechappid=mWxMerchants.getMchAppid();
		if (StringUtils.isBlank(mechid) || StringUtils.isBlank(mechappid)) {
			log.info("???????????????????????????????????????????????????mechid="+mechid+",mechappid="+mechappid);
			return;
		}
		String certificatePassword = mWxMerchants.getCertificatePassword();
		String key=mWxMerchants.getMerchantKey();
		//???????????????
		WXPayMarketingConfig config=new WXPayMarketingConfig();
		config.setAppId(mechappid);
		config.setKey(key);
		config.setMchId(mechid);
		if (StringUtils.isBlank(certificatePassword)) {
			config.setCertificatePassword(mWxMerchants.getMchid());
		} else {
			config.setCertificatePassword(certificatePassword);
		}
		String wholePath=certificatePath+File.separator+organizationId+File.separator+mWxMerchants.getCertificateAddress();
		log.info("????????????????????????????????????????????????"+wholePath);
		config.setCertificatePath(wholePath);
		//????????????????????????
		OrganizationPayRequestParam oRequestParam=new OrganizationPayRequestParam();
		oRequestParam.setAmount(amount);
		oRequestParam.setMch_appid(mechappid);
		oRequestParam.setOpenid(openid);
		oRequestParam.setNonce_str(WXPayUtil.generateNonceStr());
		oRequestParam.setPartner_trade_no(partner_trade_no);
		oRequestParam.setSpbill_create_ip(spbill_create_ip);
		oRequestParam.setMchid(mechid);
		//??????????????????????????????map
		Map<String, String> signMap=generateMap(oRequestParam);

		//???????????????sign
		String sign=WXPayUtil.generateSignature(signMap, key, SignType.MD5);
		signMap.put("sign", sign);

		WXPay wxPay=new WXPay(config);
		new WXPayAsynTask(wxPay, WechatConstants.ORGANIZATION_PAY_CHANGE_Suffix_URL, signMap, 1000, 5000).run();
	}

	/**
	 * ????????????????????????
	 * @param openid
	 * @param spbill_create_ip
	 * @param amount
	 * @param partner_trade_no
	 * @param organizationId
	 * @throws Exception
	 */
	public void qiyePaySync(String  openid,String  spbill_create_ip,int amount,String  partner_trade_no, String organizationId) throws Exception {
		log.info("???????????? opendid={} spbill_create_ip={} amount={} partner_trade_no={} organizationId={}",openid,spbill_create_ip,amount,partner_trade_no,organizationId);

		if (StringUtils.isBlank(openid) || StringUtils.isBlank(spbill_create_ip)|| StringUtils.isBlank(partner_trade_no)|| StringUtils.isBlank(organizationId)) {
			throw new SuperCodeException("????????????????????????????????????,openid="+openid+",spbill_create_ip="+spbill_create_ip+",partner_trade_no="+partner_trade_no+spbill_create_ip+",organizationId="+organizationId, 500);
		}
		MarketingWxMerchants mWxMerchants=mWxMerchantsMapper.get(organizationId);
		if (null==mWxMerchants) {
			throw new SuperCodeException("????????????"+organizationId+"????????????????????????", 500);
		}
		if (mWxMerchants.getMerchantType() == 1) {
			if (mWxMerchants.getJgwId() != null) {
				mWxMerchants = mWxMerchantsMapper.getJgw(mWxMerchants.getJgwId());
			} else {
				mWxMerchants = mWxMerchantsMapper.getDefaultJgw();
			}
		} else if (StringUtils.isBlank(mWxMerchants.getCertificateAddress())) {
			throw new SuperCodeException("????????????"+organizationId+"???????????????????????????", 500);
		}
		String mechid=mWxMerchants.getMchid();
		String mechappid=mWxMerchants.getMchAppid();
		if (StringUtils.isBlank(mechid) || StringUtils.isBlank(mechappid)) {
			throw new SuperCodeException("???????????????????????????????????????????????????mechid="+mechid+",mechappid="+mechappid, 500);
		}

		//????????????
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		WXPayTradeOrder tradeOrder = new WXPayTradeOrder();
		tradeOrder.setAmount((float)amount);
		tradeOrder.setOpenId(openid);
		tradeOrder.setTradeStatus((byte) 0);
		tradeOrder.setPartnerTradeNo(partner_trade_no);
		tradeOrder.setTradeDate(format.format(new Date()));
		tradeOrder.setOrganizationId(organizationId);
		wXPayTradeOrderMapper.insert(tradeOrder);


		String certificatePassword = mWxMerchants.getCertificatePassword();
		String key=mWxMerchants.getMerchantKey();
		//???????????????
		WXPayMarketingConfig config=new WXPayMarketingConfig();
		config.setAppId(mechappid);
		config.setKey(key);
		config.setMchId(mechid);
		if (StringUtils.isBlank(certificatePassword)) {
			config.setCertificatePassword(mWxMerchants.getMchid());
		} else {
			config.setCertificatePassword(certificatePassword);
		}
		String wholePath=certificatePath+File.separator+mWxMerchants.getOrganizationId()+File.separator+mWxMerchants.getCertificateAddress();
		// ??????db?????????
		cacheToDiscIfNecssary(wholePath,certificatePath+File.separator+mWxMerchants.getOrganizationId(), mWxMerchants.getOrganizationId());
		log.info("????????????????????????????????????????????????"+wholePath);
		config.setCertificatePath(wholePath);
		//????????????????????????
		OrganizationPayRequestParam oRequestParam=new OrganizationPayRequestParam();
		oRequestParam.setAmount(amount);
		oRequestParam.setMch_appid(mechappid);
		oRequestParam.setOpenid(openid);
		oRequestParam.setNonce_str(WXPayUtil.generateNonceStr());
		oRequestParam.setPartner_trade_no(partner_trade_no);
		oRequestParam.setSpbill_create_ip(spbill_create_ip);
		oRequestParam.setMchid(mechid);
		//??????????????????????????????map
		Map<String, String> signMap=generateMap(oRequestParam);

		//???????????????sign
		String sign=WXPayUtil.generateSignature(signMap, key, SignType.MD5);
		signMap.put("sign", sign);

		WXPay wxPay=new WXPay(config);
		WXPayAsynTask wxPayAsynTask = new WXPayAsynTask(wxPay, WechatConstants.ORGANIZATION_PAY_CHANGE_Suffix_URL, signMap, 1000, 5000);
		wxPayAsynTask.pay();

	}

	/**
	 * ????????????????????????
	 * @param openid
	 * @param spbill_create_ip
	 * @param amount
	 * @param partner_trade_no
	 * @param organizationId
	 * @throws Exception
	 */
	public void qiyePaySyncWithResend(String  openid,String  spbill_create_ip,int amount,String  partner_trade_no, String organizationId, int reSend) throws Exception {
		log.info("???????????? opendid={} spbill_create_ip={} amount={} partner_trade_no={} organizationId={}",openid,spbill_create_ip,amount,partner_trade_no,organizationId);

		if (StringUtils.isBlank(openid) || StringUtils.isBlank(spbill_create_ip)|| StringUtils.isBlank(partner_trade_no)|| StringUtils.isBlank(organizationId)) {
			throw new SuperCodeException("????????????????????????????????????,openid="+openid+",spbill_create_ip="+spbill_create_ip+",partner_trade_no="+partner_trade_no+spbill_create_ip+",organizationId="+organizationId, 500);
		}
		MarketingWxMerchants mWxMerchants=mWxMerchantsMapper.get(organizationId);
		if (null==mWxMerchants) {
			throw new SuperCodeException("????????????"+organizationId+"????????????????????????", 500);
		}
		if (mWxMerchants.getMerchantType() == 1) {
			if (mWxMerchants.getJgwId() != null) {
				mWxMerchants = mWxMerchantsMapper.getJgw(mWxMerchants.getJgwId());
			} else {
				mWxMerchants = mWxMerchantsMapper.getDefaultJgw();
			}
		} else if (StringUtils.isBlank(mWxMerchants.getCertificateAddress())) {
			throw new SuperCodeException("????????????"+organizationId+"???????????????????????????", 500);
		}
		String mechid=mWxMerchants.getMchid();
		String mechappid=mWxMerchants.getMchAppid();
		if (StringUtils.isBlank(mechid) || StringUtils.isBlank(mechappid)) {
			throw new SuperCodeException("???????????????????????????????????????????????????mechid="+mechid+",mechappid="+mechappid, 500);
		}

		//????????????
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		WXPayTradeOrder tradeOrder = new WXPayTradeOrder();
		tradeOrder.setAmount((float)amount);
		tradeOrder.setOpenId(openid);
		tradeOrder.setTradeStatus((byte) 0);
		tradeOrder.setPartnerTradeNo(partner_trade_no);
		tradeOrder.setTradeDate(format.format(new Date()));
		tradeOrder.setOrganizationId(organizationId);
		tradeOrder.setReSend(reSend);
		wXPayTradeOrderMapper.insert(tradeOrder);


		String certificatePassword = mWxMerchants.getCertificatePassword();
		String key=mWxMerchants.getMerchantKey();
		//???????????????
		WXPayMarketingConfig config=new WXPayMarketingConfig();
		config.setAppId(mechappid);
		config.setKey(key);
		config.setMchId(mechid);
		if (StringUtils.isBlank(certificatePassword)) {
			config.setCertificatePassword(mWxMerchants.getMchid());
		} else {
			config.setCertificatePassword(certificatePassword);
		}
		String wholePath=certificatePath+File.separator+mWxMerchants.getOrganizationId()+File.separator+mWxMerchants.getCertificateAddress();
		// ??????db?????????
		cacheToDiscIfNecssary(wholePath,certificatePath+File.separator+mWxMerchants.getOrganizationId(), mWxMerchants.getOrganizationId());
		log.info("????????????????????????????????????????????????"+wholePath);
		config.setCertificatePath(wholePath);
		//????????????????????????
		OrganizationPayRequestParam oRequestParam=new OrganizationPayRequestParam();
		oRequestParam.setAmount(amount);
		oRequestParam.setMch_appid(mechappid);
		oRequestParam.setOpenid(openid);
		oRequestParam.setNonce_str(WXPayUtil.generateNonceStr());
		oRequestParam.setPartner_trade_no(partner_trade_no);
		oRequestParam.setSpbill_create_ip(spbill_create_ip);
		oRequestParam.setMchid(mechid);
		//??????????????????????????????map
		Map<String, String> signMap=generateMap(oRequestParam);

		//???????????????sign
		String sign=WXPayUtil.generateSignature(signMap, key, SignType.MD5);
		signMap.put("sign", sign);

		WXPay wxPay=new WXPay(config);
		WXPayAsynTask wxPayAsynTask = new WXPayAsynTask(wxPay, WechatConstants.ORGANIZATION_PAY_CHANGE_Suffix_URL, signMap, 1000, 5000);
		wxPayAsynTask.pay();

	}

	/**
	 *
 	 * @param wholeName
	 * @param wholePath
	 * @param organizationId  ????????????????????? ?????????????????????
	 */
	private void cacheToDiscIfNecssary(String wholeName,String wholePath,String organizationId) {

		File fd=new File(wholePath);
		if(!fd.exists()){       // ??????????????????????????????
			Wrapper<MarketingWxMerchantsExt> queryWapper = new QueryWrapper<>();
			((QueryWrapper<MarketingWxMerchantsExt>) queryWapper).eq("organizationId",organizationId);
			MarketingWxMerchantsExt marketingWxMerchantsExt = marketingWxMerchantsExtMapper.selectOne(queryWapper);
			Asserts.check(marketingWxMerchantsExt!=null && marketingWxMerchantsExt.getCertificateInfo() !=null ,"??????????????????");
			try {
				fd.mkdirs();
				FileOutputStream fileOutputStream = new FileOutputStream(wholeName);
				fileOutputStream.write(marketingWxMerchantsExt.getCertificateInfo());
				fileOutputStream.flush();
				fileOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("?????????????????????????????????");
			}
		}
	}


	/**
     * ???????????????????????????map
     * @param oRequestParam
     * @return
     */
	private Map<String, String> generateMap(OrganizationPayRequestParam oRequestParam) {
		Map<String, String> map=new HashMap<String, String>();
		map.put("mch_appid", oRequestParam.getMch_appid());
		map.put("mchid", oRequestParam.getMchid());
		map.put("nonce_str", oRequestParam.getNonce_str());
		map.put("partner_trade_no", oRequestParam.getPartner_trade_no());
		map.put("openid", oRequestParam.getOpenid());
		map.put("check_name", oRequestParam.getCheck_name());
		map.put("amount", String.valueOf(oRequestParam.getAmount()));
		map.put("desc", oRequestParam.getDesc());
		map.put("spbill_create_ip", oRequestParam.getSpbill_create_ip());
		return map;
	}
}
