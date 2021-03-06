package com.jgw.supercodeplatform.marketing.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.exception.SuperCodeExtException;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.LotteryResultMO;
import com.jgw.supercodeplatform.marketing.common.model.activity.MarketingPrizeTypeMO;
import com.jgw.supercodeplatform.marketing.common.model.activity.ScanCodeInfoMO;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.DateUtil;
import com.jgw.supercodeplatform.marketing.common.util.LotteryUtilWithOutCodeNum;
import com.jgw.supercodeplatform.marketing.common.util.RestTemplateUtil;
import com.jgw.supercodeplatform.marketing.config.redis.RedisLockUtil;
import com.jgw.supercodeplatform.marketing.config.redis.RedisUtil;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.dao.activity.*;
import com.jgw.supercodeplatform.marketing.dao.user.MarketingMembersMapper;
import com.jgw.supercodeplatform.marketing.dao.weixin.MarketingWxMerchantsMapper;
import com.jgw.supercodeplatform.marketing.dao.weixin.WXPayTradeOrderMapper;
import com.jgw.supercodeplatform.marketing.dto.activity.LotteryOprationDto;
import com.jgw.supercodeplatform.marketing.dto.platform.ProductInfoDto;
import com.jgw.supercodeplatform.marketing.enums.market.ReferenceRoleEnum;
import com.jgw.supercodeplatform.marketing.pojo.*;
import com.jgw.supercodeplatform.marketing.pojo.pay.WXPayTradeOrder;
import com.jgw.supercodeplatform.marketing.pojo.platform.MarketingPlatformOrganization;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import com.jgw.supercodeplatform.marketing.service.es.activity.CodeEsService;
import com.jgw.supercodeplatform.marketing.service.weixin.MarketingWxMerchantsService;
import com.jgw.supercodeplatform.marketing.service.weixin.WXPayService;
import com.jgw.supercodeplatform.marketing.weixinpay.WXPayTradeNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PlatformLotteryService {
    @Autowired
    private MarketingPrizeTypeMapper mPrizeTypeMapper;
    @Autowired
    private MarketingActivitySetMapper mSetMapper;
    @Autowired
    private MarketingMembersMapper marketingMembersMapper;
    @Autowired
    private MarketingActivityMapper mActivityMapper;
    @Autowired
    private MarketingPrizeTypeMapper marketingPrizeTypeMapper;
    @Autowired
    private MarketingPlatformOrganizationMapper marketingPlatformOrganizationMapper;
    @Autowired
    private CodeEsService codeEsService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RedisLockUtil lock;
    @Autowired
    private WXPayTradeNoGenerator wXPayTradeNoGenerator;
    @Value("${marketing.server.ip}")
    private String serverIp;
    @Autowired
    private WXPayService wxpService;
    @Autowired
    private WXPayTradeOrderMapper wXPayTradeOrderMapper;
    @Autowired
    private MarketingWxMerchantsMapper marketingWxMerchantsMapper;
    @Autowired
    private MarketingMembersWinRecordMapper mWinRecordMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RestTemplateUtil restTemplateUtil;
    @Value("${rest.user.url}")
    private String restUserUrl;

    public LotteryOprationDto checkLotteryCondition(LotteryOprationDto lotteryOprationDto, ScanCodeInfoMO scanCodeInfoMO) throws ParseException, SuperCodeException {
        String productName = scanCodeInfoMO.getProductBatchId();
        Long activitySetId = scanCodeInfoMO.getActivitySetId();
        MarketingActivitySet mActivitySet = mSetMapper.selectById(activitySetId);
        if (null == mActivitySet) {
            throw new SuperCodeExtException("????????????????????????", 500);
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
        MarketingActivity activity = mActivityMapper.selectById(mActivitySet.getActivityId());
        if (null == activity) {
            throw new SuperCodeExtException("??????????????????", 500);
        }
        Long userId = scanCodeInfoMO.getUserId();
        List<MarketingPrizeTypeMO> moPrizeTypes = marketingPrizeTypeMapper.selectMOByActivitySetIdIncludeUnreal(activitySetId);
        if (moPrizeTypes == null || moPrizeTypes.size() <= 1) {
            return lotteryOprationDto.lotterySuccess("??????????????????????????????");
        }
        lotteryOprationDto.setSendAudit(mActivitySet.getSendAudit());
        lotteryOprationDto.setOrganizationId(scanCodeInfoMO.getOrganizationId());
        MarketingPlatformOrganization marketingPlatformOrganization = marketingPlatformOrganizationMapper.selectByActivitySetIdAndOrganizationId(mActivitySet.getId(), scanCodeInfoMO.getOrganizationId());
        if (marketingPlatformOrganization == null) {
            throw new SuperCodeExtException("????????????????????????????????????????????????");
        }
        lotteryOprationDto.setOrganizationName(marketingPlatformOrganization.getOrganizationFullName());
        lotteryOprationDto.setScanCodeInfoMO(scanCodeInfoMO);
        lotteryOprationDto.setMarketingActivity(activity);
        lotteryOprationDto.setProductName(productName);
        String conditon = mActivitySet.getValidCondition();
        if (StringUtils.isNotBlank(conditon)) {
            JSONObject conditionJson = JSON.parseObject(conditon);
            Integer maxJoinNum = conditionJson.getInteger("maxJoinNum");
            if (maxJoinNum == null) {
                lotteryOprationDto.setEachDayNumber(0);
            } else {
                lotteryOprationDto.setEachDayNumber(maxJoinNum);
            }
            lotteryOprationDto.setSourceLink(conditionJson.getString("sourceLink"));
        }
        MarketingPrizeTypeMO prizeTypeMo = getPrizeMo(moPrizeTypes, activitySetId, scanCodeInfoMO.getOpenId());
        lotteryOprationDto.setPrizeTypeMO(prizeTypeMo);
        //??????????????????
        return lotteryOprationDto;
    }


    public LotteryOprationDto holdLockJudgeES(LotteryOprationDto lotteryOprationDto) {
        ScanCodeInfoMO scanCodeInfoMO = lotteryOprationDto.getScanCodeInfoMO();
        MarketingPrizeTypeMO prizeTypeMO = lotteryOprationDto.getPrizeTypeMO();
        Float amount = prizeTypeMO.getPrizeAmount();
        String innerCode = lotteryOprationDto.getInnerCode();
        //????????????????????????????????????????????????
        Integer maxScanNumber = lotteryOprationDto.getEachDayNumber();
        String organizationId = lotteryOprationDto.getOrganizationId();
        long activitySetId = scanCodeInfoMO.getActivitySetId();
        String codeId = scanCodeInfoMO.getCodeId();
        String codeTypeId = scanCodeInfoMO.getCodeTypeId();
        String openId = scanCodeInfoMO.getOpenId();
        Long userId = scanCodeInfoMO.getUserId();
        String productId = scanCodeInfoMO.getProductId();
        String productBatchId = scanCodeInfoMO.getProductBatchId();
        String organizationName = lotteryOprationDto.getOrganizationName();
        ProductInfoDto productInfoDto = getProductInfo(productId);
        if (productInfoDto == null) {
            productInfoDto = new ProductInfoDto();
        }
        boolean acquireLock =false;
        String lockKey = activitySetId + ":" + codeId + ":" + codeTypeId;
        try {
            acquireLock = lock.lock(lockKey, 5000, 5, 200);
            if(!acquireLock) {
                log.error("{???????????????:" +lockKey+ ",?????????}");
                redisUtil.hmSet("marketing:lock:fail",lockKey,new Date());
                return lotteryOprationDto.lotterySuccess("??????????????????,???????????????");
            }
            String opneIdNoSpecialChactar = StringUtils.isBlank(openId)? null: CommonUtil.replaceSpicialChactar(openId);
            long codeCount = codeEsService.countPlatformScanCodeRecord(innerCode, null);
            if (codeCount > 0) {
                return lotteryOprationDto.lotterySuccess("????????????????????????????????????????????????");
            }
            long nowTimeMills = System.currentTimeMillis();
            //????????????????????????????????????????????????
            if (maxScanNumber != null && maxScanNumber > 0) {
                long userscanNum = codeEsService.countByUserAndActivityPlatform(opneIdNoSpecialChactar, activitySetId);
                log.info("????????????=====?????????openId="+opneIdNoSpecialChactar+",activitySetId="+activitySetId+"????????????????????????????????????="+userscanNum+",????????????????????????????????????"+maxScanNumber);
                if (userscanNum >= maxScanNumber) {
                    return lotteryOprationDto.lotterySuccess("???????????????????????????????????????");
                }
            }
            codeEsService.addPlatformScanCodeRecord(productInfoDto, innerCode, productId, productBatchId, codeId, opneIdNoSpecialChactar,userId,0, 5L, codeTypeId,activitySetId, nowTimeMills,organizationId,organizationName,amount);
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

    @Transactional(rollbackFor = Exception.class)
    public WXPayTradeOrder saveLottory(LotteryOprationDto lotteryOprationDto, String remoteAddr) throws Exception {
        String winningCode = lotteryOprationDto.getScanCodeInfoMO().getCodeId();
        String openId = lotteryOprationDto.getScanCodeInfoMO().getOpenId();
        String mobile = lotteryOprationDto.getScanCodeInfoMO().getMobile();
        MarketingActivity marketingActivity = lotteryOprationDto.getMarketingActivity();
        String productId = lotteryOprationDto.getScanCodeInfoMO().getProductId();
        String productName = lotteryOprationDto.getScanCodeInfoMO().getProductName();
        String productBatchId = lotteryOprationDto.getScanCodeInfoMO().getProductBatchId();
        //?????????ID??????????????????ID
        String organizationId = marketingWxMerchantsMapper.getDefaultJgw().getOrganizationId();
        MarketingPrizeTypeMO prizeTypeMo = lotteryOprationDto.getPrizeTypeMO();
        //????????????
        Float amount = prizeTypeMo.getPrizeAmount();
        //???????????????
        String partner_trade_no = prizeTypeMo.getAwardGrade().intValue() == 0? null:wXPayTradeNoGenerator.tradeNo();
        //??????????????????
        addWinRecord(winningCode, mobile, openId,productName,lotteryOprationDto.getScanCodeInfoMO().getActivitySetId(),prizeTypeMo.getAwardGrade(),partner_trade_no, marketingActivity, lotteryOprationDto.getOrganizationId(), lotteryOprationDto.getOrganizationName(), prizeTypeMo.getId(),amount,productId,productBatchId);
        //??????????????????????????????????????????
        if (prizeTypeMo.getAwardGrade().intValue() == 0) {
            return null;
        }
        if (prizeTypeMo.getRemainingStock() != null) {
            mPrizeTypeMapper.updateRemainingStock(prizeTypeMo.getId());
        }
        Float finalAmount = amount * 100;
        if (StringUtils.isBlank(openId)) {
            throw  new SuperCodeExtException("????????????openid????????????",200);
        }
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
        tradeOrder.setReferenceRole(ReferenceRoleEnum.ACTIVITY_MEMBER.getType());
        tradeOrder.setActivityId(marketingActivity.getId());
        if (StringUtils.isBlank(remoteAddr)) {
            remoteAddr = serverIp;
        }
        tradeOrder.setRemoteAddr(remoteAddr);
        wXPayTradeOrderMapper.insert(tradeOrder);
        //???????????????????????????????????????????????????????????????????????????
        return tradeOrder;
    }

    public RestResult<LotteryResultMO> saveOrder(WXPayTradeOrder tradeOrder, String sourceLink) throws Exception {
        String openId = tradeOrder.getOpenId();
        String remoteAddr = tradeOrder.getRemoteAddr();
        Float finalAmount = tradeOrder.getAmount();
        String partner_trade_no = tradeOrder.getPartnerTradeNo();
        String organizationId = tradeOrder.getOrganizationId();
        wxpService.qiyePay(openId, remoteAddr, finalAmount.intValue(), partner_trade_no, organizationId);
        String strAmount = String.format("%.2f", finalAmount/100);
        LotteryResultMO lotteryResultMO = new LotteryResultMO(1);
        lotteryResultMO.setData(sourceLink);
        lotteryResultMO.setMsg(strAmount);
        return RestResult.success(strAmount, lotteryResultMO);
    }

    private void addWinRecord(String outCodeId, String mobile, String openId, String productName,Long activitySetId, byte awardGrade, String tradeNo,
                              MarketingActivity activity, String organizationId,String organizationFullName, Long prizeTypeId, Float amount, String productId, String productBatchId) {
        //??????????????????
        MarketingMembersWinRecord redWinRecord=new MarketingMembersWinRecord();
        redWinRecord.setActivityId(activity.getId());
        redWinRecord.setActivityName(activity.getActivityName());
        redWinRecord.setActivitySetId(activitySetId);
        redWinRecord.setMobile(mobile);
        redWinRecord.setOpenid(openId);
        redWinRecord.setPrizeTypeId(prizeTypeId);
        redWinRecord.setWinningAmount(amount );
        redWinRecord.setProductId(productId);
        redWinRecord.setWinningCode(outCodeId);
        redWinRecord.setPrizeName(productName);
        redWinRecord.setOrganizationId(organizationId);
        redWinRecord.setOrganizationFullName(organizationFullName);
        redWinRecord.setProductBatchId(productBatchId);
        redWinRecord.setAwardGrade(awardGrade);
        redWinRecord.setTradeNo(tradeNo);
        mWinRecordMapper.addWinRecord(redWinRecord);
    }

    /**
     * ????????????
     * ???????????????????????????????????????????????????????????????1????????????????????????
     * @param prizeTypeMOList
     * @param activityId
     * @param openId
     * @return
     * @throws SuperCodeException
     */
    private synchronized MarketingPrizeTypeMO getPrizeMo(List<MarketingPrizeTypeMO> prizeTypeMOList, Long activityId, String openId) throws SuperCodeException {
        MarketingMembersWinRecord firstAwardWinRecord = mWinRecordMapper.getFirstAward(activityId, openId);
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        for (MarketingPrizeTypeMO prizeTypeMo : prizeTypeMOList) {
            if (prizeTypeMo.getAwardGrade().intValue() == 0 || prizeTypeMo.getRemainingStock() == null) {
                //???????????????????????????????????????null???????????????
                continue;
            }
            String key = prizeTypeMo.getId() + "_" + prizeTypeMo.getActivitySetId();
            //?????????????????????
            valueOperations.setIfAbsent(key, prizeTypeMo.getRemainingStock().toString());
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
            String stockNum = valueOperations.get(key);
            prizeTypeMo.setRemainingStock(Integer.valueOf(stockNum));
        }
        //??????????????????null??????????????????????????????????????????????????????????????????????????????int???????????????????????????????????????
        prizeTypeMOList.forEach(prize -> {
            if(prize.getRemainingStock() == null) {
                prize.setRemainingStock(Integer.MAX_VALUE);
            }
        });
        MarketingPrizeTypeMO prizeTypeMo = LotteryUtilWithOutCodeNum.platfromStartLottery(prizeTypeMOList, firstAwardWinRecord == null?false:true);
        if (prizeTypeMo.getAwardGrade().intValue() != 0 && prizeTypeMo.getRemainingStock().intValue() != Integer.MAX_VALUE) {
            valueOperations.increment(prizeTypeMo.getId() + "_" + prizeTypeMo.getActivitySetId(), -1L);
        }
        //????????????????????????????????????????????????????????????null
        prizeTypeMOList.forEach(prize -> {
            if(prize.getRemainingStock().intValue() == Integer.MAX_VALUE) {
                prize.setRemainingStock(null);
            }
        });
        if (prizeTypeMo.getRemainingStock() != null && prizeTypeMo.getRemainingStock().intValue() == Integer.MAX_VALUE) {
            prizeTypeMo.setRemainingStock(null);
        }
        return prizeTypeMo;
    }


    /**
     * ??????productId??????????????????
     * @param productId
     * @return
     */
    public ProductInfoDto getProductInfo(String productId) {
        try {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("productId", productId);
            ResponseEntity<String> responseEntity = restTemplateUtil.getRequestAndReturnJosn(restUserUrl + CommonConstants.PRODUCT_ONE, paramMap, null);
            String resBody = responseEntity.getBody();
            if (responseEntity.getStatusCode().equals(HttpStatus.OK) && org.apache.commons.lang3.StringUtils.isNotBlank(resBody)) {
                JSONObject resJson = JSON.parseObject(resBody);
                if (resJson.getIntValue("state") == HttpStatus.OK.value()) {
                    ProductInfoDto productInfoDto = resJson.getObject("results", ProductInfoDto.class);
                    return productInfoDto;
                }
            }
        } catch (Exception e) {
            log.error("???????????????????????????????????????", e);
        }
        return null;
    }

}
