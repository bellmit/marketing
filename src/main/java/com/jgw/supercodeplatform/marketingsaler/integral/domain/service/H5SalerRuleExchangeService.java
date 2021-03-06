package com.jgw.supercodeplatform.marketingsaler.integral.domain.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.page.AbstractPageService;
import com.jgw.supercodeplatform.marketing.exception.BizRuntimeException;
import com.jgw.supercodeplatform.marketing.service.weixin.WXPayService;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;
import com.jgw.supercodeplatform.marketingsaler.base.service.SalerCommonService;
import com.jgw.supercodeplatform.marketingsaler.integral.constants.ExchangeUpDownStatus;
import com.jgw.supercodeplatform.marketingsaler.integral.constants.UndercarriageSetWayConstant;
import com.jgw.supercodeplatform.marketingsaler.integral.interfaces.dto.DaoSearchWithOrganizationId;
import com.jgw.supercodeplatform.marketingsaler.integral.interfaces.dto.H5SalerRuleExchangeDto;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.mapper.SalerRuleExchangeMapper;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.pojo.SalerExchangeNum;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.pojo.SalerRuleExchange;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.pojo.User;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.transfer.H5SalerRuleExchangeTransfer;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.transfer.SalerRecordTransfer;
import com.jgw.supercodeplatform.marketingsaler.integral.domain.transfer.SalerRuleExchangeTransfer;
import com.jgw.supercodeplatform.prizewheels.domain.constants.CallBackConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.util.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.jgw.supercodeplatform.marketingsaler.integral.infrastructure.MoneyCalculator.*;

@Service
@Slf4j
public class H5SalerRuleExchangeService  extends SalerCommonService<SalerRuleExchangeMapper, SalerRuleExchange> {
    @Autowired
    private AutoUndercarriageService autoUndercarriageService;

    @Autowired
    private  UserService marketingUserService;

    @Autowired
    private  SalerExchangeNumService salerExchangeNumService;

    @Value("${marketing.server.ip}")
    private String serverIp;

    @Autowired
    private  SalerRecordService recordService;
    @Autowired
    private WXPayService wxPayService;


    @Autowired
    private SalerRuleExchangeMapper salerRuleExchangeMapper;



    @Transactional // todo ????????????????????????
    public RestResult exchange(H5SalerRuleExchangeDto salerRuleExchangeDto, H5LoginVO user) {
        log.info("????????????????????????????????? salerRuleExchangeDto{}",salerRuleExchangeDto);
        log.info("???????????? H5LoginVO{}",JSONObject.toJSONString(user));
        // ??????
        Asserts.check(!StringUtils.isEmpty(user.getOrganizationId()),"??????????????????????????????");
        SalerRuleExchange salerRuleExchange = baseMapper.selectOne(query().eq("Id", salerRuleExchangeDto.getId()).getWrapper());
        Asserts.check(salerRuleExchange !=null,"?????????????????????");
        Asserts.check(salerRuleExchange.getOrganizationId().equals(user.getOrganizationId()),"???????????????????????????");
        // ????????????
        autoUndercarriage(salerRuleExchange);
        // ??????????????????????????????
        checkExchangeStatus(salerRuleExchange);
        // ???????????????????????????????????????
        User userPojo = marketingUserService.canExchange(user, salerRuleExchange.getExchangeIntegral());

        // ??????????????????
        salerExchangeNumService.canExchange(userPojo,salerRuleExchange);
        // ??????????????????????????????
        double money = 0D;
        try {
            money = calculatorSalerExcgange(salerRuleExchange);
        } catch (Exception e) {
            marketingUserService.reduceIntegral(salerRuleExchange.getExchangeIntegral(),userPojo);
            // ????????????
            salerExchangeNumService.save(new SalerExchangeNum(null,userPojo.getId(),userPojo.getOrganizationId(),salerRuleExchange.getId()));
            // ???????????? ??????0???
            recordService.save(SalerRecordTransfer.buildRecord(salerRuleExchange,user,money));
            e.printStackTrace();
            return RestResult.error(e.getMessage(),e.getMessage());
        }
        log.info("????????????????????? => money{} USER{} salerRuleExchangeDto{}",money,userPojo,salerRuleExchange);

        if(money != 0D){
           // ????????????
           // ????????????
           int update = salerRuleExchangeMapper.updateReduceHaveStock(salerRuleExchange);
           Asserts.check(update==1,"?????????????????????");
           // ?????????????????????
           marketingUserService.reduceIntegral(salerRuleExchange.getExchangeIntegral(),userPojo);
           // ????????????
           salerExchangeNumService.save(new SalerExchangeNum(null,userPojo.getId(),userPojo.getOrganizationId(),salerRuleExchange.getId()));
           // ??????[????????????]
           recordService.save(SalerRecordTransfer.buildRecord(salerRuleExchange,user,money));
           // ??????
           try {
               wxPayService.qiyePaySyncWithResend(user.getOpenid(), CallBackConstant.serverIp,(int)(money*100), UUID.randomUUID().toString().replaceAll("-",""),user.getOrganizationId(),1);
           } catch (Exception e) {
               e.printStackTrace();
               log.error("???????????????????????????.........................??????salerRuleExchangeDto{},user{}",salerRuleExchangeDto,JSONObject.toJSONString(user));
               throw new BizRuntimeException("?????????????????????????????????");
           }
           int i = salerRuleExchangeMapper.reduceHaveStock(salerRuleExchange);
           Asserts.check(i==1,"??????????????????");

       }
       log.info("????????????" +  money);
        String moneyStr = String.format("%.2f", money);
        log.info("????????????moneyStr" +  moneyStr);
       return RestResult.success( moneyStr,moneyStr);

    }


    /**
     * ????????????
     * @param salerRuleExchange
     */
    private void autoUndercarriage(SalerRuleExchange salerRuleExchange) {
        if(salerRuleExchange.getUndercarriageSetWay().intValue() == UndercarriageSetWayConstant.zerostork
                && (salerRuleExchange.getHaveStock() != null && salerRuleExchange.getHaveStock()== 0)
        ){
            autoUndercarriageService.listenAutoUnder(H5SalerRuleExchangeTransfer.buildAutoUndercarriageEvent(salerRuleExchange.getId()));
        }
        if(salerRuleExchange.getUndercarriageSetWay().intValue() == UndercarriageSetWayConstant.timecoming
                && new Date().getTime() > salerRuleExchange.getUnderCarriage().getTime()
        ) {
            autoUndercarriageService.listenAutoUnder(H5SalerRuleExchangeTransfer.buildAutoUndercarriageEvent(salerRuleExchange.getId()));
        }
    }

    /**
     * ??????????????????
     * @param salerRuleExchange
     */
    private void checkExchangeStatus(SalerRuleExchange salerRuleExchange) {
        log.info("checkExchangeStatus??????{}",salerRuleExchange);
        Asserts.check(salerRuleExchange !=null,"???????????????");
        Asserts.check(salerRuleExchange.getStatus().intValue() == ExchangeUpDownStatus.up ,"????????????????????????");
        Asserts.check(salerRuleExchange.getPreHaveStock()!=null&& salerRuleExchange.getPreHaveStock()>0,"??????????????????");
        // todo ???????????????????????? ?????????????????????????????????????????????????????????
        if(salerRuleExchange.getUndercarriageSetWay().intValue() == UndercarriageSetWayConstant.timecoming
                && new Date().getTime() > salerRuleExchange.getUnderCarriage().getTime()
        ) {
            throw new BizRuntimeException("???????????????????????????...");
        }
    }

    public AbstractPageService.PageResults<List<SalerRuleExchange>>  h5PageList(DaoSearchWithOrganizationId daoSearch) {
        IPage<SalerRuleExchange> salerRuleExchangeIPage = baseMapper.selectPage(SalerRuleExchangeTransfer.getPage(daoSearch),
                SalerRuleExchangeTransfer.getH5PageParam(daoSearch.getOrganizationId()));
        return SalerRuleExchangeTransfer.toPageResults(salerRuleExchangeIPage);
    }


}
