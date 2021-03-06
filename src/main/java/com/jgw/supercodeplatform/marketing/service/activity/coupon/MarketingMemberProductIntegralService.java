package com.jgw.supercodeplatform.marketing.service.activity.coupon;

import com.alibaba.fastjson.JSON;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.model.activity.ScanCodeInfoMO;
import com.jgw.supercodeplatform.marketing.config.redis.RedisLockUtil;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingActivityProductMapper;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingActivitySetMapper;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingChannelMapper;
import com.jgw.supercodeplatform.marketing.dao.coupon.MarketingCouponMapperExt;
import com.jgw.supercodeplatform.marketing.dao.coupon.MarketingMemberCouponMapperExt;
import com.jgw.supercodeplatform.marketing.dao.coupon.MarketingMemberProductIntegralMapperExt;
import com.jgw.supercodeplatform.marketing.dto.coupon.CouponConditionDto;
import com.jgw.supercodeplatform.marketing.enums.market.MemberTypeEnums;
import com.jgw.supercodeplatform.marketing.enums.market.coupon.CouponAcquireConditionEnum;
import com.jgw.supercodeplatform.marketing.pojo.*;
import com.jgw.supercodeplatform.marketing.pojo.integral.MarketingCoupon;
import com.jgw.supercodeplatform.marketing.pojo.integral.MarketingMemberCoupon;
import com.jgw.supercodeplatform.marketing.pojo.integral.MarketingMemberProductIntegral;
import com.jgw.supercodeplatform.marketing.service.activity.MarketingActivityChannelService;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import com.jgw.supercodeplatform.marketing.service.es.activity.CodeEsService;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

 import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class MarketingMemberProductIntegralService {
	
 	
	@Autowired
	private MarketingChannelMapper marketingChannelMapper;
	
	@Autowired
	private MarketingCouponMapperExt marketingCouponMapper;
	
	@Autowired
	private MarketingActivitySetMapper marketingActivitySetMapper;
	
	@Autowired
	private MarketingActivityProductMapper marketingActivityProductMapper;
	
	@Autowired
	private MarketingMemberProductIntegralMapperExt productIntegralMapper;
	
	@Autowired
	private MarketingMemberCouponMapperExt marketingMemberCouponMapper;

	@Autowired
	private CodeEsService codeEsService;
	
	@Autowired
	private CommonService commonService;

	@Autowired
	private MarketingActivityChannelService marketingActivityChannelService;
	
	@Autowired
	private RedisLockUtil lock;
	
	/**
	 * ???????????????????????????????????????????????????????????????
	 * @param productIntegral
	 * @throws ParseException 
	 * @throws SuperCodeException 
	 */
	@Transactional(rollbackFor = Exception.class)
	public void obtainCoupon(MarketingMemberProductIntegral productIntegral, MarketingMembers members, String productName, String codeTypeId, String outerCodeId) throws ParseException, SuperCodeException {
		String integralMsg = JSON.toJSONString(productIntegral);
		String productId = productIntegral.getProductId();
		String productBatchId = productIntegral.getProductBatchId();
		MarketingActivityProduct marketingActivityProduct = marketingActivityProductMapper.selectByProductAndProductBatchIdWithReferenceRole(productId, productBatchId, MemberTypeEnums.VIP.getType());
		if (marketingActivityProduct == null) {
			log.info("????????????????????????????????????{}", integralMsg);
			return;
		}
		String sBatchIds = marketingActivityProduct.getSbatchId();
//		if (StringUtils.isBlank(sBatchIds) || sBatchIds.contains(productIntegral.getSbatchId())) {
//			log.info("??????????????????????????????????????????{}", integralMsg);
//			return;
//		}
		MarketingActivitySet marketingActivitySet = marketingActivitySetMapper.selectById(marketingActivityProduct.getActivitySetId());
		if(marketingActivitySet == null){
			log.info("????????????????????????????????????{}", integralMsg);
			return;
		}
		MarketingChannel marketingChannel = marketingActivityChannelService.checkCodeIdConformChannel(codeTypeId, outerCodeId, marketingActivityProduct.getActivitySetId());
		if (marketingChannel == null) {
			log.info("??????????????????????????????????????????{}", integralMsg);
			return;
		}
		//??????????????????????????????
		long accrueIntegral = productIntegral.getAccrueIntegral();
		MarketingMemberProductIntegral marketingMemberProductIntegral = productIntegralMapper.selectProductIntegralByMemberIdAndProductId(productIntegral.getMemberId(), productIntegral.getProductId());
		long sumIntegral = productIntegral.getAccrueIntegral();
		if(marketingMemberProductIntegral == null) {
			productIntegral.setCreateTime(new Date());
			productIntegralMapper.insertSelective(productIntegral);
		} else {
			sumIntegral = sumIntegral + marketingMemberProductIntegral.getAccrueIntegral();
			productIntegral.setId(marketingMemberProductIntegral.getId());
			productIntegral.setAccrueIntegral(sumIntegral);
			productIntegral.setUpdateTime(new Date());
			productIntegralMapper.updateByPrimaryKeySelective(productIntegral);
		}
		//???????????????
		int activityStatus = marketingActivitySet.getActivityStatus() == null?0:marketingActivitySet.getActivityStatus().intValue();
		int activityId = marketingActivitySet.getActivityId().intValue();
		String activityStartDateStr = marketingActivitySet.getActivityStartDate();
		String activityEndDateStr = marketingActivitySet.getActivityEndDate();
		long currentMills = System.currentTimeMillis();
		long activityStartMills = StringUtils.isBlank(activityStartDateStr)?0L:DateUtils.parseDate(activityStartDateStr, CommonConstants.DATE_PATTERNS).getTime();
		long activityEndMills = StringUtils.isBlank(activityEndDateStr)?0L:DateUtils.parseDate(activityEndDateStr, CommonConstants.DATE_PATTERNS).getTime();
		if(activityId == 4 && activityStatus == 1 && ((activityStartMills < currentMills && currentMills < activityEndMills)||(activityStartMills == 0 && activityEndMills == 0))) {
			String validCondition = marketingActivitySet.getValidCondition();
			MarketingActivitySetCondition activityCondtion = JSON.parseObject(validCondition, MarketingActivitySetCondition.class);
			Byte acquireCondition = activityCondtion.getAcquireCondition();
			if(acquireCondition != null) {
				Long activitySetId = marketingActivityProduct.getActivitySetId();
				List<MarketingCoupon> marketingCouponList = marketingCouponMapper.selectByActivitySetId(activitySetId);
				Integer acquireConditionIntegral = activityCondtion.getAcquireConditionIntegral();
				CouponAcquireConditionEnum couponAcquireConditionEnum = CouponAcquireConditionEnum.getConditionEnumByType(activityCondtion.getAcquireCondition());
				switch (couponAcquireConditionEnum) {
				case FIRST:
					if(marketingMemberProductIntegral == null) {
                        addMarketingMemberCoupon( marketingCouponList, members, productId, productName,outerCodeId,marketingChannel);
                    }
					break;
				case ONCE_LIMIT:
					onceLimit(acquireConditionIntegral, accrueIntegral, marketingCouponList, members, productId, productName,outerCodeId,marketingChannel);
					break;
				case LIMIT:
					limt(productIntegral.getId(), acquireConditionIntegral, sumIntegral, marketingCouponList, members, productId, productName,outerCodeId,marketingChannel);
					break;
				default:
					break;
				}
			}
		}
	}
	
	
	//??????????????????????????????????????????
	private void onceLimit(long acquireConditionIntegral,long accrueIntegral, List<MarketingCoupon> marketingCouponList, MarketingMembers member,String productId, String productName,String outerCodeId, MarketingChannel marketingChannel) throws SuperCodeException {
		if(accrueIntegral >= acquireConditionIntegral) {
			addMarketingMemberCoupon(marketingCouponList, member, productId, productName, outerCodeId, marketingChannel);
		}
	}
	
	//???????????????????????????
	private void limt(Long memberProductIntegralId, long acquireConditionIntegral,long sumIntegral,List<MarketingCoupon> marketingCouponList, MarketingMembers member,String productId, String productName, String outerCodeId, MarketingChannel marketingChannel) throws SuperCodeException {
		if(sumIntegral >= acquireConditionIntegral) {
			addMarketingMemberCoupon(marketingCouponList, member, productId, productName, outerCodeId,marketingChannel);
			MarketingMemberProductIntegral productIntegral = new MarketingMemberProductIntegral();
			productIntegral.setId(memberProductIntegralId);
			productIntegral.setAccrueIntegral(0L);
			productIntegralMapper.updateByPrimaryKeySelective(productIntegral);
		}
	}
	
	//???????????????
	private void addMarketingMemberCoupon(List<MarketingCoupon> marketingCouponList, MarketingMembers member,String productId, String productName, String outerCodeId, MarketingChannel marketingChannel) throws SuperCodeException {
		if(CollectionUtils.isNotEmpty(marketingCouponList)) {
			List<MarketingMemberCoupon> memberCouponList = new ArrayList<>();
			marketingCouponList.forEach(marketingCoupon -> {
				MarketingMemberCoupon marketingMemberCoupon = new MarketingMemberCoupon();
				marketingMemberCoupon.setCouponId(marketingCoupon.getId());
				marketingMemberCoupon.setCouponAmount(marketingCoupon.getCouponAmount());
				marketingMemberCoupon.setCreateTime(new Date());
				marketingMemberCoupon.setDeductionStartDate(marketingCoupon.getDeductionStartDate());
				marketingMemberCoupon.setDeductionEndDate(marketingCoupon.getDeductionEndDate());
				marketingMemberCoupon.setMemberId(member.getId());
				marketingMemberCoupon.setMemberPhone(member.getMobile());
				marketingMemberCoupon.setObtainCustmerName(member.getCustomerName());
				marketingMemberCoupon.setObtainCustomerId(member.getCustomerId());
				marketingMemberCoupon.setProductId(productId);
				marketingMemberCoupon.setProductName(productName);
				marketingMemberCoupon.setUsed((byte)0);
				marketingMemberCoupon.setCustomerId(marketingChannel.getCustomerId());
				marketingMemberCoupon.setCustomerName(marketingChannel.getCustomerName());
				marketingMemberCoupon.setActivitySetId(marketingCoupon.getActivitySetId());
				marketingMemberCoupon.setOuterCodeId(outerCodeId);
				CouponConditionDto CouponCondition = new CouponConditionDto();
				CouponCondition.setDeductionChannelType(marketingCoupon.getDeductionChannelType());
				CouponCondition.setEductionProductType(marketingCoupon.getDeductionProductType());
				marketingMemberCoupon.setCouponCondition(JSON.toJSONString(CouponCondition));
				memberCouponList.add(marketingMemberCoupon);
			});
			marketingMemberCouponMapper.insertList(memberCouponList);
			//???id??????0??????CouponCode
			memberCouponList.forEach(marketingCoupon -> {
				MarketingMemberCoupon marketingMemberCoupon = new MarketingMemberCoupon();
				marketingMemberCoupon.setId(marketingCoupon.getId());
				marketingMemberCoupon.setCouponCode(String.format("%06d", marketingCoupon.getId()));
				marketingMemberCouponMapper.updateByPrimaryKeySelective(marketingMemberCoupon);
			});
		}
	}
	
	/**
	 * ???????????????????????????????????????
	 * @param
	 * @param
	 * @param jwtUser
	 * @throws Exception 
	 * @throws SuperCodeException
	 */
	public void obtainCouponShopping(ScanCodeInfoMO scanCodeInfoMO, String productName, H5LoginVO jwtUser, String outerCodeId, MarketingChannel marketingChannel) throws Exception {
		String lockKey = "marketing:coupon:" + scanCodeInfoMO.getCodeTypeId() + ":" + scanCodeInfoMO.getCodeId();
		boolean lockFlag = false;
		try {
			lockFlag = lock.lock(lockKey, 6);
			if(lockFlag) {
				//???????????????????????????
				int codeScanNum = codeEsService.countCodeIdScanNum(scanCodeInfoMO.getCodeId(), scanCodeInfoMO.getCodeTypeId());
				if(codeScanNum > 0) {
					throw new SuperCodeException("???????????????????????????");
				}
				MarketingMembers member = new MarketingMembers();
				member.setId(jwtUser.getMemberId());
				member.setMobile(jwtUser.getMobile());
				member.setCustomerId(jwtUser.getCustomerId());
				member.setCustomerName(jwtUser.getCustomerName());
				List<MarketingCoupon> marketingCouponList = marketingCouponMapper.selectByActivitySetId(scanCodeInfoMO.getActivitySetId());
				addMarketingMemberCoupon(marketingCouponList, member, scanCodeInfoMO.getProductId(), productName, outerCodeId, marketingChannel);
				codeEsService.indexScanInfo(scanCodeInfoMO);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if(lockFlag){
				try{
					lock.releaseLock(lockKey);
				}catch (Exception e){
					log.error("???????????????:"+lockKey, e);
					e.printStackTrace();
				}
			}
		}
	}
	
}
