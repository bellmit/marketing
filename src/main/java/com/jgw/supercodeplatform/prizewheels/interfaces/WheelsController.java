package com.jgw.supercodeplatform.prizewheels.interfaces;


import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.page.AbstractPageService;
import com.jgw.supercodeplatform.marketing.common.util.ExcelUtils;
import com.jgw.supercodeplatform.marketing.common.util.JsonToMapUtil;
import com.jgw.supercodeplatform.marketingsaler.base.controller.SalerCommonController;
import com.jgw.supercodeplatform.prizewheels.application.service.WheelsPublishAppication;
import com.jgw.supercodeplatform.prizewheels.domain.constants.CdkTemplate;
import com.jgw.supercodeplatform.prizewheels.domain.constants.QiNiuYunConfigConstant;
import com.jgw.supercodeplatform.prizewheels.infrastructure.domainserviceimpl.CdkEventSubscriberImplV2;
import com.jgw.supercodeplatform.prizewheels.infrastructure.mysql.pojo.PrizeWheelsOrderPojo;
import com.jgw.supercodeplatform.prizewheels.infrastructure.mysql.pojo.WheelsRecordPojo;
import com.jgw.supercodeplatform.prizewheels.interfaces.dto.ActivityStatus;
import com.jgw.supercodeplatform.prizewheels.interfaces.dto.DaoSearchWithPrizeWheelsIdDto;
import com.jgw.supercodeplatform.prizewheels.interfaces.dto.WheelsDto;
import com.jgw.supercodeplatform.prizewheels.interfaces.dto.WheelsUpdateDto;
import com.jgw.supercodeplatform.prizewheels.interfaces.vo.WheelsDetailsVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("marketing/prizeWheels")
@Api(value = "", tags = "?????????")
@Slf4j

public class WheelsController extends SalerCommonController {
     @Autowired
    private WheelsPublishAppication appication;

    @Autowired
    private CdkEventSubscriberImplV2 cdkEventSubscriberImplV2;

    @Value("")
    private String cdkKey;

    @Value("{\"userName\":\"??????\",\"mobile\":\"?????????\", \"rewardName\":\"????????????\"" +
            ",\"revicerName\":\"?????????\",\"address\":\"????????????\",\"revicerMobile\":\"???????????????\"" +
            ",\"createTime\":\"????????????\"}")
    private String EXCEL_FIELD_MAP;

    @Value("{\"organizationId\":\"??????ID\",\"organizationName\":\"????????????\", \"receiverName\":\"?????????\",\"mobile\":\"????????????\",\"receiverMobile\":\"????????????\",\"address\":\"????????????\",\"content\":\"??????\",\"createDate\":\"????????????\"}")
    private String EXCEL_ORDER_FIELD_MAP;
    @ResponseBody
    @PostMapping("/add")
    @ApiOperation(value = "??????", notes = "????????????????????????")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token??????", required = true)
    public RestResult add(@Valid  @RequestBody WheelsDto wheelsDto)   {
        appication.publish(wheelsDto);
        return success();
    }

    @ResponseBody
    @PostMapping("/update")
    @ApiOperation(value = "??????", notes = "????????????????????????")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token??????", required = true)
    public RestResult update(@Valid  @RequestBody WheelsUpdateDto wheelsDto)   {
        appication.update(wheelsDto);
        return success();
    }

    @ResponseBody
    @GetMapping("/detail")
    @ApiOperation(value = "??????", notes = "????????????????????????")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token??????", required = true)
    public RestResult<WheelsDetailsVo> detail(@RequestParam("id") Long id)   {
        WheelsDetailsVo wheelsDetailsVo = appication.getWheelsDetails(id);
        return success(wheelsDetailsVo);
    }

    @ResponseBody
    @GetMapping("/delete")
    @ApiOperation(value = "??????", notes = "????????????????????????")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token??????", required = true)
    public RestResult delete(@RequestParam("id") Long id)   {
        appication.deletePrizeWheelsById(id);
        return success();
    }


    @ResponseBody
    @PostMapping("/changeStatus")
    @ApiOperation(value = "??????????????????", notes = "????????????(1????????????????????????0 ????????????)")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token??????", required = true)
    public RestResult status(@Valid  @RequestBody ActivityStatus activityStatus)   {

        appication.upadteStatus(activityStatus);
        return success();
    }


    @ResponseBody
    @GetMapping("/record")
    @ApiOperation(value = "????????????", notes = "")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token??????", required = true)
    public RestResult<AbstractPageService.PageResults<List<WheelsRecordPojo>> > record(DaoSearchWithPrizeWheelsIdDto daoSearch)   {
        return success(appication.records(daoSearch));
    }


    @ResponseBody
    @GetMapping("/pageOrder")
    @ApiOperation(value = "????????????")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token??????", required = true)
    public RestResult<AbstractPageService.PageResults<List<PrizeWheelsOrderPojo>>> orderPage(DaoSearchWithPrizeWheelsIdDto daoSearch){
        return success(appication.orderRecords(daoSearch));
    }



    @GetMapping("/export")
    @ApiOperation(value = "??????????????????",notes = "")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token??????", required = true)
    public void exportRecords(DaoSearchWithPrizeWheelsIdDto daoSearch, HttpServletResponse response) throws SuperCodeException {
        //???????????????
        daoSearch.setCurrent(1);
        daoSearch.setPageSize(100000);
        // step-1 ????????????
        AbstractPageService.PageResults<List<WheelsRecordPojo>> pageResults=appication.records(daoSearch);
        // step-2 ????????????
        List<WheelsRecordPojo> list=pageResults.getList();
        //??????
        Map<String,String> filedMap;
        try {
            filedMap= JsonToMapUtil.toMap(EXCEL_FIELD_MAP);
        } catch (Exception e) {
            log.warn("{desc???????????????????????????"+e.getMessage()+"}");
            throw new SuperCodeException("??????????????????",500);
        }
        ExcelUtils.listToExcel(list, filedMap, "????????????",response);
    }





    @GetMapping("/exportOrder")
    @ApiOperation(value = "????????????")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token??????", required = true)
    public void orderExport(DaoSearchWithPrizeWheelsIdDto daoSearch, HttpServletResponse response) throws SuperCodeException {
        //???????????????
        daoSearch.setCurrent(1);
        daoSearch.setPageSize(100000);
        // step-1 ????????????
        AbstractPageService.PageResults<List<PrizeWheelsOrderPojo>> pageResults=appication.orderRecords(daoSearch);
        // step-2 ????????????
        List<PrizeWheelsOrderPojo> list=pageResults.getList();
        //??????
        Map<String,String> filedMap;
        try {
            filedMap= JsonToMapUtil.toMap(EXCEL_ORDER_FIELD_MAP);
        } catch (Exception e) {
            log.warn("{desc???????????????????????????"+e.getMessage()+"}");
            throw new SuperCodeException("??????????????????",500);
        }
        ExcelUtils.listToExcel(list, filedMap, "????????????",response);
    }

    /**
     * @author fangshiping
     * @param response
     * @return
     * @throws IOException
     */
    @GetMapping("cdktemplate/download")
    @ApiOperation(value = "????????????",notes = "")
    public void down(HttpServletResponse response) throws IOException {
        // ??????cdkkey????????????????????????
        response.setContentType("multipart/form-data");
        response.setHeader("Content-Disposition", "attachment;fileName=cdktemplate.xls");

        // ??????excel???
        InputStream in = cdkEventSubscriberImplV2.downExcelStream(QiNiuYunConfigConstant.URL+CdkTemplate.URL);
        ServletOutputStream outputStream = response.getOutputStream();
        try {
            byte[] buf=new byte[1024];
            int len=0;
            while((len=in.read(buf))!=-1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (null!=in ) {
                in.close();
            }
            outputStream.close();
        }
    }
}
