package com.jgw.supercodeplatform.marketing.service.activity.coupon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
 import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class CouponUpdateService {


 
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
    private CouponService couponService;

    @Autowired
    private MarketingActivityProductMapper productMapper;

    @Autowired
    private MarketingCouponMapperExt couponMapper;

    @Autowired
    private CommonService commonService;
    
	@Autowired
	private MarketingActivityProductMapper mProductMapper;

    @Autowired
    private MarketingChannelMapper channelMapper;

    @Autowired
    private GetSbatchIdsByPrizeWheelsFeign getSbatchIdsByPrizeWheelsFeign;

    /**
     * ?????????????????????
     * @param updateVo
     * @return
     * @throws SuperCodeException
     */
    @Transactional(rollbackFor = {SuperCodeException.class,Exception.class})
    public RestResult<String> update(MarketingActivityCouponUpdateParam updateVo) throws SuperCodeException {
        validateBasicByUpdate(updateVo);
        validateBizByUpdate(updateVo);
        // ?????????????????????
        MarketingActivitySet activitySet = changeVoToDtoForMarketingActivitySet(updateVo);
        Long activitySetId = updateVo.getId();
        activitySet.setId(activitySetId);
        //????????????????????????????????????????????????
        List<MarketingActivityProduct> upProductList = mProductMapper.selectByActivitySetId(activitySet.getId());
        if(upProductList == null) {
            upProductList = new ArrayList<>();
        }
        /************************?????????????????????????????????????????????????????????************************/
		//????????????????????????
		List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs = new ArrayList<ProductAndBatchGetCodeMO>();
		List<MarketingActivityProduct> mList = new ArrayList<MarketingActivityProduct>();
		List<MarketingActivityProductParam> maProductParams = updateVo.getProductParams();
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
		List<MarketingActivityProduct> marketingActivityProductList = productMapper.selectByProductAndBatch(mList, ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
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
//        log.info(marketingActivityProductList.size()+"??????sbatch:{}", sbatchIdBuffer);
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
		productMapper.deleteByActivitySetId(activitySetId);
        channelMapper.deleteByActivitySetId(activitySetId);
        couponMapper.deleteByActivitySetId(activitySetId);
		setMapper.update(activitySet);
        // ???????????? TODO copy ????????????????????? ?????????????????? ?????????????????????????????????????????????????????????
        boolean send = false;
        if(updateVo.getAcquireCondition().intValue() == CouponAcquireConditionEnum.SHOPPING.getCondition().intValue() ){
            send = true;
        }
        mList.forEach(prd -> prd.setActivitySetId(activitySet.getId()));
        // ??????: ????????????
        saveProductBatchsWhenUpdate(productAndBatchGetCodeMOs, deleteProductBatchList, mList, send);
        // ?????????????????????
        saveCouponRulesWhenUpdate(updateVo.getCoupon(),activitySet.getId());
        // ???????????? TODO
        saveChannelsWhenUpdate(updateVo.getChannelParams(),activitySet.getId());
        
        return RestResult.success();
    }

    private void saveChannelsWhenUpdate(List<MarketingChannelParam> channelParams, Long activitySetId) {
    	if(!CollectionUtils.isEmpty(channelParams)) {
	    	List<MarketingChannel> mList=new ArrayList<MarketingChannel>();
	        //????????????
	        for (MarketingChannelParam marketingChannelParam : channelParams) {
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


    private void saveProductBatchsWhenUpdate(List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs , List<SbatchUrlUnBindDto> deleteProductBatchList, List<MarketingActivityProduct> mList, boolean send) throws SuperCodeException {
        // ????????????????????????
        couponService.getProductBatchSbatchId(productAndBatchGetCodeMOs, mList);
        // TODO ????????????????????????????????????
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
        Map<String, Map<String, Object>> paramsMap = commonService.getUrlToBatchParamMap(arr,
                marketingDomain + WechatConstants.SCAN_CODE_JUMP_URL,
                BusinessTypeEnum.MARKETING_ACTIVITY.getBusinessType());
        mList.forEach(marketingActivityProduct -> {
            String key = marketingActivityProduct.getProductId()+","+marketingActivityProduct.getProductBatchId();
            marketingActivityProduct.setSbatchId((String)paramsMap.get(key).get("batchId"));
        });

        //??????????????????????????????
        productMapper.batchDeleteByProBatchsAndRole(mList, ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
        productMapper.activityProductInsert(mList);

    }

    private void saveCouponRulesWhenUpdate(MarketingCouponVo couponRules, Long activitySetId) {
        List<MarketingCoupon> toDbEntitys = new ArrayList<>(5);
        List<MarketingCouponAmoutAndDateVo> couponAmoutAndDateVo = couponRules.getCouponRules();
        for(MarketingCouponAmoutAndDateVo vo: couponAmoutAndDateVo){
            MarketingCoupon toDbEntity = new MarketingCoupon();
            toDbEntity.setActivitySetId(activitySetId);
            toDbEntity.setDeductionStartDate(vo.getDeductionStartDate());
            toDbEntity.setDeductionEndDate(vo.getDeductionEndDate());
            toDbEntity.setCouponAmount(vo.getCouponAmount());
            toDbEntity.setDeductionChannelType(couponRules.getDeductionChannelType());
            toDbEntity.setDeductionProductType(couponRules.getDeductionProductType());
            toDbEntitys.add(toDbEntity);
        }
        couponMapper.batchInsert(toDbEntitys);

    }


    private MarketingActivitySet changeVoToDtoForMarketingActivitySet(MarketingActivityCouponAddParam addVO) throws SuperCodeException {
        String organizationId         = commonUtil.getOrganizationId();
        String organizationName     = commonUtil.getOrganizationName();
        String userId     = commonUtil.getUserLoginCache().getUserId();
        MarketingActivitySet activitySet  = new MarketingActivitySet();
        String username = commonUtil.getUserLoginCache().getUserName();

        // ??????????????????
        activitySet                                                        .setUpdateUserId(userId);
        activitySet                                                    .setUpdateUserName(username);
        activitySet                                              .setOrganizationId(organizationId);
        activitySet                                             .setAutoFetch(addVO.getAutoFetch());
        activitySet                                        .setOrganizatioIdlName(organizationName);
        activitySet                                     .setActivityTitle(addVO.getActivityTitle());
        activitySet                             .setActivityStatus(ActivityStatusEnum.UP.getType());
        activitySet              .setActivityId(ActivityIdEnum.ACTIVITY_COUPON.getId().longValue());
        activitySet    .setActivityEndDate(format.format(addVO.getActivityEndDate()));
        activitySet.setActivityStartDate(format.format(addVO.getActivityStartDate()));
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



    private void validateBizByUpdate(MarketingActivityCouponUpdateParam updateVo) throws SuperCodeException {
        String organizationId = commonUtil.getOrganizationId();

        // ????????????????????????
        String activityTitle = updateVo.getActivityTitle();
        MarketingActivitySet marketingActivitySet = setMapper.selectByTitleOrgIdWithActivityIdWhenUpdate(activityTitle,updateVo.getId(), organizationId, ActivityIdEnum.ACTIVITY_COUPON.getId());
        if(marketingActivitySet != null){
            throw new SuperCodeException("???????????????");
        }
    }

    private void validateBasicByUpdate(MarketingActivityCouponUpdateParam updateVo) throws SuperCodeException {
        if(updateVo == null){
            throw new SuperCodeException("???????????????????????????001");
        }
        // ????????????
        if(updateVo.getActivityTitle() == null){
            throw new SuperCodeException("???????????????????????????002");
        }

        // ??????????????????
        if(updateVo.getEachDayNumber() == null){
            updateVo.setEachDayNumber(ActivityDefaultConstant.eachDayNum);
        }else if(updateVo.getEachDayNumber() <= 0){
            throw new SuperCodeException("???????????????0...");
        }

        // ????????????
        if(updateVo.getAutoFetch() == null  ){
            throw new SuperCodeException("???????????????????????????003");
        }
        if(updateVo.getAutoFetch() != AutoGetEnum.BY_AUTO.getAuto() && updateVo.getAutoFetch() != AutoGetEnum.BY_NOT_AUTO.getAuto()){
            throw new SuperCodeException("?????????????????????????????????????????????...");
        }

        // ????????????
        if(updateVo.getAcquireCondition() == null){
            throw new SuperCodeException("???????????????????????????004...");
        }
        if(updateVo.getAcquireCondition().intValue() != CouponAcquireConditionEnum.FIRST.getCondition().intValue()
                && updateVo.getAcquireCondition().intValue() != CouponAcquireConditionEnum.ONCE_LIMIT.getCondition().intValue()
                && updateVo.getAcquireCondition().intValue() != CouponAcquireConditionEnum.LIMIT.getCondition().intValue()
                && updateVo.getAcquireCondition().intValue() != CouponAcquireConditionEnum.SHOPPING.getCondition().intValue()){
            throw new SuperCodeException("????????????????????????...");
        }

        // ????????????????????????
        if((updateVo.getAcquireCondition().intValue() == CouponAcquireConditionEnum.ONCE_LIMIT.getCondition().intValue()
                || updateVo.getAcquireCondition().intValue() == CouponAcquireConditionEnum.LIMIT.getCondition().intValue())
                && (updateVo.getAcquireConditionIntegral() ==null || updateVo.getAcquireConditionIntegral() <= 0  )){
			String messe = "????????????????????????";
			if(updateVo.getAcquireCondition().intValue() == 2) {
                messe = "????????????????????????????????????";
            }
			if(updateVo.getAcquireCondition().intValue() == 3) {
                messe = "????????????????????????????????????";
            }
			throw new SuperCodeException(messe);
        }

        // ????????????
        if(updateVo.getActivityStartDate() == null && updateVo.getActivityEndDate() != null){
            throw new SuperCodeException("????????????????????????...");
        }
        if(updateVo.getActivityStartDate() != null && updateVo.getActivityEndDate() == null){
            throw new SuperCodeException("????????????????????????...");
        }
        if(updateVo.getActivityStartDate() == null && updateVo.getActivityEndDate() == null){
            throw new SuperCodeException("???????????????????????????001");
        }
        if(updateVo.getActivityStartDate() == null && updateVo.getActivityEndDate() == null){
            updateVo.setActivityStartDate(new Date());
            try {
                updateVo.setActivityEndDate(format.parse(ActivityDefaultConstant.activityEndDate));
            } catch (ParseException e) {
                if(log.isErrorEnabled()){
                    log.error("???????????????????????????????????????????????????{}",e.getMessage());
                }
                throw new SuperCodeException("???????????????????????????????????????");
            }
        }else{
            // ???????????????????????????
            if(updateVo.getActivityStartDate().after( updateVo.getActivityEndDate() )){
                throw new SuperCodeException("??????????????????...");
            }
        }
        // ????????????
        validateBasicByUpdateForProducts(updateVo.getProductParams());
        // ????????????
        validateBasicByUpdateForChannels(updateVo.getChannelParams());
        // ?????????????????????
        validateBasicByUpdateForCouponRules(updateVo.getCoupon());

    }


    private void validateBasicByUpdateForCouponRules(MarketingCouponVo couponRules) throws SuperCodeException {
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
    private void validateBasicByUpdateForChannels(List<MarketingChannelParam> channelParams) throws SuperCodeException {
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

//                if(StringUtils.isBlank(channelParam.getCustomerSuperior())){
//                    throw new SuperCodeException("??????????????????004");
//                }

//                if(channelParam.getCustomerSuperiorType() == null){
//                    throw new SuperCodeException("??????????????????005");
//                }

            }
        }
    }

    /**
     * ??????????????????????????????
     * @param productParams
     * @throws SuperCodeException
     */
    private void validateBasicByUpdateForProducts(List<MarketingActivityProductParam> productParams) throws SuperCodeException {
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



}
