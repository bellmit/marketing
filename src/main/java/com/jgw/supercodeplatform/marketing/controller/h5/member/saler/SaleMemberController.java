package com.jgw.supercodeplatform.marketing.controller.h5.member.saler;


import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.page.AbstractPageService;
import com.jgw.supercodeplatform.marketing.dto.DaoSearchWithOrganizationIdParam;
import com.jgw.supercodeplatform.marketing.dto.SaleInfo;
import com.jgw.supercodeplatform.marketing.dto.integral.ExchangeProductParam;
import com.jgw.supercodeplatform.marketing.dto.integral.IntegralExchangeSkuDetailAndAddress;
import com.jgw.supercodeplatform.marketing.enums.market.MemberTypeEnums;
import com.jgw.supercodeplatform.marketing.pojo.integral.IntegralRecord;
import com.jgw.supercodeplatform.marketing.service.LotteryService;
import com.jgw.supercodeplatform.marketing.service.es.activity.CodeEsService;
import com.jgw.supercodeplatform.marketing.service.integral.IntegralRecordService;
import com.jgw.supercodeplatform.marketing.service.user.MarketingMembersService;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.elasticsearch.monitor.os.OsStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.text.ParseException;
import java.util.Map;

/**
 * 销售员扫码领红包
 */
@RestController
@RequestMapping("/marketing/saleMember/")
@Api(tags = "销售员H5")
public class SaleMemberController {
    @Autowired
    private IntegralRecordService service;
    @Autowired
    private CodeEsService es;
    @Autowired
    private LotteryService lotteryService;



    @GetMapping("info")
    @ApiOperation(value = "销售员中心", notes = "")
    @ApiImplicitParams(value= {@ApiImplicitParam(paramType="header",value = "会员请求头",name="jwt-token")})
    public RestResult info(@ApiIgnore H5LoginVO jwtUser, DaoSearchWithOrganizationIdParam search) throws Exception {
        if(MemberTypeEnums.SALER.getType().intValue()!=jwtUser.getMemberType()){
            throw new SuperCodeException("会员角色错误...");
        }
        SaleInfo saleInfo = new SaleInfo();



        // 1 获取红包统计信息
        Map acquireMoneyAndAcquireNums = service.getAcquireMoneyAndAcquireNums(jwtUser.getMemberId(), jwtUser.getMemberType(), jwtUser.getOrganizationId());

        // 2 获取红包信息
        // 分页信息传递
        IntegralRecord params = new IntegralRecord();
        params.setMemberType(MemberTypeEnums.SALER.getType());
        // 一个导购只能是一个组织
        params.setOrganizationId(jwtUser.getOrganizationId());
        params.setSalerId(jwtUser.getMemberId());
        params.setStartNumber(search.getStartNumber());
        params.setPageSize(search.getPageSize());
        // 查询
        AbstractPageService.PageResults<IntegralRecord> objectPageResults = service.listSearchViewLike(params);

        // 3 获取扫码信息
        Integer scanNum = es.searchScanInfoNum(jwtUser.getMemberId(), jwtUser.getMemberType());
        // 4 数据转换
        saleInfo.setScanQRCodeNum(scanNum);
        saleInfo.setScanAmoutNum((Integer) acquireMoneyAndAcquireNums.get("count"));
        saleInfo.setAmoutNum((Float) acquireMoneyAndAcquireNums.get("sum"));
        saleInfo.setAmoutNumStr(saleInfo.getAmoutNum()+"");
        // TODO page转前端格式
        saleInfo.setPageInfo(objectPageResults);
        return RestResult.success("success",saleInfo);
    }
}
