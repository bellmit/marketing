package com.jgw.supercodeplatform.marketing.service.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.jgw.supercodeplatform.common.pojo.common.JsonResult;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.exception.SuperCodeExtException;
import com.jgw.supercodeplatform.marketing.common.model.HttpClientResult;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.ProductAndBatchGetCodeMO;
import com.jgw.supercodeplatform.marketing.common.properties.NormalProperties;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.HttpRequestUtil;
import com.jgw.supercodeplatform.marketing.common.util.RestTemplateUtil;
import com.jgw.supercodeplatform.marketing.config.redis.RedisUtil;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.constants.RedisKey;
import com.jgw.supercodeplatform.marketing.constants.SystemLabelEnum;
import com.jgw.supercodeplatform.marketing.constants.WechatConstants;
import com.jgw.supercodeplatform.marketing.dto.OuterCodesEntity;
import com.jgw.supercodeplatform.marketing.dto.OuterCodesEntity.OuterCode;
import com.jgw.supercodeplatform.marketing.dto.activity.MarketingMemberAndScanCodeInfoParam;
import com.jgw.supercodeplatform.marketing.enums.CodeTypeEnum;
import com.jgw.supercodeplatform.marketing.enums.EsIndex;
import com.jgw.supercodeplatform.marketing.enums.EsType;
import com.jgw.supercodeplatform.marketing.exception.BizRuntimeException;
import com.jgw.supercodeplatform.prizewheels.infrastructure.feigns.dto.SbatchUrlDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CommonService {
	@Autowired
    private CommonUtil commonUtil;

    @Autowired
    private RestTemplateUtil restTemplateUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Value("${rest.user.url}")
    private String restUserUrl;
    
    @Value("${rest.logistics.url}")
    private String logisticsUrl;

	@Value("${rest.codemanager.url}")
	private String codeManagerUrl;

	@Value("${rest.code.url}")
	private String msCodeUrl;
	@Autowired
	@Qualifier("elClient")
	private TransportClient eClient;

	public RestResult<String> sendPhoneCode(String mobile) throws Exception {
		RestResult<String> resuRestResult=new RestResult<String>();
		if (StringUtils.isBlank(mobile)) {
			resuRestResult.setState(500);
			resuRestResult.setMsg("?????????????????????");
			return resuRestResult;
		}
		String code=commonUtil.getSixRandom();
		JSONObject jsonObj=new JSONObject();
		jsonObj.put("code", code);
		jsonObj.put("phoneNumber", mobile);
		String json=jsonObj.toJSONString();
		ResponseEntity<String> responseEntity=restTemplateUtil.postJsonDataAndReturnJosn(restUserUrl+WechatConstants.SEND_PHONE_CODE_URL, json, null);
		int status=responseEntity.getStatusCodeValue();
		String body=responseEntity.getBody();
		//??????HTTP????????????
		if (status>=200 && status<300 ) {
			JSONObject codeObj=JSONObject.parseObject(body);
			boolean flag=codeObj.containsKey("state");
			//????????????????????????????????????
			if (flag && 200==codeObj.getIntValue("state")) {
				boolean redisset=redisUtil.set(RedisKey.phone_code_prefix+mobile, code, NormalProperties.MAIL_CODE_TIMEOUT);
			    //?????????????????????redis??????
				if (redisset) {
					resuRestResult.setState(200);
					resuRestResult.setMsg("???????????????????????????");
					return resuRestResult;
				}else {
					resuRestResult.setState(500);
					resuRestResult.setMsg("?????????????????????redis??????????????????");
					return resuRestResult;
				}
			}
			resuRestResult.setState(500);
			resuRestResult.setMsg(codeObj.getString("msg"));
			return resuRestResult;
		}
		resuRestResult.setState(500);
		resuRestResult.setMsg(body);
		return resuRestResult;
	}


	public JSONArray getBatchInfo(List<ProductAndBatchGetCodeMO>productAndBatchGetCodeMOs,String superToken,String url) throws SuperCodeException {
		String jsonData=JSONObject.toJSONString(productAndBatchGetCodeMOs);
		Map<String,String> headerMap=new HashMap<String, String>();
		headerMap.put("super-token", superToken);
		ResponseEntity<String>  response=restTemplateUtil.postJsonDataAndReturnJosn(codeManagerUrl+url, jsonData, headerMap);
		log.info("???????????????????????????????????????:"+response.toString());
		String batchInfoBody=response.getBody();
		JSONObject obj=JSONObject.parseObject(batchInfoBody);
		int batchInfostate=obj.getInteger("state");
		if (200!=batchInfostate) {
			throw new SuperCodeException("????????????????????????????????????????????????????????????"+batchInfoBody, 500);
		}
		JSONArray array=obj.getJSONArray("results");
		if (null==array || array.isEmpty()) {
			throw new SuperCodeException("????????????????????????????????????????????????????????????????????????????????????????????????", 500);
		}
		Map<String, JSONObject> map = new HashMap<>();
		for(int i=0;i<array.size();i++) {
			JSONObject batchobj = array.getJSONObject(i);
			String productId = batchobj.getString("productId");
			String productBatchId = batchobj.getString("productBatchId");
			String codeBatch = batchobj.getString("globalBatchId");
			if (StringUtils.isBlank(codeBatch) || "null".equalsIgnoreCase(codeBatch)) {
				continue;
			}
			String key = productId + "," + productBatchId + "," + codeBatch;
			if (map.get(key) == null) {
				map.put(key, batchobj);
			}
		}
		return new JSONArray(new ArrayList<>(map.values()));
	}
    /**
     * ?????????????????????url???????????????
     * @param clientRole???0???????????????1????????????
     * @param url
     * @return
     * @throws SuperCodeException
     */
	public List<Map<String, Object>> getUrlToBatchParam(JSONArray array,String url,int businessType, Integer clientRole) throws SuperCodeException {
		List<Map<String, Object>> bindBatchList=new ArrayList<Map<String,Object>>();
		for(int i=0;i<array.size();i++) {
			JSONObject batchobj=array.getJSONObject(i);
			String productId=batchobj.getString("productId");
			String productBatchId=batchobj.getString("productBatchId");
			Long codeTotal=batchobj.getLong("codeTotal");
			String codeBatch=batchobj.getString("globalBatchId");
			if (StringUtils.isBlank(productId)||StringUtils.isBlank(productBatchId)||StringUtils.isBlank(codeBatch) || null==codeTotal) {
				throw new SuperCodeException("??????????????????????????????????????????????????????????????????????????????id??????????????????"+productId+","+productBatchId, 500);
			}
			Map<String, Object> batchMap=new HashMap<String, Object>();
			batchMap.put("batchId", codeBatch);
			batchMap.put("businessType", businessType);
			batchMap.put("url",  url);
			if (clientRole != null) {
				batchMap.put("clientRole",  clientRole);
			}
			bindBatchList.add(batchMap);
		}
		return bindBatchList;
	}

	public List<Map<String, Object>> getUrlToBatchParam(JSONArray array,String url,int businessType) throws SuperCodeException {
		return getUrlToBatchParam(array, url, businessType, null);
	}

	/**
	 * ??????????????????
	 * @param array
	 * @param url
	 * @param businessType
	 * @return
	 */
	public List<SbatchUrlDto> getUrlToBatchDto(JSONArray array, String url, int businessType, Integer clientRole) {
		List<SbatchUrlDto> bindBatchList=new ArrayList<>();
		for(int i=0;i<array.size();i++) {
			JSONObject batchobj=array.getJSONObject(i);
			String productId=batchobj.getString("productId");
			String productBatchId=batchobj.getString("productBatchId");
			Long codeTotal=batchobj.getLong("codeTotal");
			String codeBatch=batchobj.getString("globalBatchId");
			if (StringUtils.isBlank(productId)||StringUtils.isBlank(codeBatch) || null==codeTotal) {
				throw new SuperCodeExtException("??????????????????????????????????????????????????????????????????????????????id??????????????????"+productId+","+productBatchId, 500);
			}
			SbatchUrlDto batchUrlDto = new SbatchUrlDto();
			batchUrlDto.setBatchId(Long.valueOf(codeBatch));
			batchUrlDto.setBusinessType(businessType);
			batchUrlDto.setUrl(url);
			batchUrlDto.setProductId(productId);
			if (clientRole != null) {
				batchUrlDto.setClientRole(clientRole.toString());
			} else {
				batchUrlDto.setClientRole("0");
			}
			batchUrlDto.setProductBatchId(productBatchId);
			bindBatchList.add(batchUrlDto);
		}
		return bindBatchList;
	}

	public List<SbatchUrlDto> getUrlToBatchDto(JSONArray array, String url, int businessType) {
		return getUrlToBatchDto(array, url, businessType, null);
	}

    /**
     * ?????????????????????url???????????????
     * @param ????????????????????????????????????????????????????????????????????????
     * @param url
     * @return
     * @throws SuperCodeException
     */
	public Map<String, Map<String, Object>> getUrlToBatchParamMap(JSONArray array,String url,int businessType) throws SuperCodeException {
		Map<String, Map<String, Object>> bindBatchMap=new HashMap<>();
		for(int i=0;i<array.size();i++) {
			JSONObject batchobj=array.getJSONObject(i);
			String productId=batchobj.getString("productId");
			String productBatchId=batchobj.getString("productBatchId");
			Long codeTotal=batchobj.getLong("codeTotal");
			String codeBatch=batchobj.getString("globalBatchId");
			if (StringUtils.isBlank(productId)||StringUtils.isBlank(codeBatch) || null==codeTotal) {
				throw new SuperCodeException("??????????????????????????????????????????????????????????????????????????????id??????????????????"+productId+","+productBatchId, 500);
			}
			String key = productId + "," + productBatchId;
			Map<String, Object> batchMap = bindBatchMap.get(key);
			if(batchMap == null){
				batchMap = new HashMap<String, Object>();
				batchMap.put("businessType", businessType);
				batchMap.put("url",  url);
			}
			String batchId = (String) batchMap.get("batchId");
			if(batchId == null) {
				batchMap.put("batchId", codeBatch);
			}
			if(batchId != null && codeBatch!= null && !batchId.contains(codeBatch)) {
				batchId = batchId + "," + codeBatch;
				batchMap.put("batchId", batchId);
			}
			bindBatchMap.put(key, batchMap);
		}
		return bindBatchMap;
	}

	/**
	 * ??????????????????url
	 * @param
	 * @param superToken
	 * @return
	 * @throws SuperCodeException
	 */
	public String bindUrlToBatch(List<Map<String, Object>> bindBatchList,String superToken) throws SuperCodeException {
		//???????????????url??????
		String bindJson=JSONObject.toJSONString(bindBatchList);
		Map<String,String> headerMap=new HashMap<String, String>();
		headerMap.put("super-token", superToken);
		ResponseEntity<String>  bindBatchresponse=restTemplateUtil.postJsonDataAndReturnJosn(codeManagerUrl+WechatConstants.CODEMANAGER_BIND_BATCH_TO_URL, bindJson, headerMap);
		log.info("??????????????????????????????url????????????:"+bindBatchresponse.toString());
		String body=bindBatchresponse.getBody();
		return body;
	}

	/**
	 * ????????????????????????url
	 * @param
	 * @param superToken
	 * @return
	 * @throws SuperCodeException
	 */
	public String deleteUrlToBatch(List<Map<String, Object>> deleteBatchList,String superToken) throws SuperCodeException {
		//???????????????url??????
		String deleteJson=JSONObject.toJSONString(deleteBatchList);
		Map<String,String> headerMap=new HashMap<String, String>();
		headerMap.put("super-token", superToken);
		ResponseEntity<String>  bindBatchresponse=restTemplateUtil.postJsonDataAndReturnJosn(codeManagerUrl+WechatConstants.CODEMANAGER_DELETE_BATCH_TO_URL, deleteJson, headerMap);
		log.info("??????????????????????????????url????????????:"+bindBatchresponse.toString());
		String body=bindBatchresponse.getBody();
		return body;
	}

    /**
     * ?????????????????????????????????????????????
     * @param productIds
     * @param superToken
     * @return
     * @throws SuperCodeException
     */
	public JSONArray requestPriductBatchIds(List<String> productIds, String superToken)
			throws SuperCodeException {

		if (null==productIds || productIds.isEmpty()) {
			throw new SuperCodeException("??????????????????????????????????????????????????????id??????????????????", 500);
		}
		Map<String,String>headerMap=new HashMap<String, String>();
		headerMap.put("super-token", superToken);
		headerMap.put(commonUtil.getSysAuthHeaderKey(), commonUtil.getSecretKeyForBaseInfo());

		Map<String, Object>params=new HashMap<String, Object>();
		StringBuffer buf=new StringBuffer();
		for (String productId : productIds) {
			buf.append(productId).append(",");
		}

		params.put("productIds",String.join(",", productIds));
		
		ResponseEntity<String> response=restTemplateUtil.getRequestAndReturnJosn(restUserUrl+CommonConstants.USER_REQUEST_PRODUCT_BATCH, params, headerMap);
		log.info("???????????????????????????????????????????????????????????????"+response.toString());
		String body=response.getBody();
		JSONObject jsonObject=JSONObject.parseObject(body);
		Integer state=jsonObject.getInteger("state");
		if (null==state || state.intValue()!=200) {
			throw new SuperCodeException("????????????????????????????????????????????????:"+body, 500);
		}
		return jsonObject.getJSONArray("results");
	}


	/**
	 * ??????????????????????????????????????????
	 * @return
	 * @throws SuperCodeException
	 */
	public JSONArray getOrgsInfoByOrgIds(List<String> orgIds) throws SuperCodeException {
		Map<String, Object> params=new HashMap<String, Object>();
		params.put("organizationIds", JSONObject.toJSONString(orgIds));
		Map<String,String>headerMap=new HashMap<String, String>();
		headerMap.put(commonUtil.getSysAuthHeaderKey(), commonUtil.getSecretKeyForBaseInfo());
//		HashMap<String, String> superToken = new HashMap<>();
//		superToken.put("super-token",commonUtil.getSuperToken());
		ResponseEntity<String>responseEntity=restTemplateUtil.getRequestAndReturnJosn(restUserUrl+CommonConstants.USER_REQUEST_ORGANIZATION_BATCH, params, null);
		String body=responseEntity.getBody();
		log.info("???????????????????????????????????????????????????????????????"+body);

		JSONObject jsonBody=JSONObject.parseObject(body);
		Integer state=jsonBody.getInteger("state");
		if (null==state || state.intValue()!=200) {
			throw new SuperCodeException("????????????????????????????????????????????????:"+body, 500);
		}
		JSONArray arr=jsonBody.getJSONArray("results");
		if (null==arr || arr.size()==0) {
			throw new SuperCodeException("????????????id??????????????????????????????????????????????????????:", 500);
		}
		return arr;
	}


    /**
     * ????????????id??????????????????
     * @param organizationId
     * @return
     * @throws SuperCodeException
     */
	public String getOrgNameByOrgId(String organizationId) throws SuperCodeException {
		if (StringUtils.isBlank(organizationId)) {
			throw new SuperCodeException("????????????id???????????????????????????id????????????", 500);
		}
		String organizationName= (String) redisUtil.hmGet(RedisKey.organizationId_prefix, organizationId);
		if (null==organizationName) {
			List<String> orgIds=new ArrayList<String>();
			orgIds.add(organizationId);
			JSONArray arr=getOrgsInfoByOrgIds(orgIds);
			organizationName=arr.getJSONObject(0).getString("organizationFullName");
			redisUtil.hmSet(RedisKey.organizationId_prefix, organizationId, organizationName);

			Long seconds=redisUtil.leftExpireSeconds(RedisKey.organizationId_prefix);

			if (null==seconds) {
				redisUtil.expire(RedisKey.organizationId_prefix, 7200, TimeUnit.SECONDS);
			}
		}
		return organizationName;
	}


	public String getTicketByAccessToken(String appId,String secret,String organizationId) throws Exception {
		if (StringUtils.isBlank(appId) || StringUtils.isBlank(secret)|| StringUtils.isBlank(organizationId)) {
			throw new SuperCodeException("??????access_tokens?????????????????????", 500);
		}
		String key = RedisKey.WECHAT_TICKET_PREFIX + organizationId;
		String ticket = redisUtil.get(key);
		if (StringUtils.isNotBlank(ticket)) {
			return ticket;
		}
		String accessToken = getAccessTokenByOrgId(appId, secret, organizationId);
		HttpClientResult result=HttpRequestUtil.doGet("https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token="+accessToken+"&type=jsapi");
		String tickContent=result.getContent();
		log.info("?????????tick????????????"+tickContent);
		if (!tickContent.contains("ticket")) {
			throw new BizRuntimeException("??????tiket??????");
		}
		JSONObject tiketJson = JSON.parseObject(tickContent);
		long expiresIn = tiketJson.getLongValue("expires_in") - 20;
		ticket = tiketJson.getString("ticket");
		redisUtil.set(key, ticket, expiresIn);
		return ticket;
	}


    /**
     * ??????access_token
     * @param organizationId
     * @return
     * @throws Exception
     */
	public String getAccessTokenByOrgId(String appId,String secret,String organizationId) throws Exception {
		if (StringUtils.isBlank(appId) || StringUtils.isBlank(secret)|| StringUtils.isBlank(organizationId)) {
			throw new SuperCodeException("??????access_tokens?????????????????????", 500);
		}
		String key=RedisKey.ACCESS_TOKEN_prefix+organizationId;
		String accesstoken=redisUtil.get(key);
		if (StringUtils.isNotBlank(accesstoken)) {
			return accesstoken;
		}else {
			String access_token_url ="https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="+appId+"&secret="+secret;
			HttpClientResult reHttpClientResult=HttpRequestUtil.doGet(access_token_url);
			String body=reHttpClientResult.getContent();
			log.info("????????????????????????token??????;"+body);
			if (body.contains("access_token")) {
				JSONObject tokenObj=JSONObject.parseObject(body);
				String token=tokenObj.getString("access_token");
				long expiresIn = tokenObj.getLongValue("expires_in") - 20;
				redisUtil.set(key, token, expiresIn);
				return token;
			}
			throw new SuperCodeException("????????????access_toke?????????"+body, 500);
		}
	}

	/**
	 * ???????????????????????????
	 * @param codeId
	 * @param codeTypeId
	 * @return
	 * @throws SuperCodeException
	 */
	public String checkCodeValid(String codeId, String codeTypeId) {
		Map<String, String> headerparams = new HashMap<String, String>();
		headerparams.put("token",commonUtil.getCodePlatformToken() );
		log.info("????????????????????????????????????????????????outerCodeId{}???codeTypeId{}???",codeId,codeTypeId);

		ResponseEntity<String>responseEntity=restTemplateUtil.getRequestAndReturnJosn(msCodeUrl + "/outer/info/one?outerCodeId="+codeId+"&codeTypeId="+codeTypeId, null, headerparams);
		log.info("?????????????????????????????????????????????"+responseEntity.toString());
		if(responseEntity.getStatusCode() != HttpStatus.OK ){
			throw new BizRuntimeException("??????????????????????????????????????????????????????");
		}

		String codeBody=responseEntity.getBody();
		JSONObject jsonCodeBody=JSONObject.parseObject(codeBody);
		JSONObject results = jsonCodeBody.getJSONObject("results");
		if(results == null){
			throw new BizRuntimeException("?????????????????????????????????????????????");
		}
		String sBatchId=results.getString("sbatchId");
		if (StringUtils.isBlank(sBatchId)) {
			throw  new SuperCodeExtException("?????????,???????????????",500);
		}
		return sBatchId;
	}



	/**
	 * ???????????????????????????
	 * @param codeTypeId
	 * @return
	 * @throws SuperCodeException
	 */
	public void checkCodeTypeValid(Long codeTypeId) {
		if(codeTypeId == null){
			throw  new SuperCodeExtException("?????????,???????????????");
		}
		if (SystemLabelEnum.MARKETING_12.getCodeTypeId().intValue() != codeTypeId.intValue() && SystemLabelEnum.MARKETING_13.getCodeTypeId().intValue() != codeTypeId.intValue()) {
            throw  new SuperCodeExtException("?????????,???????????????");
		}
	}

	/**
	 * ???????????????????????????????????????
	 * @param codeTypeId
	 * @return
	 * @throws SuperCodeException
	 */
	public void checkCodeMarketFakeValid(Long codeTypeId) {
		if(codeTypeId == null){
			throw  new SuperCodeExtException("????????????????????????????????????????????????");
		}
		if (SystemLabelEnum.MARKETING_12.getCodeTypeId().intValue() != codeTypeId.intValue()&& SystemLabelEnum.MARKETING_13.getCodeTypeId().intValue() != codeTypeId.intValue() && SystemLabelEnum.FAKE.getCodeTypeId().intValue() != codeTypeId.intValue()) {
			throw  new SuperCodeExtException("????????????????????????????????????????????????");
		}
	}

	public boolean generateQR(String content, HttpServletResponse response) throws WriterException, IOException {
		//????????????????????????????????????
        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);  // ????????????
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        //??????????????????(?????????)???QR?????????????????????
        StringBuilder sb = new StringBuilder();
        sb.append(content);
        BitMatrix byteMatrix = qrCodeWriter.encode(sb.toString(), BarcodeFormat.QR_CODE, 1600, 1600, hintMap);
        // ???BufferedImage??????QRCode  (matrixWidth ????????????????????????)
        int matrixWidth = byteMatrix.getWidth();
        BufferedImage image = new BufferedImage(matrixWidth-200, matrixWidth-200, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, matrixWidth, matrixWidth);
        // ????????????????????????????????????
        graphics.setColor(Color.BLACK);
        for (int i = 0; i < matrixWidth; i++){
            for (int j = 0; j < matrixWidth; j++){
                if (byteMatrix.get(i, j)){
                    graphics.fillRect(i-100, j-100, 1, 1);
                }
            }
        }
        return ImageIO.write(image, "JPEG", response.getOutputStream());
	}
	/**
	 * ???????????????
	 * @param mobile ?????????
	 * @param verificationCode ?????????
	 * @return
	 */
	public boolean validateMobileCode(String mobile,String verificationCode){
		String redisPhoneCode=redisUtil.get(RedisKey.phone_code_prefix+ mobile);
		if (StringUtils.isBlank(redisPhoneCode) ) {
			return false;
		}
		if (!redisPhoneCode.equals(verificationCode)) {
			return false;
		}
		return true;

	}

    public void indexScanInfo(MarketingMemberAndScanCodeInfoParam infoParam) throws SuperCodeException{
		if(infoParam.getMemberType() == null ){
			log.warn("??????MemberType?????????{}",JSONObject.toJSONString(infoParam));
			throw new SuperCodeException("??????MemberType?????????");
		}
		JSONObject.toJSONString(infoParam);
		// ????????????????????????
		try {
			eClient.prepareIndex(EsIndex.MARKET_SCAN_INFO.getIndex(), EsType.INFO.getType())
					.setSource(JSONObject.toJSONString(infoParam), XContentType.JSON).get();
		} catch (Exception e) {
			e.printStackTrace();
			throw new SuperCodeException("index ??????????????????");
		}
	}
    
    /**
     * 
     * @param outerCodeId
     * @param
     * @return
     * @throws SuperCodeException
     */
    public Map<String, String> queryCurrentCustomer(String codeTypeId, String outerCodeId) {
		Map<String, String> customerMap = new HashMap<>();
    	List<String> logisticsList = getLogisticsCodeIds(codeTypeId, outerCodeId);
		if (CollectionUtils.isEmpty(logisticsList)) {
			return customerMap;
		}
		StringBuffer paramBuff = new StringBuffer();
		logisticsList.forEach(code -> paramBuff.append("&outerCodeIds=").append(code));
    	ResponseEntity<String> responseEntity = restTemplateUtil.getRequestAndReturnJosn(logisticsUrl+CommonConstants.OUTERCODE_CUSTOMER + "?" +paramBuff.substring(1), null, null);
    	String body = responseEntity.getBody();
		JSONObject jsonObject=JSONObject.parseObject(body);
		Integer state=jsonObject.getInteger("state");
		if (null == state || state.intValue()!=200) {
			throw new SuperCodeExtException("???????????????????????????:"+body, 500);
		}
		JSONObject resultJson = jsonObject.getJSONObject("results");
		if(MapUtils.isNotEmpty(resultJson)) {
			String customerId = resultJson.getString("customerId");
			String customerName = resultJson.getString("customerName");
			customerMap.put("customerId", customerId);
			customerMap.put("customerName", customerName);
			return customerMap;
		}
    	return customerMap;
    }

	/**
	 * ??????????????????????????????
	 * @param
	 * @param codeTypeId
	 * @return ??????
	 */
    public String getInnerCode(String codeId, String codeTypeId) {
		Map<String, Object> params = new HashMap<>();
		params.put("outerCodeId", codeId);
		params.put("codeTypeId", codeTypeId);
		ResponseEntity<String> responseEntity = restTemplateUtil.getRequestAndReturnJosn(msCodeUrl+CommonConstants.OUTER_INFO_ONE, params, null);
		String body = responseEntity.getBody();
		JSONObject jsonObject=JSONObject.parseObject(body);
		Integer state=jsonObject.getInteger("state");
		if (null == state || state.intValue()!=200) {
			throw new SuperCodeExtException("????????????????????????:"+body, 500);
		}
		JSONObject resultJson = jsonObject.getJSONObject("results");
		if(resultJson != null) {
			return resultJson.getString("innerCodeId");
		}
		throw new SuperCodeExtException("???????????????????????????");
	}

	private List<String> getLogisticsCodeIds(String codeTypeId, String outerCodeId){
		//String param = "codeTypeId="+codeTypeId+"&outerCodeIdList=[\""+outerCodeId+"\"]";
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("codeTypeId", codeTypeId);
		paramMap.put("outerCodeIdList", "[\""+outerCodeId+"\"]");
		ResponseEntity<String> responseEntity = restTemplateUtil.getRequestAndReturnJosn(logisticsUrl + CommonConstants.OUTER_LIST, paramMap, null);
		String body = responseEntity.getBody();
		JsonResult<?> outerCodeEntityJson = JSON.parseObject(body, JsonResult.class);
		Assert.isTrue(outerCodeEntityJson != null && outerCodeEntityJson.getResults() != null, "??????????????????????????????");
		Object resObj = outerCodeEntityJson.getResults();
		String resStr;
		if(resObj instanceof String) {
            resStr = (String)resObj;
        } else {
            resStr = JSON.toJSONString(resObj);
        }
		OuterCodesEntity outerCodesEntity = JSON.parseObject(resStr, OuterCodesEntity.class);
		List<List<OuterCode>> outerCodeListList = outerCodesEntity.getOuterCodeIdList();
		Assert.notEmpty(outerCodeListList, "??????????????????????????????");
		String logisticsCodeId = null, sequenceCodeId = null;
		for(List<OuterCode> outerCodeList : outerCodeListList) {
			for(OuterCode outerCode : outerCodeList) {
				if(CodeTypeEnum.LOGISTICS.getTypeId().equals(outerCode.getCodeTypeId())) {
                    logisticsCodeId = outerCode.getOutCodeId();
                }
				if(CodeTypeEnum.SEQUENCE.getTypeId().equals(outerCode.getCodeTypeId())) {
                    sequenceCodeId = outerCode.getOutCodeId();
                }
			}
		}
		Assert.isTrue(StringUtils.isNotBlank(logisticsCodeId) || StringUtils.isNotBlank(sequenceCodeId), "?????????????????????????????????????????????????????????");
		List<String> logisticsList = new ArrayList<>();
		if(StringUtils.isNotBlank(logisticsCodeId)) {
            logisticsList.add(logisticsCodeId);
        }
		if(StringUtils.isNotBlank(sequenceCodeId)) {
            logisticsList.add(sequenceCodeId);
        }
		return logisticsList;
	}
}
