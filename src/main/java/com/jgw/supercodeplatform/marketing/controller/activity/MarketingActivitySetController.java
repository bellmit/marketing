package com.jgw.supercodeplatform.marketing.controller.activity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.dto.activity.MarketingActivityCreateParam;
import com.jgw.supercodeplatform.marketing.dto.activity.MarketingActivitySetStatusUpdateParam;
import com.jgw.supercodeplatform.marketing.dto.activity.MarketingReceivingPageParam;
import com.jgw.supercodeplatform.marketing.pojo.MarketingActivitySet;
import com.jgw.supercodeplatform.marketing.service.activity.MarketingActivityProductService;
import com.jgw.supercodeplatform.marketing.service.activity.MarketingActivitySetService;
import com.jgw.supercodeplatform.marketing.vo.activity.ReceivingAndWinningPageVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/marketing/activity/set")
@Api(tags = "活动设置管理")
public class MarketingActivitySetController {

	@Autowired
	private MarketingActivitySetService service;

    @Autowired
    private MarketingActivityProductService maProductService;
    
    
    /**
     * 活动编辑;本期不做
     * @param marketingActivityParam
     * @return
     * @throws Exception
     */
    @GetMapping("/activityInfo")
    @ApiOperation("获取活动数据")
    @ApiImplicitParams({
    @ApiImplicitParam(name = "super-token", paramType = "header", value = "token信息", required = true),
    @ApiImplicitParam(name="activitySetId",paramType="query",value="活动设置ID",required=true)})
    public RestResult<MarketingActivityCreateParam> activityInfo(@RequestParam Long activitySetId) throws Exception {
    	return new RestResult<>(200, "查询成功", new MarketingActivityCreateParam());
    }



    /**
     * 活动创建
     * @param marketingActivityParam
     * @return
     * @throws Exception
     */
    @PostMapping("/add")
    @ApiOperation("活动创建")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token信息", required = true)
    public RestResult<String> create(@RequestBody MarketingActivityCreateParam activitySetParam) throws Exception {
    	return service.memberActivityAdd(activitySetParam);
    }
    
    /**
     * 活动创建
     * @param marketingActivityParam
     * @return
     * @throws Exception
     */
    @PostMapping("/guideActivityAdd")
    @ApiOperation("活动创建")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token信息", required = true)
    public RestResult<String> guideActivityAdd(@RequestBody MarketingActivityCreateParam activitySetParam) throws Exception {
    	return service.guideActivityAdd(activitySetParam);
    }
    
    
    /**
     * 活动编辑
     * @param marketingActivityParam
     * @return
     * @throws Exception
     */
    @PostMapping("/update")
    @ApiOperation("活动更新")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token信息", required = true)
    public RestResult<String> update(@RequestBody MarketingActivityCreateParam activitySetParam) throws Exception {
    	return service.update(activitySetParam);
    }
    
    /**
     * 停用或启用活动
     * @param marketingActivityParam
     * @return
     * @throws Exception
     */
    @PostMapping("/disOrEnable")
    @ApiOperation("停用或启用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token信息", required = true)
    })
    public RestResult<String> disOrEnable(@RequestBody MarketingActivitySetStatusUpdateParam mUpdateStatus) throws Exception {
    	return service.updateActivitySetStatus(mUpdateStatus);
    }
    
    /**
     * 
     * @param marketingActivityParam
     * @return
     * @throws Exception
     */
    @GetMapping("/getPageInfo")
    @ApiOperation("根据活动id获取领取页和中奖页数据")
    @ApiImplicitParams({@ApiImplicitParam(paramType="header",value = "新平台token--开发联调使用",name="super-token"),@ApiImplicitParam(paramType="query",value = "活动设置主键id",name="activitySetId")})
    public RestResult<ReceivingAndWinningPageVO> getPageInfo(@RequestParam(required=true) Long activitySetId) throws Exception {
    	return service.getPageInfo(activitySetId);
    }
    

    @PostMapping("/updatePage")
    @ApiOperation("更新领取页中奖页")
    @ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token信息", required = true)
    public RestResult<String> updatePage(@RequestBody MarketingReceivingPageParam mUpdateParam) throws Exception {
    	return service.updatePage(mUpdateParam);
    }

    /**
     * 获取活动基础信息
     * @param activitySetId
     * @return
     */
    @GetMapping(value = "/getBaseInfo")
    @ApiOperation("编辑活动： 获取活动基础信息")
    @ApiImplicitParams({@ApiImplicitParam(paramType="header",value = "新平台token--开发联调使用",name="super-token"),@ApiImplicitParam(paramType="query",value = "活动设置主键id",name="activitySetId")})
    public RestResult<MarketingActivitySet> getActivityBaseInfoByeditPage(@RequestParam Long activitySetId){
        return service.getActivityBaseInfoByeditPage(activitySetId);

    }


    @GetMapping("/relationActProds")
    @ApiOperation("获取活动做过码关联的产品及产品批次数据")
    @ApiImplicitParams({@ApiImplicitParam(name = "super-token", paramType = "header", defaultValue = "64b379cd47c843458378f479a115c322", value = "token信息", required = true)})
    public JSONObject relationActProds() throws Exception {
    	
        return maProductService.relationActProds();
    }



}
