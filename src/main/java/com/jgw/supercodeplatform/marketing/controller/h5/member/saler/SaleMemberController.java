package com.jgw.supercodeplatform.marketing.controller.h5.member.saler;


import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.cache.GlobalRamCache;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.ScanCodeInfoMO;
import com.jgw.supercodeplatform.marketing.common.page.AbstractPageService.PageResults;
import com.jgw.supercodeplatform.marketing.common.util.JWTUtil;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingActivityProductMapper;
import com.jgw.supercodeplatform.marketing.dto.SaleInfo;
import com.jgw.supercodeplatform.marketing.dto.activity.MarketingMemberAndScanCodeInfoParam;
import com.jgw.supercodeplatform.marketing.enums.market.MemberTypeEnums;
import com.jgw.supercodeplatform.marketing.pojo.MarketingActivityProduct;
import com.jgw.supercodeplatform.marketing.pojo.UserWithWechat;
import com.jgw.supercodeplatform.marketing.pojo.integral.IntegralRecord;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import com.jgw.supercodeplatform.marketing.service.es.activity.CodeEsService;
import com.jgw.supercodeplatform.marketing.service.integral.IntegralRecordService;
import com.jgw.supercodeplatform.marketing.service.user.MarketingSaleMemberService;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;
import com.jgw.supercodeplatform.marketingsaler.integral.application.group.BaseCustomerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * ????????????????????????
 */
@RestController
@RequestMapping("/marketing/saleMember/")
@Api(tags = "?????????H5")
@Slf4j
public class SaleMemberController {
     @Autowired
    private IntegralRecordService service;
    @Autowired
    private CodeEsService es;

    @Autowired
    private CommonService commonService;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private GlobalRamCache globalRamCache;

    @Autowired
    private MarketingSaleMemberService marketingSaleMemberService;

    @Autowired
    private BaseCustomerService baseCustomerService;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${cookie.domain}")
    private String cookieDomain;

    @GetMapping("info")
    @ApiOperation(value = "???????????????", notes = "")
    @ApiImplicitParams(value= {@ApiImplicitParam(paramType="header",value = "???????????????",name="jwt-token")})
    public RestResult<SaleInfo> info(@ApiIgnore H5LoginVO jwtUser) throws Exception {
        if(jwtUser.getMemberType() == null){
            throw new SuperCodeException("??????????????????????????????...");

        }
        if(MemberTypeEnums.SALER.getType().intValue()!=jwtUser.getMemberType()){
            throw new SuperCodeException("??????????????????...");
        }
        SaleInfo saleInfo = new SaleInfo();
        // 1 ????????????????????????
        Map acquireMoneyAndAcquireNums = service.getAcquireMoneyAndAcquireNums(jwtUser.getMemberId(), jwtUser.getMemberType(), jwtUser.getOrganizationId());
        // 4 ????????????


        UserWithWechat userWithWechat = marketingSaleMemberService.selectById(jwtUser.getMemberId());
        saleInfo.setUserName(StringUtils.isEmpty(userWithWechat.getUserName()) ? userWithWechat.getWxName() :userWithWechat.getUserName());
        saleInfo.setWechatHeadImgUrl(userWithWechat != null ? userWithWechat.getWechatHeadImgUrl():null);
        saleInfo.setScanQRCodeNum((Integer) acquireMoneyAndAcquireNums.get("scanNum"));
        Long count = (Long) acquireMoneyAndAcquireNums.get("count");
        saleInfo.setScanAmoutNum((count.intValue()));
        saleInfo.setAmoutNum(acquireMoneyAndAcquireNums.get("sum") != null ? (Double) acquireMoneyAndAcquireNums.get("sum"):0D);
        saleInfo.setAmoutNumStr(saleInfo.getAmoutNum()+"");
        saleInfo.setHaveIntegral(userWithWechat.getHaveIntegral());

      return RestResult.success("success",saleInfo);
    }




    @GetMapping("page")
    @ApiOperation(value = "???????????????page", notes = "")
    @ApiImplicitParams(value= {@ApiImplicitParam(paramType="header",value = "???????????????",name="jwt-token")})
    public RestResult<PageResults<IntegralRecord>> page(@ApiIgnore H5LoginVO jwtUser, IntegralRecord search) throws Exception {
        if(jwtUser.getMemberType()==null|| MemberTypeEnums.SALER.getType().intValue()!=jwtUser.getMemberType()){
            throw new SuperCodeException("??????????????????...");
        }
        // ??????????????????
        search.setMemberType(MemberTypeEnums.SALER.getType());
        // ?????????????????????????????????
        search.setOrganizationId(jwtUser.getOrganizationId());
        search.setSalerId(jwtUser.getMemberId());
        // ??????
        PageResults<IntegralRecord> objectPageResults = service.listSearchViewLike(search);
        // 4 ????????????
        return RestResult.success("success",objectPageResults);
    }

    /**
     * ?????????????????????????????????????????????????????????
     * @param orgId
     * @param wxstate
     * @param jwtUser
     * @return
     */
    @GetMapping("getOrgName")
    @ApiOperation(value = "??????????????????????????????wxstate", notes = "")
    public RestResult<Map<String,String>> getOrgNameAndAnsycPushScanIfo(@RequestParam("organizationId") String orgId , @RequestParam("wxstate")String wxstate, @ApiIgnore H5LoginVO jwtUser, HttpServletResponse response) throws SuperCodeException {
        // ????????????
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // ??????????????????
                MarketingMemberAndScanCodeInfoParam infoParam = new MarketingMemberAndScanCodeInfoParam();
                try{
                    ScanCodeInfoMO scanCodeInfoMO = globalRamCache.getScanCodeInfoMO(wxstate);
                    BeanUtils.copyProperties(scanCodeInfoMO, infoParam);
                    infoParam.setUserId(jwtUser.getMemberId());
                    infoParam.setUserName(jwtUser.getMemberName());
                    infoParam.setMemberType(jwtUser.getMemberType());
                    commonService.indexScanInfo(infoParam);


                }catch (Exception e){
                    log.info("????????????????????????");
                    log.info(e.getMessage(), e);
                }
            }
        });
        // ????????????: ??????????????????
        String jwtToken = JWTUtil.createTokenWithClaim(jwtUser);
        Cookie jwtTokenCookie = new Cookie(CommonConstants.JWT_TOKEN,jwtToken);
        // jwt????????????2?????????????????????
        jwtTokenCookie.setMaxAge(60*60*2);
        jwtTokenCookie.setPath("/");
        jwtTokenCookie.setDomain(cookieDomain);
        response.addCookie(jwtTokenCookie);
        boolean haveOrgId = validateParam(orgId, wxstate);
        return getNameByIdWithDefaultWhenError(orgId,haveOrgId);
    }
    private RestResult<Map<String, String>> getNameByIdWithDefaultWhenError(String orgId,boolean haveOrgId) {
        String defaultValue = "??????";
        Map<String, String> orgInfo = new HashMap<>();
        if(!haveOrgId){
            orgInfo.put("organizationName",defaultValue);
        }else{
            String organizationName = null;
            try {
                organizationName = commonService.getOrgNameByOrgId(orgId);
            } catch (SuperCodeException e) {
                e.printStackTrace();
            }
            if(StringUtils.isBlank(organizationName)){
                organizationName = defaultValue;
            }
            orgInfo.put("organizationName",organizationName);

        }
        return RestResult.success("success",orgInfo);
    }

    private boolean validateParam(String orgId, String wxstate)   {
        if(StringUtils.isBlank(orgId)){
            return false;
        }
        if(StringUtils.isBlank(wxstate)){
            log.error("????????????:????????????state ??????");
        }
        return true;
    }


    /**
     * ?????????????????????
     *
     */

    @Autowired
    private MarketingActivityProductMapper productMapper;

    @GetMapping("getWxstate")
    @ApiOperation(value = "????????????:???????????????,??????wxstate", notes = "")
    public RestResult produceScaninfoAndGetWxstate(
                                                   @RequestParam("outerCodeId") String  outerCodeId,
                                                   @RequestParam("activitySetId") Long activitySetId,
                                                   @RequestParam("userId") Long userId ) throws Exception {
        String wxstate = "wxstate12345678900987654321";

        ScanCodeInfoMO pMo=new ScanCodeInfoMO();
        MarketingActivityProduct marketingActivityProduct = productMapper.selectByActivitySetId(activitySetId).get(0);

        pMo.setCodeId(outerCodeId);
        pMo.setCodeTypeId(marketingActivityProduct.getCodeType());
        pMo.setProductBatchId(marketingActivityProduct.getProductBatchId());
        pMo.setProductId(marketingActivityProduct.getProductId());
        pMo.setActivitySetId(activitySetId);
        ;
        pMo.setUserId(userId);
        UserWithWechat userWithWechat = marketingSaleMemberService.selectById(userId);
        pMo.setMobile(userWithWechat.getMobile());
        pMo.setOpenId(userWithWechat.getOpenid());
        pMo.setOrganizationId(userWithWechat.getOrganizationId());
        globalRamCache.putScanCodeInfoMO(wxstate,pMo);
        return RestResult.success("success","wxstate12345678900987654321");
    }


}
