package com.jgw.supercodeplatform.marketing.controller.h5.activity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.cache.GlobalRamCache;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.ScanCodeInfoMO;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.pojo.MarketingWxMerchants;
import com.jgw.supercodeplatform.marketing.service.activity.MarketingActivitySetService;
import com.jgw.supercodeplatform.marketing.service.weixin.MarketingWxMerchantsService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Controller
@RequestMapping("/marketing/front/scan")
@Api(tags = "h5接收码管理跳转路径")
public class ScanCodeController {
	protected static Logger logger = LoggerFactory.getLogger(ScanCodeController.class);
    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private MarketingActivitySetService mActivitySetService;

    @Autowired
    private MarketingWxMerchantsService mWxMerchantsService;

    @Value("${marketing.domain.url}")
    private String wxauthRedirectUri;


    @Value("${marketing.activity.h5page.url}")
    private String h5pageUrl;

    @Value("${rest.user.url}")
    private String restUserUrl;

    @Autowired
    private GlobalRamCache globalRamCache;

    /**
     * 客户扫码码平台跳转到营销系统地址接口
     * @param codeId
     * @param codeTypeId
     * @param productId
     * @param productBatchId
     * @return
     * @throws UnsupportedEncodingException
     * @throws ParseException
     * @throws Exception
     */
    @RequestMapping(value = "/",method = RequestMethod.GET)
    @ApiOperation(value = "码平台跳转营销系统路径", notes = "")
    public String bind(@RequestParam(name="outerCodeId")String outerCodeId,@RequestParam(name="codeTypeId")String codeTypeId,@RequestParam(name="productId")String productId,@RequestParam(name="productBatchId")String productBatchId) throws Exception {
    	String	wxstate=commonUtil.getUUID();

    	String url=activityJudege(outerCodeId, codeTypeId, productId, productBatchId, wxstate);


        return "redirect:"+url;
    }

    public String activityJudege(String outerCodeId,String codeTypeId,String productId,String productBatchId,String wxstate) throws UnsupportedEncodingException, ParseException, SuperCodeException {
    	RestResult<ScanCodeInfoMO> restResult=mActivitySetService.judgeActivityScanCodeParam(outerCodeId,codeTypeId,productId,productBatchId);
    	if (restResult.getState()==500) {
    		logger.info("扫码接口返回错误，错误信息为："+restResult.getMsg());
    		 return h5pageUrl+"?success=0&msg="+URLEncoder.encode(URLEncoder.encode(restResult.getMsg(),"utf-8"),"utf-8");
		}

    	ScanCodeInfoMO sCodeInfoMO=restResult.getResults();
    	//在校验产品及产品批次时可以从活动设置表中获取组织id
        String organizationId=sCodeInfoMO.getOrganizationId();
        MarketingWxMerchants mWxMerchants=mWxMerchantsService.selectByOrganizationId(organizationId);
        if (null==mWxMerchants || StringUtils.isBlank(mWxMerchants.getMchAppid())) {
        	 return "redirect:"+h5pageUrl+"?success=0&msg="+URLEncoder.encode(URLEncoder.encode("该产品对应的企业未进行公众号绑定或企业APPID未设置。企业id："+organizationId,"utf-8"),"utf-8");
		}
        sCodeInfoMO.setOrganizationId(organizationId);
        globalRamCache.putScanCodeInfoMO(wxstate,sCodeInfoMO);
        logger.info("扫码后sCodeInfoMO信息："+sCodeInfoMO);

    	//微信授权需要对redirect_uri进行urlencode
        String wholeUrl=wxauthRedirectUri+"/marketing/front/auth/code";
    	String encoderedirectUri=URLEncoder.encode(wholeUrl, "utf-8");
        logger.info("扫码唯一标识wxstate="+wxstate+"，授权跳转路径url="+encoderedirectUri+",appid="+mWxMerchants.getMchAppid()+",h5pageUrl="+h5pageUrl);
        String url=h5pageUrl+"?wxstate="+wxstate+"&appid="+mWxMerchants.getMchAppid()+"&redirect_uri="+encoderedirectUri+"&success=1"+"&organizationId="+organizationId;
        return url;
    }
}
