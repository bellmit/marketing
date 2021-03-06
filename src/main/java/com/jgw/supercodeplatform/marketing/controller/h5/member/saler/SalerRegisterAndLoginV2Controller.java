package com.jgw.supercodeplatform.marketing.controller.h5.member.saler;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.constants.PcccodeConstants;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.HttpsUtil;
import com.jgw.supercodeplatform.marketing.common.util.JWTUtil;
import com.jgw.supercodeplatform.marketing.config.redis.RedisUtil;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.constants.RedisKey;
import com.jgw.supercodeplatform.marketing.dto.CustomerInfo;
import com.jgw.supercodeplatform.marketing.dto.MarketingSaleMembersAddParam;
import com.jgw.supercodeplatform.marketing.dto.SalerLoginParam;
import com.jgw.supercodeplatform.marketing.enums.market.BrowerTypeEnum;
import com.jgw.supercodeplatform.marketing.enums.market.MemberTypeEnums;
import com.jgw.supercodeplatform.marketing.enums.market.SaleUserStatus;
import com.jgw.supercodeplatform.marketing.pojo.MarketingUser;
import com.jgw.supercodeplatform.marketing.pojo.UserWithWechat;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import com.jgw.supercodeplatform.marketing.service.user.MarketingSaleMemberService;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
 import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * ??????V1?????????????????????????????????,??????????????????
 *
 */
@Controller
@RequestMapping("/marketing/front/saler/v2")
@Api(tags = "?????????????????????")
@Slf4j
public class SalerRegisterAndLoginV2Controller {

 
//    redirect_uri???????????????????????????????????????
    private final String redirctUrl              = "http://marketing.kf315.net/marketing/front/saler/register";
    private final String loginRedirctUrl              = "http://marketing.kf315.net/marketing/front/saler/update";
    // ??????code
    private final String OAUTH2_WX_URL           = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx32ab5628a5951ecc&redirect_uri="+"[backUrl]"+"&response_type=code&scope=snsapi_base&state="+"[mobile]"+"#wechat_redirect";
    // ???????????????access_token openid
    private final String openidandaccesstokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=wx32ab5628a5951ecc&secret=e3fb09c9126cd8bc12399e56a35162c4&code=[code]&grant_type=authorization_code";
    @Autowired
    private MarketingSaleMemberService service;
    // TODO ?????????????????????
    @Value("https://www.baidu.com")
    private String WEB_URL ;
    /**
     * ???????????????????????????
     */
//    @Value("${redis.saler.register}")
    private static final String REGISTER_PERFIX = "saler:register:";
    @Autowired
    private CommonUtil commonUtil;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ModelMapper modelMapper;
    @Value("${cookie.domain}")
    private String cookieDomain;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private CommonService commonService;
    /**
     * ??????
     * @param loginUser
     * @param response
     * @return
     * @throws SuperCodeException
     */
    @ResponseBody
    @GetMapping("login")
    @ApiOperation(value = "???????????????", notes = "")
    public RestResult<Long> login(SalerLoginParam loginUser ,HttpServletResponse response) throws SuperCodeException{
            // ?????? ??????????????????openid??????
            // ????????????
            // TODO  ?????????
            MarketingUser user = service.selectBylogin(loginUser);
            // ???jwt
            if(user != null){
                if(!StringUtils.isBlank(loginUser.getOpenid())){
                    // ????????????????????????,???????????????
                    // ???????????????openid/????????????openid???????????????openid???
                    UserWithWechat userWithWechat = service.selectByOpenidAndOrgId(loginUser.getOpenid(), loginUser.getOrganizationId());
                    // openid??????????????????
                    if(userWithWechat == null){
                        service.addUserOpenId(user, loginUser.getOpenid());
                    }
                    if(userWithWechat!=null && !userWithWechat.getMobile().equals(loginUser.getMobile())){
                        return RestResult.error("???????????????????????????????????????...",null,500);
                    }
                }
                if(user.getState().intValue() == SaleUserStatus.AUDITED.getStatus().intValue()){
                    return RestResult.error("?????????????????????????????????????????????????????????",null,500);
                }
                if(user.getState().intValue() == SaleUserStatus.DISABLE.getStatus().intValue()){
                    return RestResult.error("??????????????????",null,500);
                }
                H5LoginVO jwtUser = new H5LoginVO();
                jwtUser.setMobile(loginUser.getMobile());
                jwtUser.setMemberName(user.getUserName());
                jwtUser.setMemberId(user.getId());
                jwtUser.setOrganizationId(user.getOrganizationId());
                jwtUser.setMemberType(MemberTypeEnums.SALER.getType());
                jwtUser.setCustomerId(user.getCustomerId());
                jwtUser.setCustomerName(user.getCustomerName());
                jwtUser.setHaveIntegral(user.getHaveIntegral());
                jwtUser.setOpenid(loginUser.getOpenid());
                redisUtil.set("memberuser:id:"+user.getId(), loginUser.getOpenid(), (long) (60*60*2));
//                try {
//                    jwtUser.setOrganizationName(commonService.getOrgNameByOrgId(loginUser.getOrganizationId()));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                // TODO ??????????????????????????????????????????
                String jwtToken = JWTUtil.createTokenWithClaim(jwtUser);
                Cookie jwtTokenCookie = new Cookie(CommonConstants.JWT_TOKEN,jwtToken);
                // jwt????????????2?????????????????????
                jwtTokenCookie.setMaxAge(60*60*2);
                jwtTokenCookie.setPath("/");
                jwtTokenCookie.setDomain(cookieDomain);
                response.addCookie(jwtTokenCookie);
            }else{
                // ?????????????????????
                return RestResult.error("????????????????????????",null,401);

            }
        // ????????????openid
        return RestResult.success("success",user.getId());
    }


   @PostMapping("/tempRegister")
   @ResponseBody
   @ApiOperation(value = "????????????????????????????????????????????????", notes = "")
   public RestResult loadingRegisterBeforeWxReturnOpenId(@Valid  @RequestBody  MarketingSaleMembersAddParam userInfo, HttpServletResponse response) throws SuperCodeException, IOException {
           // ???????????? ????????????????????????openiD???????????????
         service.saveRegisterUser(userInfo);
         return RestResult.success();
   }





    /**
     * ????????????https
     * @param openidandaccesstokenUrl
     * @return
     */
    private String getOpenid(String openidandaccesstokenUrl) {
        String result = HttpsUtil.get(openidandaccesstokenUrl,null);
        return  result;
    }


}
