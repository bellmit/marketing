package com.jgw.supercodeplatform.marketing.controller.h5.activity;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.jgw.supercodeplatform.marketing.pojo.MarketingChannel;
import com.jgw.supercodeplatform.marketing.service.activity.MarketingActivityChannelService;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.ScanCodeInfoMO;
import com.jgw.supercodeplatform.marketing.common.page.AbstractPageService.PageResults;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingActivityProductMapper;
import com.jgw.supercodeplatform.marketing.dto.coupon.CouponConditionDto;
import com.jgw.supercodeplatform.marketing.dto.coupon.CouponCustmerVerifyPageParam;
import com.jgw.supercodeplatform.marketing.dto.coupon.CouponObtainParam;
import com.jgw.supercodeplatform.marketing.dto.coupon.CouponPageParam;
import com.jgw.supercodeplatform.marketing.dto.coupon.CouponVerifyPram;
import com.jgw.supercodeplatform.marketing.enums.market.ActivityIdEnum;
import com.jgw.supercodeplatform.marketing.enums.market.CouponVerifyEnum;
import com.jgw.supercodeplatform.marketing.enums.market.MemberTypeEnums;
import com.jgw.supercodeplatform.marketing.enums.market.coupon.CouponAcquireConditionEnum;
import com.jgw.supercodeplatform.marketing.pojo.MarketingActivityProduct;
import com.jgw.supercodeplatform.marketing.pojo.MarketingActivitySet;
import com.jgw.supercodeplatform.marketing.pojo.MarketingActivitySetCondition;
import com.jgw.supercodeplatform.marketing.pojo.integral.MarketingMemberCoupon;
import com.jgw.supercodeplatform.marketing.service.activity.MarketingActivitySetService;
import com.jgw.supercodeplatform.marketing.service.activity.coupon.MarketingMemberProductIntegralService;
import com.jgw.supercodeplatform.marketing.service.integral.CouponCustmerVerifyService;
import com.jgw.supercodeplatform.marketing.service.integral.CouponMemberService;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;
import com.jgw.supercodeplatform.marketing.vo.coupon.CouponPageVo;
import com.jgw.supercodeplatform.marketing.vo.coupon.CouponVerifyVo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

@SuppressWarnings("deprecation")
@RestController
@RequestMapping("/marketing/front/coupon")
@Api(tags = "?????????H5")
public class CouponController {

	@Autowired
	private CommonService commonService;
	@Autowired
	private MarketingActivityChannelService marketingActivityChannelService;
	@Autowired
	private CouponMemberService couponMemberService;
	@Autowired
	private CouponCustmerVerifyService couponCustmerVerifyService;
	@Autowired
	private MarketingActivitySetService marketingActivitySetService;
	@Autowired
	private MarketingMemberProductIntegralService marketingMemberProductIntegralService;
	@Autowired
	private MarketingActivityProductMapper marketingActivityProductMapper;
    
	@GetMapping("/listCoupon")
	@ApiOperation("???????????????")
	@ApiImplicitParams(@ApiImplicitParam(paramType="header",value = "?????????token",name="jwt-token"))
	public RestResult<List<CouponPageVo>> listCoupon(@Validated CouponPageParam couponPageParam, @ApiIgnore H5LoginVO jwtUser) throws Exception{
		couponPageParam.setMemberId(jwtUser.getMemberId());
		List<CouponPageVo> couponList = couponMemberService.searchResult(couponPageParam);
		return new RestResult<>(HttpStatus.SC_OK, "????????????", couponList);
	}
	
	/**
	 * ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
	 * @param couponObtainParam
	 * @param jwtUser
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@PostMapping("/obtainCoupon")
	@ApiOperation("?????????????????????")
	@ApiImplicitParams({@ApiImplicitParam(paramType="header",value = "?????????token",name="jwt-token")})
	public RestResult<?> obtainCoupon(@Valid @RequestBody CouponObtainParam couponObtainParam, @ApiIgnore H5LoginVO jwtUser) throws Exception{
		List<MarketingActivityProduct> marketingActivityProductList = marketingActivityProductMapper.selectByProductWithReferenceRole(couponObtainParam.getProductId(), MemberTypeEnums.VIP.getType());
		if(CollectionUtils.isEmpty(marketingActivityProductList)) {
            throw new SuperCodeException("????????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
		String productName = marketingActivityProductList.get(0).getProductName();
		Long activitySetId = marketingActivityProductList.get(0).getActivitySetId();
		MarketingActivitySet marketingActivitySet = marketingActivitySetService.selectById(activitySetId);
		if(marketingActivitySet == null) {
            throw new SuperCodeException("???????????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
		Integer activityStatus = marketingActivitySet.getActivityStatus();
		if(activityStatus == null || activityStatus.intValue() != 1) {
            throw new SuperCodeException("?????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
		long activityId = marketingActivitySet.getActivityId();
		if(activityId != 4) {
            throw new SuperCodeException("????????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
		String activityStartDateStr = marketingActivitySet.getActivityStartDate();
		String activityEndDateStr = marketingActivitySet.getActivityEndDate();
		long currentMills = System.currentTimeMillis();
		long activityStartMills = StringUtils.isBlank(activityStartDateStr)?0L:DateUtils.parseDate(activityStartDateStr, CommonConstants.DATE_PATTERNS).getTime();
		long activityEndMills = StringUtils.isBlank(activityEndDateStr)?0L:DateUtils.parseDate(activityEndDateStr, CommonConstants.DATE_PATTERNS).getTime();
		if(activityStartMills != 0 && activityEndMills != 0) {
			if(activityStartMills > currentMills) {
                throw new SuperCodeException("?????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
			if(activityEndMills < currentMills) {
                throw new SuperCodeException("?????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
		}
		String validCondition = marketingActivitySet.getValidCondition();
		MarketingActivitySetCondition activityCondtion = JSON.parseObject(validCondition, MarketingActivitySetCondition.class);
		Byte acquireCondition = activityCondtion.getAcquireCondition();
		if(!CouponAcquireConditionEnum.SHOPPING.getCondition().equals(acquireCondition)) {
			throw new SuperCodeException("?????????????????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
		String codeId = couponObtainParam.getOuterCodeId();
		String codeTypeId = couponObtainParam.getCodeTypeId();
		commonService.checkCodeValid(codeId, codeTypeId);
		commonService.checkCodeTypeValid(Long.valueOf(codeTypeId));
		//?????????????????????
		MarketingChannel marketingChannel = marketingActivityChannelService.checkCodeIdConformChannel(codeTypeId, codeId, activityId);
		if (marketingChannel == null){
			return RestResult.fail("?????????????????????",null);
		}
		ScanCodeInfoMO scanCodeInfoMO = new ScanCodeInfoMO();
		scanCodeInfoMO.setActivitySetId(activitySetId);
		scanCodeInfoMO.setActivityId(ActivityIdEnum.ACTIVITY_COUPON.getId().longValue());
		scanCodeInfoMO.setActivityType(ActivityIdEnum.ACTIVITY_COUPON.getType());
		scanCodeInfoMO.setCodeId(codeId);
		scanCodeInfoMO.setCodeTypeId(codeTypeId);
		scanCodeInfoMO.setCreateTime(DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss"));
		scanCodeInfoMO.setOrganizationId(jwtUser.getOrganizationId());
		scanCodeInfoMO.setProductBatchId(couponObtainParam.getProductBatchId());
		scanCodeInfoMO.setProductId(couponObtainParam.getProductId());
		scanCodeInfoMO.setScanCodeTime(new Date());
		scanCodeInfoMO.setMobile(jwtUser.getMobile());
		scanCodeInfoMO.setUserId(jwtUser.getMemberId());
		marketingMemberProductIntegralService.obtainCouponShopping(scanCodeInfoMO ,productName, jwtUser, couponObtainParam.getOuterCodeId(), marketingChannel);
		return RestResult.success();
	}
	
	@PostMapping("/couponVerify")
	@ApiOperation("???????????????")
	@ApiImplicitParams({@ApiImplicitParam(paramType="header",value = "?????????token",name="jwt-token")})
	public RestResult<Double> couponVerify(@Valid @RequestBody CouponVerifyPram couponVerifyPram, @ApiIgnore H5LoginVO jwtUser) throws SuperCodeException{
		MarketingMemberCoupon marketingMemberCoupon = couponMemberService.getMarketingMemberCouponByCouponCode(couponVerifyPram.getCouponCode());
		if(marketingMemberCoupon == null) {
            throw new SuperCodeException("??????????????????????????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
		String phone = marketingMemberCoupon.getMemberPhone();
		if(!couponVerifyPram.getMemberPhone().equals(phone)) {
            throw new SuperCodeException("??????????????????????????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
		Byte used = marketingMemberCoupon.getUsed();
		if(used != null && used.intValue() == 1) {
            throw new SuperCodeException("???????????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
		long currentMills = System.currentTimeMillis();
		Date deductionStartDate = marketingMemberCoupon.getDeductionStartDate();
		if(deductionStartDate != null && deductionStartDate.getTime() > currentMills) {
            throw new SuperCodeException("??????????????????????????? ????????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
		Date deductionEndDate = marketingMemberCoupon.getDeductionEndDate();
		if(deductionEndDate != null && deductionEndDate.getTime() < currentMills) {
            throw new SuperCodeException("????????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
		String marketingCouponStr = marketingMemberCoupon.getCouponCondition();
		CouponConditionDto marketingCoupon = StringUtils.isBlank(marketingCouponStr)?null:JSON.parseObject(marketingCouponStr,CouponConditionDto.class);
		if(marketingCoupon != null) {
			int deductionChannelType = marketingCoupon.getDeductionChannelType().intValue();
			String customerId = marketingMemberCoupon.getCustomerId();
//			if(StringUtils.isNotBlank(customerId)) {
//				if(deductionChannelType == 1 && !StringUtils.equals(customerId, jwtUser.getCustomerId())) 
//					throw new SuperCodeException("??????????????????????????? ?????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
//				if(deductionChannelType == 0) {
//					List<MarketingChannel> channelList = marketingChannelMapper.selectByCustomerId(customerId);
//					if(!CollectionUtils.isEmpty(channelList)) {
//						List<MarketingChannel> li = channelList.stream().filter(channel -> channel.getCustomerId().equals(jwtUser.getCustomerId())).collect(Collectors.toList());
//						if(li.size() == 0)
//							throw new SuperCodeException("??????????????????????????? ?????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
//					} else {
//						throw new SuperCodeException("??????????????????????????? ?????????????????????", HttpStatus.SC_INTERNAL_SERVER_ERROR);
//					}
//				}
//			}
		}
		//???????????????????????????
		MarketingMemberCoupon memberCoupon = new MarketingMemberCoupon();
		memberCoupon.setId(marketingMemberCoupon.getId());
		memberCoupon.setVerifyMemberId(jwtUser.getMemberId());
		memberCoupon.setVerifyCustomerId(jwtUser.getCustomerId());
		memberCoupon.setVerifyCustomerName(jwtUser.getCustomerName());
		memberCoupon.setVerifyPersonName(jwtUser.getMemberName());
		memberCoupon.setVerifyPersonPhone(jwtUser.getMobile());
		memberCoupon.setVerifyPersonType(CouponVerifyEnum.SALER.getType());
		memberCoupon.setVerifyTime(new Date());
		memberCoupon.setUsed((byte)1);
		couponMemberService.verifyCoupon(memberCoupon);
		return new RestResult<>(HttpStatus.SC_OK, "????????????", marketingMemberCoupon.getCouponAmount());
	}
	
	@GetMapping("/listVerify")
	@ApiOperation("????????????????????????")
	@ApiImplicitParams(@ApiImplicitParam(paramType="header",value = "?????????token",name="jwt-token"))
	public RestResult<PageResults<List<CouponVerifyVo>>> listVerify(CouponCustmerVerifyPageParam verifyPageParam, @ApiIgnore H5LoginVO jwtUser) throws Exception{
		verifyPageParam.setVerifyCustomerId(jwtUser.getCustomerId());
		verifyPageParam.setVerifyMemberId(jwtUser.getMemberId());
		PageResults<List<CouponVerifyVo>> verifyResult = couponCustmerVerifyService.listSearchViewLike(verifyPageParam);
		return new RestResult<>(HttpStatus.SC_OK, "????????????", verifyResult);
	}

}
