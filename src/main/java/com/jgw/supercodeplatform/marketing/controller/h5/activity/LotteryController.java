package com.jgw.supercodeplatform.marketing.controller.h5.activity;

import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;

import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.model.activity.LotteryResultMO;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.service.LotteryService;
import com.jgw.supercodeplatform.marketing.service.SalerLotteryService;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/marketing/front/lottery")
@Api(tags = "h5用户领奖方法")
public class LotteryController extends CommonUtil {
	private static Logger logger = LoggerFactory.getLogger(LotteryController.class);
    @Autowired
    private LotteryService service;

    @Autowired
    private CommonService commonService;

    @Autowired
    private SalerLotteryService  salerLotteryService;

    private static final Long codeTypeId = 12L;

    @Value("${cookie.domain}")
	private String cookieDomain;
	/**
	 * 用户服务地址
	 */
	@Value("${rest.user.url}")
	private String USER_SERVICE;

    @GetMapping("/lottery")
    @ApiOperation(value = "用户点击领奖方法", notes = "")
    public RestResult<LotteryResultMO> lottery(String wxstate) throws Exception {
    	logger.info("领奖传入微信参数:{}", wxstate);
        return service.lottery(wxstate, request.getRemoteAddr());
    }
    
    
    @GetMapping("/previewLottery")
    @ApiOperation(value = "活动预览领奖方法", notes = "")
    public RestResult<LotteryResultMO> previewLottery(String uuid) throws Exception {
        return service.previewLottery(uuid, request);
    }

    /**
     * 除去码是否贝扫是共享数据,其他都是用户数据
     * 码存在扫码【成功】 领奖时候保存码【领奖成功】
     * @param wxstate
     * @param jwtUser
     * @return
     * @throws SuperCodeException
     * @throws ParseException
     */
    @GetMapping("/salerLottery")
    @ApiOperation(value = "导购领奖方法", notes = "导购活动领取")
    @ApiImplicitParams(value= {@ApiImplicitParam(paramType="header",value = "会员请求头",name="jwt-token")})
    public RestResult<String> salerLottery( String codeId,Long codeTypeId ,String wxstate, @ApiIgnore H5LoginVO jwtUser, HttpServletRequest request) throws Exception {
        // 是不是营销码制，不是不可通过
        if(codeTypeId == null|| codeTypeId != 12){
            throw new SuperCodeException("非营销码...");
        }
        commonService.checkCodeTypeValid(codeTypeId);
        commonService.checkCodeValid(codeId,codeTypeId+"");
        return salerLotteryService.salerlottery(wxstate,jwtUser,request);
    }
    
   
}
