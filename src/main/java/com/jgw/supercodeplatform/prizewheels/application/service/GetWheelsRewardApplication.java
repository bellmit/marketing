package com.jgw.supercodeplatform.prizewheels.application.service;

import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.marketing.config.redis.RedisLockUtil;
import com.jgw.supercodeplatform.marketing.config.redis.RedisUtil;
import com.jgw.supercodeplatform.marketing.exception.BizRuntimeException;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;
import com.jgw.supercodeplatform.prizewheels.application.transfer.PrizeWheelsOrderTransfer;
import com.jgw.supercodeplatform.prizewheels.application.transfer.ProductTransfer;
import com.jgw.supercodeplatform.prizewheels.application.transfer.WheelsRewardTransfer;
import com.jgw.supercodeplatform.prizewheels.application.transfer.WheelsTransfer;
import com.jgw.supercodeplatform.prizewheels.domain.constants.LoseAwardConstant;
import com.jgw.supercodeplatform.prizewheels.domain.event.ScanRecordWhenRewardEvent;
import com.jgw.supercodeplatform.prizewheels.domain.model.*;
import com.jgw.supercodeplatform.prizewheels.domain.publisher.ScanRecordWhenRewardPublisher;
import com.jgw.supercodeplatform.prizewheels.domain.repository.*;
import com.jgw.supercodeplatform.prizewheels.domain.service.CodeDomainService;
import com.jgw.supercodeplatform.prizewheels.domain.service.ProductDomainService;
import com.jgw.supercodeplatform.prizewheels.domain.service.WheelsRewardDomainService;
import com.jgw.supercodeplatform.prizewheels.domain.subscribers.ScanRecordWhenRewardSubscriber;
import com.jgw.supercodeplatform.prizewheels.infrastructure.mysql.pojo.ProductPojo;
import com.jgw.supercodeplatform.prizewheels.infrastructure.mysql.pojo.WheelsPojo;
import com.jgw.supercodeplatform.prizewheels.infrastructure.mysql.pojo.WheelsRewardPojo;
import com.jgw.supercodeplatform.prizewheels.interfaces.dto.PrizeWheelsOrderDto;
import com.jgw.supercodeplatform.prizewheels.interfaces.dto.PrizeWheelsRewardDto;
import com.jgw.supercodeplatform.prizewheels.interfaces.dto.ProductUpdateDto;
import com.jgw.supercodeplatform.prizewheels.interfaces.dto.WheelsRewardUpdateDto;
import com.jgw.supercodeplatform.prizewheels.interfaces.vo.WheelsDetailsVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

import static com.jgw.supercodeplatform.marketing.scheduled.PlatformPaySchedule.SEND_FAIL_WX_ORDER;

@Slf4j
@Service
public class GetWheelsRewardApplication {

    private static final String PREFIXX = "marketing:prizeWheels:h5Reward:";
    @Autowired
    private WheelsTransfer wheelsTransfer;
    @Autowired
    private ProductTransfer productTransfer;

    @Autowired
    private PrizeWheelsOrderTransfer prizeWheelsOrderTransfer;

    @Autowired
    private WheelsRewardTransfer wheelsRewardTransfer;

    @Autowired
    private WheelsPublishRepository wheelsPublishRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WheelsRewardRepository wheelsRewardRepository;

    @Autowired
    private ScanRecordRepository scanRecordRepository;

    @Autowired
    private CodeDomainService codeDomainService;

    @Autowired
    private ProductDomainService productDomainService;

    @Autowired
    private WheelsRewardDomainService wheelsRewardDomainService;

    @Autowired
    private ScanRecordWhenRewardPublisher scanRecordWhenRewardPublisher;


    @Autowired
    private ScanRecordWhenRewardSubscriber scanRecordWhenRewardSubscriber;

    @Autowired
    private PrizeWheelsOrderRepository prizeWheelsOrderRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private RedisLockUtil lock;

    /**
     * ???????????????????????????: ??????aop
     * ????????????????????????:????????????????????????????????????????????????????????????????????????
     * @param prizeWheelsRewardDto
     * @param user
     * @return
     */
    @Transactional(rollbackFor = {RuntimeException.class,Exception.class})
    public H5RewardInfo reward(PrizeWheelsRewardDto prizeWheelsRewardDto, H5LoginVO user) {
        String outerCodeId = prizeWheelsRewardDto.getOuterCodeId();
        boolean acquireLock = false;
        try {
            acquireLock = lock.lock(PREFIXX + outerCodeId, 60000, 1, 50);
            if (!acquireLock) {
                log.info("????????????{}???", PREFIXX + outerCodeId);
                throw new BizRuntimeException("??????????????????????????????...");
            }
            log.info("???????????????:??????{}???????????????{}", JSONObject.toJSONString(user), JSONObject.toJSONString(prizeWheelsRewardDto));
            // ????????????
            Long id = prizeWheelsRewardDto.getId(); // ??????id
            List<Product> products = productRepository.getByPrizeWheelsId(id);
            Wheels wheelsInfo = wheelsPublishRepository.getWheelsInfo(id);
            List<WheelsReward> wheelsRewards = wheelsRewardRepository.getDomainByPrizeWheelsId(id);
            String codeTypeId = prizeWheelsRewardDto.getCodeTypeId();
            ScanRecord mayBeScanedCode = scanRecordRepository.getCodeRecord(outerCodeId, codeTypeId);
            // ????????????

            // 1-1 ??????????????????
            String sbatchId = codeDomainService.vaildAndGetBatchId(codeTypeId, outerCodeId);
            // 1-2 ??????id??????????????????
            productDomainService.isPrizeWheelsMatchThisBatchId(products, sbatchId);

            // 1-3 ??????????????? ?????????????????????[????????????]
            codeDomainService.noscanedOrTerminated(mayBeScanedCode, wheelsInfo.getWxErcode());

            // 2 ????????????
            wheelsInfo.checkAcitivyStatusWhenHReward(user.getOrganizationId());


            // ????????????
            // 2-1 ???????????????????????????
            newScanRecord(user, codeTypeId, outerCodeId);
            // ???????????? ???????????????????????????
            ProbabilityCalculator probabilityCalculator = new ProbabilityCalculator();
            probabilityCalculator.initRewards(wheelsRewards);
            WheelsReward finalReward = probabilityCalculator.calculator();

            // ????????????
            H5RewardInfo reward = wheelsRewardDomainService.getReward(finalReward, user, outerCodeId, codeTypeId, id);


            return reward;

        } catch (BizRuntimeException e){
            e.printStackTrace();
            throw e;
        } finally {
            if (acquireLock) {
                lock.releaseLock(PREFIXX + outerCodeId);
            }
        }
    }

    /**
     * ??????:????????????????????????
     * @param user
     * @param codeTypeId
     * @param outerCodeId
     */
    private void newScanRecord(H5LoginVO user, String codeTypeId, String outerCodeId) {
        ScanRecord scanRecord = new ScanRecord(outerCodeId,Integer.parseInt(codeTypeId));
        String userName = !StringUtils.isEmpty(user.getMemberName())?user.getMemberName():user.getMobile();
        // TODO wxName?????????
        scanRecord.initScanerInfo(user.getMemberId(),userName,user.getMobile(),user.getOrganizationId(),user.getOrganizationName(),user.getOpenid(), user.getMemberName());
        scanRecordWhenRewardPublisher.addSubscriber(scanRecordWhenRewardSubscriber);
        scanRecordWhenRewardPublisher.commitAsyncEvent(new ScanRecordWhenRewardEvent(scanRecord));
    }


    /**
     *  TODO ?????????????????????
     * @param productBatchId
     * @return
     */
    public WheelsDetailsVo detail(String productBatchId, String productId) {
        log.info("H5???????????????:??????ID{}:????????????ID{}",productId, productBatchId);
        //
        WheelsDetailsVo wheelsDetailsVo=new WheelsDetailsVo();
        //????????????
        // TODO ??????????????????????????????????????????????????????pojo
        List<ProductPojo> productPojos = productRepository.getPojoByBatchId(productId,productBatchId);
        // TODO ??????????????????:?????? Asserts
        Asserts.check(!CollectionUtils.isEmpty(productPojos),"????????????????????????");
        // ???????????????ID
        Long id=productPojos.get(0).getActivitySetId();
        List<ProductUpdateDto> productUpdateDtos=productTransfer.productPojoToProductUpdateDto(productPojos);
        wheelsDetailsVo.setProductDtos(productUpdateDtos);

        WheelsPojo wheelsPojo=wheelsPublishRepository.getWheelsById(id);
        // TODO ??????????????????:?????? Asserts
        Asserts.check(wheelsPojo!=null,"???????????????????????????");
        wheelsDetailsVo=wheelsTransfer.tranferWheelsPojoToDomain(wheelsPojo);
        //????????????
        List<WheelsRewardPojo> wheelsRewardPojos=wheelsRewardRepository.getByPrizeWheelsId(id);
        // TODO ??????????????????::???????????? Asserts
        Asserts.check(!CollectionUtils.isEmpty(wheelsRewardPojos),"????????????????????????");
        //??????list????????????????????????????????????????????????????????????
        WheelsRewardPojo notwheelsRewardPojo=new WheelsRewardPojo();
        // TODO ??????????????????::????????????
        for (WheelsRewardPojo wheelsRewardPojo:wheelsRewardPojos){
            if (wheelsRewardPojo.getLoseAward().intValue() == LoseAwardConstant.yes.intValue()){
                notwheelsRewardPojo=wheelsRewardPojo;
                break;
            }
        }
        wheelsRewardPojos.remove(notwheelsRewardPojo);

        List<WheelsRewardUpdateDto> wheelsRewardUpdateDtos=wheelsRewardTransfer.transferRewardToDomain(wheelsRewardPojos);
        // TODO ??????????????????
        wheelsDetailsVo.setWheelsRewardDtos(wheelsRewardUpdateDtos);
        wheelsDetailsVo.setAutoType(!CollectionUtils.isEmpty(productPojos)?productPojos.get(0).getAutoType():1);
        //??????????????????
        wheelsDetailsVo.setLoseAwardProbability(notwheelsRewardPojo.getProbability());
        return wheelsDetailsVo;
    }

    @Transactional
    public void setAdddress(PrizeWheelsOrderDto prizeWheelsOrderDto,H5LoginVO user) {
        PrizeWheelsOrder prizeWheelsOrder = prizeWheelsOrderTransfer.tranferToDomain(prizeWheelsOrderDto);
        prizeWheelsOrder.initRealRewardInfo(user.getMemberId(),user.getMobile(),user.getMemberName(),user.getOrganizationId(),user.getOrganizationName());
        prizeWheelsOrderRepository.addOrder(prizeWheelsOrder);


         recordRepository.updateRecordInfoWhenReal(prizeWheelsOrderDto);

    }

}
