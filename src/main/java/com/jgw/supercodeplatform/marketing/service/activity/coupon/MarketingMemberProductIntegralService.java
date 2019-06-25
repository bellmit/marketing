package com.jgw.supercodeplatform.marketing.service.activity.coupon;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingActivityProductMapper;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingActivitySetMapper;
import com.jgw.supercodeplatform.marketing.dao.coupon.MarketingCouponMapperExt;
import com.jgw.supercodeplatform.marketing.dao.coupon.MarketingMemberCouponMapperExt;
import com.jgw.supercodeplatform.marketing.dao.coupon.MarketingMemberProductIntegralMapperExt;
import com.jgw.supercodeplatform.marketing.enums.market.MemberTypeEnums;
import com.jgw.supercodeplatform.marketing.enums.market.coupon.CouponAcquireConditionEnum;
import com.jgw.supercodeplatform.marketing.pojo.MarketingActivityProduct;
import com.jgw.supercodeplatform.marketing.pojo.MarketingActivitySet;
import com.jgw.supercodeplatform.marketing.pojo.MarketingActivitySetCondition;
import com.jgw.supercodeplatform.marketing.pojo.MarketingMembers;
import com.jgw.supercodeplatform.marketing.pojo.integral.MarketingCoupon;
import com.jgw.supercodeplatform.marketing.pojo.integral.MarketingMemberCoupon;
import com.jgw.supercodeplatform.marketing.pojo.integral.MarketingMemberProductIntegral;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;

@Service
public class MarketingMemberProductIntegralService {
	
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

	/**
	 * 添加产品积分记录并判断参加的活动获取抵扣券
	 * @param productIntegral
	 * @throws ParseException 
	 */
	@Transactional(rollbackFor = Exception.class)
	public void obtainCoupon(MarketingMemberProductIntegral productIntegral, MarketingMembers members, String productName) throws ParseException {
		//添加或者更新累计积分
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
		String productId = productIntegral.getProductId();
		String productBatchId = productIntegral.getProductBatchId();
		//获取抵扣券
		MarketingActivityProduct marketingActivityProduct = marketingActivityProductMapper.selectByProductAndProductBatchIdWithReferenceRole(productId, productBatchId, MemberTypeEnums.VIP.getType());
		if(marketingActivityProduct != null) {
			MarketingActivitySet marketingActivitySet = marketingActivitySetMapper.selectById(marketingActivityProduct.getActivitySetId());
			int activityStatus = marketingActivitySet.getActivityStatus() == null?0:marketingActivitySet.getActivityStatus().intValue();
			int activityId = marketingActivitySet.getActivityId().intValue();
			String activityStartDateStr = marketingActivitySet.getActivityStartDate();
			String activityEndDateStr = marketingActivitySet.getActivityEndDate();
			long currentMills = System.currentTimeMillis();
			long activityStartMills = StringUtils.isBlank(activityStartDateStr)?0L:DateUtils.parseDate(activityStartDateStr, new String[]{"yyyy-MM-dd HH:mm:ss"}).getTime();
			long activityEndMills = StringUtils.isBlank(activityEndDateStr)?0L:DateUtils.parseDate(activityEndDateStr, new String[]{"yyyy-MM-dd HH:mm:ss"}).getTime();
			if(activityId == 4 && activityStatus == 1 && ((activityStartMills < currentMills && currentMills < activityEndMills)||(activityStartMills == 0 && activityEndMills == 0))) {
				String validCondition = marketingActivitySet.getValidCondition();
				MarketingActivitySetCondition activityCondtion = JSON.parseObject(validCondition, MarketingActivitySetCondition.class);
				Byte acquireCondition = activityCondtion.getAcquireCondition();
				if(acquireCondition != null) {
					Long activitySetId = marketingActivityProduct.getActivitySetId();
					List<MarketingCoupon> marketingCouponList = marketingCouponMapper.selectByActivitySetId(activitySetId);
					Integer acquireConditionIntegral = activityCondtion.getAcquireConditionIntegral();
					Long accrueIntegral = productIntegral.getAccrueIntegral();
					CouponAcquireConditionEnum couponAcquireConditionEnum = CouponAcquireConditionEnum.getConditionEnumByType(activityCondtion.getAcquireCondition());
					switch (couponAcquireConditionEnum) {
					case FIRST:
						addMarketingMemberCoupon(marketingCouponList, members, productId, productName);
						break;
					case ONCE_LIMIT:
						onceLimit(acquireConditionIntegral, accrueIntegral, marketingCouponList, members, productId, productName);
						break;
					case LIMIT:
						limt(productIntegral.getId(), acquireConditionIntegral, sumIntegral, marketingCouponList, members, productId, productName);
						break;
					default:
						break;
					}
				}
			}
		}
	}
	
	
	//一次积分达到限定值获得优惠券
	private void onceLimit(long acquireConditionIntegral,long accrueIntegral, List<MarketingCoupon> marketingCouponList, MarketingMembers member,String productId, String productName) {
		if(accrueIntegral >= acquireConditionIntegral) {
			addMarketingMemberCoupon(marketingCouponList, member, productId, productName);
		}
	}
	
	//累计积分达到限定值
	private void limt(Long memberProductIntegralId, long acquireConditionIntegral,long sumIntegral,List<MarketingCoupon> marketingCouponList, MarketingMembers member,String productId, String productName) {
		if(sumIntegral > acquireConditionIntegral) {
			addMarketingMemberCoupon(marketingCouponList, member, productId, productName);
			MarketingMemberProductIntegral productIntegral = new MarketingMemberProductIntegral();
			productIntegral.setId(memberProductIntegralId);
			productIntegral.setAccrueIntegral(sumIntegral - acquireConditionIntegral);
			productIntegralMapper.updateByPrimaryKeySelective(productIntegral);
		}
	}
	
	//添加抵扣券
	private void addMarketingMemberCoupon(List<MarketingCoupon> marketingCouponList, MarketingMembers member,String productId, String productName) {
		List<MarketingMemberCoupon> memberCouponList = new ArrayList<>();
		marketingCouponList.forEach(marketingCoupon -> {
			MarketingMemberCoupon marketingMemberCoupon = new MarketingMemberCoupon();
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
		});
		marketingMemberCouponMapper.insertList(memberCouponList);
		//用id拼接0得到CouponCode
		memberCouponList.forEach(marketingCoupon -> {
			MarketingMemberCoupon marketingMemberCoupon = new MarketingMemberCoupon();
			marketingMemberCoupon.setId(marketingCoupon.getId());
			marketingMemberCoupon.setCouponCode(String.format("%06d", marketingCoupon.getId()));
			marketingMemberCouponMapper.updateByPrimaryKeySelective(marketingMemberCoupon);
		});
	}
	
	/**
	 * 用户扫码指定产品领取抵扣券
	 * @param activitySetId
	 * @param productId
	 * @param jwtUser
	 * @throws SuperCodeException
	 */
	@Transactional(rollbackFor = Exception.class)
	public void obtainCouponShopping(Long activitySetId, String productId,String productName, H5LoginVO jwtUser) throws SuperCodeException {
		MarketingMembers member = new MarketingMembers();
		member.setId(jwtUser.getMemberId());
		member.setMobile(jwtUser.getMobile());
		member.setCustomerId(jwtUser.getCustomerId());
		member.setCustomerName(jwtUser.getCustomerName());
		List<MarketingCoupon> marketingCouponList = marketingCouponMapper.selectByActivitySetId(activitySetId);
		addMarketingMemberCoupon(marketingCouponList, member, productId, productName);
	}
	
	
	
}