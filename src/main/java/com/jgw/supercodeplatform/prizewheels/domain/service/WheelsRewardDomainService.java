package com.jgw.supercodeplatform.prizewheels.domain.service;

import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;
import com.jgw.supercodeplatform.prizewheels.domain.constants.RewardTypeConstant;
import com.jgw.supercodeplatform.prizewheels.domain.event.CdkEvent;
import com.jgw.supercodeplatform.prizewheels.domain.model.WheelsRecord;
import com.jgw.supercodeplatform.prizewheels.domain.model.WheelsReward;
import com.jgw.supercodeplatform.prizewheels.domain.model.WheelsRewardCdk;
import com.jgw.supercodeplatform.prizewheels.domain.publisher.CdkEventPublisher;
import com.jgw.supercodeplatform.prizewheels.domain.repository.RecordRepository;
import com.jgw.supercodeplatform.prizewheels.domain.repository.WheelsRewardCdkRepository;
import com.jgw.supercodeplatform.prizewheels.domain.subscribers.CdkEventSubscriber;
import com.jgw.supercodeplatform.prizewheels.infrastructure.domainserviceimpl.CdkEventSubscriberImplV2;
import com.jgw.supercodeplatform.prizewheels.infrastructure.expectionsUtil.ErrorCodeEnum;
import org.apache.http.util.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 领域服务 : 仅处理WheelsReward 直接实现类
 */
@Service
public class WheelsRewardDomainService {
    @Autowired
    private CdkEventPublisher cdkEventPublisher;

    @Autowired
    @Qualifier("cdkEventSubscriberImpl")
    private CdkEventSubscriber cdkEventSubscriber;

    @Autowired
    @Qualifier("cdkEventSubscriberImplV2")
    private CdkEventSubscriber cdkEventSubscriberV2;
    @Autowired
    private WheelsRewardCdkRepository wheelsRewardCdkRepository;

    @Autowired
    private RecordRepository recordRepository;


    public void checkWhenUpdate(List<WheelsReward> wheelsRewards) {
        Asserts.check(!CollectionUtils.isEmpty(wheelsRewards), ErrorCodeEnum.NULL_ERROR.getErrorMessage());

        for(WheelsReward wheelsReward : wheelsRewards){
           // Asserts.check(wheelsReward.getId()!= null && wheelsReward.getId() > 0,ErrorCodeEnum.NULL_ERROR.getErrorMessage());
            Asserts.check(wheelsReward.getPrizeWheelId()  != null && wheelsReward.getPrizeWheelId() > 0
                    ,ErrorCodeEnum.NULL_ERROR.getErrorMessage());
        }

        double pro = 0D;
        for(WheelsReward wheelsReward : wheelsRewards){
            pro = pro + wheelsReward.getProbability();
        };
        Asserts.check(pro == 100D,"概率总和100%");
    }


    public void initPrizeWheelsid(List<WheelsReward> wheelsRewards, Long prizeWheelsid) {
        for(WheelsReward wheelsReward : wheelsRewards){
            wheelsReward.setPrizeWheelId(prizeWheelsid);
        }
    }

    public void checkWhenAdd(List<WheelsReward> wheelsRewards) {
        Asserts.check(!CollectionUtils.isEmpty(wheelsRewards), ErrorCodeEnum.NULL_ERROR.getErrorMessage());
        double pro = 0D;

        for(WheelsReward wheelsReward : wheelsRewards){
            pro = pro + wheelsReward.getProbability();
        };
        Asserts.check(pro == 100D,"概率总和100%");
    }


    public void cdkEventCommitedWhenNecessary(List<WheelsReward> wheelsRewards) {
        for(WheelsReward wheelsReward : wheelsRewards){
            Asserts.check(wheelsReward.getId()!= null, ErrorCodeEnum.NULL_ERROR.getErrorMessage());
            if(!StringUtils.isEmpty(wheelsReward.getCdkUuid())){
                CdkEvent cdkEvent = new CdkEvent(wheelsReward.getId(), wheelsReward.getCdkUuid());
//                cdkEventPublisher.addSubscriber(cdkEventSubscriber);   // excel 已经导入,关联wheelsRewards主键即可  前端组件不支持我也是醉了
                cdkEventPublisher.addSubscriber(cdkEventSubscriberV2); // excel导入到七牛云,此时读excel

                cdkEventPublisher.publish(cdkEvent);

            }
        }

    }

    /**
     * c端用户领取这个奖项
     * @param finalReward
     * @param user
     * @param outerCodeId
     * @param codeTypeId
     */
    public WheelsRewardCdk getReward(WheelsReward finalReward, H5LoginVO user, String outerCodeId, String codeTypeId,Long prizeWheelsId) {
        // 领取成功 cdk - 1 领取记录
        if(finalReward.getType().intValue() == RewardTypeConstant.virtual){
            WheelsRewardCdk cdkWhenH5Reward = wheelsRewardCdkRepository.getCdkWhenH5Reward(prizeWheelsId);


            WheelsRecord wheelsRecord = new WheelsRecord();
            wheelsRecord.setCreateTime(new Date());
            wheelsRecord.setMobile(user.getMobile());
            wheelsRecord.setRewardName(finalReward.getName());
            wheelsRecord.setType(RewardTypeConstant.virtual);
            wheelsRecord.setUserId(user.getMemberId()+"");
            wheelsRecord.setUserName(user.getMemberName());
            recordRepository.newRecordWhenH5Reward(wheelsRecord);

            return cdkWhenH5Reward;

        }

        if(finalReward.getType().intValue() == RewardTypeConstant.real){
            throw new RuntimeException("系统暂不支持实物");

        }

        throw new RuntimeException("奖励类型暂不支持...");
    }
}