package com.jgw.supercodeplatform.marketing.controller.h5.activity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSON;
import com.jgw.supercodeplatform.exception.SuperCodeExtException;
import com.jgw.supercodeplatform.marketing.cache.GlobalRamCache;
import com.jgw.supercodeplatform.marketing.common.model.activity.ScanCodeInfoMO;
import com.jgw.supercodeplatform.marketing.constants.SystemLabelEnum;
import com.jgw.supercodeplatform.marketing.dto.WxOrderPayDto;
import com.jgw.supercodeplatform.marketing.dto.activity.LotteryOprationDto;
import com.jgw.supercodeplatform.marketing.enums.market.MemberTypeEnums;
import com.jgw.supercodeplatform.marketing.pojo.MarketingChannel;
import com.jgw.supercodeplatform.marketing.service.activity.MarketingActivityChannelService;
import com.jgw.supercodeplatform.marketing.service.activity.MarketingActivitySetService;
import com.jgw.supercodeplatform.marketingsaler.integral.application.group.CodeManagerService;
import com.jgw.supercodeplatform.marketingsaler.integral.interfaces.dto.FromFakeOutCodeToMarketingInfoDto;
import com.jgw.supercodeplatform.marketingsaler.integral.interfaces.dto.OutCodeInfoDto;
import com.jgw.supercodeplatform.marketingsaler.outservicegroup.feigns.CodeManagerFromFadeToMarketingFeign;
import lombok.extern.slf4j.Slf4j;
 import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.LotteryResultMO;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.dto.activity.MarketingDeliveryAddressParam;
import com.jgw.supercodeplatform.marketing.exception.LotteryException;
import com.jgw.supercodeplatform.marketing.pojo.integral.IntegralOrder;
import com.jgw.supercodeplatform.marketing.service.LotteryService;
import com.jgw.supercodeplatform.marketing.service.SalerLotteryService;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import com.jgw.supercodeplatform.marketing.service.integral.IntegralOrderExcelService;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/marketing/front/lottery")
@Api(tags = "h5??????????????????")
@Slf4j
public class LotteryController extends CommonUtil {
     @Autowired
    private LotteryService service;

    @Autowired
    private CommonService commonService;

    @Autowired
    private SalerLotteryService  salerLotteryService;
    
    @Autowired
    private IntegralOrderExcelService integralOrderExcelService;

    @Autowired
    private MarketingActivityChannelService marketingActivityChannelService;

    @Autowired
    private GlobalRamCache globalRamCache;

    @Autowired
    private CodeManagerService codeManagerService;

    @Value("${cookie.domain}")
	private String cookieDomain;
	/**
	 * ??????????????????
	 */
	@Value("${rest.user.url}")
	private String USER_SERVICE;

    @GetMapping("/lottery")
    @ApiOperation(value = "????????????????????????", notes = "")
    public RestResult<LotteryResultMO> lottery(@ApiIgnore H5LoginVO jwtUser,String wxstate) throws Exception {
        if(jwtUser.getMemberType() == null || MemberTypeEnums.VIP.getType().intValue() != jwtUser.getMemberType() ){
            throw new SuperCodeException("??????????????????");
        }
        ScanCodeInfoMO scanCodeInfoMO = globalRamCache.getScanCodeInfoMO(wxstate);
        scanCodeInfoMO.setUserId(jwtUser.getMemberId());
        scanCodeInfoMO.setOpenId(jwtUser.getOpenid());
        scanCodeInfoMO.setMobile(jwtUser.getMobile());
        LotteryOprationDto lotteryOprationDto = new LotteryOprationDto();
        MarketingChannel marketingChannel = marketingActivityChannelService.checkCodeIdConformChannel(scanCodeInfoMO.getCodeTypeId(), scanCodeInfoMO.getCodeId(), scanCodeInfoMO.getActivitySetId());
        if (marketingChannel == null){
            return RestResult.successWithData(new LotteryResultMO("??????????????????"));
        }
        //???????????????????????????????????????
        service.checkLotteryCondition(lotteryOprationDto, scanCodeInfoMO);
        RestResult<LotteryResultMO> restResult = lotteryOprationDto.getRestResult();
        if(restResult != null){
            return restResult;
        }
        //???????????????
        service.holdLockJudgeES(lotteryOprationDto);
        if(lotteryOprationDto.getSuccessLottory() == 0) {
            return lotteryOprationDto.getRestResult();
        }
        //??????
        service.drawLottery(lotteryOprationDto);
        //??????????????????
        WxOrderPayDto orderPayDto = service.saveLottory(lotteryOprationDto, request.getRemoteAddr());
        if (orderPayDto != null) {
            service.saveTradeOrder(orderPayDto);
        }
        return lotteryOprationDto.getRestResult();
    }
    
    
    @GetMapping("/previewLottery")
    @ApiOperation(value = "????????????????????????", notes = "")
    public RestResult<LotteryResultMO> previewLottery(String uuid) throws Exception {
        return service.previewLottery(uuid, request);
    }

    /**
     * ????????????????????????????????????,????????????????????????
     * ??????????????????????????? ???????????????????????????????????????
     * @param wxstate
     * @param jwtUser
     * @return
     * @throws SuperCodeException
     * @throws ParseException
     */
    @GetMapping("/salerLottery")
    @ApiOperation(value = "??????????????????", notes = "??????????????????")
    @ApiImplicitParams(value= {@ApiImplicitParam(paramType="header",value = "???????????????",name="jwt-token")})
    public RestResult<LotteryResultMO> salerLottery(String wxstate, @ApiIgnore H5LoginVO jwtUser, HttpServletRequest request) throws Exception {
        if (wxstate.startsWith("0_")) {
            RestResult<LotteryResultMO> restResult = new RestResult<>();
            restResult.setMsg(wxstate.split("_")[1]);
            restResult.setState(500);
            LotteryResultMO lotteryResultMO = new LotteryResultMO();
            lotteryResultMO.setWinnOrNot(0);
            lotteryResultMO.setMsg(restResult.getMsg());
            restResult.setResults(lotteryResultMO);
            return restResult;
        }
        ScanCodeInfoMO scanCodeInfoMO = salerLotteryService.validateBasicBySalerlottery(wxstate, jwtUser);
        Long codeTypeId = Long.valueOf(scanCodeInfoMO.getCodeTypeId());
        String codeId = scanCodeInfoMO.getCodeId();
        // ??????????????????????????????????????????????????????
        commonService.checkCodeMarketFakeValid(codeTypeId);
        commonService.checkCodeValid(codeId,codeTypeId+"");
        //??????????????????????????????????????????????????????
        OutCodeInfoDto outCodeInfoDto = new OutCodeInfoDto(codeId, codeTypeId.toString());
        outCodeInfoDto.setCodeTypeId(codeTypeId.toString());
        outCodeInfoDto.setOuterCodeId(codeId);
        OutCodeInfoDto marketOutCodeInfoDto = codeManagerService.codeFromfakeToMarket(outCodeInfoDto);
        scanCodeInfoMO.setCodeTypeId(marketOutCodeInfoDto.getCodeTypeId());
        scanCodeInfoMO.setCodeId(marketOutCodeInfoDto.getOuterCodeId());
        MarketingChannel marketingChannel = marketingActivityChannelService.checkCodeIdConformChannel(scanCodeInfoMO.getCodeTypeId(), scanCodeInfoMO.getCodeId(), scanCodeInfoMO.getActivitySetId());
        if (marketingChannel == null){
            throw new SuperCodeExtException("????????????????????????????????????????????????");
        }
        LotteryResultMO lotteryResultMO = salerLotteryService.salerlottery(scanCodeInfoMO,jwtUser,request);
        RestResult<LotteryResultMO> restResult = RestResult.success();
        restResult.setMsg(lotteryResultMO.getMsg());
        restResult.setResults(lotteryResultMO);
        return restResult;
    }
    
    
    @PostMapping("/addPrizeOrder")
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????")
    @ApiImplicitParam(paramType="header",value = "???????????????",name="jwt-token")
    public RestResult<String> addPrizeOrder(@RequestBody MarketingDeliveryAddressParam marketingDeliveryAddressParam, @ApiIgnore H5LoginVO jwtUser){
    	IntegralOrder integralOrder = new IntegralOrder();
    	integralOrder.setMemberId(jwtUser.getMemberId());
    	integralOrder.setMemberName(jwtUser.getMemberName());
    	integralOrder.setOrderId(jwtUser.getOrganizationId());
    	integralOrder.setOrganizationId(jwtUser.getOrganizationId());
    	integralOrder.setOrganizationName(jwtUser.getOrganizationName());
    	integralOrderExcelService.addPrizeOrder(marketingDeliveryAddressParam, integralOrder);
    	return RestResult.success();
    }
   
}
