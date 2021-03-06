package com.jgw.supercodeplatform.marketing.service.activity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.zxing.WriterException;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.check.activity.StandActicityParamCheck;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.MarketingSalerActivitySetMO;
import com.jgw.supercodeplatform.marketing.common.model.activity.ProductAndBatchGetCodeMO;
import com.jgw.supercodeplatform.marketing.common.model.activity.ScanCodeInfoMO;
import com.jgw.supercodeplatform.marketing.common.page.AbstractPageService;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.DateUtil;
import com.jgw.supercodeplatform.marketing.config.redis.RedisUtil;
import com.jgw.supercodeplatform.marketing.constants.ActivityDefaultConstant;
import com.jgw.supercodeplatform.marketing.constants.BusinessTypeEnum;
import com.jgw.supercodeplatform.marketing.constants.RedisKey;
import com.jgw.supercodeplatform.marketing.constants.WechatConstants;
import com.jgw.supercodeplatform.marketing.dao.activity.*;
import com.jgw.supercodeplatform.marketing.dto.DaoSearchWithOrganizationIdParam;
import com.jgw.supercodeplatform.marketing.dto.MarketingSalerActivityCreateParam;
import com.jgw.supercodeplatform.marketing.dto.activity.*;
import com.jgw.supercodeplatform.marketing.enums.market.ActivityIdEnum;
import com.jgw.supercodeplatform.marketing.enums.market.MemberTypeEnums;
import com.jgw.supercodeplatform.marketing.enums.market.ReferenceRoleEnum;
import com.jgw.supercodeplatform.marketing.pojo.*;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import com.jgw.supercodeplatform.marketing.vo.activity.ReceivingAndWinningPageVO;
import com.jgw.supercodeplatform.pojo.cache.AccountCache;
import com.jgw.supercodeplatform.prizewheels.infrastructure.feigns.GetSbatchIdsByPrizeWheelsFeign;
import com.jgw.supercodeplatform.prizewheels.infrastructure.feigns.dto.SbatchUrlDto;
import com.jgw.supercodeplatform.prizewheels.infrastructure.feigns.dto.SbatchUrlUnBindDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MarketingActivitySetService extends AbstractPageService<DaoSearchWithOrganizationIdParam> {
 
	@Autowired
	private MarketingActivitySetMapper mSetMapper;

	@Autowired
	private MarketingWinningPageMapper marWinningPageMapper;

	@Autowired
	private MarketingReceivingPageMapper maReceivingPageMapper;

	@Autowired
	private MarketingPrizeTypeMapper mPrizeTypeMapper;

	@Autowired
	private MarketingChannelMapper mChannelMapper;

	@Autowired
	private MarketingActivityProductMapper mProductMapper;

	@Autowired
	private StandActicityParamCheck standActicityParamCheck;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private RedisUtil redisUtil;

	@Autowired
	private CommonService commonService;

	@Autowired
	private MarketingActivityChannelService channelService;

    @Value("${rest.codemanager.url}")
	private String codeManagerUrl;

	@Value("${marketing.domain.url}")
	private String marketingDomain;

	@Autowired
	private GetSbatchIdsByPrizeWheelsFeign getSbatchIdsByPrizeWheelsFeign;

	/**
	 * ????????????id?????????????????????????????????
	 * @param activitySetId
	 * @return
	 */
	public RestResult<ReceivingAndWinningPageVO> getPageInfo(Long activitySetId) {
		MarketingWinningPage marWinningPage=marWinningPageMapper.getByActivityId(activitySetId);
		MarketingReceivingPage mReceivingPage=maReceivingPageMapper.getByActivityId(activitySetId);
		MarketingActivitySet marketingActivitySet = mSetMapper.selectById(activitySetId);
		String merchantsInfo = marketingActivitySet.getMerchantsInfo();
		if (StringUtils.isNotBlank(merchantsInfo) && merchantsInfo.length() > 2) {
			JSONObject merchantJson = JSON.parseObject(merchantsInfo);
			mReceivingPage.setMchAppid(merchantJson.getString("mchAppid"));
			mReceivingPage.setMerchantSecret(merchantJson.getString("merchantSecret"));
		}
		RestResult<ReceivingAndWinningPageVO> restResult=new RestResult<ReceivingAndWinningPageVO>();
		ReceivingAndWinningPageVO rePageVO=new ReceivingAndWinningPageVO();
		rePageVO.setMaReceivingPage(mReceivingPage);
		rePageVO.setMaWinningPage(marWinningPage);
		restResult.setState(200);
		restResult.setResults(rePageVO);
		restResult.setMsg("??????");
		return restResult;
	}
	/**
	 * ??????????????????
	 * @param activitySetParam
	 * @return
	 * @throws SuperCodeException
	 */
	@Transactional(rollbackFor = Exception.class)
	public RestResult<String> memberActivityAdd(MarketingActivityCreateParam activitySetParam) throws SuperCodeException {
		MarketingActivitySet mActivitySet =baseAdd(activitySetParam);
		//???????????????
		MarketingReceivingPageParam mReceivingPageParam=activitySetParam.getmReceivingPageParam();
		saveReceivingPage(mReceivingPageParam,mActivitySet.getId());

		RestResult<String> restResult=new RestResult<String>();
		restResult.setState(200);
		restResult.setMsg("??????");
		return restResult;
	}

	/**
	 *
	 * @param activitySetParam
	 * @return
	 * @throws SuperCodeException
	 */
	public RestResult<String> guideActivityAdd(MarketingActivityCreateParam activitySetParam) throws SuperCodeException {
		baseAdd(activitySetParam);
		RestResult<String> restResult=new RestResult<String>();
		restResult.setState(200);
		restResult.setMsg("??????");
		return restResult;

	}
	/**
	 * ????????????
	 * @param activitySetParam
	 * @return
	 * @throws SuperCodeException
	 */
	public MarketingActivitySet baseAdd(MarketingActivityCreateParam activitySetParam) throws SuperCodeException {

		String organizationId=commonUtil.getOrganizationId();
		String organizationName=commonUtil.getOrganizationName();
		List<MarketingChannelParam> mChannelParams=activitySetParam.getmChannelParams();
		List<MarketingActivityProductParam> maProductParams=activitySetParam.getmProductParams();
		//??????????????????
		List<MarketingPrizeTypeParam>mPrizeTypeParams=activitySetParam.getMarketingPrizeTypeParams();
		MarketingActivitySetParam setParam = activitySetParam.getmActivitySetParam();
		MarketingActivitySet existmActivitySet =mSetMapper.selectByTitleOrgId(setParam.getActivityTitle(),organizationId);
		if (null!=existmActivitySet) {
			throw new SuperCodeException("??????????????????????????????????????????????????????", 500);
		}
		//??????????????????
		MarketingActivitySet mActivitySet = convertActivitySet(activitySetParam.getmActivitySetParam(),activitySetParam.getmReceivingPageParam(), organizationId,organizationName);
		
		//??????????????????
		standActicityParamCheck.basePrizeTypeCheck(mPrizeTypeParams);

		//????????????
	    standActicityParamCheck.baseProductBatchCheck(maProductParams);
		//??????????????????????????????????????????????????????
		saveProductBatchs(maProductParams, mActivitySet, ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
		Long activitySetId= mActivitySet.getId();
		if (null!=mChannelParams && mChannelParams.size()!=0) {
			//????????????
			saveChannels(mChannelParams,activitySetId);
		}
		//????????????
		savePrizeTypes(mPrizeTypeParams,activitySetId);
		return mActivitySet;
	}
	/**
	 * ????????????????????????????????????????????? ?????????????????????
	 * @param activitySetParam
	 * @return
	 * @throws SuperCodeException
	 */
	@Transactional
	public RestResult<String> update(MarketingActivityCreateParam activitySetParam) throws SuperCodeException {
		String organizationId=commonUtil.getOrganizationId();
		String organizationName=commonUtil.getOrganizationName();
		List<MarketingChannelParam> mChannelParams=activitySetParam.getmChannelParams();
		List<MarketingActivityProductParam> maProductParams=activitySetParam.getmProductParams();
		//??????????????????
		List<MarketingPrizeTypeParam> mPrizeTypeParams=activitySetParam.getMarketingPrizeTypeParams();
		
		MarketingActivitySetParam mSetParam=activitySetParam.getmActivitySetParam();
		Long activitySetId=mSetParam.getId();
		if (null == activitySetId) {
			throw new SuperCodeException("????????????id????????????", 500);
		}
		MarketingReceivingPageParam mReceivingPageParam=activitySetParam.getmReceivingPageParam();
		//??????????????????
		MarketingActivitySet mActivitySet = convertActivitySet(mSetParam,activitySetParam.getmReceivingPageParam(),organizationId,organizationName);
		List<MarketingActivityProduct> upProductList = mProductMapper.selectByActivitySetId(activitySetId);
        if(upProductList == null) {
            upProductList = new ArrayList<>();
        }
		/************************?????????????????????????????????????????????????????????************************/
		//????????????????????????
		List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs = new ArrayList<ProductAndBatchGetCodeMO>();
		List<MarketingActivityProduct> mList = new ArrayList<MarketingActivityProduct>();
		for (MarketingActivityProductParam marketingActivityProductParam : maProductParams) {
			String productId = marketingActivityProductParam.getProductId();
			List<ProductBatchParam> batchParams = marketingActivityProductParam.getProductBatchParams();
			ProductAndBatchGetCodeMO productAndBatchGetCodeMO = new ProductAndBatchGetCodeMO();
			List<Map<String, String>> productBatchList = new ArrayList<Map<String, String>>();
			if (CollectionUtils.isEmpty(batchParams)) {
				MarketingActivityProduct mActivityProduct = new MarketingActivityProduct();
				mActivityProduct.setProductId(marketingActivityProductParam.getProductId());
				mActivityProduct.setProductName(marketingActivityProductParam.getProductName());
				mActivityProduct.setReferenceRole(ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
				mList.add(mActivityProduct);
			} else {
				for (ProductBatchParam prBatchParam : batchParams) {
					String productBatchId = prBatchParam.getProductBatchId();
					MarketingActivityProduct mActivityProduct = new MarketingActivityProduct();
					mActivityProduct.setActivitySetId(activitySetId);
					mActivityProduct.setProductBatchId(productBatchId);
					mActivityProduct.setProductBatchName(prBatchParam.getProductBatchName());
					mActivityProduct.setProductId(marketingActivityProductParam.getProductId());
					mActivityProduct.setProductName(marketingActivityProductParam.getProductName());
					mActivityProduct.setReferenceRole(ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
					mList.add(mActivityProduct);
					// ?????????????????????????????????????????????????????????
					Map<String, String> batchmap = new HashMap<String, String>();
					batchmap.put("productBatchId", prBatchParam.getProductBatchId());
					productBatchList.add(batchmap);
				}
			}
			// ???????????????????????????????????????????????????
			productAndBatchGetCodeMO.setProductBatchList(productBatchList);
			productAndBatchGetCodeMO.setProductId(productId);
			productAndBatchGetCodeMOs.add(productAndBatchGetCodeMO);
		}
		List<SbatchUrlUnBindDto> deleteProductBatchList = new ArrayList<>();
		//?????????????????????url???product
		List<MarketingActivityProduct> maProductList = mProductMapper.selectByProductAndBatch(mList, ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
		if(maProductList == null) {
            maProductList = new ArrayList<>();
        }
		maProductList.addAll(upProductList);
		List<MarketingActivityProduct> marketingActivityProductList = maProductList.stream().distinct().collect(Collectors.toList());
		StringBuffer sbatchIdBuffer = new StringBuffer();
		if(!CollectionUtils.isEmpty(marketingActivityProductList)) {
			marketingActivityProductList.forEach(marketingActivityProduct -> {
				String sbatchIds = marketingActivityProduct.getSbatchId();
				if (StringUtils.isNotBlank(sbatchIds)) {
					String[] sbatchIdArray = sbatchIds.split(",");
					for(String sbatchId : sbatchIdArray) {
						SbatchUrlUnBindDto sbatchUrlDto = new SbatchUrlUnBindDto();
						sbatchUrlDto.setUrl(marketingDomain + WechatConstants.SCAN_CODE_JUMP_URL);
						sbatchUrlDto.initAllBusinessType();
						sbatchUrlDto.setBatchId(Long.parseLong(sbatchId));
						sbatchUrlDto.setClientRole(MemberTypeEnums.VIP.getType()+"");
						sbatchUrlDto.setProductId(marketingActivityProduct.getProductId());
						sbatchUrlDto.setProductBatchId(marketingActivityProduct.getProductBatchId());
						deleteProductBatchList.add(sbatchUrlDto);
					}
				}
			});
		}
//		if(sbatchIdBuffer.length() > 0) {
//			String sbatchIds = sbatchIdBuffer.substring(1);
//			String[] sbatchIdArray = sbatchIds.split(",");
//			for(String sbatchId : sbatchIdArray) {
//				SbatchUrlUnBindDto sbatchUrlDto = new SbatchUrlUnBindDto();
//				sbatchUrlDto.setUrl(marketingDomain + WechatConstants.SCAN_CODE_JUMP_URL);
//				sbatchUrlDto.initAllBusinessType();
//				sbatchUrlDto.setBatchId(Long.parseLong(sbatchId));
//				sbatchUrlDto.setClientRole(MemberTypeEnums.VIP.getType()+"");
//				deleteProductBatchList.add(sbatchUrlDto);
//			}
//		}
		/***************************************************/
		mSetMapper.update(mActivitySet);
		mPrizeTypeMapper.deleteByActivitySetId(activitySetId);
		mProductMapper.deleteByActivitySetId(activitySetId);
		mChannelMapper.deleteByActivitySetId(activitySetId);
		updatePage(mReceivingPageParam);
		//??????????????????
		standActicityParamCheck.basePrizeTypeCheck(mPrizeTypeParams);
		//????????????
	    standActicityParamCheck.baseProductBatchCheck(maProductParams);
		mList.forEach(prd -> prd.setActivitySetId(mActivitySet.getId()));
		//??????????????????????????????????????????????????????
		saveProductBatchs(productAndBatchGetCodeMOs, deleteProductBatchList, mList, 0);
		if (null!=mChannelParams && mChannelParams.size()!=0) {
			//????????????
			saveChannels(mChannelParams,activitySetId);
		}
		
		//????????????
		savePrizeTypes(mPrizeTypeParams,activitySetId);
		RestResult<String> restResult=new RestResult<String>();
		restResult.setState(200);
		restResult.setMsg("??????");
		return restResult;
	}



	public MarketingActivitySet convertActivitySet(MarketingActivitySetParam activitySetParam,MarketingReceivingPageParam marketingReceivingPageParam,  String organizationId, String organizationName) throws SuperCodeException {
		String title=activitySetParam.getActivityTitle();
		if (StringUtils.isBlank(title)) {
			throw new SuperCodeException("???????????????????????????????????????", 500);
		}
		activityTimeCheck(activitySetParam.getActivityStartDate(),activitySetParam.getActivityEndDate());
		Long id=activitySetParam.getId();
		MarketingActivitySet mSet=new MarketingActivitySet();
		// ????????????????????????
		AccountCache userLoginCache = commonUtil.getUserLoginCache();
		mSet.setUpdateUserId(userLoginCache.getUserId());
		mSet.setUpdateUserName(userLoginCache.getUserName());
		String activityStartDate = StringUtils.isBlank(activitySetParam.getActivityStartDate())?null:activitySetParam.getActivityStartDate();
		String activityEndDate = StringUtils.isBlank(activitySetParam.getActivityEndDate())?null:activitySetParam.getActivityEndDate();
		mSet.setActivityEndDate(activityEndDate);
		mSet.setActivityId(activitySetParam.getActivityId());
		mSet.setActivityRangeMark(activitySetParam.getActivityRangeMark());
		mSet.setActivityStartDate(activityStartDate);
		mSet.setActivityTitle(title);
		mSet.setAutoFetch(activitySetParam.getAutoFetch());
		mSet.setId(id);
		mSet.setActivityDesc(activitySetParam.getActivityDesc());
		MarketingActivitySetCondition condition = new MarketingActivitySetCondition();
		condition.setEachDayNumber(activitySetParam.getEachDayNumber() == null?200:activitySetParam.getEachDayNumber());
		condition.setConsumeIntegral(activitySetParam.getConsumeIntegralNum());
		condition.setParticipationCondition(activitySetParam.getParticipationCondition());
		mSet.setValidCondition(condition.toJsonString());
		// ??????????????????????????????????????????????????????????????????????????????
		mSet.setActivityStatus(1);
		mSet.setOrganizationId(organizationId);
		mSet.setOrganizatioIdlName(organizationName);
		JSONObject merchantJson = new JSONObject();
		merchantJson.put("mchAppid", marketingReceivingPageParam.getMchAppid());
		merchantJson.put("merchantSecret", marketingReceivingPageParam.getMerchantSecret());
		mSet.setMerchantsInfo(merchantJson.toJSONString());
		return mSet;
	}

	/**
	 * ????????????????????????
	 * @param
	 * @throws SuperCodeException
	 */
	private void activityTimeCheck(String activityStartDate,String activityEndDate) throws SuperCodeException {
		// ??????????????????????????????
		if(StringUtils.isBlank(activityStartDate)){
			activityStartDate=null;
        }

		if(StringUtils.isBlank(activityEndDate)){
			activityEndDate=null;
        }
		Date endDate = null;
		Date startDate = null;
		SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
        if (null==activityStartDate && null==activityEndDate) {
        	activityEndDate=format.format(new Date());
		}else if (null!=activityStartDate && null!=activityEndDate) {
			try {
				endDate = DateUtil.parse(activityEndDate,"yyyy-MM-dd");
				startDate = DateUtil.parse(activityStartDate,"yyyy-MM-dd");
				if(startDate.after(endDate)){
					throw new SuperCodeException("???????????????????????????",500);
				}
			} catch (ParseException e) {
				throw new SuperCodeException("???????????????????????????",500);
			}
		}else {
			throw new SuperCodeException("??????????????????????????????????????????",500);
		}
	}
	/**
	 * ???????????????
	 * @param mReceivingPageParam
	 * @param activitySetId
	 */
	private void saveReceivingPage(MarketingReceivingPageParam mReceivingPageParam, Long activitySetId) throws SuperCodeException {
		// ??????
		if (StringUtils.isBlank(mReceivingPageParam.getTemplateId())){
			throw new SuperCodeException("?????????????????????", 500);
		}
		if (activitySetId == null || activitySetId <= 0){
			throw new SuperCodeException("?????????????????????", 500);
		}
		if (mReceivingPageParam.getIsReceivePage() == null ){
			throw new SuperCodeException("?????????????????????", 500);
		}
		if (mReceivingPageParam.getIsQrcodeView() == null ){
			throw new SuperCodeException("?????????????????????", 500);
		}

		// ??????
		MarketingReceivingPage mPage=new MarketingReceivingPage();
		mPage.setIsQrcodeView(mReceivingPageParam.getIsQrcodeView());
		mPage.setIsReceivePage(mReceivingPageParam.getIsReceivePage());
		mPage.setPicAddress(mReceivingPageParam.getPicAddress());
		mPage.setActivitySetId(activitySetId);
		mPage.setQrcodeUrl(mReceivingPageParam.getQrcodeUrl());
		mPage.setTemplateId(mReceivingPageParam.getTemplateId());
		mPage.setTextContent(mReceivingPageParam.getTextContent());
		mPage.setFlipTimes(mReceivingPageParam.getFlipTimes());
		maReceivingPageMapper.insert(mPage);
	}

	/**
	 * ??????????????????
	 * @param mPrizeTypeParams
	 * @param activitySetId
	 * @throws SuperCodeException
	 */
	private void savePrizeTypes(List<MarketingPrizeTypeParam> mPrizeTypeParams, Long activitySetId) throws SuperCodeException {

		List<MarketingPrizeType> mList=new ArrayList<MarketingPrizeType>(mPrizeTypeParams.size());
		int sumprizeProbability=0;
		for (MarketingPrizeTypeParam marketingPrizeTypeParam : mPrizeTypeParams) {
			Integer prizeProbability=marketingPrizeTypeParam.getPrizeProbability();
			MarketingPrizeType mPrizeType=new MarketingPrizeType();
			mPrizeType.setActivitySetId(activitySetId);
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
			sumprizeProbability+=prizeProbability;
		}
		if (sumprizeProbability>100) {
			throw new SuperCodeException("???????????????????????????????????????100", 500);
		}else if (sumprizeProbability<100){
			int i = 100-sumprizeProbability;
			MarketingPrizeType NoReal=new MarketingPrizeType();
			NoReal.setActivitySetId(activitySetId);
			NoReal.setPrizeAmount((float)0);
			NoReal.setPrizeProbability(i);
			NoReal.setPrizeTypeName("?????????");
			NoReal.setIsRrandomMoney((byte) 0);
			NoReal.setRealPrize((byte) 0);
			mList.add(NoReal);
		}
		mPrizeTypeMapper.batchInsert(mList);
	}
	/**
	 * ??????????????????
	 * @param
	 * @param
	 * @param
	 * @return
	 * @throws SuperCodeException
	 */
	public void saveProductBatchs(List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs, List<SbatchUrlUnBindDto> deleteProductBatchList, List<MarketingActivityProduct> mList, int referenceRole) throws SuperCodeException {
		//????????????????????????????????????????????????????????????
		String superToken = commonUtil.getSuperToken();
		log.info("??????????????????????????????????????????????????????{}", JSON.toJSONString(productAndBatchGetCodeMOs));
		JSONArray arr = commonService.getBatchInfo(productAndBatchGetCodeMOs, superToken, WechatConstants.CODEMANAGER_GET_BATCH_CODE_INFO_URL);
		log.info("??????????????????????????????????????????????????????{}", arr.toJSONString());
		String bindUrl = marketingDomain + WechatConstants.SCAN_CODE_JUMP_URL;
		if (referenceRole == ReferenceRoleEnum.ACTIVITY_SALER.getType().intValue()) {
			bindUrl = marketingDomain + WechatConstants.SALER_SCAN_CODE_JUMP_URL;
		}
		List<SbatchUrlDto> paramsList = commonService.getUrlToBatchDto(arr,bindUrl,
				BusinessTypeEnum.MARKETING_ACTIVITY.getBusinessType(), referenceRole);
		if(!CollectionUtils.isEmpty(deleteProductBatchList)) {
			RestResult<Object> objectRestResult = getSbatchIdsByPrizeWheelsFeign.removeOldProduct(deleteProductBatchList);
			log.info("?????????????????????{}", JSON.toJSONString(objectRestResult));
			if (objectRestResult == null || objectRestResult.getState().intValue() != 200) {
				throw new SuperCodeException("??????????????????????????????url?????????" + objectRestResult, 500);
			}
		}
		// ?????????????????????url
		RestResult bindBatchobj = getSbatchIdsByPrizeWheelsFeign.bindingUrlAndBizType(paramsList);
		Integer batchstate = bindBatchobj.getState();
		if (ObjectUtils.notEqual(batchstate, HttpStatus.SC_OK)) {
			throw new SuperCodeException("??????????????????????????????url?????????" + JSON.toJSONString(bindBatchobj), 500);
		}
		Map<String, Map<String, Object>> paramsMap = commonService.getUrlToBatchParamMap(arr,bindUrl,
				BusinessTypeEnum.MARKETING_ACTIVITY.getBusinessType());
		mList.forEach(marketingActivityProduct -> {
			String key = marketingActivityProduct.getProductId()+","+marketingActivityProduct.getProductBatchId();
			Map<String, Object> batchMap = paramsMap.get(key);
			if(batchMap != null) {
                marketingActivityProduct.setSbatchId((String)batchMap.get("batchId"));
            }
		});
		//??????????????????????????????
		mProductMapper.batchDeleteByProBatchsAndRole(mList, referenceRole);
		mProductMapper.activityProductInsert(mList);
	}

	public void saveProductBatchs(List<MarketingActivityProductParam> maProductParams, MarketingActivitySet activitySet, int referenceRole) throws SuperCodeException {
		/************************?????????????????????????????????????????????????????????************************/
		//????????????????????????
		List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs = new ArrayList<ProductAndBatchGetCodeMO>();
		List<MarketingActivityProduct> mList = new ArrayList<MarketingActivityProduct>();
		for (MarketingActivityProductParam marketingActivityProductParam : maProductParams) {
			String productId = marketingActivityProductParam.getProductId();
			List<ProductBatchParam> batchParams = marketingActivityProductParam.getProductBatchParams();
			ProductAndBatchGetCodeMO productAndBatchGetCodeMO = new ProductAndBatchGetCodeMO();
			List<Map<String, String>> productBatchList = new ArrayList<Map<String, String>>();
			if (CollectionUtils.isEmpty(batchParams)) {
				MarketingActivityProduct mActivityProduct = new MarketingActivityProduct();
				mActivityProduct.setProductId(marketingActivityProductParam.getProductId());
				mActivityProduct.setProductName(marketingActivityProductParam.getProductName());
				mActivityProduct.setReferenceRole(ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
				mList.add(mActivityProduct);
            } else {
				for (ProductBatchParam prBatchParam : batchParams) {
					String productBatchId = prBatchParam.getProductBatchId();
					MarketingActivityProduct mActivityProduct = new MarketingActivityProduct();
					mActivityProduct.setProductBatchId(productBatchId);
					mActivityProduct.setProductBatchName(prBatchParam.getProductBatchName());
					mActivityProduct.setProductId(marketingActivityProductParam.getProductId());
					mActivityProduct.setProductName(marketingActivityProductParam.getProductName());
					mActivityProduct.setReferenceRole(ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
					mList.add(mActivityProduct);
					// ?????????????????????????????????????????????????????????
					Map<String, String> batchmap = new HashMap<String, String>();
					batchmap.put("productBatchId", prBatchParam.getProductBatchId());
					productBatchList.add(batchmap);
				}
			}
            // ???????????????????????????????????????????????????
            productAndBatchGetCodeMO.setProductBatchList(productBatchList);
            productAndBatchGetCodeMO.setProductId(productId);
            productAndBatchGetCodeMOs.add(productAndBatchGetCodeMO);
		}
		List<SbatchUrlUnBindDto> deleteProductBatchList = new ArrayList<>();
		//?????????????????????url???product
		List<MarketingActivityProduct> marketingActivityProductList = mProductMapper.selectByProductAndBatch(mList, ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
		//???????????????sbatchId
		if(!CollectionUtils.isEmpty(marketingActivityProductList)) {
			marketingActivityProductList.forEach(marketingActivityProduct -> {
				String sbatchIds = marketingActivityProduct.getSbatchId();
				if (StringUtils.isNotBlank(sbatchIds)) {
					String[] sbatchIdArray = sbatchIds.split(",");
					for(String sbatchId : sbatchIdArray) {
						SbatchUrlUnBindDto sbatchUrlDto = new SbatchUrlUnBindDto();
						sbatchUrlDto.setUrl(marketingDomain + WechatConstants.SCAN_CODE_JUMP_URL);
						sbatchUrlDto.initAllBusinessType();
						sbatchUrlDto.setBatchId(Long.parseLong(sbatchId));
						sbatchUrlDto.setClientRole(MemberTypeEnums.VIP.getType() + "");
						sbatchUrlDto.setProductBatchId(marketingActivityProduct.getProductBatchId());
						sbatchUrlDto.setProductId(marketingActivityProduct.getProductId());
						deleteProductBatchList.add(sbatchUrlDto);
					}
				}
			});
		}
//		if(sbatchIdBuffer.length() > 0) {
//			String sbatchIds = sbatchIdBuffer.substring(1);
//			String[] sbatchIdArray = sbatchIds.split(",");
//			for(String sbatchId : sbatchIdArray) {
//				SbatchUrlUnBindDto sbatchUrlDto = new SbatchUrlUnBindDto();
//				sbatchUrlDto.setUrl(marketingDomain + WechatConstants.SCAN_CODE_JUMP_URL);
//				sbatchUrlDto.initAllBusinessType();
//				sbatchUrlDto.setBatchId(Long.parseLong(sbatchId));
//				sbatchUrlDto.setClientRole(MemberTypeEnums.VIP.getType()+"");
//				deleteProductBatchList.add(sbatchUrlDto);
//			}
//		}
		mSetMapper.insert(activitySet);
		mList.forEach(prd -> prd.setActivitySetId(activitySet.getId()));
		saveProductBatchs(productAndBatchGetCodeMOs, deleteProductBatchList, mList, referenceRole);
	}

	/**
	 * ??????????????????
	 * @param mChannelParams
	 * @param activitySetId
	 * @throws SuperCodeException
	 */
	private void saveChannels(List<MarketingChannelParam> mChannelParams,Long activitySetId) throws SuperCodeException {
		if(!CollectionUtils.isEmpty(mChannelParams)) {
			List<MarketingChannel> mList=new ArrayList<MarketingChannel>();
			//????????????
			for (MarketingChannelParam marketingChannelParam : mChannelParams) {
				Byte customerType=marketingChannelParam.getCustomerType();
				// ??????????????????customerId??????customerCode
				String customerId=marketingChannelParam.getCustomerId();
				MarketingChannel mChannel=new MarketingChannel();
				mChannel.setActivitySetId(activitySetId);
				mChannel.setCustomerId(marketingChannelParam.getCustomerId());
				mChannel.setCustomerName(marketingChannelParam.getCustomerName());
				mChannel.setCustomerSuperior(marketingChannelParam.getCustomerSuperior());
				mChannel.setCustomerSuperiorType(marketingChannelParam.getCustomerSuperiorType());
				mChannel.setCustomerType(customerType);
				mList.add(mChannel);
				List<MarketingChannelParam> childrens=marketingChannelParam.getChildrens();
				recursiveCreateChannel(customerId,customerType,activitySetId,childrens,mList);
			}
			mChannelMapper.batchInsert(mList);
		}
	}


	/**
	 * ????????????????????????
	 * @param parentCustomerCode
	 * @param parentCustomerType
	 * @param activitySetId
	 * @param childrens
	 * @param mList
	 */
	private void recursiveCreateChannel(String parentCustomerCode, Byte parentCustomerType, Long activitySetId,
										List<MarketingChannelParam> childrens, List<MarketingChannel> mList) {
		if (null==childrens || childrens.isEmpty()) {
			return;
		}
		//????????????
		for (MarketingChannelParam marketingChannelParam : childrens) {
			Byte customerType=marketingChannelParam.getCustomerType();
			// ???????????????CustomerId???????????????customerCode
			String customerId=marketingChannelParam.getCustomerId();
			MarketingChannel mChannel=new MarketingChannel();
			mChannel.setActivitySetId(activitySetId);
			mChannel.setCustomerId(customerId);
			mChannel.setCustomerName(marketingChannelParam.getCustomerName());
			mChannel.setCustomerSuperior(parentCustomerCode);
			mChannel.setCustomerSuperiorType(parentCustomerType);
			mChannel.setCustomerType(customerType);
			mList.add(mChannel);
			List<MarketingChannelParam> childrens2=marketingChannelParam.getChildrens();
			recursiveCreateChannel(customerId,customerType,activitySetId,childrens2,mList);
		}
	}
	/**
	 * ????????????????????????
	 * @param
	 * @return
	 */
	@Transactional
	public RestResult<String> updatePage(MarketingReceivingPageParam mReceivingPageParam) {
		RestResult<String> restResult=new RestResult<String>();

		// ?????????????????????
		MarketingReceivingPage mReceivingPage=new MarketingReceivingPage();
		mReceivingPage.setId(mReceivingPageParam.getId());
		mReceivingPage.setIsQrcodeView(mReceivingPageParam.getIsQrcodeView());
		mReceivingPage.setIsReceivePage(mReceivingPageParam.getIsReceivePage());
		mReceivingPage.setPicAddress(mReceivingPageParam.getPicAddress());
		mReceivingPage.setQrcodeUrl(mReceivingPageParam.getQrcodeUrl());
		mReceivingPage.setTemplateId(mReceivingPageParam.getTemplateId());
		mReceivingPage.setTextContent(mReceivingPageParam.getTextContent());
		mReceivingPage.setFlipTimes(mReceivingPageParam.getFlipTimes());
		maReceivingPageMapper.update(mReceivingPage);

		restResult.setState(200);
		restResult.setMsg("????????????");
		return restResult;
	}


	public RestResult<ScanCodeInfoMO> judgeActivityScanCodeParam(String outerCodeId, String codeTypeId, String productId, String productBatchId, byte referenceRole) throws ParseException {
		return judgeActivityScanCodeParam(outerCodeId, codeTypeId, productId, productBatchId, referenceRole, null);
	}
	/**
	 * ???????????????????????????????????????
	 * @param productBatchId
	 * @param productId
	 * @param codeTypeId
	 * @param referenceRole
	 * @param
	 * @return
	 * @throws SuperCodeException
	 * @throws ParseException
	 */
	public RestResult<ScanCodeInfoMO> judgeActivityScanCodeParam(String outerCodeId, String codeTypeId, String productId, String productBatchId, byte referenceRole, Integer businessType) throws ParseException {
		RestResult<ScanCodeInfoMO> restResult=new RestResult<ScanCodeInfoMO>();
		if (StringUtils.isBlank(outerCodeId) || StringUtils.isBlank(outerCodeId)||StringUtils.isBlank(productId)) {
			restResult.setState(500);
			restResult.setMsg("???????????????????????????????????????");
			return restResult;
		}
		//1???????????????????????????????????????
		MarketingActivityProduct mProduct=mProductMapper.selectByProductAndProductBatchIdWithReferenceRole(productId,productBatchId,referenceRole);
		if (null==mProduct) {
			restResult.setState(500);
			restResult.setMsg("????????????????????????????????????????????????");
			return restResult;
		}

		Long activitySetId=mProduct.getActivitySetId();
		//2???????????????????????????????????????????????????
		MarketingActivitySet mActivitySet=mSetMapper.selectById(activitySetId);
		if (null==mActivitySet) {
			restResult.setState(500);
			restResult.setMsg("??????????????????????????????");
			return restResult;
		}
		Integer activityStatus= mActivitySet.getActivityStatus();
		if (null==activityStatus || 0==activityStatus) {
			restResult.setState(500);
			restResult.setMsg("???????????????");
			return restResult;
		}
		if (businessType != null) {
			Long activityId = mActivitySet.getActivityId();
			Integer bizType = ActivityDefaultConstant.ActivityBizTypeMap.get(activityId);
			if (bizType != null && !businessType.equals(bizType)) {
				restResult.setState(500);
				restResult.setMsg("????????????????????????????????????????????????");
				return restResult;
			}

		}
		//2??????????????????????????????????????????????????????????????????????????????????????????????????????
		String startdate=mActivitySet.getActivityStartDate();
		String enddate=mActivitySet.getActivityEndDate();

		SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
		String nowdate=format.format(new Date());
		long currentTime=format.parse(nowdate).getTime();

		if (StringUtils.isNotBlank(startdate)) {
			long startTime=format.parse(startdate).getTime();
			if (currentTime<startTime) {
				restResult.setState(500);
				restResult.setMsg("??????????????????");
				return restResult;
			}
		}

		if (StringUtils.isNotBlank(enddate)) {
			long endTime=format.parse(enddate).getTime();
			if (currentTime>endTime) {
				restResult.setState(500);
				restResult.setMsg("???????????????");
				return restResult;
			}
		}
		ScanCodeInfoMO pMo=new ScanCodeInfoMO();
		pMo.setCodeId(outerCodeId);
		pMo.setCodeTypeId(codeTypeId);
		pMo.setProductBatchId(productBatchId);
		pMo.setProductId(productId);
		pMo.setActivitySetId(activitySetId);
		pMo.setOrganizationId(mActivitySet.getOrganizationId());

		// ?????????????????????????????????????????????????????????
		pMo.setActivityType(ActivityIdEnum.ACTIVITY_2.getType());
		pMo.setActivityId(mActivitySet.getActivityId());

		restResult.setResults(pMo);
		restResult.setState(200);
		return restResult;
	}

	public RestResult<String> updateActivitySetStatus(MarketingActivitySetStatusUpdateParam mUpdateStatus){
		if(mUpdateStatus.getActivityStatus() == null ){
			throw new RuntimeException("??????????????????...");
		}
		if(mUpdateStatus.getActivitySetId() == null || mUpdateStatus.getActivitySetId() <=0){
			throw new RuntimeException("ID?????????...");
		}
		mSetMapper.updateActivitySetStatus(mUpdateStatus);
		RestResult<String> restResult=new RestResult<String>();
		restResult.setState(200);
		restResult.setMsg("????????????");
		return restResult;
	}

	public MarketingActivitySet selectById(Long activitySetId) {
		return mSetMapper.selectById(activitySetId);
	}
	public Integer selectEachDayNumber(Long activitySetId) {
		return mSetMapper.selectEachDayNumber(activitySetId);
	}

	/**
	 * ????????????????????????
	 * @param activitySetId
	 * @return
	 */
	public RestResult<MarketingActivitySetParam> getActivityBaseInfoByeditPage(Long activitySetId) {
		RestResult<MarketingActivitySetParam> restResult = new RestResult<>();
		// ??????
		if(activitySetId == null || activitySetId <= 0 ){
			restResult.setState(500);
			restResult.setMsg("??????id????????????");
			return  restResult;
		}
		// ??????
		MarketingActivitySet marketingActivitySet = mSetMapper.selectById(activitySetId);
		MarketingActivitySetParam MarketingActivitySetParam = new MarketingActivitySetParam();
		BeanUtils.copyProperties(marketingActivitySet, MarketingActivitySetParam);
		if(marketingActivitySet != null && StringUtils.isNotBlank(marketingActivitySet.getValidCondition())) {
			MarketingActivitySetCondition conditonJson = JSON.parseObject(marketingActivitySet.getValidCondition(), MarketingActivitySetCondition.class);
			MarketingActivitySetParam.setConsumeIntegralNum(conditonJson.getConsumeIntegral());
			MarketingActivitySetParam.setEachDayNumber(conditonJson.getEachDayNumber());
			MarketingActivitySetParam.setParticipationCondition(conditonJson.getParticipationCondition());
		}
		// ??????
		restResult.setState(200);
		restResult.setMsg("success");
		restResult.setResults(MarketingActivitySetParam);
		return  restResult;

	}


    /**
	 * ????????????????????????
	 */
    @Override
    protected List<MarketingSalerActivitySetMO> searchResult(DaoSearchWithOrganizationIdParam searchParams) throws Exception {
        // ???????????????????????????????????????
        List<MarketingSalerActivitySetMO> list = mSetMapper.list(searchParams);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        // ?????????????????????????????????
        list.forEach(mo -> {
            List<MarketingChannel> marketingChannels = mChannelMapper.selectByActivitySetId(mo.getId());
            // ??????????????????????????? 1???????????????????????????2????????????????????????????????????????????????3??????????????????????????????
            // ???????????????????????????????????????????????????????????????
            List<MarketingChannel> treeMarketingChannels = channelService.getTree(marketingChannels);
            mo.setMarketingChannels(treeMarketingChannels);
        });
        return list;
    }

    @Override
    protected int count(DaoSearchWithOrganizationIdParam searchParams) throws Exception {
        return mSetMapper.count(searchParams);
    }

	public RestResult<String> updateSalerActivitySetStatus(MarketingActivitySetStatusUpdateParam setStatusUpdateParam) throws SuperCodeException {
		// ???????????????????????????
//		AccountCache userLoginCache = getUserLoginCache();
		mSetMapper.updateActivitySetStatus(setStatusUpdateParam);
		RestResult<String> restResult=new RestResult<String>();
		restResult.setState(200);
		restResult.setMsg("????????????");
		return restResult;
	}


	public RestResult<String> updateSalerActivitySetStatus(MarketingActivitySetStatusBatchUpdateParam batchUpdateParam) throws SuperCodeException {
        // ???????????????????????????
        AccountCache userLoginCache = getUserLoginCache();
        batchUpdateParam.getActivitySetIds().forEach(activitySetId -> {
            mSetMapper.updateSalerActivitySetStatus(activitySetId, batchUpdateParam.getActivityStatus(), userLoginCache.getUserId(), userLoginCache.getUserName());
        });
        RestResult<String> restResult=new RestResult<String>();
        restResult.setState(200);
        restResult.setMsg("????????????");
        return restResult;
    }


	/**
	 * ??????????????????
	 * @param activitySetId
	 * @return
	 */
	public MarketingSalerActivityCreateParam activityInfo(Long activitySetId) {
		MarketingSalerActivityCreateParam marketingActivityCreateParam = new MarketingSalerActivityCreateParam();
		//?????????????????????
		MarketingActivitySet marketingActivitySet = mSetMapper.selectById(activitySetId);
		MarketingActivitySetParam marketingActivitySetParam = new MarketingActivitySetParam();
		BeanUtils.copyProperties(marketingActivitySet, marketingActivitySetParam);
		String conditionStr = marketingActivitySet.getValidCondition();
		if(StringUtils.isNotBlank(conditionStr)) {
			MarketingActivitySetCondition condition = JSON.parseObject(conditionStr, MarketingActivitySetCondition.class);
			marketingActivitySetParam.setParticipationCondition(condition.getParticipationCondition());
			marketingActivitySetParam.setEachDayNumber(condition.getEachDayNumber());
			marketingActivitySetParam.setConsumeIntegralNum(condition.getConsumeIntegral());
		}
		marketingActivityCreateParam.setmActivitySetParam(marketingActivitySetParam);
		//????????????????????????????????????
		List<MarketingActivityProduct> marketingActivityProductList = mProductMapper.selectByActivitySetId(activitySetId);
		Map<String, MarketingActivityProductParam> mActivityProductParamMap = new HashMap<>();
		if(!CollectionUtils.isEmpty(marketingActivityProductList)) {
			for(MarketingActivityProduct product : marketingActivityProductList) {
				String productId = product.getProductId();
				MarketingActivityProductParam marketingActivityProductParam = mActivityProductParamMap.get(productId);
				if(marketingActivityProductParam == null) {
					marketingActivityProductParam = new MarketingActivityProductParam();
					marketingActivityProductParam.setProductId(product.getProductId());
					marketingActivityProductParam.setProductName(product.getProductName());
					//????????????
					List<ProductBatchParam> prdBatchList = new ArrayList<>();
					if (StringUtils.isNotBlank(product.getProductBatchId())) {
						ProductBatchParam productBatchParam = new ProductBatchParam();
						productBatchParam.setProductBatchId(product.getProductBatchId());
						productBatchParam.setProductBatchName(product.getProductBatchName());
						prdBatchList.add(productBatchParam);
					}
					marketingActivityProductParam.setProductBatchParams(prdBatchList);
					mActivityProductParamMap.put(productId, marketingActivityProductParam);
				} else {
					ProductBatchParam productBatchParam = new ProductBatchParam();
					productBatchParam.setProductBatchId(product.getProductBatchId());
					productBatchParam.setProductBatchName(product.getProductBatchName());
					marketingActivityProductParam.getProductBatchParams().add(productBatchParam);
				}
			}
		}
		marketingActivityCreateParam.setmProductParams(new ArrayList<>(mActivityProductParamMap.values()));
		//????????????????????????
		List<MarketingPrizeType> marketingPrizeTypeList = mPrizeTypeMapper.selectByActivitySetId(activitySetId);
		if(!CollectionUtils.isEmpty(marketingPrizeTypeList)) {
			List<MarketingPrizeTypeParam> marketingPrizeTypeParams = marketingPrizeTypeList.stream().map(priceType -> {
				MarketingPrizeTypeParam marketingPrizeTypeParam = new MarketingPrizeTypeParam();
				BeanUtils.copyProperties(priceType, marketingPrizeTypeParam);
				return marketingPrizeTypeParam;
			}).collect(Collectors.toList());
			marketingActivityCreateParam.setMarketingPrizeTypeParams(marketingPrizeTypeParams);
		}
		//?????????????????????
		List<MarketingChannel> marketingChannelList  = mChannelMapper.selectByActivitySetId(activitySetId);
		if(!CollectionUtils.isEmpty(marketingChannelList)) {
			Map<String, MarketingChannelParam> MarketingChannelParamMap = marketingChannelList.stream()
				.collect(Collectors.toMap(
					MarketingChannel::getCustomerId, marketingChannel -> {
					MarketingChannelParam marketingChannelParam = new MarketingChannelParam();
					BeanUtils.copyProperties(marketingChannel, marketingChannelParam);
					return marketingChannelParam;
			}));
			Set<MarketingChannelParam> MarketingChannelParam = getSonByFatherWithAllData(MarketingChannelParamMap);
			marketingActivityCreateParam.setmChannelParams(new ArrayList<MarketingChannelParam>(MarketingChannelParam));
		}
		return marketingActivityCreateParam;
	}


	//??????????????????????????????????????????
	private Set<MarketingChannelParam> getSonByFatherWithAllData(Map<String, MarketingChannelParam> marketingChannelMap) {
		Set<MarketingChannelParam> channelSet = new HashSet<>();
		Collection<MarketingChannelParam> channelCollection = marketingChannelMap.values();
		for(MarketingChannelParam marketingChannel : channelCollection) {
			MarketingChannelParam channel = putChildrenChannel(marketingChannelMap, marketingChannel);
			if(channel != null) {
                channelSet.add(channel);
            }
		}
		return channelSet;
	}

	//???????????????????????????????????????
	private MarketingChannelParam putChildrenChannel(Map<String, MarketingChannelParam> marketingChannelMap, MarketingChannelParam channel) {
		MarketingChannelParam reChannel = null;
		if(marketingChannelMap.containsKey(channel.getCustomerSuperior())) {
			MarketingChannelParam parentChannel = marketingChannelMap.get(channel.getCustomerSuperior());
			List<MarketingChannelParam> childList = parentChannel.getChildrens();
			//???????????????children?????????????????????????????????????????????????????????????????????????????????????????????????????????
			//???????????????????????????????????????????????????????????????????????????????????????????????????????????????
			if(CollectionUtils.isEmpty(childList)) {
				parentChannel.setChildrens(Lists.newArrayList(channel));
				reChannel = putChildrenChannel(marketingChannelMap, parentChannel);
			} else {
				if(!childList.contains(channel)) {
                    childList.add(channel);
                }
			}
		} else {
			reChannel = channel;
		}
		return reChannel;
	}

	public RestResult<String> preview(MarketingActivityPreviewParam mPreviewParam) throws WriterException, IOException, SuperCodeException {
		RestResult<String> restResult=new RestResult<String>();
		List<MarketingPrizeTypeParam> moPrizeTypes=mPreviewParam.getMarketingPrizeTypeParams();
		if (null==moPrizeTypes || moPrizeTypes.isEmpty()) {
			restResult.setState(500);
			restResult.setMsg("??????????????????????????????");
			return restResult;
		}
		//??????????????????
		standActicityParamCheck.basePrizeTypeCheck(moPrizeTypes);

		String uuid=commonUtil.getUUID();
		String json=JSONObject.toJSONString(mPreviewParam);
		boolean flag=redisUtil.set(RedisKey.ACTIVITY_PREVIEW_PREFIX+uuid, json, 600L);
		if (flag) {
			restResult.setResults(uuid);
			restResult.setState(200);
			restResult.setMsg("??????");
		}else {
			restResult.setState(500);
			restResult.setMsg("??????");
		}
		return restResult;
	}

	public RestResult<MarketingReceivingPage> getPreviewParam(String uuid) {
		RestResult<MarketingReceivingPage> restResult=new RestResult<>();
		String value=redisUtil.get(RedisKey.ACTIVITY_PREVIEW_PREFIX+uuid);
		if (StringUtils.isBlank(value)) {
			restResult.setState(500);
			restResult.setMsg("????????????????????????????????????");
			return restResult;
		}
		MarketingReceivingPage marketingReceivingPage = new MarketingReceivingPage();
		MarketingActivityPreviewParam mPreviewParam=JSONObject.parseObject(value, MarketingActivityPreviewParam.class);
		MarketingActivitySetParam mActivitySetParam = mPreviewParam.getmActivitySetParam();
		MarketingReceivingPageParam mReceivingPageParam = mPreviewParam.getmReceivingPageParam();
		BeanUtils.copyProperties(mReceivingPageParam, marketingReceivingPage);
		if(mActivitySetParam != null) {
            marketingReceivingPage.setActivityDesc(mActivitySetParam.getActivityDesc());
        }
		restResult.setResults(marketingReceivingPage);
		restResult.setState(200);
		return restResult;
	}


	public MarketingActivitySet getOnlyPlatformActivity(){
		return mSetMapper.getOnlyPlatformActivity();
	}

}
