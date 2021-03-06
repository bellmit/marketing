package com.jgw.supercodeplatform.marketing.service.integral;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.ProductAndBatchGetCodeMO;
import com.jgw.supercodeplatform.marketing.common.page.AbstractPageService;
import com.jgw.supercodeplatform.marketing.common.page.DaoSearch;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.RestTemplateUtil;
import com.jgw.supercodeplatform.marketing.constants.BusinessTypeEnum;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.constants.WechatConstants;
import com.jgw.supercodeplatform.marketing.dao.integral.IntegralRuleMapperExt;
import com.jgw.supercodeplatform.marketing.dao.integral.IntegralRuleProductMapperExt;
import com.jgw.supercodeplatform.marketing.dto.DaoSearchWithOrganizationIdParam;
import com.jgw.supercodeplatform.marketing.dto.integral.BatchSetProductRuleParam;
import com.jgw.supercodeplatform.marketing.dto.integral.Product;
import com.jgw.supercodeplatform.marketing.enums.market.MemberTypeEnums;
import com.jgw.supercodeplatform.marketing.pojo.integral.IntegralRule;
import com.jgw.supercodeplatform.marketing.pojo.integral.IntegralRuleProduct;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import com.jgw.supercodeplatform.prizewheels.infrastructure.feigns.GetSbatchIdsByPrizeWheelsFeign;
import com.jgw.supercodeplatform.prizewheels.infrastructure.feigns.dto.SbatchUrlDto;
import com.jgw.supercodeplatform.prizewheels.infrastructure.feigns.dto.SbatchUrlUnBindDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Map.Entry;

@Service
@Slf4j
public class IntegralRuleProductService extends AbstractPageService<DaoSearch>{
   
    @Autowired
    private IntegralRuleProductMapperExt dao;

    @Autowired
    private IntegralRuleMapperExt inRuleMapperExt;
    
    @Autowired
    private CommonUtil commonUtil;
    
    @Autowired
    private RestTemplateUtil restTemplateUtil;
    
    @Autowired
    private CommonService commonService;
    
    @Value("${rest.codemanager.url}")
    private String codeManagerRestUrl;
    
    
    @Value("${rest.user.url}")
    private String restUserUrl;

	@Value("${marketing.domain.url}")
	private String marketingDomain;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private GetSbatchIdsByPrizeWheelsFeign getSbatchIdsByPrizeWheelsFeign;

	@Override
	protected List<IntegralRuleProduct> searchResult(DaoSearch searchParams) throws Exception {
		String organizationId=commonUtil.getOrganizationId();
		DaoSearchWithOrganizationIdParam searchParamsDTO = modelMapper.map(searchParams, DaoSearchWithOrganizationIdParam.class);
		searchParamsDTO.setOrganizationId(organizationId);
		searchParamsDTO.setStartNumber((searchParamsDTO.getCurrent() -1)*searchParamsDTO.getPageSize());
		List<IntegralRuleProduct> list=dao.list(searchParamsDTO);
		return list;
	}

	@Override
	protected int count(DaoSearch searchParams) throws Exception {
		String organizationId=commonUtil.getOrganizationId();
		DaoSearchWithOrganizationIdParam searchParamsDTO = modelMapper.map(searchParams, DaoSearchWithOrganizationIdParam.class);
		searchParamsDTO.setOrganizationId(organizationId);
		return dao.count(searchParamsDTO);
	}

	public void deleteByProductIds(List<String> productIds) throws SuperCodeException {
      if (null==productIds || productIds.isEmpty()) {
	 	throw new SuperCodeException("??????id????????????", 500);
	  }
      String superToken = commonUtil.getSuperToken();
//      List<IntegralRuleProduct> productList = dao.selectByProductIdsAndOrgId(productIds, commonUtil.getOrganizationId());
//		JSONArray jsonArray= commonService.requestPriductBatchIds(productIds, superToken);
		//??????????????????????????????
		List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs = constructProductAndBatchMOByProductIds(productIds);
		integralUrlUnBindBatch(BusinessTypeEnum.INTEGRAL.getBusinessType(),superToken, productAndBatchGetCodeMOs);
		  dao.deleteByProductIds(productIds);
	}
	/**
	 * ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????url?????????????????????
	 * @param bProductRuleParam
	 * @throws SuperCodeException
	 */
    @Transactional
	public void batchSetRule(BatchSetProductRuleParam bProductRuleParam) throws SuperCodeException {
		List<Product> products=bProductRuleParam.getProducts();	
		if (null==products || products.isEmpty()) {
			throw new SuperCodeException("??????????????????", 500);
		}
		for (Product product : products) {
			if (StringUtils.isBlank(product.getProductId())) {
				throw new SuperCodeException("productId????????????", 500);
			}
		}
		String organizationId=commonUtil.getOrganizationId();
		IntegralRule integralRule=inRuleMapperExt.selectByOrgId(organizationId);
		if (null==integralRule) {
			throw new SuperCodeException("????????????????????????????????????????????????????????????????????????", 500);
		}
		Byte memberType=0;
		Float perConsume=bProductRuleParam.getPerConsume();
		Float productPrice=bProductRuleParam.getProductPrice();
		Integer rewardIntegral=bProductRuleParam.getRewardIntegral();
		Byte rewardRule=bProductRuleParam.getRewardRule();
		
		List<String> productIds=new ArrayList<String>();
		List<IntegralRuleProduct> ruleProducts=new ArrayList<IntegralRuleProduct>();
		for (Product product : products) {
			productIds.add(product.getProductId());
		}
		List<IntegralRuleProduct> excludeRuleProducts=dao.selectByProductIdsAndOrgId(productIds,organizationId);
		if (null!=excludeRuleProducts && !excludeRuleProducts.isEmpty()) {
			StringBuffer buf=new StringBuffer();
			buf.append("??????  ");
			for (IntegralRuleProduct integralRuleProduct : excludeRuleProducts) {
				buf.append(integralRuleProduct.getProductName()).append(",");
			}
			buf.append("??????????????????????????????");
			throw new SuperCodeException(buf.toString(), 500);
		}
		String superToken=commonUtil.getSuperToken();
		//????????????id????????????????????????????????????????????????
//		JSONArray jsonArray= commonService.requestPriductBatchIds(productIds, superToken);
		//??????????????????????????????
		List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs = constructProductAndBatchMOByProductIds(productIds);
		List<Map<String, Object>> updateProductList=new ArrayList<Map<String,Object>>();
		for (Product product : products) {
			String productId=product.getProductId();
			IntegralRuleProduct ruleProduct=new IntegralRuleProduct();
			ruleProduct.setIntegralRuleId(integralRule.getId());
			ruleProduct.setMemberType(memberType);
			ruleProduct.setOrganizationId(organizationId);
			ruleProduct.setPerConsume(perConsume);
			ruleProduct.setProductPrice(productPrice);
			ruleProduct.setRewardIntegral(rewardIntegral);
			ruleProduct.setRewardRule(rewardRule);
			ruleProduct.setProductId(productId);
			ruleProduct.setProductName(product.getProductName());
			ruleProducts.add(ruleProduct);
			if (null!=productPrice) {
				Map<String, Object> updateProductMap=new HashMap<String, Object>();
				updateProductMap.put("price", productPrice);
				updateProductMap.put("productId", productId);
				updateProductList.add(updateProductMap);
			}
		}
		//???????????????????????????url????????????
		integralUrlBindBatch(BusinessTypeEnum.INTEGRAL.getBusinessType(),superToken, productAndBatchGetCodeMOs);
		
		dao.batchInsert(ruleProducts);
		//????????????????????????
		updateBaseProductPrice(updateProductList,superToken);
		
	}
    
    
	@Transactional
	public void singleSetRuleProduct( IntegralRuleProduct inRuleProduct) throws SuperCodeException {
		inRuleProduct.setMemberType((byte)0);
		if (null==inRuleProduct.getId()) {
			
			String organizationId=commonUtil.getOrganizationId();
			IntegralRuleProduct eruleProduct=dao.selectByProductIdAndOrgId(inRuleProduct.getProductId(),organizationId);
			if (null!=eruleProduct) {
				throw new SuperCodeException("???????????????????????????????????????id", 500);
			}
			dao.insertSelective(inRuleProduct);
			String superToken=commonUtil.getSuperToken();
			
			List<String> productIds=new ArrayList<String>();
			String productId=inRuleProduct.getProductId();
			if (StringUtils.isBlank(productId)) {
				throw new SuperCodeException("productId????????????", 500);
			}
			
			productIds.add(productId);
			//????????????id????????????????????????????????????????????????
//			JSONArray jsonArray= commonService.requestPriductBatchIds(productIds, superToken);
			//??????????????????????????????
			List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs = constructProductAndBatchMOByProductIds(productIds);
			integralUrlBindBatch(BusinessTypeEnum.INTEGRAL.getBusinessType(),superToken, productAndBatchGetCodeMOs);
			
			//????????????????????????
			List<Map<String, Object>> updateProductList=new ArrayList<Map<String,Object>>();
			Float price=inRuleProduct.getProductPrice();
			if (null!=price) {
				Map<String, Object> updateProductMap=new HashMap<String, Object>();
				updateProductMap.put("price", price);
				updateProductMap.put("productId", productId);
				updateProductList.add(updateProductMap);
				updateBaseProductPrice(updateProductList,superToken);
			}
		}else {
			if (StringUtils.isBlank(inRuleProduct.getProductId())) {
				inRuleProduct.setProductId(null);
			}
			
			if (StringUtils.isBlank(inRuleProduct.getProductName())) {
				inRuleProduct.setProductName(null);
			}
			
			dao.updateByPrimaryKeySelective(inRuleProduct);
		}
	}
	
    /**
     * ??????????????????????????????????????????
     * @param updateProductList
     * @throws SuperCodeException 
     */
    private void updateBaseProductPrice(List<Map<String, Object>> updateProductList,String superToken) throws SuperCodeException {
    	if (null!=updateProductList && !updateProductList.isEmpty()) {
    		String json=JSONObject.toJSONString(updateProductList);
    		Map<String,String> headerMap=new HashMap<String, String>();
    		headerMap.put("super-token", superToken);
    		ResponseEntity<String> resopEntity=restTemplateUtil.putJsonDataAndReturnJosn(restUserUrl+CommonConstants.USER_BATCH_UPDATE_PRODUCT_MARKETING_INFO, json, headerMap);
    		log.info("?????????????????????????????????????????????"+resopEntity.toString());
    		String body=resopEntity.getBody();
    		JSONObject bodyJosn=JSONObject.parseObject(body);
    		Integer state=bodyJosn.getInteger("state");
    		if (null==state || state.intValue()!=200) {
    			throw new SuperCodeException("?????????????????????????????????????????????????????????"+bodyJosn.getString("msg"), 500);
    		}
		}
    }

	private List<ProductAndBatchGetCodeMO> constructProductAndBatchMOByProductIds(Collection<String> productIds) {
		List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOList = new ArrayList<>();
		if (productIds == null) {
			return productAndBatchGetCodeMOList;
		}
		productIds.forEach(productId -> {
			ProductAndBatchGetCodeMO productAndBatchGetCodeMO = new ProductAndBatchGetCodeMO();
			productAndBatchGetCodeMO.setProductId(productId);
			productAndBatchGetCodeMO.setProductBatchList(new ArrayList<>());
			productAndBatchGetCodeMOList.add(productAndBatchGetCodeMO);
		});
		return productAndBatchGetCodeMOList;
	}

	/**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????
     * @param jsonArray
     * @return
     */
	private List<ProductAndBatchGetCodeMO> constructProductAndBatchMOByPPArr(JSONArray jsonArray) {
		Map<String, ProductAndBatchGetCodeMO> productMap=new HashMap<String, ProductAndBatchGetCodeMO>();
		for(int i=0;i<jsonArray.size();i++) {
			JSONObject prodObject=jsonArray.getJSONObject(i);
			String productId=prodObject.getString("productId");
			//TODO ???????????????????????????????????????
			String productBatchId=prodObject.getString("traceBatchInfoId");
			
			ProductAndBatchGetCodeMO  productAndBatchGetCodeMO=productMap.get(productId);
			if (null==productAndBatchGetCodeMO) {
				productAndBatchGetCodeMO=new ProductAndBatchGetCodeMO();
				productAndBatchGetCodeMO.setProductId(productId);
			}
			
			List<Map<String, String>> productBatchIdS=productAndBatchGetCodeMO.getProductBatchList();
			if (null==productBatchIdS) {
				productBatchIdS=new ArrayList<Map<String,String>>();
			}
			Map<String, String> batchmap=new HashMap<String, String>();
			batchmap.put("productBatchId", productBatchId);
			productBatchIdS.add(batchmap);
			productAndBatchGetCodeMO.setProductBatchList(productBatchIdS);
			productMap.put(productId, productAndBatchGetCodeMO);
		}
		List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs=new ArrayList<ProductAndBatchGetCodeMO>();
		for(Entry<String, ProductAndBatchGetCodeMO> entry:productMap.entrySet()) {
			productAndBatchGetCodeMOs.add(entry.getValue());
		}
		return productAndBatchGetCodeMOs;
	}

    /**
     * ?????????????????????????????????url???????????????
     * @param businessType
     * @param superToken
     * @param productAndBatchGetCodeMOs
     * @throws SuperCodeException
     */
	private void integralUrlUnBindBatch(int businessType, String superToken, List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs)
			throws SuperCodeException {
		if (null!=productAndBatchGetCodeMOs && !productAndBatchGetCodeMOs.isEmpty()) {
			JSONArray array = commonService.getBatchInfo(productAndBatchGetCodeMOs, superToken,WechatConstants.CODEMANAGER_GET_BATCH_CODE_INFO_URL);
			List<SbatchUrlUnBindDto> deleteProductBatchList = new ArrayList<>();
			for(int i=0;i<array.size();i++) {
				JSONObject batchobj=array.getJSONObject(i);
				String productId=batchobj.getString("productId");
				String productBatchId=batchobj.getString("productBatchId");
				Long codeTotal=batchobj.getLong("codeTotal");
				String codeBatch=batchobj.getString("globalBatchId");
				if (StringUtils.isBlank(productId)||StringUtils.isBlank(codeBatch) || null==codeTotal) {
					throw new SuperCodeException("??????????????????????????????????????????????????????????????????????????????id??????????????????"+productId+","+productBatchId, 500);
				}
				String[] sbatchIdArray = codeBatch.split(",");
				for(String sbatchId : sbatchIdArray) {
					SbatchUrlUnBindDto sbatchUrlDto = new SbatchUrlUnBindDto();
					sbatchUrlDto.setUrl(marketingDomain + WechatConstants.SCAN_CODE_JUMP_URL);
					List<Integer> bizTypeList = new ArrayList<>();
					bizTypeList.add(businessType);
					sbatchUrlDto.setBusinessTypes(bizTypeList);
					sbatchUrlDto.setBatchId(Long.parseLong(sbatchId));
					sbatchUrlDto.setClientRole(MemberTypeEnums.VIP.getType()+"");
					sbatchUrlDto.setProductId(productId);
					sbatchUrlDto.setProductBatchId(productBatchId);
					deleteProductBatchList.add(sbatchUrlDto);
				}
			}
			//????????????????????????????????????productId???batchId,????????????productBatchId;
			Map<String, SbatchUrlUnBindDto> sbatchUrlUnbindDtoMap = new HashMap<>();
			deleteProductBatchList.forEach(sbatchUrlUnbindDto -> {
				Long batchId = sbatchUrlUnbindDto.getBatchId();
				String productId = sbatchUrlUnbindDto.getProductId();
				String bpKey = batchId + "_" +productId;
				SbatchUrlUnBindDto childSbatchUrlUnbindDto = sbatchUrlUnbindDtoMap.get(bpKey);
				if (childSbatchUrlUnbindDto == null) {
					childSbatchUrlUnbindDto = new SbatchUrlUnBindDto();
					BeanUtils.copyProperties(sbatchUrlUnbindDto, childSbatchUrlUnbindDto);
					childSbatchUrlUnbindDto.setProductBatchId(null);
					sbatchUrlUnbindDtoMap.put(bpKey, childSbatchUrlUnbindDto);
				}
			});
			List<SbatchUrlUnBindDto> sbatchUrlUnBindDtoList = new ArrayList<>(sbatchUrlUnbindDtoMap.values());
//			List<Map<String, Object>> batchInfoparams=commonService.getUrlToBatchParam(obj.getJSONArray("results"), marketingDomain+WechatConstants.SCAN_CODE_JUMP_URL,businessType);
			log.info("????????????????????????{}", JSON.toJSONString(sbatchUrlUnBindDtoList));
			RestResult<Object> objectRestResult = getSbatchIdsByPrizeWheelsFeign.removeOldProduct(sbatchUrlUnBindDtoList);
			log.info("?????????????????????{}", JSON.toJSONString(objectRestResult));
			if (objectRestResult == null || objectRestResult.getState().intValue() != 200) {
				throw new SuperCodeException("??????????????????????????????url?????????" + objectRestResult, 500);
			}
		}
	}

	/**
	 * ?????????????????????????????????url???????????????
	 * @param businessType
	 * @param superToken
	 * @param productAndBatchGetCodeMOs
	 * @throws SuperCodeException
	 */
	private void integralUrlBindBatch(int businessType, String superToken, List<ProductAndBatchGetCodeMO> productAndBatchGetCodeMOs)
			throws SuperCodeException {
		if (null!=productAndBatchGetCodeMOs && !productAndBatchGetCodeMOs.isEmpty()) {
			JSONArray array = commonService.getBatchInfo(productAndBatchGetCodeMOs, superToken,WechatConstants.CODEMANAGER_GET_BATCH_CODE_INFO_URL);
			List<SbatchUrlDto> batchInfoparams=commonService.getUrlToBatchDto(array, marketingDomain+WechatConstants.SCAN_CODE_JUMP_URL,businessType);
			//????????????????????????????????????productId???batchId,????????????productBatchId;
			Map<String, SbatchUrlDto> sbatchUrlDtoMap = new HashMap<>();
			batchInfoparams.forEach(sbatchUrlDto -> {
				Long batchId = sbatchUrlDto.getBatchId();
				String productId = sbatchUrlDto.getProductId();
				String bpKey = batchId + "_" +productId;
				SbatchUrlDto childSbatchUrlDto = sbatchUrlDtoMap.get(bpKey);
				if (childSbatchUrlDto == null) {
					childSbatchUrlDto = new SbatchUrlDto();
					BeanUtils.copyProperties(sbatchUrlDto, childSbatchUrlDto);
					childSbatchUrlDto.setProductBatchId(null);
					sbatchUrlDtoMap.put(bpKey, childSbatchUrlDto);
				}
			});
			List<SbatchUrlDto> sbatchUrlDtoList = new ArrayList<>(sbatchUrlDtoMap.values());
			log.info("??????????????????????????????{}", JSON.toJSONString(sbatchUrlDtoList));
			RestResult bindBatchBody=getSbatchIdsByPrizeWheelsFeign.bindingUrlAndBizType(sbatchUrlDtoList);
			log.info("??????????????????????????????{}", JSON.toJSONString(bindBatchBody));
			int bindBatchstate=bindBatchBody.getState();
			if (200!=bindBatchstate) {
				throw new SuperCodeException("???????????????????????????????????????url?????????"+bindBatchBody, 500);
			}
		}
	}

	public RestResult<AbstractPageService.PageResults<List<IntegralRuleProduct>>>  unSelectPage(DaoSearch daoSearch) throws SuperCodeException {
		
		 RestResult<AbstractPageService.PageResults<List<IntegralRuleProduct>>> restResult=new RestResult<AbstractPageService.PageResults<List<IntegralRuleProduct>>>();
		Map<String, Object>params=new HashMap<String, Object>();
		Integer current=daoSearch.getCurrent();
		Integer pagesize=daoSearch.getPageSize();
		
		params.put("current", current);
		params.put("pageSize", pagesize);
		String organizationId = commonUtil.getOrganizationId();
		params.put("organizationId",organizationId );
		params.put("search", daoSearch.getSearch());
		List<String> productIds = dao.selectProductIdsByOrgId(organizationId);
		if (null!=productIds && !productIds.isEmpty()) {
			params.put("excludeProductIds",String.join(",", productIds));
		}
		log.info("CODEMANAGER_RELATION_PRODUCT_URL = /code/relation/list/relation/productRecord : params ==> {}",JSONObject.toJSONString(params));
		Map<String, String> headerMap = new HashMap<>();
		headerMap.put("super-token", getSuperToken());
		ResponseEntity<String> responseEntity = restTemplateUtil.getRequestAndReturnJosn(codeManagerRestUrl+CommonConstants.CODEMANAGER_RELATION_PRODUCT_URL, params, headerMap);
		String body = responseEntity.getBody();
		log.info("??????????????????????????????????????????????????????"+body);
		JSONObject json=JSONObject.parseObject(body);
		int state=json.getInteger("state");
		List<IntegralRuleProduct> ruleproductList=new ArrayList<IntegralRuleProduct>();
		if (state==200) {
			restResult.setState(200);
			JSONArray arry=json.getJSONObject("results").getJSONArray("list");
  			for (int i=0 ;i<arry.size();i++) {
 				JSONObject ruleProduct=arry.getJSONObject(i);
 				String prductId = ruleProduct.getString("productId");
// 				if(productIds == null || !productIds.contains(prductId)) {
					IntegralRuleProduct product=new IntegralRuleProduct();
	 				product.setId(ruleProduct.getString("esId"));
	 				product.setProductId(prductId);
					product.setProductName(ruleProduct.getString("productName"));
					ruleproductList.add(product);
// 				}
			}
			String pagination_str=json.getJSONObject("results").getString("pagination");
			com.jgw.supercodeplatform.marketing.common.page.Page page=JSONObject.parseObject(pagination_str, com.jgw.supercodeplatform.marketing.common.page.Page.class);
			AbstractPageService.PageResults<List<IntegralRuleProduct>> pageResults=new PageResults<List<IntegralRuleProduct>>(ruleproductList, page);
		    restResult.setResults(pageResults);
		}else {
			throw new SuperCodeException("???????????????????????????", 500);
		}
		return restResult;
	}

	public IntegralRuleProduct selectByProductIdAndOrgId(String productId, String organizationId) {
		return dao.selectByProductIdAndOrgId(productId, organizationId);
	}
    
    
}
