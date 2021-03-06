package com.jgw.supercodeplatform.marketingsaler.integral.domain.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.config.redis.RedisLockUtil;
import com.jgw.supercodeplatform.marketing.enums.market.IntegralReasonEnum;
import com.jgw.supercodeplatform.marketing.exception.BizRuntimeException;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;
import com.jgw.supercodeplatform.marketingsaler.base.service.SalerCommonService;
import com.jgw.supercodeplatform.marketingsaler.common.UserConstants;
import com.jgw.supercodeplatform.marketingsaler.integral.application.group.OuterCodeInfoService;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.mapper.SalerRuleRewardMapper;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.pojo.SalerRecord;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.pojo.SalerRuleReward;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.pojo.SalerRuleRewardNum;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.pojo.User;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.transfer.H5SalerRuleRewardTransfer;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.transfer.SalerRecordTranser;
import com.jgw.supercodeplatform.marketingsaler.integral.interfaces.dto.OutCodeInfoDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
@Slf4j
@Service
public class H5SalerRuleRewardService  extends SalerCommonService<SalerRuleRewardMapper, SalerRuleReward> {
    @Autowired private UserService userService;
    @Autowired private RedisLockUtil lockUtil;
    @Autowired private OuterCodeInfoService outerCodeInfoService;
    @Autowired private SalerRuleRewardNumService salerRuleRewardNumService;
    @Autowired SalerRecordService salerRecordService;
    /**
     * h5?????????:
     * @param reward ?????????????????????
     * @param user
     */
    @Transactional
    public Integer getIntegral(String outCodeId,String codeTypeId,SalerRuleReward reward, H5LoginVO user)   {
        log.info("???????????? outCodeId{}",outCodeId);
        log.info("???????????? codeTypeId{}",codeTypeId);
        log.info("???????????? SalerRuleReward{}",reward);
        log.info("???????????? H5LoginVO{}", JSONObject.toJSONString(user));
        // ????????????
        try {
            boolean lock = lockUtil.lock(UserConstants.SALER_INTEGRAL_REWARD_PREFIX + outCodeId);
            if(!lock){
                throw new BizRuntimeException("??????,???????????????!???????????????...");
            }
            // ???????????????:?????????????????????????????????????????????????????????????????????????????????????????????
            try {
                RestResult<Long> currentLevel = outerCodeInfoService.getCurrentLevel(new OutCodeInfoDto(outCodeId, codeTypeId));
                Asserts.check(currentLevel!=null && currentLevel.getResults().intValue() == UserConstants.SINGLE_CODE.intValue(),"?????????");
            } catch (Exception e) {
                e.printStackTrace();
                throw new BizRuntimeException("?????????????????????????????????");
            }
            // ???????????????
            boolean exists = salerRuleRewardNumService.exists(outCodeId);
            if(exists){
                throw new BizRuntimeException("?????????????????????");
            }
            //
            // ??????????????????
            Asserts.check(user.getMemberId()!=null,"??????id?????????");
            Asserts.check(!StringUtils.isEmpty(user.getOrganizationId()),"?????????????????????");
            User userPojo = userService.exists(user);
            Asserts.check(userPojo != null,"??????????????????????????????");


            // ????????????????????????
            SalerRuleReward rewardPojo = baseMapper.selectOne(query().eq("ProductId", reward.getProductId()).eq("OrganizationId", user.getOrganizationId()).getWrapper());
            Asserts.check(rewardPojo != null,"?????????????????????????????????");


            // ??????????????????
            salerRuleRewardNumService.save(new SalerRuleRewardNum(null, user.getMemberId(), user.getOrganizationId(), outCodeId));

            // ????????????
            int realRewardIntegral = H5SalerRuleRewardTransfer.computeIntegral(rewardPojo);
            rewardPojo.setRewardIntegral(realRewardIntegral);

            SalerRecord salerRecord = SalerRecordTranser.getSalerRecord(outCodeId,codeTypeId, reward, user, userPojo, rewardPojo);
            salerRecordService.save(salerRecord);
            // ??????????????????
            userService.addIntegral(realRewardIntegral,userPojo);
            return realRewardIntegral;
        } catch (BizRuntimeException e) {
            e.printStackTrace();
            throw new BizRuntimeException(e.getMessage());
        } finally {
            lockUtil.releaseLock(UserConstants.SALER_INTEGRAL_REWARD_PREFIX + outCodeId);
        }

    }


}
