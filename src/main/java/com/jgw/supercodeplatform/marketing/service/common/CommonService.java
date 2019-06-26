package com.jgw.supercodeplatform.marketing.service.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.jgw.supercodeplatform.exception.SuperCodeException;
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
import com.jgw.supercodeplatform.marketing.dto.activity.MarketingMemberAndScanCodeInfoParam;
import com.jgw.supercodeplatform.marketing.enums.EsIndex;
import com.jgw.supercodeplatform.marketing.enums.EsType;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

@Service
public class CommonService {
	protected static Logger logger = LoggerFactory.getLogger(CommonService.class);
    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private RestTemplateUtil restTemplateUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Value("${rest.user.url}")
    private String restUserUrl;

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
			resuRestResult.setMsg("手机号不能为空");
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
		//如果HTTP响应成功
		if (status>=200 && status<300 ) {
			JSONObject codeObj=JSONObject.parseObject(body);
			boolean flag=codeObj.containsKey("state");
			//如果基础平台接口响应成功
			if (flag && 200==codeObj.getIntValue("state")) {
				boolean redisset=redisUtil.set(RedisKey.phone_code_prefix+mobile, code, NormalProperties.MAIL_CODE_TIMEOUT);
			    //如果验证码保存redis成功
				if (redisset) {
					resuRestResult.setState(200);
					resuRestResult.setMsg("手机验证码发送成功");
					return resuRestResult;
				}else {
					resuRestResult.setState(500);
					resuRestResult.setMsg("手机验证码设置redis失败，请重试");
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


	public String getBatchInfo(List<ProductAndBatchGetCodeMO>productAndBatchGetCodeMOs,String superToken,String url) throws SuperCodeException {
		String jsonData=JSONObject.toJSONString(productAndBatchGetCodeMOs);
		Map<String,String> headerMap=new HashMap<String, String>();
		headerMap.put("super-token", superToken);
		ResponseEntity<String>  response=restTemplateUtil.postJsonDataAndReturnJosn(codeManagerUrl+url, jsonData, headerMap);
		logger.info("请求码管理批次信息返回数据:"+response.toString());
		String body=response.getBody();
		return body;
	}
    /**
     * 获取绑定批次和url的请求参数
     * @param obj：通过产品和产品批次获取的码管理平台生码批次信息
     * @param url
     * @return
     * @throws SuperCodeException
     */
	public List<Map<String, Object>> getUrlToBatchParam(JSONArray array,String url,int businessType) throws SuperCodeException {
		List<Map<String, Object>> bindBatchList=new ArrayList<Map<String,Object>>();
		for(int i=0;i<array.size();i++) {
			JSONObject batchobj=array.getJSONObject(i);
			String productId=batchobj.getString("productId");
			String productBatchId=batchobj.getString("productBatchId");
			Long codeTotal=batchobj.getLong("codeTotal");
			String codeBatch=batchobj.getString("globalBatchId");
			if (StringUtils.isBlank(productId)||StringUtils.isBlank(productBatchId)||StringUtils.isBlank(codeBatch) || null==codeTotal) {
				throw new SuperCodeException("获取码管理批次信息返回数据不合法有参数为空，对应产品id及产品批次为"+productId+","+productBatchId, 500);
			}
			Map<String, Object> batchMap=new HashMap<String, Object>();
			batchMap.put("batchId", codeBatch);
			batchMap.put("businessType", businessType);
			batchMap.put("url",  url);
			bindBatchList.add(batchMap);
		}
		return bindBatchList;
	}

    /**
     * 获取绑定批次和url的请求参数
     * @param obj：通过产品和产品批次获取的码管理平台生码批次信息
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
			if (StringUtils.isBlank(productId)||StringUtils.isBlank(productBatchId)||StringUtils.isBlank(codeBatch) || null==codeTotal) {
				throw new SuperCodeException("获取码管理批次信息返回数据不合法有参数为空，对应产品id及产品批次为"+productId+","+productBatchId, 500);
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
	 * 生码批次绑定url
	 * @param url
	 * @param superToken
	 * @return
	 * @throws SuperCodeException
	 */
	public String bindUrlToBatch(List<Map<String, Object>> bindBatchList,String superToken) throws SuperCodeException {
		//生码批次跟url绑定
		String bindJson=JSONObject.toJSONString(bindBatchList);
		Map<String,String> headerMap=new HashMap<String, String>();
		headerMap.put("super-token", superToken);
		ResponseEntity<String>  bindBatchresponse=restTemplateUtil.postJsonDataAndReturnJosn(codeManagerUrl+WechatConstants.CODEMANAGER_BIND_BATCH_TO_URL, bindJson, headerMap);
		logger.info("请求码管理绑定批次与url返回数据:"+bindBatchresponse.toString());
		String body=bindBatchresponse.getBody();
		return body;
	}

	/**
	 * 删除生码批次绑定url
	 * @param url
	 * @param superToken
	 * @return
	 * @throws SuperCodeException
	 */
	public String deleteUrlToBatch(List<Map<String, Object>> deleteBatchList,String superToken) throws SuperCodeException {
		//生码批次跟url绑定
		String deleteJson=JSONObject.toJSONString(deleteBatchList);
		Map<String,String> headerMap=new HashMap<String, String>();
		headerMap.put("super-token", superToken);
		ResponseEntity<String>  bindBatchresponse=restTemplateUtil.postJsonDataAndReturnJosn(codeManagerUrl+WechatConstants.CODEMANAGER_DELETE_BATCH_TO_URL, deleteJson, headerMap);
		logger.info("请求码管理删除批次与url返回数据:"+bindBatchresponse.toString());
		String body=bindBatchresponse.getBody();
		return body;
	}

    /**
     * 根据产品集合获取产品和批次集合
     * @param productIds
     * @param superToken
     * @return
     * @throws SuperCodeException
     */
	public JSONArray requestPriductBatchIds(List<String> productIds, String superToken)
			throws SuperCodeException {

		if (null==productIds || productIds.isEmpty()) {
			throw new SuperCodeException("根据产品集合获取产品批次集合出错产品id集合不能为空", 500);
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
		logger.info("根据产品集合请求基础平台批次数据收到响应："+response.toString());
		String body=response.getBody();
		JSONObject jsonObject=JSONObject.parseObject(body);
		Integer state=jsonObject.getInteger("state");
		if (null==state || state.intValue()!=200) {
			throw new SuperCodeException("根据产品集合获取产品批次集合出错:"+body, 500);
		}
		return jsonObject.getJSONArray("results");
	}


	/**
	 * 请求基础平台批量获取组织信息
	 * @return
	 * @throws SuperCodeException
	 */
	public JSONArray getOrgsInfoByOrgIds(List<String> orgIds) throws SuperCodeException {
		Map<String, Object> params=new HashMap<String, Object>();
		params.put("organizationIds", JSONObject.toJSONString(orgIds));
		Map<String,String>headerMap=new HashMap<String, String>();
		headerMap.put(commonUtil.getSysAuthHeaderKey(), commonUtil.getSecretKeyForBaseInfo());
		HashMap<String, String> superToken = new HashMap<>();
		superToken.put("super-token",commonUtil.getSuperToken());
		ResponseEntity<String>responseEntity=restTemplateUtil.getRequestAndReturnJosn(restUserUrl+CommonConstants.USER_REQUEST_ORGANIZATION_BATCH, params, superToken);
		String body=responseEntity.getBody();
		logger.info("请求基础平台批量获取组织信息接口返回信息："+body);

		JSONObject jsonBody=JSONObject.parseObject(body);
		Integer state=jsonBody.getInteger("state");
		if (null==state || state.intValue()!=200) {
			throw new SuperCodeException("请求基础平台批量获取组织信息出错:"+body, 500);
		}
		JSONArray arr=jsonBody.getJSONArray("results");
		if (null==arr || arr.size()==0) {
			throw new SuperCodeException("根据组织id集合请求基础平台批量获取组织信息为空:", 500);
		}
		return arr;
	}


    /**
     * 根据组织id获取组织名称
     * @param organizationId
     * @return
     * @throws SuperCodeException
     */
	public String getOrgNameByOrgId(String organizationId) throws SuperCodeException {
		if (StringUtils.isBlank(organizationId)) {
			throw new SuperCodeException("根据组织id请求组织名称时组织id不能为空", 500);
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


    /**
     * 获取access_token
     * @param organizationId
     * @return
     * @throws Exception
     */
	public String getAccessTokenByOrgId(String appId,String secret,String organizationId) throws Exception {
		if (StringUtils.isBlank(appId) || StringUtils.isBlank(secret)|| StringUtils.isBlank(organizationId)) {
			throw new SuperCodeException("获取access_tokens的参数不能为空", 500);
		}
		String key=RedisKey.ACCESS_TOKEN_prefix+organizationId;
		String accesstoken=redisUtil.get(key);
		if (StringUtils.isNotBlank(accesstoken)) {
			return accesstoken;
		}else {
			String access_token_url ="https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="+appId+"&secret="+secret;
			HttpClientResult reHttpClientResult=HttpRequestUtil.doGet(access_token_url);
			String body=reHttpClientResult.getContent();
			logger.info("请求获取用户信息token返回;"+body);
			if (body.contains("access_token")) {
				JSONObject tokenObj=JSONObject.parseObject(body);
				String token=tokenObj.getString("access_token");
				// TODO 影响调试
//				redisUtil.set(key, token);
//				redisUtil.expire(key, 5400, TimeUnit.SECONDS);
				return token;
			}
			throw new SuperCodeException("获取微信access_toke失败："+body, 500);
		}
	}

	/**
	 * 校验抽奖码是否存在
	 * @param codeId
	 * @param codeTypeId
	 * @return
	 * @throws SuperCodeException
	 */
	public void checkCodeValid(String codeId, String codeTypeId) throws SuperCodeException {
		Map<String, String>headerparams=new HashMap<String, String>();
		headerparams.put("token",commonUtil.getCodePlatformToken() );
		ResponseEntity<String>responseEntity=restTemplateUtil.getRequestAndReturnJosn(msCodeUrl + "/outer/info/one?outerCodeId="+codeId+"&codeTypeId="+codeTypeId, null, headerparams);
		logger.info("根据码和码制获取码平台码信息："+responseEntity.toString());
		String codeBody=responseEntity.getBody();
		JSONObject jsonCodeBody=JSONObject.parseObject(codeBody);
		String sBatchId=jsonCodeBody.getJSONObject("results").getString("sBatchId");
		if (StringUtils.isBlank(sBatchId)) {
			throw  new SuperCodeException("对不起,该码不存在",500);
		}
	}



	/**
	 * 校验是否为营销码制
	 * @param codeTypeId
	 * @return
	 * @throws SuperCodeException
	 */
	public void checkCodeTypeValid( Long codeTypeId) throws SuperCodeException {
		if(codeTypeId == null){
			throw  new SuperCodeException("对不起,非营销码制");
		}
		if (SystemLabelEnum.MARKETING.getCodeTypeId().intValue() != codeTypeId.intValue()) {
            throw  new SuperCodeException("对不起,非营销码制");
		}
	}



	public boolean generateQR(String content, HttpServletResponse response) throws WriterException, IOException {
		//设置二维码纠错级别ＭＡＰ
        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);  // 矫错级别
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        //创建比特矩阵(位矩阵)的QR码编码的字符串
        StringBuilder sb = new StringBuilder();
        sb.append(content);
        BitMatrix byteMatrix = qrCodeWriter.encode(sb.toString(), BarcodeFormat.QR_CODE, 1600, 1600, hintMap);
        // 使BufferedImage勾画QRCode  (matrixWidth 是行二维码像素点)
        int matrixWidth = byteMatrix.getWidth();
        BufferedImage image = new BufferedImage(matrixWidth-200, matrixWidth-200, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, matrixWidth, matrixWidth);
        // 使用比特矩阵画并保存图像
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
	 * 验证码校验
	 * @param mobile 手机号
	 * @param verificationCode 验证码
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
			logger.error("扫码MemberType不存在{}",JSONObject.toJSONString(infoParam));
			throw new SuperCodeException("扫码MemberType不存在");
		}
		JSONObject.toJSONString(infoParam);
		// 保存用户产品信息
		try {
			eClient.prepareIndex(EsIndex.MARKET_SCAN_INFO.getIndex(), EsType.INFO.getType())
					.setSource(JSONObject.toJSONString(infoParam), XContentType.JSON).get();
		} catch (Exception e) {
			e.printStackTrace();
			throw new SuperCodeException("index 扫码信息失败");
		}
	}
}
