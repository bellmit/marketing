package com.jgw.supercodeplatform.marketing.service;

import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.exception.SuperCodeExtException;
import com.jgw.supercodeplatform.marketing.cache.GlobalRamCache;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.LotteryResultMO;
import com.jgw.supercodeplatform.marketing.common.model.activity.MarketingPrizeTypeMO;
import com.jgw.supercodeplatform.marketing.common.model.activity.ScanCodeInfoMO;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.DateUtil;
import com.jgw.supercodeplatform.marketing.common.util.LotteryUtilWithOutCodeNum;
import com.jgw.supercodeplatform.marketing.config.redis.RedisLockUtil;
import com.jgw.supercodeplatform.marketing.config.redis.RedisUtil;
import com.jgw.supercodeplatform.marketing.constants.RedisKey;
import com.jgw.supercodeplatform.marketing.dao.activity.*;
import com.jgw.supercodeplatform.marketing.dao.integral.IntegralRecordMapperExt;
import com.jgw.supercodeplatform.marketing.dao.user.MarketingMembersMapper;
import com.jgw.supercodeplatform.marketing.dao.weixin.WXPayTradeOrderMapper;
import com.jgw.supercodeplatform.marketing.dto.WxOrderPayDto;
import com.jgw.supercodeplatform.marketing.dto.activity.LotteryOprationDto;
import com.jgw.supercodeplatform.marketing.dto.activity.MarketingActivityPreviewParam;
import com.jgw.supercodeplatform.marketing.dto.activity.MarketingPrizeTypeParam;
import com.jgw.supercodeplatform.marketing.enums.market.IntegralReasonEnum;
import com.jgw.supercodeplatform.marketing.enums.market.ReferenceRoleEnum;
import com.jgw.supercodeplatform.marketing.pojo.*;
import com.jgw.supercodeplatform.marketing.pojo.integral.IntegralRecord;
import com.jgw.supercodeplatform.marketing.pojo.pay.WXPayTradeOrder;
import com.jgw.supercodeplatform.marketing.pojo.platform.MarketingPlatformOrganization;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import com.jgw.supercodeplatform.marketing.service.es.activity.CodeEsService;
import com.jgw.supercodeplatform.marketing.service.user.MarketingMembersService;
import com.jgw.supercodeplatform.marketing.service.weixin.WXPayService;
import com.jgw.supercodeplatform.marketing.weixinpay.WXPayTradeNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LotteryService {

 
	@Autowired
	private MarketingPrizeTypeMapper mMarketingPrizeTypeMapper;

	@Autowired
	private IntegralRecordMapperExt integralRecordMapperExt;

	@Autowired
	private MarketingActivityProductMapper maProductMapper;

	@Autowired
	private MarketingActivityMapper mActivityMapper;

	@Autowired
	private MarketingMembersWinRecordMapper mWinRecordMapper;

	@Autowired
	private WXPayTradeOrderMapper wXPayTradeOrderMapper;

	@Autowired
	private MarketingMembersService marketingMembersService;

	@Autowired
	private WXPayService wxpService;

	@Autowired
	private CodeEsService codeEsService;

	@Autowired
	private CommonService commonService;

	@Autowired
	private WXPayTradeNoGenerator wXPayTradeNoGenerator ;

	@Autowired
	private MarketingMembersMapper marketingMembersMapper;

	@Autowired
	private MarketingActivitySetMapper mSetMapper;

	@Autowired
	private MarketingPrizeTypeMapper mPrizeTypeMapper;

	@Autowired
	private RedisUtil redisUtil;

	@Autowired
	private RedisLockUtil lock;

	@Value("${marketing.server.ip}")
	private String serverIp;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private MarketingPlatformOrganizationMapper marketingPlatformOrganizationMapper;

	/**
	 * ?????????????????????????????????
	 * @param scanCodeInfoMO
	 * @return ?????? RestResult??????MarketingPrizeTypeMO
	 * @throws ParseException
	 * @throws SuperCodeException
	 */
	public LotteryOprationDto checkLotteryCondition(LotteryOprationDto lotteryOprationDto, ScanCodeInfoMO scanCodeInfoMO) throws ParseException, SuperCodeException {
		String codeId=scanCodeInfoMO.getCodeId();
		String codeTypeId=scanCodeInfoMO.getCodeTypeId();
		String productId=scanCodeInfoMO.getProductId();
		String productBatchId=scanCodeInfoMO.getProductBatchId();
		String sbatchId = scanCodeInfoMO.getSbatchId();
		commonService.checkCodeValid(codeId, codeTypeId);
		commonService.checkCodeTypeValid(Long.valueOf(codeTypeId));
		Long activitySetId=scanCodeInfoMO.getActivitySetId();
		MarketingActivitySet mActivitySet = mSetMapper.selectById(activitySetId);
		if (null == mActivitySet) {
			return lotteryOprationDto.lotterySuccess("????????????????????????");
		}
		if(mActivitySet.getActivityStatus() == 0) {
			throw new SuperCodeExtException("??????????????????", 500);
		}
		long currentMills = System.currentTimeMillis();
		String startDateStr = mActivitySet.getActivityStartDate();
		if(StringUtils.isNotBlank(startDateStr)) {
			long startMills = DateUtil.parse(startDateStr, "yyyy-MM-dd").getTime();
			if(currentMills < startMills){
				throw new SuperCodeExtException("?????????????????????", 500);
			}
		}
		String endDateStr = mActivitySet.getActivityEndDate();
		if(StringUtils.isNotBlank(endDateStr)) {
			long endMills = DateUtil.parse(endDateStr, "yyyy-MM-dd").getTime();
			if(currentMills > endMills){
				throw new SuperCodeExtException("?????????????????????", 500);
			}
		}
		MarketingActivityProduct mActivityProduct = maProductMapper.selectByProductAndProductBatchIdWithReferenceRoleAndSetId(productId, productBatchId, ReferenceRoleEnum.ACTIVITY_MEMBER.getType(), activitySetId);
		String productSbatchId = mActivityProduct.getSbatchId();
		if(productSbatchId == null || !productSbatchId.contains(sbatchId)) {
			throw new SuperCodeExtException("???????????????", 500);
		}
		MarketingActivity activity = mActivityMapper.selectById(mActivitySet.getActivityId());
		if (null == activity) {
			throw new SuperCodeExtException("??????????????????", 500);
		}
		Long userId = scanCodeInfoMO.getUserId();
		MemberWithWechat memberWithWechat = marketingMembersService.selectById(userId);
		if(memberWithWechat == null){
			throw new SuperCodeException("?????????????????????",500);
		}
		MarketingMembers marketingMembersInfo = new MarketingMembers();
		BeanUtils.copyProperties(memberWithWechat, marketingMembersInfo);
		marketingMembersInfo.setId(userId);
		if( null != marketingMembersInfo.getState() && marketingMembersInfo.getState() == 0){
			throw new SuperCodeException("?????????,??????????????????????????????",500);
		}
		String condition = mActivitySet.getValidCondition();
		MarketingActivitySetCondition mSetCondition = null;
		if (StringUtils.isNotBlank(condition)) {
			mSetCondition = JSONObject.parseObject(condition, MarketingActivitySetCondition.class);
		} else {
			mSetCondition = new MarketingActivitySetCondition();
		}
		int consumeIntegralNum = mSetCondition.getConsumeIntegral() == null? 0: mSetCondition.getConsumeIntegral();
		int haveIntegral = marketingMembersInfo.getHaveIntegral() == null? 0:marketingMembersInfo.getHaveIntegral();
		if (haveIntegral < consumeIntegralNum) {
			throw new SuperCodeException("?????????,???????????????????????????"+consumeIntegralNum+"???????????????????????????",500);
		}
		List<MarketingPrizeTypeMO> moPrizeTypes = mMarketingPrizeTypeMapper.selectMOByActivitySetIdIncludeUnreal(activitySetId);
		if (CollectionUtils.isEmpty(moPrizeTypes)) {
			return lotteryOprationDto.lotterySuccess("??????????????????????????????");
		}
		lotteryOprationDto.setSendAudit(mActivitySet.getSendAudit());
		lotteryOprationDto.setMarketingMembersInfo(marketingMembersInfo);
		lotteryOprationDto.setOrganizationId(scanCodeInfoMO.getOrganizationId());
		if (lotteryOprationDto.getOrganizationName() == null ) {
			lotteryOprationDto.setOrganizationName(mActivitySet.getOrganizatioIdlName());
		}
		lotteryOprationDto.setScanCodeInfoMO(scanCodeInfoMO);
		lotteryOprationDto.setConsumeIntegralNum(consumeIntegralNum);
		lotteryOprationDto.setHaveIntegral(haveIntegral);
		lotteryOprationDto.setProductName(mActivityProduct.getProductName());
		lotteryOprationDto.setMarketingActivity(activity);
		//??????????????????
		lotteryOprationDto.setPrizeTypeMO(LotteryUtilWithOutCodeNum.startLottery(moPrizeTypes));
		return lotteryOprationDto;
	}

	public LotteryOprationDto drawLottery(LotteryOprationDto lotteryOprationDto) {
		ScanCodeInfoMO scanCodeInfoMO = lotteryOprationDto.getScanCodeInfoMO();
		MarketingPrizeTypeMO prizeTypeMO = lotteryOprationDto.getPrizeTypeMO();
		IntegralRecord integralRecord = new IntegralRecord();
		integralRecord.setCodeTypeId(scanCodeInfoMO.getCodeTypeId());
		integralRecord.setOuterCodeId(scanCodeInfoMO.getCodeId());
		integralRecord.setProductId(scanCodeInfoMO.getProductId());
		integralRecord.setProductName(lotteryOprationDto.getProductName());
		integralRecord.setActivitySetId(scanCodeInfoMO.getActivitySetId());
		integralRecord.setOrganizationId(lotteryOprationDto.getOrganizationId());
		integralRecord.setOrganizationName(lotteryOprationDto.getOrganizationName());
		integralRecord.setIntegralReason(IntegralReasonEnum.ACTIVITY_INTEGRAL.getIntegralReason());
		integralRecord.setIntegralReasonCode(IntegralReasonEnum.ACTIVITY_INTEGRAL.getIntegralReasonCode());
		lotteryOprationDto.setIntegralRecord(integralRecord);
		//??????realprize?????????0,0?????????????????????????????????????????????????????????????????????
		int realPrize = prizeTypeMO.getRealPrize().intValue();
		if(realPrize == 0) {
			return lotteryOprationDto.lotterySuccess("????????????????????????????????????????????????????????????????????????");
		}
		Byte awardType = prizeTypeMO.getAwardType();
		//??????awardType????????????????????????4???????????????????????????????????????????????????4
		if (awardType == null) {
			awardType = (byte)4;
			prizeTypeMO.setAwardType(awardType);
			lotteryOprationDto.setPrizeTypeMO(prizeTypeMO);
		}
		//???????????????1??????9???????????????????????????
		if (awardType.intValue() == 1 || awardType.intValue() == 9) {
			ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
			String key = prizeTypeMO.getId() + "_"+prizeTypeMO.getActivitySetId();
			//?????????????????????
			valueOperations.setIfAbsent(key, prizeTypeMO.getRemainingStock().toString());
			redisTemplate.expire(key, 60, TimeUnit.SECONDS);
			Long stockNum = valueOperations.increment(key, -1);
			if (stockNum == null || stockNum < 0) {
				return lotteryOprationDto.lotterySuccess("????????????????????????????????????????????????????????????????????????");
			}
		}
		return lotteryOprationDto.lotterySuccess(1);
	}


	@Transactional(rollbackFor = Exception.class)
	public WxOrderPayDto saveLottory(LotteryOprationDto lotteryOprationDto, String remoteAddr) throws Exception {
		IntegralRecord integralRecord = lotteryOprationDto.getIntegralRecord();
		MarketingMembers marketingMembersInfo = lotteryOprationDto.getMarketingMembersInfo();
		int consumeIntegralNum = lotteryOprationDto.getConsumeIntegralNum();
		//????????????????????????
		if (consumeIntegralNum > 0) {
			integralRecord.setIntegralNum(0 - consumeIntegralNum);
			addToInteral(marketingMembersInfo, integralRecord);
		}
		int changeIntegral = 0 - consumeIntegralNum;
		ScanCodeInfoMO scanCodeInfoMO = lotteryOprationDto.getScanCodeInfoMO();
		String mobile = scanCodeInfoMO.getMobile();
		String openId = scanCodeInfoMO.getOpenId();
		String outerCodeId = scanCodeInfoMO.getCodeId();
		Long activitySetId = scanCodeInfoMO.getActivitySetId();
		MarketingActivity activity = lotteryOprationDto.getMarketingActivity();
		MarketingPrizeTypeMO prizeTypeMO = lotteryOprationDto.getPrizeTypeMO();
		String organizationId = lotteryOprationDto.getOrganizationId();
		String productId = scanCodeInfoMO.getProductId();
		String productBatchId = scanCodeInfoMO.getProductBatchId();
		Byte awardType = prizeTypeMO.getAwardType();
		if (awardType == null ) {
			awardType = (byte)4;
		}
		LotteryResultMO lotteryResultMO = lotteryOprationDto.getLotteryResultMO();
		Float amount = null;
		//??????????????????????????????????????????????????????
		lotteryResultMO.setAwardType(awardType);
		//SuccessLottory == 1
		if (lotteryOprationDto.getSuccessLottory() == 1) {
			switch (awardType.intValue()) {
				case 1:
				case 9://??????
					mPrizeTypeMapper.updateRemainingStock(prizeTypeMO.getId());
					lotteryResultMO.setMsg("??????????????????" + prizeTypeMO.getPrizeTypeName());
					Map<String, Object> lotteryDataMap = new HashMap<>();
					lotteryDataMap.put("prizeId", prizeTypeMO.getId());
					lotteryDataMap.put("prizeName", prizeTypeMO.getPrizeTypeName());
					lotteryResultMO.setData(lotteryDataMap);
					break;
				case 2: //??????
					lotteryResultMO.setData(prizeTypeMO.getCardLink());
					lotteryResultMO.setMsg("??????????????????" + prizeTypeMO.getPrizeTypeName());
					break;
				case 3: //??????
					int awardIntegralNum = prizeTypeMO.getAwardIntegralNum().intValue();
					changeIntegral = changeIntegral + awardIntegralNum;
					lotteryResultMO.setMsg("??????????????????" + awardIntegralNum + "??????");
					integralRecord.setIntegralNum(awardIntegralNum);
					addToInteral(marketingMembersInfo, integralRecord);
					break;
				case 4:
					amount = prizeTypeMo(prizeTypeMO);
					String strAmount = String.format("%.2f", amount);
					lotteryResultMO.setData(strAmount);
					lotteryResultMO.setMsg(strAmount);
				default:
					break;
			}
		}
		addWinRecord(outerCodeId, mobile, openId, activitySetId, activity, organizationId,lotteryOprationDto.getOrganizationName(), prizeTypeMO, amount, productId, productBatchId);
		if (changeIntegral != 0) {
			marketingMembersMapper.deleteIntegral(0 - changeIntegral, marketingMembersInfo.getId());
			if (changeIntegral > 0) {
				marketingMembersMapper.addAccumulateIntegral(changeIntegral, marketingMembersInfo.getId());
			}
		}
		RestResult restResult = lotteryOprationDto.getRestResult();
		restResult.setMsg(lotteryResultMO.getMsg());
		if(awardType.intValue() == 4 && amount != null && amount > 0) {
			if (StringUtils.isBlank(openId)) {
				throw new SuperCodeExtException("????????????????????????");
			}
			WxOrderPayDto wxOrderPayDto = new WxOrderPayDto();
			wxOrderPayDto.setAmount(amount * 100);
			wxOrderPayDto.setMobile(mobile);
			wxOrderPayDto.setOpenId(openId);
			wxOrderPayDto.setOrganizationId(organizationId);
			wxOrderPayDto.setOuterCodeId(outerCodeId);
			wxOrderPayDto.setReferenceRole(ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
			wxOrderPayDto.setRemoteAddr(remoteAddr);
			wxOrderPayDto.setSendAudit(lotteryOprationDto.getSendAudit());
			return wxOrderPayDto;
		}
		if (awardType.intValue() != 4 && lotteryOprationDto.getSuccessLottory() == 1) {
			lotteryResultMO.setWinnOrNot(1);
			lotteryOprationDto.setLotteryResultMO(lotteryResultMO);
			restResult.setResults(lotteryResultMO);
			restResult.setMsg(lotteryResultMO.getMsg());
			return null;
		}
		lotteryResultMO = new LotteryResultMO("?????????????????????????????????");
		lotteryResultMO.setData("?????????????????????????????????");
		restResult.setResults(lotteryResultMO);
		restResult.setMsg(lotteryResultMO.getMsg());
		lotteryOprationDto.setRestResult(restResult);
		lotteryOprationDto.setLotteryResultMO(lotteryResultMO);
		return null;
	}

	/**
	 * ?????????????????????????????????
	 *
	 * @param wxOrderPayDto
	 * @throws Exception
	 */
	public void saveTradeOrder(WxOrderPayDto wxOrderPayDto) throws Exception {
		float amount = wxOrderPayDto.getAmount();
		String mobile = wxOrderPayDto.getMobile();
		String openId = wxOrderPayDto.getOpenId();
		String organizationId = wxOrderPayDto.getOrganizationId();
		String outerCodeId = wxOrderPayDto.getOuterCodeId();
		byte referenceRole = wxOrderPayDto.getReferenceRole();
		String remoteAddr = wxOrderPayDto.getRemoteAddr();
		byte sendAudit = wxOrderPayDto.getSendAudit();
		weixinpay(sendAudit,outerCodeId, mobile, openId, organizationId, amount, remoteAddr, referenceRole);
	}

	private void addWinRecord(String outCodeId, String mobile, String openId, Long activitySetId,
							  MarketingActivity activity, String organizationId,String organizationFullName, MarketingPrizeTypeMO mPrizeTypeMO, Float amount, String productId, String productBatchId) {
		//??????????????????
		MarketingMembersWinRecord redWinRecord=new MarketingMembersWinRecord();
		redWinRecord.setActivityId(activity.getId());
		redWinRecord.setActivityName(activity.getActivityName());
		redWinRecord.setActivitySetId(activitySetId);
		redWinRecord.setMobile(mobile);
		redWinRecord.setOpenid(openId);
		redWinRecord.setPrizeTypeId(mPrizeTypeMO.getId());
		redWinRecord.setWinningAmount(amount );
		redWinRecord.setProductId(productId);
		redWinRecord.setWinningCode(outCodeId);
		redWinRecord.setPrizeName(mPrizeTypeMO.getPrizeTypeName());
		redWinRecord.setOrganizationId(organizationId);
		redWinRecord.setOrganizationFullName(organizationFullName);
		redWinRecord.setProductBatchId(productBatchId);
		mWinRecordMapper.addWinRecord(redWinRecord);
	}

	private void addToInteral(MarketingMembers marketingMembersInfo, IntegralRecord integralRecord) {
		integralRecord.setCreateDate(new Date());
		integralRecord.setCustomerId(marketingMembersInfo.getCustomerId());
		integralRecord.setCustomerName(marketingMembersInfo.getCustomerName());
		integralRecord.setMemberId(marketingMembersInfo.getId());
		integralRecord.setMemberName(marketingMembersInfo.getUserName());
		integralRecord.setMemberType(marketingMembersInfo.getMemberType());
		integralRecord.setMobile(marketingMembersInfo.getMobile());
		integralRecord.setStatus("1");
		integralRecordMapperExt.insertSelective(integralRecord);
	}
	/////////////////////////////////////////ES???????????????/////////////////////////////////////////////////////
	public LotteryOprationDto holdLockJudgeES(LotteryOprationDto lotteryOprationDto) {
		ScanCodeInfoMO scanCodeInfoMO = lotteryOprationDto.getScanCodeInfoMO();
		int memberType = lotteryOprationDto.getMarketingMembersInfo().getMemberType();
		Long memberId = lotteryOprationDto.getMarketingMembersInfo().getId();
		Integer eachDayNumber = lotteryOprationDto.getEachDayNumber();
		String organizationId = lotteryOprationDto.getOrganizationId();
		MarketingMembers marketingMembersInfo = lotteryOprationDto.getMarketingMembersInfo();
		IntegralRecord integralRecord = lotteryOprationDto.getIntegralRecord();
		//MarketingActivityProduct marketingActivityProduct
		long activitySetId = scanCodeInfoMO.getActivitySetId();
		String codeId = scanCodeInfoMO.getCodeId();
		String codeTypeId = scanCodeInfoMO.getCodeTypeId();
		String openId = scanCodeInfoMO.getOpenId();
		String productId = scanCodeInfoMO.getProductId();
		String productBatchId = scanCodeInfoMO.getProductBatchId();
		boolean acquireLock =false;
		String lockKey = activitySetId + ":" + codeId + ":" + codeTypeId;
		try {
			acquireLock = lock.lock(lockKey, 5000, 5, 200);
			if(!acquireLock) {
				log.error("{???????????????:" +lockKey+ ",?????????}");
				redisUtil.hmSet("marketing:lock:fail",lockKey,new Date());
				return lotteryOprationDto.lotterySuccess("??????????????????,???????????????");
			}
			String opneIdNoSpecialChactar = StringUtils.isBlank(openId)? null:CommonUtil.replaceSpicialChactar(openId);
			long codeCount = codeEsService.countByCode(codeId, codeTypeId, memberType);
			if (codeCount > 0) {
				return lotteryOprationDto.lotterySuccess("????????????????????????????????????????????????");
			}
			long nowTimeMills = DateUtils.parseDate(DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.format(System.currentTimeMillis()),"yyyy-MM-dd").getTime();
			//????????????????????????????????????????????????
			if (eachDayNumber != null && eachDayNumber > 0) {
				long userscanNum = codeEsService.countByUserAndActivityQuantum(opneIdNoSpecialChactar, activitySetId, nowTimeMills);
				log.info("????????????=====?????????openId="+opneIdNoSpecialChactar+",activitySetId="+activitySetId+"????????????????????????????????????="+userscanNum+",????????????????????????????????????"+eachDayNumber);
				if (userscanNum >= eachDayNumber) {
					return lotteryOprationDto.lotterySuccess("?????????????????????????????????????????????");
				}
			}
			codeEsService.addScanCodeRecord(opneIdNoSpecialChactar, productId, productBatchId, codeId, codeTypeId, activitySetId,nowTimeMills,organizationId,0,memberId);
			lotteryOprationDto.setSuccessLottory(1);
			return lotteryOprationDto;
		} catch (Exception e){
			log.error("?????????????????????????????????", e);
			return lotteryOprationDto.lotterySuccess("??????????????????,???????????????");
		} finally {
			if(acquireLock){
				lock.releaseLock(lockKey);
			}
		}
	}

	/////////////////////////////////////////ES???????????????/////////////////////////////////////////////////////

	private Float prizeTypeMo(MarketingPrizeTypeMO mPrizeTypeMO) {
		Float amount=mPrizeTypeMO.getPrizeAmount();
		Byte randAmount=mPrizeTypeMO.getIsRrandomMoney();
		//??????????????????????????????????????????
		if (randAmount.equals((byte)1)) {
			float min=mPrizeTypeMO.getLowRand();
			float max=mPrizeTypeMO.getHighRand();
			amount = new Random().nextFloat() *((max-min)) +min;
		}
		Float finalAmount = amount;//??????????????????
		return finalAmount;
	}

	private void weixinpay(byte sendAudit, String winningCode, String mobile, String openId, String organizationId, Float finalAmount, String remoteAddr, byte referenceRole)
			throws SuperCodeException, Exception {
		if (StringUtils.isBlank(openId)) {
			throw  new SuperCodeException("????????????openid????????????",500);
		}
		log.error("{ ??????????????????????????????=> + " + mobile +"==}");
		//???????????????
		String partner_trade_no=wXPayTradeNoGenerator.tradeNo();
		//????????????
		SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		WXPayTradeOrder tradeOrder=new WXPayTradeOrder();
		tradeOrder.setAmount(finalAmount);
		tradeOrder.setOpenId(openId);
		tradeOrder.setTradeStatus((byte)0);
		tradeOrder.setPartnerTradeNo(partner_trade_no);
		tradeOrder.setTradeDate(format.format(new Date()));
		tradeOrder.setOrganizationId(organizationId);
		tradeOrder.setWinningCode(winningCode);
		tradeOrder.setReferenceRole(referenceRole);
		if (StringUtils.isBlank(remoteAddr)) {
			remoteAddr = serverIp;
		}
		tradeOrder.setRemoteAddr(remoteAddr);
		wXPayTradeOrderMapper.insert(tradeOrder);
		//???????????????????????????????????????????????????????????????????????????
		if (sendAudit == 0) {
			wxpService.qiyePay(openId, remoteAddr, finalAmount.intValue(), partner_trade_no, organizationId);
		}
	}

	public RestResult<LotteryResultMO> previewLottery(String uuid, HttpServletRequest request) throws SuperCodeException {
		RestResult<LotteryResultMO> restResult = new RestResult<>();
		String value = redisUtil.get(RedisKey.ACTIVITY_PREVIEW_PREFIX + uuid);
		if (StringUtils.isBlank(value)) {
			restResult.setState(200);
			restResult.setMsg("??????????????????????????????????????????");
			return restResult;
		}
		MarketingActivityPreviewParam mPreviewParam = JSONObject.parseObject(value,
				MarketingActivityPreviewParam.class);
		List<MarketingPrizeTypeParam> moPrizeTypes = mPreviewParam.getMarketingPrizeTypeParams();

		List<MarketingPrizeTypeMO> mList = new ArrayList<>(moPrizeTypes.size());
		int sumprizeProbability = 0;
		for (MarketingPrizeTypeParam marketingPrizeTypeParam : moPrizeTypes) {
			Integer prizeProbability = marketingPrizeTypeParam.getPrizeProbability();
			MarketingPrizeTypeMO mPrizeType = new MarketingPrizeTypeMO();
			mPrizeType.setPrizeAmount(marketingPrizeTypeParam.getPrizeAmount());
			mPrizeType.setPrizeProbability(prizeProbability);
			mPrizeType.setPrizeTypeName(marketingPrizeTypeParam.getPrizeTypeName());
			mPrizeType.setIsRrandomMoney(marketingPrizeTypeParam.getIsRrandomMoney());
			mPrizeType.setRealPrize((byte) 1);
			mPrizeType.setLowRand(marketingPrizeTypeParam.getLowRand());
			mPrizeType.setHighRand(marketingPrizeTypeParam.getHighRand());
			mPrizeType.setAwardType(marketingPrizeTypeParam.getAwardType());
			mPrizeType.setAwardIntegralNum(marketingPrizeTypeParam.getAwardIntegralNum());
			mPrizeType.setCardLink(marketingPrizeTypeParam.getCardLink());
			mPrizeType.setRemainingStock(marketingPrizeTypeParam.getRemainingStock());
			mList.add(mPrizeType);
			sumprizeProbability += prizeProbability;
		}
		if (sumprizeProbability > 100) {
			throw new SuperCodeException("???????????????????????????????????????100", 500);
		} else if (sumprizeProbability < 100) {
			int i = 100 - sumprizeProbability;
			MarketingPrizeTypeMO NoReal = new MarketingPrizeTypeMO();
			NoReal.setPrizeAmount((float) 0);
			NoReal.setPrizeProbability(i);
			NoReal.setPrizeTypeName("?????????");
			NoReal.setIsRrandomMoney((byte) 0);
			NoReal.setRealPrize((byte) 0);
			mList.add(NoReal);
		}

		// ??????????????????
		MarketingPrizeTypeMO mPrizeTypeMO = LotteryUtilWithOutCodeNum.startLottery(mList);
		LotteryResultMO lResultMO=new LotteryResultMO();
		// ??????realprize?????????0,0?????????????????????????????????????????????????????????????????????
		Byte realPrize = mPrizeTypeMO.getRealPrize();
		if (realPrize.equals((byte) 0)) {
			restResult.setState(200);
			lResultMO.setWinnOrNot(0);
			lResultMO.setMsg("????????????????????????????????????????????????????????????????????????");
		} else {
			Byte awardType = mPrizeTypeMO.getAwardType();
			if (null==awardType ||awardType.intValue()==4 ) {
				restResult.setState(200);
				lResultMO.setWinnOrNot(1);
				lResultMO.setData(mPrizeTypeMO.getPrizeAmount());
				lResultMO.setAwardType((byte)4);
				restResult.setResults(lResultMO);
				return restResult;
			}
			lResultMO.setAwardType(awardType);
			lResultMO.setWinnOrNot(1);
			// ??????????????????????????????????????????????????????
			try {
				switch (awardType.intValue()) {
				case 1:// ??????
					lResultMO.setMsg("??????????????????" + mPrizeTypeMO.getPrizeTypeName());
					break;
				case 2: // ??????
					lResultMO.setMsg("??????????????????" + mPrizeTypeMO.getPrizeTypeName());
					lResultMO.setData(mPrizeTypeMO.getCardLink());
					break;
				case 3: // ??????
					Integer awardIntegralNum = mPrizeTypeMO.getAwardIntegralNum();
					lResultMO.setMsg("??????????????????" + awardIntegralNum + "??????");
					lResultMO.setData(awardIntegralNum);
					break;
				case 9:// ??????
					lResultMO.setMsg("??????????????????" + mPrizeTypeMO.getPrizeTypeName());
					break;
				default:
					System.out.println(1);
					break;
				}
			} catch (Exception e) {
			}
		}
		restResult.setResults(lResultMO);
		restResult.setState(200);
		return restResult;
	}

}
