package com.jgw.supercodeplatform.marketing.controller.h5.activity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.exception.SuperCodeExtException;
import com.jgw.supercodeplatform.marketing.cache.GlobalRamCache;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.ScanCodeInfoMO;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.IpUtils;
import com.jgw.supercodeplatform.marketing.constants.BusinessTypeEnum;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.constants.WechatConstants;
import com.jgw.supercodeplatform.marketing.enums.market.ActivityIdEnum;
import com.jgw.supercodeplatform.marketing.enums.market.ReferenceRoleEnum;
import com.jgw.supercodeplatform.marketing.pojo.MarketingWxMerchants;
import com.jgw.supercodeplatform.marketing.service.activity.MarketingActivitySetService;
import com.jgw.supercodeplatform.marketing.service.es.activity.CodeEsService;
import com.jgw.supercodeplatform.marketing.service.weixin.MarketingWxMerchantsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
  import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("deprecation")
@Controller
@RequestMapping("/marketing/front/scan")
@Api(tags = "h5???????????????????????????")
@Slf4j
public class ScanCodeController {
     //??????????????????????????????
	private final static String salerUrlsuffix = "#/salesRedBag/index";
    @Autowired
    private CommonUtil commonUtil;
    @Autowired
    private MarketingActivitySetService mActivitySetService;

    @Autowired
    private MarketingWxMerchantsService mWxMerchantsService;
    
    @Autowired
    private RestTemplate asyncRestTemplate;

    @Value("${marketing.domain.url}")
    private String wxauthRedirectUri;

    @Value("${rest.antismashinggoods.url}")
    private String antismashinggoodsUrl;
    
    @Value("${marketing.activity.h5page.url}")
    private String h5pageUrl;

    @Value("${rest.user.url}")
    private String restUserUrl;

    @Value("${rest.user.domain}")
    private String restUserDomain;

    @Value("${marketing.integral.h5page.urls}")
    private String integralH5Pages;

    /**
     * ?????????????????????
     */
    @Value("${rest.user.url}")
    private String SALER_LOTTERY_URL;



    @Autowired
    private GlobalRamCache globalRamCache;


    @Autowired
    private CodeEsService es;
    /**
     * ??????????????????????????????????????????????????????
     * @param outerCodeId
     * @param codeTypeId
     * @param productId
     * @param productBatchId
     * @return
     * @throws UnsupportedEncodingException
     * @throws ParseException
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(value = "?????????????????????????????????", notes = "")
    public String bind(@RequestParam String outerCodeId,@RequestParam String codeTypeId,@RequestParam String productId,@RequestParam String productBatchId, @RequestParam String sBatchId, @RequestParam Integer businessType, HttpServletRequest request) throws Exception {
    	if (StringUtils.isBlank(productBatchId) || StringUtils.equalsIgnoreCase(productBatchId, "null")) {
            productBatchId = null;
        }
        Map<String, String> uriVariables = new HashMap<>();
    	uriVariables.put("judgeType", "2");
    	uriVariables.put("outerCodeId", outerCodeId);
    	uriVariables.put("codeTypeId",codeTypeId);
    	uriVariables.put("ipAddr",IpUtils.getClientIpAddr(request));
        HttpHeaders requestHeaders = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        requestHeaders.setContentType(type);
        requestHeaders.add("Accept", MediaType.APPLICATION_JSON.toString());
        //body
        HttpEntity<String> requestEntity = new HttpEntity<>(JSON.toJSONString(uriVariables), requestHeaders);
    	try {
    		asyncRestTemplate.postForEntity(antismashinggoodsUrl+CommonConstants.JUDGE_FLEE_GOOD, requestEntity, JSONObject.class);
    	} catch (Exception e) {
			log.error("??????????????????", e);
		}
    	String wxstate=commonUtil.getUUID();
        log.info("???????????????????????????outerCodeId="+outerCodeId+",codeTypeId="+codeTypeId+",productId="+productId+",productBatchId="+productBatchId+",sBatchId="+sBatchId);
        //    	String url=activityJudege(outerCodeId, codeTypeId, productId, productBatchId, wxstate,(byte)0, sBatchId, businessType);
        String errorUrl = activitySetJudege(outerCodeId, codeTypeId, productId, productBatchId, wxstate, (byte) 0, sBatchId, businessType);
        if (StringUtils.isNotBlank(errorUrl)) {
            return "redirect:" + errorUrl;
        }
    	ScanCodeInfoMO scanCodeInfoMO = globalRamCache.getScanCodeInfoMO(wxstate);
        if(scanCodeInfoMO != null ) {
            // ???????????????
            scanCodeInfoMO.setScanCodeTime(new Date());
            es.indexScanInfo(scanCodeInfoMO);
        }
        String organizationId = scanCodeInfoMO.getOrganizationId();
        long activityId = scanCodeInfoMO.getActivityId();
        long activitySetId = scanCodeInfoMO.getActivitySetId();
        String redirectUrl = getUrlByActId(activityId);
        if (redirectUrl == null) {
            redirectUrl = h5pageUrl+"?success=0&msg="+URLEncoder.encode(URLEncoder.encode("??????????????????????????????","utf-8"),"utf-8");
        }
        redirectUrl = redirectUrl + "?success=1&wxstate=" + wxstate + "&activitySetId=" + activitySetId + "&organizationId=" +organizationId;

        return "redirect:"+redirectUrl;
    }


    /**
     * ??????????????????????????????????????????????????????
     * @param outerCodeId
     * @param codeTypeId
     * @param productId
     * @param productBatchId
     * @return
     * @throws UnsupportedEncodingException
     * @throws ParseException
     * @throws Exception
     */
    @RequestMapping(value = "/saler",method = RequestMethod.GET)
    @ApiOperation(value = "???????????????????????????????????????", notes = "")
    public String daogou(@RequestParam String outerCodeId,@RequestParam String codeTypeId,@RequestParam String productId,@RequestParam String productBatchId,@RequestParam String sBatchId, @RequestParam String memberId) throws Exception {
        if (StringUtils.isBlank(productBatchId) || StringUtils.equalsIgnoreCase(productBatchId, "null")) {
            productBatchId = null;
        }
        log.info("???????????????????????????outerCodeId="+outerCodeId+",codeTypeId="+codeTypeId+",productId="+productId+",productBatchId="+productBatchId+"sBatchId="+sBatchId);
    	String	wxstate=commonUtil.getUUID();
    	String url=activityJudegeBySaler(outerCodeId, codeTypeId, productId, productBatchId,sBatchId, wxstate, ReferenceRoleEnum.ACTIVITY_SALER.getType());
        // ???????????????????????????URL
        return "redirect:"+url+"&memberId="+memberId;
    }

    /**
     * ??????wxstate ???????????????
     * @param outerCodeId
     * @param codeTypeId
     * @param productId
     * @param productBatchId
     * @param wxstate
     * @param referenceRole
     * @return
     * @throws SuperCodeException
     * @throws UnsupportedEncodingException
     * @throws ParseException
     */
    private String activityJudegeBySaler(String outerCodeId, String codeTypeId, String productId, String productBatchId,String sBatchId, String wxstate, byte referenceRole) throws SuperCodeException, UnsupportedEncodingException, ParseException {

        RestResult<ScanCodeInfoMO> restResult=mActivitySetService.judgeActivityScanCodeParam(outerCodeId,codeTypeId,productId,productBatchId,referenceRole);
        if (restResult.getState()==500) {
            log.info("?????????????????????????????????????????????"+restResult.getMsg());
            return h5pageUrl+salerUrlsuffix+"?wxstate=0_"+URLEncoder.encode(restResult.getMsg(),"utf-8");
        }

        ScanCodeInfoMO sCodeInfoMO=restResult.getResults();
        sCodeInfoMO.setSbatchId(sBatchId);
        //????????????????????????????????????????????????????????????????????????id
        String organizationId=sCodeInfoMO.getOrganizationId();
        sCodeInfoMO.setOrganizationId(organizationId);
        globalRamCache.putScanCodeInfoMO(wxstate,sCodeInfoMO);

        log.info("?????????sCodeInfoMO?????????"+sCodeInfoMO);
        String url=h5pageUrl+salerUrlsuffix+"?wxstate="+wxstate+"&organizationId="+sCodeInfoMO.getOrganizationId();
        return url;


    }

    /**
     * ????????????/??????????????????????????????
     * @param outerCodeId
     * @param codeTypeId
     * @param productId
     * @param productBatchId
     * @param wxstate
     * @param referenceRole
     * @return
     * @throws UnsupportedEncodingException
     * @throws ParseException
     * @throws SuperCodeException
     */
    public String activityJudege(String outerCodeId,String codeTypeId,String productId,String productBatchId,String wxstate, byte referenceRole,String sbatchId, Integer businessType) throws UnsupportedEncodingException, ParseException {
    	RestResult<ScanCodeInfoMO> restResult=mActivitySetService.judgeActivityScanCodeParam(outerCodeId,codeTypeId,productId,productBatchId,referenceRole, businessType);
    	if (restResult.getState()==500) {
    		log.info("?????????????????????????????????????????????"+restResult.getMsg());
    		 return h5pageUrl+"?success=0&msg="+URLEncoder.encode(URLEncoder.encode(restResult.getMsg(),"utf-8"),"utf-8");
		}

    	ScanCodeInfoMO sCodeInfoMO=restResult.getResults();
    	//????????????????????????????????????????????????????????????????????????id
        String organizationId=sCodeInfoMO.getOrganizationId();
        MarketingWxMerchants mWxMerchants=mWxMerchantsService.selectByOrganizationId(organizationId);
        if (null==mWxMerchants) {
        	 return h5pageUrl+"?success=0&msg="+URLEncoder.encode(URLEncoder.encode("?????????????????????????????????????????????????????????APPID??????????????????id???"+organizationId,"utf-8"),"utf-8");
		} else {
            organizationId = mWxMerchants.getOrganizationId();
            if (mWxMerchants.getMerchantType().intValue() == 1) {
                if (mWxMerchants.getJgwId() != null) {
                    mWxMerchants = mWxMerchantsService.getJgw(mWxMerchants.getJgwId());
                } else {
                    mWxMerchants = mWxMerchantsService.getDefaultJgw();
                }
            }
        }
        sCodeInfoMO.setSbatchId(sbatchId);
        sCodeInfoMO.setOrganizationId(organizationId);
        globalRamCache.putScanCodeInfoMO(wxstate,sCodeInfoMO);
        log.info("?????????sCodeInfoMO?????????"+sCodeInfoMO);

    	//?????????????????????redirect_uri??????urlencode
        String wholeUrl=wxauthRedirectUri+"/marketing/front/auth/code";
    	String encoderedirectUri=URLEncoder.encode(wholeUrl, "utf-8");
        log.info("??????????????????wxstate="+wxstate+"?????????????????????url="+encoderedirectUri+",appid="+mWxMerchants.getMchAppid()+",h5pageUrl="+h5pageUrl);
        String url=h5pageUrl+"?wxstate="+wxstate+"&appid="+mWxMerchants.getMchAppid()+"&redirect_uri="+encoderedirectUri+"&success=1"+"&organizationId="+organizationId;
        return url;
    }

    @GetMapping("/code/callback")
    @ApiOperation("??????????????????")
    public String getWXCode(@RequestParam String code, @RequestParam String state) {
        log.info("????????????????????????code=" + code + ",state=" + state);
        if (StringUtils.isBlank(state)) {
            throw new SuperCodeExtException("state????????????", 500);
        }
        try {
            return "redirect:" + restUserDomain + "/wechat/org/info?code=" + code + "&state=" + state;
        } catch (Exception e) {
            throw new SuperCodeExtException("?????????????????????");
        }
    }


    private String activitySetJudege(String outerCodeId,String codeTypeId,String productId,String productBatchId,String wxstate, byte referenceRole,String sbatchId, Integer businessType) throws UnsupportedEncodingException, ParseException {
        RestResult<ScanCodeInfoMO> restResult=mActivitySetService.judgeActivityScanCodeParam(outerCodeId,codeTypeId,productId,productBatchId,referenceRole, businessType);
        if (restResult.getState()==500) {
            log.info("?????????????????????????????????????????????"+restResult.getMsg());
            return h5pageUrl+"?success=0&msg="+URLEncoder.encode(URLEncoder.encode(restResult.getMsg(),"utf-8"),"utf-8");
        }

        ScanCodeInfoMO sCodeInfoMO=restResult.getResults();
        //????????????????????????????????????????????????????????????????????????id
        String organizationId=sCodeInfoMO.getOrganizationId();
        sCodeInfoMO.setSbatchId(sbatchId);
        sCodeInfoMO.setOrganizationId(organizationId);
        globalRamCache.putScanCodeInfoMO(wxstate,sCodeInfoMO);
        log.info("?????????sCodeInfoMO?????????"+sCodeInfoMO);
        return null;
    }

    private String getUrlByActId(long actId){
        String redirectUrl = null;
        if (actId == ActivityIdEnum.ACTIVITY_WX_RED_PACKAGE.getId().longValue() || actId == ActivityIdEnum.ACTIVITY_2.getId().longValue()) {
            redirectUrl = h5pageUrl;
        }
        if (actId == ActivityIdEnum.ACTIVITY_SALER.getId().longValue()) {
            redirectUrl = h5pageUrl + WechatConstants.SALER_LOGIN_URL;
        }
        if (actId == ActivityIdEnum.ACTIVITY_COUPON.getId().longValue()) {
            redirectUrl = integralH5Pages.split(",")[0];
        }
        return redirectUrl;
    }

}
