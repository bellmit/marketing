package com.jgw.supercodeplatform.marketing.service.activity.coupon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.ProductAndBatchGetCodeMO;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.constants.ActivityDefaultConstant;
import com.jgw.supercodeplatform.marketing.constants.BusinessTypeEnum;
import com.jgw.supercodeplatform.marketing.constants.WechatConstants;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingActivityProductMapper;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingActivitySetMapper;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingChannelMapper;
import com.jgw.supercodeplatform.marketing.dao.coupon.MarketingCouponMapperExt;
import com.jgw.supercodeplatform.marketing.dto.activity.MarketingActivityProductParam;
import com.jgw.supercodeplatform.marketing.dto.activity.MarketingChannelParam;
import com.jgw.supercodeplatform.marketing.dto.activity.ProductBatchParam;
import com.jgw.supercodeplatform.marketing.dto.coupon.MarketingActivityCouponAddParam;
import com.jgw.supercodeplatform.marketing.dto.coupon.MarketingActivityCouponUpdateParam;
import com.jgw.supercodeplatform.marketing.dto.coupon.MarketingCouponAmoutAndDateVo;
import com.jgw.supercodeplatform.marketing.dto.coupon.MarketingCouponVo;
import com.jgw.supercodeplatform.marketing.enums.market.*;
import com.jgw.supercodeplatform.marketing.enums.market.coupon.CouponAcquireConditionEnum;
import com.jgw.supercodeplatform.marketing.enums.market.coupon.CouponWithAllChannelEnum;
import com.jgw.supercodeplatform.marketing.enums.market.coupon.DeductionChannelTypeEnum;
import com.jgw.supercodeplatform.marketing.enums.market.coupon.DeductionProductTypeEnum;
import com.jgw.supercodeplatform.marketing.pojo.MarketingActivityProduct;
import com.jgw.supercodeplatform.marketing.pojo.MarketingActivitySet;
import com.jgw.supercodeplatform.marketing.pojo.MarketingActivitySetCondition;
import com.jgw.supercodeplatform.marketing.pojo.MarketingChannel;
import com.jgw.supercodeplatform.marketing.pojo.integral.MarketingCoupon;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import com.jgw.supercodeplatform.prizewheels.infrastructure.feigns.GetSbatchIdsByPrizeWheelsFeign;
import com.jgw.supercodeplatform.prizewheels.infrastructure.feigns.dto.SbatchUrlDto;
import com.jgw.supercodeplatform.prizewheels.infrastructure.feigns.dto.SbatchUrlUnBindDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ???????????????????????????service
 */
@Service
@Slf4j
public class CouponService {
	/**
	 * ??????ID???????????????
	 */
	private static final String SPILT ="," ;
 
	private static SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");

	@Value("${rest.codemanager.url}")
	private String codeManagerUrl;

	@Value("${marketing.domain.url}")
	private String marketingDomain;

	@Autowired
	private MarketingActivitySetMapper setMapper;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private CommonService commonService;

	@Autowired
	private MarketingActivityProductMapper productMapper;

	@Autowired
	private MarketingCouponMapperExt couponMapper;

	@Autowired
	private MarketingChannelMapper channelMapper;

    @Autowired
    private GetSbatchIdsByPrizeWheelsFeign getSbatchIdsByPrizeWheelsFeign;
	
	/**
	 * ?????????????????????
	 * @param activitySetId
	 * @return
	 */
	public RestResult<MarketingActivityCouponUpdateParam> detail(Long activitySetId) throws SuperCodeException{
		// ??????
		if(activitySetId == null || activitySetId <= 0 ){
			throw new SuperCodeException("??????id?????????...");
		}

		MarketingActivityCouponUpdateParam marketingActivityCouponUpdateParam = new MarketingActivityCouponUpdateParam();
		// ??????????????????
		setActivitySetInfoToVo(activitySetId,marketingActivityCouponUpdateParam);
		// ????????????
		setCouponRuleInfoToVo(activitySetId,marketingActivityCouponUpdateParam);
		// ????????????
		setChannelInfoToVo(activitySetId,marketingActivityCouponUpdateParam);
		// ????????????
		setProductInfoToVo(activitySetId,marketingActivityCouponUpdateParam);

		return RestResult.success("success",marketingActivityCouponUpdateParam);
	}

	/**
	 * ?????????????????????
	 * @param addVO
	 * @return
	 * @throws SuperCodeException
	 */
	@Transactional(rollbackFor = {SuperCodeException.class,Exception.class})
	public RestResult<String> add(MarketingActivityCouponAddParam addVO) throws SuperCodeException {
		// ????????????
		// ????????????
		validateBasicByAdd(addVO);
		// ?????????????????????????????????
		validateBizByAdd(addVO);
		/************************?????????????????????????????????????????????????????????************************/
		//????????????????????????
		List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs = new ArrayList<ProductAndBatchGetCodeMO>();
		List<MarketingActivityProduct> mList = new ArrayList<MarketingActivityProduct>();
		List<MarketingActivityProductParam> maProductParams = addVO.getProductParams();
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
		List<MarketingActivityProduct> marketingActivityProductList = productMapper.selectByProductAndBatch(mList, ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
//        StringBuffer sbatchIdBuffer = new StringBuffer();
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
						sbatchUrlDto.setProductBatchId(marketingActivityProduct.getProductBatchId());
						sbatchUrlDto.setProductId(marketingActivityProduct.getProductId());
						deleteProductBatchList.add(sbatchUrlDto);
					}
                }
            });
        }
//		log.info(marketingActivityProductList.size()+"??????sbatch:{}", sbatchIdBuffer);
//        if(sbatchIdBuffer.length() > 0) {
//            String sbatchIds = sbatchIdBuffer.substring(1);
//            String[] sbatchIdArray = sbatchIds.split(",");
//            for(String sbatchId : sbatchIdArray) {
//                SbatchUrlUnBindDto sbatchUrlDto = new SbatchUrlUnBindDto();
//                sbatchUrlDto.setUrl(marketingDomain + WechatConstants.SCAN_CODE_JUMP_URL);
//                sbatchUrlDto.initAllBusinessType();
//                sbatchUrlDto.setBatchId(Long.parseLong(sbatchId));
//                sbatchUrlDto.setClientRole(MemberTypeEnums.VIP.getType()+"");
//                deleteProductBatchList.add(sbatchUrlDto);
//            }
//        }
		/***************************************************/
		// ????????????
		MarketingActivitySet activitySet = changeVoToDtoForMarketingActivitySet(addVO);
		setMapper.insert(activitySet);
		mList.forEach(prd -> prd.setActivitySetId(activitySet.getId()));
		// ???????????? TODO copy ????????????????????? ??????????????????
		saveChannels(addVO.getChannelParams(),activitySet.getId());

		// ???????????? TODO copy ????????????????????? ?????????????????? ?????????????????????????????????????????????????????????
		boolean send = false;
		if(addVO.getAcquireCondition().intValue() == CouponAcquireConditionEnum.SHOPPING.getCondition().intValue() ){
			send = true;
		}
		saveProductBatchs(productAndBatchGetCodeMOs, deleteProductBatchList, mList, send);
		// ?????????????????????
		saveCouponRules(addVO.getCoupon(),activitySet.getId());
		return RestResult.success();
	}

	private void saveCouponRules(MarketingCouponVo couponRules, Long activitySetId) throws SuperCodeException {
		List<MarketingCoupon> toDbEntitys = new ArrayList<>(5);
		List<MarketingCouponAmoutAndDateVo> couponAmoutAndDateVo = couponRules.getCouponRules();
		for(MarketingCouponAmoutAndDateVo vo: couponAmoutAndDateVo){
			MarketingCoupon toDbEntity = new MarketingCoupon();
			toDbEntity.setOrganizationId(commonUtil.getOrganizationId());
			toDbEntity.setOrganizationName(commonUtil.getOrganizationName());
			toDbEntity.setActivitySetId(activitySetId);
			toDbEntity.setCouponAmount(vo.getCouponAmount());
			toDbEntity.setDeductionEndDate(vo.getDeductionEndDate());
			toDbEntity.setDeductionStartDate(vo.getDeductionStartDate());
			toDbEntity.setDeductionChannelType(couponRules.getDeductionChannelType());
			toDbEntity.setDeductionProductType(couponRules.getDeductionProductType());
			toDbEntitys.add(toDbEntity);
		}
		couponMapper.batchInsert(toDbEntitys);


	}


	private void saveProductBatchs(List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs, List<SbatchUrlUnBindDto> deleteProductBatchList, List<MarketingActivityProduct> mList, boolean send) throws SuperCodeException {
		getProductBatchSbatchId(productAndBatchGetCodeMOs, mList);
		//????????????????????????????????????????????????????????????
		String superToken = commonUtil.getSuperToken();
		JSONArray arr = commonService.getBatchInfo(productAndBatchGetCodeMOs, superToken, WechatConstants.CODEMANAGER_GET_BATCH_CODE_INFO_URL);
		if(!CollectionUtils.isEmpty(deleteProductBatchList)) {
			RestResult<Object> objectRestResult = getSbatchIdsByPrizeWheelsFeign.removeOldProduct(deleteProductBatchList);
			log.info("?????????????????????{}", JSON.toJSONString(objectRestResult));
			if (objectRestResult == null || objectRestResult.getState().intValue() != 200) {
				throw new SuperCodeException("??????????????????????????????url?????????" + objectRestResult, 500);
			}
		}
		if(send) {
			int businessType = BusinessTypeEnum.MARKETING_COUPON.getBusinessType();
			List<SbatchUrlDto> paramsList = commonService.getUrlToBatchDto(arr, marketingDomain + WechatConstants.SCAN_CODE_JUMP_URL,businessType);
			// ?????????????????????url
			RestResult bindBatchobj = getSbatchIdsByPrizeWheelsFeign.bindingUrlAndBizType(paramsList);
			Integer batchstate = bindBatchobj.getState();
			if (ObjectUtils.notEqual(batchstate, HttpStatus.SC_OK)) {
				throw new SuperCodeException("??????????????????????????????url?????????" + JSON.toJSONString(bindBatchobj), HttpStatus.SC_INTERNAL_SERVER_ERROR);
			}
		}
		Map<String, Map<String, Object>> paramsMap = commonService.getUrlToBatchParamMap(arr, marketingDomain + WechatConstants.SCAN_CODE_JUMP_URL,
				BusinessTypeEnum.MARKETING_COUPON.getBusinessType());
		mList.forEach(marketingActivityProduct -> {
			String key = marketingActivityProduct.getProductId()+","+marketingActivityProduct.getProductBatchId();
			Map<String, Object> batchMap = paramsMap.get(key);
			if (batchMap != null) {
				marketingActivityProduct.setSbatchId((String) batchMap.get("batchId"));
			}
		});
		//??????????????????????????????
		productMapper.batchDeleteByProBatchsAndRole(mList, ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
		productMapper.activityProductInsert(mList);
	}

	/**
	 * ?????????????????????????????????????????????
	 * @param productAndBatchGetCodeMOs
	 * @param mList
	 * @throws SuperCodeException
	 */
	public void getProductBatchSbatchId(List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs, List<MarketingActivityProduct> mList) throws SuperCodeException {
		// ????????????????????????
		String superToken = commonUtil.getSuperToken();
		JSONArray arr  = commonService.getBatchInfo(productAndBatchGetCodeMOs, superToken, WechatConstants.CODEMANAGER_GET_BATCH_CODE_INFO_URL);
		// ??????????????????
		Map<String, Set<String>> productSbathIds = new HashMap<>();
		mList.forEach(marketingActivityProduct -> {
			Set<String> sbathIds = new HashSet<>();
			for(int i=0;i<arr.size();i++) {
				// ?????????????????????
				String globalBacthId = arr.getJSONObject(i).getString("globalBacthId");
				String productId = arr.getJSONObject(i).getString("productId");
				String productBatchId = arr.getJSONObject(i).getString("productBatchId");
				if (StringUtils.isBlank(productBatchId)) {
					productBatchId = null;
				}
				sbathIds.add(globalBacthId);
				if (StringUtils.equals(marketingActivityProduct.getProductId(), productId)
						&& StringUtils.equals(marketingActivityProduct.getProductBatchId(), productBatchId)) {
					sbathIds.add(globalBacthId);
					productSbathIds.put(productId+productBatchId,sbathIds);
				}
			}
			Set<String> sbathIdsDto = productSbathIds.get(marketingActivityProduct.getProductId() + marketingActivityProduct.getProductBatchId());
			if(!CollectionUtils.isEmpty(sbathIdsDto)){
				String[] sbathIdsDtoArray = new String[sbathIdsDto.size()];
				sbathIdsDto.toArray(sbathIdsDtoArray);
				String sbatchId = StringUtils.join(sbathIdsDtoArray, SPILT);
				marketingActivityProduct.setSbatchId(sbatchId);
			}
		});
	}

	private void saveChannels(List<MarketingChannelParam> channelParams, Long activitySetId) {
		//????????????
		if(!CollectionUtils.isEmpty(channelParams)) {
			List<MarketingChannel> mList=new ArrayList<MarketingChannel>();
			for (MarketingChannelParam marketingChannelParam : channelParams) {
				Byte customerType=marketingChannelParam.getCustomerType();
				// ??????????????????customerId??????customerCode
				MarketingChannel mChannel=new MarketingChannel();
				mList.add(mChannel);
				mChannel.setCustomerType(customerType);
				mChannel.setActivitySetId(activitySetId);
				String customerId=marketingChannelParam.getCustomerId();
				mChannel.setCustomerId(marketingChannelParam.getCustomerId());
				mChannel.setCustomerName(marketingChannelParam.getCustomerName());
				mChannel.setCustomerSuperior(marketingChannelParam.getCustomerSuperior());
				List<MarketingChannelParam> childrens=marketingChannelParam.getChildrens();
				mChannel.setCustomerSuperiorType(marketingChannelParam.getCustomerSuperiorType());
				recursiveCreateChannel(customerId,customerType,activitySetId,childrens,mList);
			}
			channelMapper.batchInsert(mList);
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


	private MarketingActivitySet changeVoToDtoForMarketingActivitySet(MarketingActivityCouponAddParam addVO) throws SuperCodeException {
		// ????????????
		Date date = new Date();
		String formatDate          = CouponService.format.format(date);
		String organizationId         = commonUtil.getOrganizationId();
		String organizationName     = commonUtil.getOrganizationName();
		String userId     = commonUtil.getUserLoginCache().getUserId();
		MarketingActivitySet activitySet  = new MarketingActivitySet();
		String username = commonUtil.getUserLoginCache().getUserName();

		// ??????????????????
		activitySet                                                        .setUpdateUserId(userId);
		activitySet                                                      .setCreateDate(formatDate);
		activitySet                                                      .setUpdateDate(formatDate);
		activitySet                                                    .setUpdateUserName(username);
		activitySet                                              .setOrganizationId(organizationId);
		activitySet                                             .setAutoFetch(addVO.getAutoFetch());
		activitySet                                        .setOrganizatioIdlName(organizationName);
		activitySet                                     .setActivityTitle(addVO.getActivityTitle());
		activitySet                             .setActivityStatus(ActivityStatusEnum.UP.getType());
		activitySet              .setActivityId(ActivityIdEnum.ACTIVITY_COUPON.getId().longValue());
		activitySet    .setActivityEndDate(CouponService.format.format(addVO.getActivityEndDate()));
		activitySet.setActivityStartDate(CouponService.format.format(addVO.getActivityStartDate()));
		// ??????
		MarketingActivitySetCondition validCondition = new MarketingActivitySetCondition();
		validCondition                         .setEachDayNumber(addVO.getEachDayNumber());
		validCondition                   .setAcquireCondition(addVO.getAcquireCondition());
		validCondition         .setAllChannels(CouponWithAllChannelEnum.NOT_ALL.getType());
		validCondition   .setAcquireConditionIntegral(addVO.getAcquireConditionIntegral());

		if(!CollectionUtils.isEmpty(addVO.getChannelParams())){
			validCondition.setAllChannels(CouponWithAllChannelEnum.ALL.getType());
		}

		activitySet.setValidCondition(validCondition.toJsonString());
		return activitySet;
	}


	/**
	 * ????????????????????????
	 * @param addVO
	 */
	private void validateBizByAdd(MarketingActivityCouponAddParam addVO) throws SuperCodeException {
		String organizationId = commonUtil.getOrganizationId();

		// ????????????????????????
		String activityTitle = addVO.getActivityTitle();
		MarketingActivitySet marketingActivitySet = setMapper.selectByTitleOrgIdWithActivityId(activityTitle, organizationId, ActivityIdEnum.ACTIVITY_COUPON.getId());
		if(marketingActivitySet != null){
			throw new SuperCodeException("???????????????");
		}
		//        ??????????????????????????????????????????
		//        if(!CollectionUtils.isEmpty(addVO.getProductParams())){
		//            List<MarketingActivityProduct> batchProductInfos = new ArrayList<>();
		//            for(MarketingActivityProductParam product : addVO.getProductParams()){
		//                for(ProductBatchParam batch : product.getProductBatchParams()){
		//                    MarketingActivityProduct p = new MarketingActivityProduct();
		//                    p.setProductId(product.getProductId());
		//                    p.setProductBatchId(batch.getProductBatchId());
		//                    batchProductInfos.add(p);
		//                }
		//            }
		//            // ??????????????????????????????
		//            productMapper.batchDeleteByProBatchsAndRole(batchProductInfos, RoleTypeEnum.MEMBER.getMemberType());
		//        }

	}


	private void validateBasicByAdd(MarketingActivityCouponAddParam addVO) throws SuperCodeException {
		MarketingActivitySet existmActivitySet =setMapper.selectByTitleOrgId(addVO.getActivityTitle(), commonUtil.getOrganizationId());
		if (null!=existmActivitySet) {
			throw new SuperCodeException("??????????????????????????????????????????????????????", 500);
		}
		// ??????????????????
		if(addVO.getEachDayNumber() == null){
			addVO.setEachDayNumber(ActivityDefaultConstant.eachDayNum);
		}else if(addVO.getEachDayNumber() <= 0){
			throw new SuperCodeException("???????????????0...");
		}

		// ????????????
		if(addVO.getAutoFetch() == null  ){
			throw new SuperCodeException("???????????????????????????003");
		}

		if(addVO.getAutoFetch() != AutoGetEnum.BY_AUTO.getAuto() && addVO.getAutoFetch() != AutoGetEnum.BY_NOT_AUTO.getAuto()){
			throw new SuperCodeException("?????????????????????????????????????????????...");
		}

		// ????????????
		if(addVO.getAcquireCondition() == null){
			throw new SuperCodeException("???????????????????????????004...");
		}

		if(addVO.getAcquireCondition().intValue() != CouponAcquireConditionEnum.FIRST.getCondition().intValue()
				&& addVO.getAcquireCondition().intValue() != CouponAcquireConditionEnum.ONCE_LIMIT.getCondition().intValue()
				&& addVO.getAcquireCondition().intValue() != CouponAcquireConditionEnum.LIMIT.getCondition().intValue()
				&& addVO.getAcquireCondition().intValue() != CouponAcquireConditionEnum.SHOPPING.getCondition().intValue()){
			throw new SuperCodeException("????????????????????????...");
		}

		// ????????????????????????
		if((addVO.getAcquireCondition().intValue() == CouponAcquireConditionEnum.ONCE_LIMIT.getCondition().intValue()
				|| addVO.getAcquireCondition().intValue() == CouponAcquireConditionEnum.LIMIT.getCondition().intValue())
				&& (addVO.getAcquireConditionIntegral() ==null || addVO.getAcquireConditionIntegral() <= 0  )){
			String messe = "????????????????????????";
			if(addVO.getAcquireCondition().intValue() == 2) {
                messe = "????????????????????????????????????";
            }
			if(addVO.getAcquireCondition().intValue() == 3) {
                messe = "????????????????????????????????????";
            }
			throw new SuperCodeException(messe);
		}

		// ????????????
		if(addVO.getActivityStartDate() == null && addVO.getActivityEndDate() != null){
			throw new SuperCodeException("????????????????????????001...");
		}

		if(addVO.getActivityStartDate() != null && addVO.getActivityEndDate() == null){
			throw new SuperCodeException("????????????????????????002...");
		}

		//        if(addVO.getActivityStartDate() == null && addVO.getActivityEndDate() == null){
		//            throw new SuperCodeException("???????????????????????????001");
		//        }

		if(addVO.getActivityStartDate() == null && addVO.getActivityEndDate() == null){
			addVO.setActivityStartDate(new Date());
			try {
				addVO.setActivityEndDate(format.parse(ActivityDefaultConstant.activityEndDate));
			} catch (ParseException e) {
				if(log.isErrorEnabled()){
					log.error("???????????????????????????????????????????????????{}",e.getMessage());
				}
				throw new SuperCodeException("???????????????????????????????????????");
			}
		}else{
			// ???????????????????????????
			if(addVO.getActivityStartDate().after( addVO.getActivityEndDate() )){
				throw new SuperCodeException("??????????????????...");
			}
		}
		// ????????????
		validateBasicByAddForProducts(addVO.getProductParams());
		// ????????????
		validateBasicByAddForChannels(addVO.getChannelParams());
		// ?????????????????????
		validateBasicByAddForCouponRules(addVO.getCoupon());
	}


	private void validateBasicByAddForCouponRules(MarketingCouponVo couponRules) throws SuperCodeException {
		if(couponRules == null){
			throw new SuperCodeException("????????????????????????");
		}

		if(CollectionUtils.isEmpty(couponRules.getCouponRules())){
			throw new SuperCodeException("???????????????????????????...");
		}

		// ????????????????????????
		if(couponRules.getDeductionChannelType() == null
				|| (couponRules.getDeductionChannelType() != DeductionChannelTypeEnum.ONLY_CHANNELS.getType()
				&& couponRules.getDeductionChannelType() != DeductionChannelTypeEnum.NO_LIMIT.getType())){
			throw new SuperCodeException("????????????????????????????????????");
		}

		// ???????????????????????????
		if(couponRules.getDeductionProductType() == null
				|| couponRules.getDeductionProductType() != DeductionProductTypeEnum.NO_LIMIT.getType()){
			throw new SuperCodeException("????????????????????????...");
		}

		Date date = new Date();
		for(MarketingCouponAmoutAndDateVo couponAmoutAndDateVo : couponRules.getCouponRules()){
			if(couponAmoutAndDateVo.getCouponAmount() == null || couponAmoutAndDateVo.getCouponAmount() <= 0 ){
				throw new SuperCodeException("????????????...");
			}

			if(couponAmoutAndDateVo.getDeductionStartDate() == null
					|| couponAmoutAndDateVo.getDeductionEndDate() == null
					|| couponAmoutAndDateVo.getDeductionEndDate().before(date)
					|| couponAmoutAndDateVo.getDeductionEndDate().before(couponAmoutAndDateVo.getDeductionStartDate())){
				throw new SuperCodeException("??????????????????..."); // ??????????????????????????????
			}
		}
	}


	/**
	 * ?????????????????????
	 * @param channelParams
	 * @throws SuperCodeException
	 */
	private void validateBasicByAddForChannels(List<MarketingChannelParam> channelParams) throws SuperCodeException {
		if(CollectionUtils.isEmpty(channelParams)){
			//   ?????????????????? [???activitysetjson???????????????????????????????????????allchannels]
		}else{
			for(MarketingChannelParam channelParam:channelParams){
				if(channelParam.getCustomerType() == null){
					throw new SuperCodeException("??????????????????001");
				}

				if(StringUtils.isBlank(channelParam.getCustomerId())){
					throw new SuperCodeException("??????????????????002");
				}

				if(StringUtils.isBlank(channelParam.getCustomerName())){
					throw new SuperCodeException("??????????????????003");
				}
				//              ???????????????null
				//                if(channelParam.getCustomerSuperiorType() == null){
				//                    throw new SuperCodeException("??????????????????004");
				//                }
				//
				//                if(StringUtils.isBlank(channelParam.getCustomerSuperior())){
				//                    throw new SuperCodeException("??????????????????005");
				//                }
				//


			}
		}
	}


	/**
	 * ??????????????????????????????
	 * @param productParams
	 * @throws SuperCodeException
	 */
	private void validateBasicByAddForProducts(List<MarketingActivityProductParam> productParams) throws SuperCodeException {
		if (CollectionUtils.isEmpty(productParams)) {
			throw new SuperCodeException("??????????????????");
		} else {
			for (MarketingActivityProductParam productParam : productParams) {
				if (StringUtils.isBlank(productParam.getProductId()) || StringUtils.isBlank(productParam.getProductName())) {
					throw new SuperCodeException("??????????????????001");
				}
				if (!CollectionUtils.isEmpty(productParam.getProductBatchParams())) {
					for (ProductBatchParam batchParam : productParam.getProductBatchParams()) {
						if (StringUtils.isBlank(batchParam.getProductBatchId()) || StringUtils.isBlank(batchParam.getProductBatchName())) {
							throw new SuperCodeException("????????????????????????001");
						}
					}
				}
			}
		}
	}


	/**
	 * ??????????????????
	 * @param activitySetId
	 * @param vo
	 * @return
	 * @throws SuperCodeException
	 */
	public MarketingActivityCouponUpdateParam setActivitySetInfoToVo(Long activitySetId, MarketingActivityCouponUpdateParam vo) throws SuperCodeException {
		MarketingActivitySet marketingActivitySet = setMapper.selectById(activitySetId);
		if(marketingActivitySet == null){
			throw new SuperCodeException("???????????????...");
		}
		vo = copyField(vo,marketingActivitySet);
		return vo;

	}

	private MarketingActivityCouponUpdateParam copyField(MarketingActivityCouponUpdateParam vo, MarketingActivitySet dto) throws SuperCodeException {
		vo.setId(dto.getId());
		try {
			if( dto.getActivityEndDate()!=null ){
				vo.setActivityEndDate(format.parse(dto.getActivityEndDate()));
			}
			if( dto.getActivityStartDate()!=null ){
				vo.setActivityStartDate(format.parse(dto.getActivityStartDate()));
			}
		} catch (ParseException e) {
			e.printStackTrace();
			throw new SuperCodeException("????????????????????????????????????");
		}
		vo.setActivityTitle(dto.getActivityTitle());
		vo.setAutoFetch(dto.getAutoFetch());

		String validCondition = dto.getValidCondition();
		MarketingActivitySetCondition validConditionObj = JSONObject.parseObject(validCondition, MarketingActivitySetCondition.class);
		vo.setEachDayNumber(validConditionObj.getEachDayNumber());
		vo.setAcquireCondition(validConditionObj.getAcquireCondition());
		vo.setAcquireConditionIntegral(validConditionObj.getAcquireConditionIntegral());
		return vo;
	}


	/**
	 * ?????????????????????
	 * @param activitySetId
	 * @param vo
	 * @return
	 */
	public MarketingActivityCouponUpdateParam setCouponRuleInfoToVo(Long activitySetId, MarketingActivityCouponUpdateParam vo) {
		// ??????VO
		// TODO ????????????????????? start
		MarketingCouponVo couponRules = new MarketingCouponVo();
		List<MarketingCouponAmoutAndDateVo> couponAmoutAndDateVos= new ArrayList<>(5);
		List<MarketingCoupon> marketingCoupons = couponMapper.selectByActivitySetId(activitySetId);
		if(CollectionUtils.isEmpty(marketingCoupons)){
			return null;
		}
		for(MarketingCoupon rule : marketingCoupons){
			MarketingCouponAmoutAndDateVo ruleVo = new MarketingCouponAmoutAndDateVo();
			ruleVo.setCouponAmount(rule.getCouponAmount());
			ruleVo.setDeductionStartDate(rule.getDeductionStartDate());
			ruleVo.setDeductionEndDate(rule.getDeductionEndDate());
			couponAmoutAndDateVos.add(ruleVo);
		}
		couponRules.setCouponRules(couponAmoutAndDateVos);
		couponRules.setDeductionChannelType(marketingCoupons.get(0).getDeductionChannelType());
		couponRules.setDeductionProductType(marketingCoupons.get(0).getDeductionProductType());
		// TODO ????????????????????? end
		vo.setCoupon(couponRules);
		return vo;
	}


	/**
	 * ??????????????????  TODO ????????????service
	 * @param activitySetId
	 * @param vo
	 * @return
	 */
	public MarketingActivityCouponUpdateParam setProductInfoToVo(Long activitySetId, MarketingActivityCouponUpdateParam vo) {
		// ??????????????????-????????????
		List<MarketingActivityProduct> marketingActivityProducts = productMapper.selectByActivitySetId(activitySetId);
		if(CollectionUtils.isEmpty(marketingActivityProducts)){
			return null;
		}
		// ?????????????????????????????????????????????
		Map<String, MarketingActivityProductParam> transferDataMap = new HashMap<>();
		for (MarketingActivityProduct marketingActivityProduct : marketingActivityProducts) {
			String productId = marketingActivityProduct.getProductId();
			MarketingActivityProductParam transferData = transferDataMap.get(productId);
			if (transferData == null) {
				transferData = new MarketingActivityProductParam();
				transferData.setProductId(productId);
				transferData.setProductName(marketingActivityProduct.getProductName());
				transferDataMap.put(productId, transferData);
			}
		}
		transferDataMap.forEach((productId, value) -> {
			List<ProductBatchParam> productParams = new ArrayList<>();
			for (MarketingActivityProduct marketingActivityProduct : marketingActivityProducts) {
				String productBatchId = marketingActivityProduct.getProductBatchId();
				if (productId.equals(marketingActivityProduct.getProductId()) && StringUtils.isNotBlank(productBatchId)) {
					ProductBatchParam batch = new ProductBatchParam();
					batch.setProductBatchId(productBatchId);
					batch.setProductBatchName(marketingActivityProduct.getProductBatchName());
					productParams.add(batch);
				}
			}
			value.setProductBatchParams(productParams);
		});
		vo.setProductParams(new ArrayList<>(transferDataMap.values()));
		return vo;
	}

	/**
	 * ?????????????????? TODO ????????????service
	 * @param activitySetId
	 * @param vo
	 * @return
	 */
	public MarketingActivityCouponUpdateParam setChannelInfoToVo(Long activitySetId, MarketingActivityCouponUpdateParam vo) {
		List<MarketingChannel> marketingChannelList  = channelMapper.selectByActivitySetId(activitySetId);
		if(!CollectionUtils.isEmpty(marketingChannelList)) {
			Map<String, MarketingChannelParam> MarketingChannelParamMap = marketingChannelList.stream()
					.collect(Collectors.toMap(
							MarketingChannel::getCustomerId, marketingChannel -> {
								MarketingChannelParam marketingChannelParam = new MarketingChannelParam();
								BeanUtils.copyProperties(marketingChannel, marketingChannelParam);
								return marketingChannelParam;
							}));
			Set<MarketingChannelParam> marketingChannelParam = getSonByFatherWithAllData(MarketingChannelParamMap);
			vo.setChannelParams(new ArrayList<>(marketingChannelParam));
		}
		return vo;
	}


	//******************************************************tree start***********************************************************
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

	//******************************************************tree end*************************************************************



}
