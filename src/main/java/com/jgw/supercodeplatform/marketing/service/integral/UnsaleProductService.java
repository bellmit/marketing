package com.jgw.supercodeplatform.marketing.service.integral;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.page.AbstractPageService;
import com.jgw.supercodeplatform.marketing.common.page.Page;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.RestTemplateUtil;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.dao.integral.IntegralExchangeMapperExt;
import com.jgw.supercodeplatform.marketing.dao.integral.ProductUnsaleMapperExt;
import com.jgw.supercodeplatform.marketing.dto.baseservice.product.sale.ProductMarketingSearchView;
import com.jgw.supercodeplatform.marketing.dto.baseservice.product.sale.ProductMarketingSkuSingleView;
import com.jgw.supercodeplatform.marketing.dto.baseservice.product.sale.ProductView;
import com.jgw.supercodeplatform.marketing.dto.baseservice.product.unsale.NonSelfSellingProductMarketingSearchView;
import com.jgw.supercodeplatform.marketing.dto.baseservice.product.unsale.NonSelfSellingProductMarketingSkuSingleView;
import com.jgw.supercodeplatform.marketing.dto.baseservice.product.unsale.UnSaleProductPageResults;
import com.jgw.supercodeplatform.marketing.dto.baseservice.vo.ProductAndSkuVo;
import com.jgw.supercodeplatform.marketing.dto.integral.ProductPageFromBaseServiceParam;
import com.jgw.supercodeplatform.marketing.dto.integral.ProductPageParam;
import com.jgw.supercodeplatform.marketing.dto.integral.SkuInfo;
import com.jgw.supercodeplatform.marketing.pojo.integral.IntegralExchange;
import com.jgw.supercodeplatform.marketing.pojo.integral.ProductUnsale;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class UnsaleProductService extends AbstractPageService<ProductUnsale> {
    @Value("${rest.user.url}")
    private  String baseService;

    @Autowired
    private ProductUnsaleMapperExt mapper;
    @Autowired
    private RestTemplateUtil restTemplateUtil;

    @Autowired
    private RestTemplate restTemplate;


    @Autowired
    private IntegralExchangeMapperExt integralExchangeMapper;


    @Autowired
    private CommonUtil commonUtil;



    @Autowired
    private ModelMapper modelMapper;

    /**
     * ?????????????????????
     * @param organizationId
     * @return
     */
    public RestResult< AbstractPageService.PageResults<List<ProductAndSkuVo>> > selectUnSalePruduct(String organizationId, ProductPageParam pageParam) throws SuperCodeException{
        if(StringUtils.isBlank(organizationId)){
            throw  new SuperCodeException("??????id????????????");
        }
        // ????????????
        if(StringUtils.isBlank(organizationId)){
            throw new SuperCodeException("????????????ID????????????");
        }

        // ????????????????????????????????????????????????
        Set<IntegralExchange> excludeProducts = integralExchangeMapper.selectUnSalePruduct(organizationId);
        // ???????????????Id
        Set<String> excludeProductIds = new HashSet<>();
        // ???????????????skuId
        Set<String> excludeSkuIds = new HashSet<>();
        Map<String,List<String>> newExcludeSkuIds = new HashMap<>();

        // ?????????????????????????????????
        for(IntegralExchange excludeProduct:excludeProducts){
            if(!StringUtils.isBlank(excludeProduct.getProductId()) && excludeProduct.getSkuStatus() == 0){
                excludeProductIds.add(excludeProduct.getProductId());
            }
            if(!StringUtils.isBlank(excludeProduct.getSkuId())){
//                excludeSkuIds.add(excludeProduct.getSkuId());
                List<String> skuIds = newExcludeSkuIds.get(excludeProduct.getProductId());
                if(skuIds == null){
                    skuIds = new ArrayList<String>();
                }
                skuIds.add(excludeProduct.getSkuId());
                newExcludeSkuIds.put(excludeProduct.getProductId(),skuIds);
            }
        }

        // ???????????????????????????????????????????????????;????????????????????????skuid??????productId
        if(pageParam.getId() !=null  ){
            IntegralExchange integralExchange = integralExchangeMapper.selectByPrimaryKey(pageParam.getId());
            if(integralExchange == null){
                throw new SuperCodeException("?????????????????????...");
            }
            if(!StringUtils.isBlank(integralExchange.getProductId()) && integralExchange.getSkuStatus() == 0){
                excludeProductIds.remove(integralExchange.getProductId());
            }
            if(!StringUtils.isBlank(integralExchange.getSkuId())){
//                excludeSkuIds.remove(integralExchange.getSkuId());
                List<String> skuIds = newExcludeSkuIds.get(integralExchange.getProductId());
                if(skuIds != null){
                    skuIds.remove(integralExchange.getSkuId());
                }
                if(CollectionUtils.isEmpty(skuIds)){
                    newExcludeSkuIds.remove(integralExchange.getProductId());

                }
            }
        }
        // sku
        // productID_[SKU]
        // ?????????sku,????????????,?????????sku,??????sku
        // PRODUCT_id
        // ??????????????????

        // ??????????????????
        ProductPageFromBaseServiceParam queryCondition = modelMapper.map(pageParam, ProductPageFromBaseServiceParam.class);
        queryCondition.setExcludeProductIds(new ArrayList(excludeProductIds));
        queryCondition.setExcludeSkuIds(newExcludeSkuIds);
        RestResult< AbstractPageService.PageResults<List<ProductAndSkuVo>> > restResult = getProductFromBaseService(queryCondition,false);
        if(restResult.getState() == 200){
            return  restResult;
        }else{
            return RestResult.error(null);
        }
    }


    /**
     * ????????????id:??????????????????
     * @param organizationId
     * @return
     */
    public RestResult< AbstractPageService.PageResults<List<ProductAndSkuVo>> > selectSalePruduct(String organizationId, ProductPageParam pageParam) throws SuperCodeException{
        // ????????????
        if(StringUtils.isBlank(organizationId)){
            throw new SuperCodeException("????????????ID????????????");
        }
        long startTime = System.currentTimeMillis();
        // ?????????????????????????????????????????????
        Set<IntegralExchange> excludeProducts = integralExchangeMapper.selectSalePruduct(organizationId);

        // ???????????????Id
        Set<String> excludeProductIds = new HashSet<>();
        // ???????????????skuId
        Set<String> excludeSkuIds = new HashSet<>();
        Map<String,List<String>> newExcludeSkuIds = new HashMap<>();
        // ????????????????????????????????? ???skuid,????????????????????????productid
        if(!excludeProducts.isEmpty()){
            for(IntegralExchange excludeProduct:excludeProducts){
                if(!StringUtils.isBlank(excludeProduct.getProductId()) && excludeProduct.getSkuStatus() == 0){
                    excludeProductIds.add(excludeProduct.getProductId());
                }
                // ???skuid
                if(!StringUtils.isBlank(excludeProduct.getSkuId()) && excludeProduct.getSkuStatus() == 1){
//                    excludeSkuIds.add(excludeProduct.getSkuId());
                    List<String> skuIds = newExcludeSkuIds.get(excludeProduct.getProductId());
                    if(skuIds == null){
                        skuIds = new ArrayList<String>();
                    }
                    skuIds.add(excludeProduct.getSkuId());
                    newExcludeSkuIds.put(excludeProduct.getProductId(),skuIds);
                }

            }
        }
        // ???????????????????????????????????????????????????;????????????????????????skuid??????productId
        if(pageParam.getId() !=null  ){
            IntegralExchange integralExchange = integralExchangeMapper.selectByPrimaryKey(pageParam.getId());
            if(integralExchange == null){
                throw new SuperCodeException("?????????????????????...");
            }
            if(!StringUtils.isBlank(integralExchange.getProductId()) && integralExchange.getSkuStatus() == 0){
                excludeProductIds.remove(integralExchange.getProductId());
            }
            if(!StringUtils.isBlank(integralExchange.getSkuId())){
               // excludeSkuIds.remove(integralExchange.getSkuId());
                List<String> skuIds = newExcludeSkuIds.get(integralExchange.getProductId());
                if(skuIds != null){
                    skuIds.remove(integralExchange.getSkuId());
                }
                if(CollectionUtils.isEmpty(skuIds)){
                    newExcludeSkuIds.remove(integralExchange.getProductId());

                }

            }
        }

        // ??????????????????
        ProductPageFromBaseServiceParam queryCondition = modelMapper.map(pageParam, ProductPageFromBaseServiceParam.class);
        // ??????????????????????????????????????????
        queryCondition.setExcludeProductIds(new ArrayList(excludeProductIds));
        queryCondition.setExcludeSkuIds(newExcludeSkuIds);
        // ????????????

        RestResult< AbstractPageService.PageResults<List<ProductAndSkuVo>> > restResult = getProductFromBaseService(queryCondition,true);
        if(restResult.getState() == 200){
             return  restResult;
        }else{
            return RestResult.error(null);
        }
    }

//    @HystrixCommand(fallbackMethod = "getProductFromBaseServiceHystrix")
    public RestResult< AbstractPageService.PageResults<List<ProductAndSkuVo>> > getProductFromBaseService(ProductPageFromBaseServiceParam queryCondition,boolean isSale)throws SuperCodeException{
        Map<String, String> header = new HashMap<>();
        header.put("super-token",commonUtil.getSuperToken());
        // ??????json??????????????????
        String queryConditionStr = JSONObject.toJSONString(queryCondition);

        Map queryConditionMap = modelMapper.map(JSONObject.parse(queryConditionStr), HashMap.class);
        // ?????????????????????????????????
        List<String> excludeProductIds = (List<String>) queryConditionMap.get("excludeProductIds");
        Map<String,List<String>> excludeSkuIds = (Map<String,List<String>>) queryConditionMap.get("excludeSkuIds");
        if(!CollectionUtils.isEmpty(excludeProductIds)){
            queryConditionMap.put("excludeProductIds",JSONObject.toJSONString(excludeProductIds));
        }
        if(!CollectionUtils.isEmpty(excludeSkuIds)){
            queryConditionMap.put("excludeSkuIds",JSONObject.toJSONString(excludeSkuIds));
        }
        long startTime = System.currentTimeMillis();
        if(isSale){
            // ????????????
            // Map map = modelMapper.map(queryCondition, HashMap.class); ????????????

            ResponseEntity<String> response = restTemplateUtil.getRequestAndReturnJosn(baseService + CommonConstants.SALE_PRODUCT_URL,queryConditionMap, header);
            if(log.isInfoEnabled()){
                log.info("{????????????????????????}"+(System.currentTimeMillis()-startTime));
            }
            // ??????????????? start ????????????  ?????????????????????????????????
            JSONObject restResultJson = JSONObject.parseObject(response.getBody());
            JSONObject pageResults = (JSONObject) restResultJson.get("results");
            Page pagination = modelMapper.map(pageResults.get("pagination"), Page.class);
            JSONArray list1 = pageResults.getJSONArray("list");
            List<ProductView> listBySelf = new ArrayList<>();
            for(int i=0; i<list1.size(); i++){
                // ????????????
                ProductView pDTO = new ProductView();
                JSONObject productView = (JSONObject)list1.get(i);

                String productId = productView.getString("productId");

                // ??????????????????sku???????????????,??????????????????,??????????????????sku????????????????????????
                // ??????????????????????????????????????????sku?????????????????????????????????
                // ????????????????????????
                // ????????????:h5???????????????????????????????????????????????????????????????bug,[???????????????sku?????????????????????]

                String productName = productView.getString("productName");
                String productUrl = productView.getString("productUrl");
                // ??????????????????
                JSONObject productMarketingJson = (JSONObject) productView.get("productMarketing");
                ProductMarketingSearchView productMarketing = null;
                if(productMarketingJson != null){
                    productMarketing = new ProductMarketingSearchView();
                // ????????????????????????:?????????
                    productMarketing.setViewPrice(productMarketingJson.getBigDecimal("viewPrice"));

                }

                // ??????????????????-sku??????
                if(productMarketing !=null){
                    List<ProductMarketingSkuSingleView> skuInfos = new ArrayList<>();
                    JSONArray productMarketingSkusJson = productMarketingJson.getJSONArray("productMarketingSkus");

                    // sku?????????skuid,name,url
                    for(int j=0;j<productMarketingSkusJson.size();j++){
                        ProductMarketingSkuSingleView sku = new ProductMarketingSkuSingleView();
                        JSONObject skuJson = productMarketingSkusJson.getJSONObject(j);
                        Long skuId = skuJson.getLong("id");
                        String skuName = skuJson.getString("sku");
                        String skuUrl = skuJson.getString("pic");
                        sku.setId(skuId);
                        sku.setSku(skuName);
                        sku.setPic(skuUrl);
                        skuInfos.add(sku);

                    }

                    productMarketing.setProductMarketingSkus(skuInfos);
                }

                pDTO.setProductId(productId);
                pDTO.setProductName(productName);
                pDTO.setProductUrl(productUrl);
                pDTO.setProductMarketing(productMarketing);
                listBySelf.add(pDTO);
            }
            // ??????????????? end
            // ??????????????????????????????ID,??????????????????????????????SKUID,???????????????
            RestResult<PageResults<List<ProductAndSkuVo>>> pageResultsRestResult = changeBaseServiceDtoToVo(null, listBySelf,pagination,null);

            return pageResultsRestResult;
        }else{
            // ???????????????
            ResponseEntity<String> response = restTemplateUtil.getRequestAndReturnJosn(baseService + CommonConstants.UN_SALE_PRODUCT_URL,queryConditionMap, header);
            if(log.isInfoEnabled()){
                log.info("{????????????????????????}"+(System.currentTimeMillis()-startTime));
            }
            RestResult restResult = JSONObject.parseObject(response.getBody(), RestResult.class);

            UnSaleProductPageResults results =   modelMapper.map(restResult.getResults(),UnSaleProductPageResults.class);
            List<NonSelfSellingProductMarketingSearchView> list = modelMapper.map(results.getList(),List.class);
            return changeUnSaleBaseServiceDtoToVo(results,list);
        }
    }

    /**
     * ??????????????????????????????VO
     * @param results
     * @param list
     * @return
     */
    private RestResult<AbstractPageService.PageResults<List<ProductAndSkuVo>>> changeUnSaleBaseServiceDtoToVo(UnSaleProductPageResults results, List<NonSelfSellingProductMarketingSearchView> list) {
        // ??????????????????VO??????
        List<ProductAndSkuVo> listVO = new ArrayList<ProductAndSkuVo>();
        // ????????????
        for(NonSelfSellingProductMarketingSearchView baseServicePrudoctDto: list) {
            // ??????VO
            ProductAndSkuVo productVO = new ProductAndSkuVo();
            // ??????ID
            productVO.setProductId(baseServicePrudoctDto.getProductId());
            // ????????????
            productVO.setProductName(baseServicePrudoctDto.getProductName());
            // ????????????
            productVO.setProductPic(baseServicePrudoctDto.getProductUrl());
            // ?????????
            if(baseServicePrudoctDto.getViewPrice() != null){
                productVO.setShowPriceStr(baseServicePrudoctDto.getViewPrice().toString());
            }else {
                // ???????????????
                productVO.setShowPriceStr("0.00");
            }
            // ??????VOsku??????
            List<SkuInfo> listSkuVO = new ArrayList<>();
            for(NonSelfSellingProductMarketingSkuSingleView skuDto : baseServicePrudoctDto.getProductMarketingSkus()) {
                // skuVO??????
                SkuInfo skuVO = new SkuInfo();
                // skuID
                skuVO.setSkuId(skuDto.getId()+"");
                // SKU??????
                skuVO.setSkuName(skuDto.getSku());
                // sku??????
                skuVO.setSkuUrl(skuDto.getPic());
                listSkuVO.add(skuVO);

            }
            productVO.setSkuInfo(listSkuVO);
            listVO.add(productVO);

        }

        // ????????????
        Page page = modelMapper.map(results.getPagination(),Page.class);
        AbstractPageService.PageResults<List<ProductAndSkuVo>> pageVO = new AbstractPageService.PageResults( listVO,page);
        pageVO.setOther(results.getOther());
        return  RestResult.success("",pageVO);
    }

    /**
     * ?????????????????????VO
     * @param results
     * @param list
     * @return
     */
    private RestResult<AbstractPageService.PageResults<List<ProductAndSkuVo>>> changeBaseServiceDtoToVo(com.jgw.supercodeplatform.marketing.dto.baseservice.product.sale.PageResults results, List<ProductView> list, Page page ,Object other) {
        // ????????????
        long startTime = System.currentTimeMillis();
        List<ProductAndSkuVo> listVO = new ArrayList<ProductAndSkuVo>();

        for(ProductView baseserviceProductDto :list){
            ProductAndSkuVo towebProductVo = new ProductAndSkuVo();
            ProductMarketingSearchView productMarketing = baseserviceProductDto.getProductMarketing();
            // ??????ID
            String productId = baseserviceProductDto.getProductId();
            towebProductVo.setProductId(productId);

            // ????????????
            String productName = baseserviceProductDto.getProductName();
            towebProductVo.setProductName(productName);

            // ????????????
            String productUrl = baseserviceProductDto.getProductUrl();
            towebProductVo.setProductPic(productUrl);

            // SKU?????? ???????????????
            if(productMarketing != null){
                // ?????????
                if(productMarketing.getViewPrice() != null){
                    towebProductVo.setShowPriceStr(productMarketing.getViewPrice().toString());
                }else {
                    // ???????????????
                    towebProductVo.setShowPriceStr("0.00");
                }

                // ??????sku??????skuDTO
                List<ProductMarketingSkuSingleView> productMarketingSkus = productMarketing.getProductMarketingSkus();
                // ??????skuVO
                List<SkuInfo> listSkuVO = new ArrayList<>();
                for(ProductMarketingSkuSingleView skuDto : productMarketingSkus){
                    SkuInfo skuVO = new SkuInfo();
                    // ??????????????????????????????;??????String[???????????????????????????????????????String,??????????????????????????????????????????]
                    // skuID
                    skuVO.setSkuId(skuDto.getId()+"");

                    // sku??????
                    skuVO.setSkuName(skuDto.getSku());
                    // sku??????
                    skuVO.setSkuUrl(skuDto.getPic());
                    // ??????sku??????
                    listSkuVO.add(skuVO);
                }
                towebProductVo.setSkuInfo(listSkuVO);

            }else {
                // ?????????????????????null
                List<SkuInfo> listSkuVO = new ArrayList<>();
                towebProductVo.setSkuInfo(listSkuVO);

            }


            listVO.add(towebProductVo);
        }
        log.info("{??????????????????????????????1}"+(System.currentTimeMillis()-startTime));

        // ????????????
        AbstractPageService.PageResults<List<ProductAndSkuVo>> pageVO = new AbstractPageService.PageResults( listVO,page);
        pageVO.setOther(other);
        log.info("{??????????????????????????????2}"+(System.currentTimeMillis()-startTime));

        return  RestResult.success("",pageVO);
    }

    /**
     * ??????????????????
     * @param excludeProductIds
     * @param isSale
     * @return
     * @throws SuperCodeException
     */
    public RestResult<Object> getProductFromBaseServiceHystrix( List<String> excludeProductIds,boolean isSale)throws SuperCodeException{
        return RestResult.error("");
    }

    /**
     * ?????????????????????????????????
     * @param searchParams
     * @return
     * @throws Exception
     */
    @Override
    protected List<ProductUnsale> searchResult(ProductUnsale searchParams) throws Exception {
        return mapper.list(searchParams);
    }

    /**
     * ???????????????????????????????????????
     * @param searchParams
     * @return
     * @throws Exception
     */
    @Override
    protected int count(ProductUnsale searchParams) throws Exception {
        return mapper.count(searchParams);
    }

    /**
     * ?????????????????????
     * @param id
     * @param organizationId
     * @return
     */
    public ProductUnsale selectById(Long id, String organizationId) throws SuperCodeException{
        // ????????????
        ProductUnsale productUnsale = mapper.selectByPrimaryKey(id);
        if(productUnsale == null){
            throw  new SuperCodeException("????????????????????????");
        }
        if(!organizationId.equals(productUnsale.getOrganizationId())){
            throw  new SuperCodeException("???????????????");
        }

        // ??????sku
        String skuJsonString = productUnsale.getUnsaleProductSkuInfo();
        List<SkuInfo> skuChilds = JSONArray.parseArray(skuJsonString, SkuInfo.class);
        productUnsale.setSkuChild(skuChilds);
        return productUnsale;
    }

    /**
     * ?????????????????????
     * @param productUnsale
     * @return
     */
    public int add(ProductUnsale productUnsale) throws SuperCodeException{
        if(StringUtils.isBlank(productUnsale.getUnsaleProductName())){
            throw new SuperCodeException("????????????????????????");
        }
        if(StringUtils.isBlank(productUnsale.getUnsaleProductPic())){
            throw new SuperCodeException("????????????????????????");
        }

        // ??????????????????
        productUnsale.setShowPrice(Float.parseFloat(productUnsale.getShowPriceStr()));
        if(productUnsale.getShowPrice() == null || productUnsale.getShowPrice() <= 0){
            throw new SuperCodeException("????????????????????????0.00");
        }

        if(StringUtils.isBlank(productUnsale.getDetail())){
            throw new SuperCodeException("????????????????????????");
        }

        if(StringUtils.isBlank(productUnsale.getOrganizationId()) || StringUtils.isBlank(productUnsale.getOrganizationName())){
            throw new SuperCodeException("?????????????????????");
        }

        if(StringUtils.isBlank(productUnsale.getCreateUserId()) || StringUtils.isBlank(productUnsale.getCreateUserName())){
            throw new SuperCodeException("?????????????????????");
        }
        // SKU????????????
        List<SkuInfo> skuChild = productUnsale.getSkuChild();

        if(CollectionUtils.isEmpty(skuChild)){
            productUnsale.setUnsaleProductSkuNum(0);
        }else {
            if(skuChild.size() > 10){
                throw new SuperCodeException("sku????????????????????????");
            }
            // ??????sku???json??????
            String skuJsonString = null;
            try {
                skuJsonString = JSONObject.toJSONString(skuChild);
            } catch (Exception e) {
                log.error("sku????????????:"+skuChild);
                e.printStackTrace();
                throw new SuperCodeException("sku????????????");
            }

            productUnsale.setUnsaleProductSkuInfo(skuJsonString);
        }

        // ????????????
        productUnsale.setProductId(UUID.randomUUID().toString().replace("-",""));
        productUnsale.setCreateDate(new Date());
        int i = mapper.insertSelective(productUnsale);
        if(i!=1){
            throw new SuperCodeException("???????????????????????????");
        }
        return i;
    }

    /**
     * ?????????????????????
     * @param productUnsale
     * @return
     */
    public int update(ProductUnsale productUnsale) throws SuperCodeException{
        if(productUnsale.getId() == null || productUnsale.getId() <= 0){
            throw new SuperCodeException("???????????????id????????????");
        }
        if(StringUtils.isBlank(productUnsale.getOrganizationId())){
            throw new SuperCodeException("??????ID?????????");
        }
        ProductUnsale productUnsaleDO = mapper.selectByPrimaryKey(productUnsale.getId());
        if(productUnsaleDO == null || !productUnsaleDO.getOrganizationId().equals(productUnsale.getOrganizationId()) ){
            throw new SuperCodeException("????????????");

        }
        List<SkuInfo> skuChild = productUnsale.getSkuChild();

        if(CollectionUtils.isEmpty(skuChild)){
            productUnsale.setUnsaleProductSkuNum(0);
        }else {
            if(skuChild.size() > 10){
                throw new SuperCodeException("sku????????????????????????");
            }
            // ??????sku???json??????
            String skuJsonString = null;
            try {
                skuJsonString = JSONObject.toJSONString(skuChild);
            } catch (Exception e) {
                log.error("sku????????????:" + skuChild);
                e.printStackTrace();
                throw new SuperCodeException("?????????????????????sku????????????");
            }
            productUnsale.setUnsaleProductSkuInfo(skuJsonString);
        }
        int i = mapper.updateByPrimaryKeySelective(productUnsale);
        if(i!=1){
            throw new SuperCodeException("???????????????????????????");
        }
        return i;
    }

    /**
     * ?????????????????????
     * @param id
     */
    public int delete(Long id,String organizationId) throws SuperCodeException{
        if(StringUtils.isBlank(organizationId)){
            throw new SuperCodeException("??????ID?????????");
        }

        ProductUnsale productUnsale = mapper.selectByPrimaryKey(id);
        if(productUnsale == null){
            throw new SuperCodeException("????????????????????????");
        }
        if(!productUnsale.getOrganizationId().equals(organizationId)){
            log.error("??????id"+organizationId+"?????????????????????"+id+"????????????");
            throw new SuperCodeException("????????????");
        }
        int i = mapper.deleteByPrimaryKey(id);
        if(i!=1){
            throw new SuperCodeException("???????????????????????????");
        }
        return i;
    }
}
