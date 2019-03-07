package com.jgw.supercodeplatform.marketing.controller.wechat.front;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.cache.GlobalRamCache;
import com.jgw.supercodeplatform.marketing.common.model.HttpClientResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.ScanCodeInfoMO;
import com.jgw.supercodeplatform.marketing.common.util.HttpRequestUtil;
import com.jgw.supercodeplatform.marketing.constants.WechatConstants;
import com.jgw.supercodeplatform.marketing.pojo.MarketingMembers;
import com.jgw.supercodeplatform.marketing.service.user.MarketingMembersService;
/**
 * 微信授权等
 * @author czm
 *
 */
@RestController
@RequestMapping("/marketing/front/auth")
public class WeixinAuthController {
	protected static Logger logger = LoggerFactory.getLogger(WeixinAuthController.class);

	@Autowired
	private MarketingMembersService marketingMembersService;
	
    @Value("${marketing.activity.h5page.url}")
    private String h5pageUrl;
    
    /**
     * 微信授权回调方法
     * @param code
     * @param state
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/code")
    public String getWXCode(String code ,String state) throws Exception {
    	logger.info("微信授权回调获取code="+code+",state="+state);
    	if (StringUtils.isBlank(state)) {
    		throw new SuperCodeException("state不能为空", 500);
		}
    	String tokenParams="?appid="+WechatConstants.APPID+"&secret="+WechatConstants.secret+"&code="+code+"&grant_type=authorization_code";
    	HttpClientResult tokenhttpResult=HttpRequestUtil.doGet(WechatConstants.AUTH_ACCESS_TOKEN_URL+tokenParams);
    	String tokenContent=tokenhttpResult.getContent();
    	logger.info("调用获取授权access_token后返回内容："+tokenContent);
        if (tokenContent.contains("errcode")) {
			throw new SuperCodeException(tokenContent, 500);
		}
        
        JSONObject accessTokenObj=JSONObject.parseObject(tokenContent);
        String openid=accessTokenObj.getString("openid");
        String access_token=accessTokenObj.getString("access_token");
        Long expires_in=accessTokenObj.getLong("expires_in");
        String refresh_token=accessTokenObj.getString("refresh_token");
        String scope=accessTokenObj.getString("scope");
        
        String userInfoParams="?access_token="+access_token+"&openid="+openid+"&lang=zh_CN";
        HttpClientResult userinfohttpResult=HttpRequestUtil.doGet(WechatConstants.USER_INFO_URL+userInfoParams);
        String userinfoContent=userinfohttpResult.getContent();
    	logger.info("调用获取基础用户信息接口后返回内容："+userinfoContent);
        if (userinfoContent.contains("errcode")) {
        	throw new SuperCodeException(tokenContent, 500);
		}
        JSONObject userinfoObj=JSONObject.parseObject(userinfoContent);
        String nickName=userinfoObj.getString("nickname");
        logger.info("--------------------授权成功--------------------------------");
       
        //判断是否需要保存用户
        ScanCodeInfoMO scInfoMO=GlobalRamCache.scanCodeInfoMap.get(state);
        if (null==scInfoMO) {
			throw new SuperCodeException("授权回调方法无法根据state="+state+"获取到用户扫码缓存信息请重试", 500);
		}
        
        synchronized (this) {
        	MarketingMembers members=marketingMembersService.selectByOpenIdAndOrgId(openid, scInfoMO.getOrganizationId());
        	if (null==members) {
        		members=new MarketingMembers();
        		members.setOpenid(openid);
        		members.setWxName(nickName);
        		members.setOrganizationId(scInfoMO.getOrganizationId());
        		marketingMembersService.addMember(members);
        	}
		}
        return h5pageUrl+"?openId="+openid+"&wxstate="+state+"&activitySetId="+scInfoMO.getActivitySetId();
    }

}
