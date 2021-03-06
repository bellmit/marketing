package com.jgw.supercodeplatform.marketing.controller.h5.member;

import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.constants.PcccodeConstants;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.util.BeanPropertyUtil;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.JWTUtil;
import com.jgw.supercodeplatform.marketing.config.redis.RedisUtil;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.constants.RedisKey;
import com.jgw.supercodeplatform.marketing.dto.members.H5MembersInfoParam;
import com.jgw.supercodeplatform.marketing.dto.members.MarketingMembersAddParam;
import com.jgw.supercodeplatform.marketing.dto.members.MarketingMembersUpdateParam;
import com.jgw.supercodeplatform.marketing.enums.market.MemberTypeEnums;
import com.jgw.supercodeplatform.marketing.pojo.MarketingMembers;
import com.jgw.supercodeplatform.marketing.pojo.MemberWithWechat;
import com.jgw.supercodeplatform.marketing.pojo.UserWithWechat;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import com.jgw.supercodeplatform.marketing.service.user.MarketingMembersService;
import com.jgw.supercodeplatform.marketing.service.user.MarketingSaleMemberService;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/marketing/front/members")
@Api(tags = "h5??????????????????????????????????????????")
public class MarketingMembersFrontController extends CommonUtil {
 	@Autowired
	private MarketingMembersService marketingMembersService;

	@Autowired
	private CommonService commonService;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private MarketingSaleMemberService marketingSaleMemberService;
	
	@Autowired
	private RedisUtil redisUtil;
	
	
	@Value("${cookie.domain}")
	private String cookieDomain;
	/**
	 * ??????????????????
	 */
	@Value("${rest.user.url}")
	private String USER_SERVICE;


	@RequestMapping(value = "/getMemberId",method = RequestMethod.GET)
	@ApiOperation(value = "??????????????????|??????????????????", notes = "")
	@ApiImplicitParams(value= {  @ApiImplicitParam(name = "jwt-token", paramType = "header", defaultValue = "ldpfbsujjknla;s.lasufuafpioquw949gyobrljaugf89iweubjkrlnkqsufi.awi2f7ygihuoquiu", value = "jwt-token??????", required = true)
	})
	public RestResult<H5MembersInfoParam> get(@ApiIgnore H5LoginVO jwtUser) throws Exception {
		MarketingMembers memberById = marketingMembersService.getMemberById(jwtUser.getMemberId());
 		H5MembersInfoParam memberVO = modelMapper.map(memberById, H5MembersInfoParam.class);
		return RestResult.success("success", memberVO);

	}


	@RequestMapping(value = "/update",method = RequestMethod.POST)
	@ApiOperation(value = "??????????????????|??????????????????", notes = "")
	@ApiImplicitParams(value= {  @ApiImplicitParam(name = "jwt-token", paramType = "header", defaultValue = "ldpfbsujjknla;s.lasufuafpioquw949gyobrljaugf89iweubjkrlnkqsufi.awi2f7ygihuoquiu", value = "jwt-token??????", required = true)
	})
	public RestResult update(@RequestBody MarketingMembersUpdateParam member, @ApiIgnore H5LoginVO jwtUser ) throws Exception {
		// ??????jwt-token + H5LoginVO??????????????????
		// ???????????????: ????????????????????????????????????,??????????????????KEY,???????????????
		// ??????????????????????????????????????????????????????
		String verificationCode = redisUtil.get(RedisKey.phone_code_prefix + member.getMobile());
		if(StringUtils.isBlank(verificationCode)){
			throw new SuperCodeException("??????????????????");
		}
		if (member.getVerificationCode() == null || !verificationCode.equals(member.getVerificationCode())){
			// ?????????????????????
			throw new SuperCodeException("???????????????,???????????????");
		}
		Long id=member.getId();
		if (null==id) {
			// ?????????????????????
			throw new SuperCodeException("??????id????????????");
		}
		MarketingMembers memberById = marketingMembersService.getMemberById(id);
		if (null==memberById) {
			// ?????????????????????
			throw new SuperCodeException("??????????????????id?????????");
		}
		
		String organizationId=memberById.getOrganizationId();
		//?????????????????????
		String mobile=member.getMobile();
		//??????????????????????????????????????????????????????????????????????????????
		if (StringUtils.isNotBlank(mobile)) {
			//?????????????????????????????????????????????????????????????????????????????????????????????
			MarketingMembers memberByPhone =marketingMembersService.selectByPhoneAndOrgIdExcludeId(mobile,organizationId,id);
			if (null!=memberByPhone ) {
				throw new SuperCodeException("?????????????????????");
			}
		}
		MarketingMembers memberDto = modelMapper.map(member, MarketingMembers.class);
		// ????????????????????????????????????????????????????????????,????????????????????????
		if(!StringUtils.isBlank(memberById.getBabyBirthday())){
			memberDto.setBabyBirthday(null);
		}
		if(!StringUtils.isBlank(memberById.getBirthday())){
			memberDto.setBirthday(null);
		}

		if(!StringUtils.isBlank(member.getpCCcode())){
			List<JSONObject> objects = JSONObject.parseArray(member.getpCCcode(),JSONObject.class);
			int size = objects.size();
			JSONObject province = size > 0 ? objects.get(0)  : new JSONObject()  ;
			JSONObject city = size > 1  ? objects.get(1) : new JSONObject() ;
			JSONObject country = size > 2 ? objects.get(2) : new JSONObject();
			memberDto.setProvinceCode(province.getString(PcccodeConstants.areaCode));
			memberDto.setCityCode(city.getString(PcccodeConstants.areaCode));
			memberDto.setCountyCode(country.getString(PcccodeConstants.areaCode));
			memberDto.setProvinceName(province.getString(PcccodeConstants.areaName));
			memberDto.setCityName(city.getString(PcccodeConstants.areaName));
			memberDto.setCountyName(country.getString(PcccodeConstants.areaName));
		}


		marketingMembersService.update(memberDto);
		return RestResult.success("success",null);

	}
	@RequestMapping(value = "/login",method = RequestMethod.GET)
    @ApiOperation(value = "h5??????", notes = "")
    @ApiImplicitParams(value= {@ApiImplicitParam(paramType="query",value = "?????????",name="mobile"),
    		@ApiImplicitParam(paramType="query",value = "???????????????????????????????????????????????????",name="wxstate"),
    		@ApiImplicitParam(paramType="query",value = "?????????????????????????????????",name="openid"),
    		@ApiImplicitParam(paramType="query",value = "?????????????????????????????????",name="organizationId"),
			@ApiImplicitParam(paramType="query",value = "???????????????",name="verificationCode"),
			@ApiImplicitParam(paramType="query",value = "????????????:???????????????????????????",name="deviceType")
    		})
    public RestResult<H5LoginVO> login(@RequestParam String mobile,
									   @RequestParam String verificationCode,
									   @RequestParam(required=false) Integer deviceType,
									   @RequestParam(required=false) String wxstate,
									   @RequestParam(required=false) String openid,
									   @RequestParam(required=false) String organizationId,
									   HttpServletResponse response) throws Exception {
        return marketingMembersService.login(mobile,wxstate,verificationCode,openid,organizationId,deviceType,response);
    }

    @RequestMapping(value = "/register",method = RequestMethod.POST)
    @ApiOperation(value = "????????????????????????", notes = "")
    public RestResult<String> register(@Valid@RequestBody MarketingMembersAddParam marketingMembersAddParam) throws Exception {
		marketingMembersAddParam = BeanPropertyUtil.beanBlank2Null(marketingMembersAddParam, MarketingMembersAddParam.class);
		checkPhoneFormat(marketingMembersAddParam.getMobile());
        marketingMembersService.addMember(marketingMembersAddParam);
        return new RestResult<String>(200, "success",null );
    }
    
    @RequestMapping(value = "/infoImprove",method = RequestMethod.POST)
    @ApiOperation(value = "h5????????????", notes = "")
    public RestResult<String> infoImprove(@Valid@RequestBody MarketingMembersUpdateParam marketingMembersUpdateParam) throws Exception {
		marketingMembersUpdateParam = BeanPropertyUtil.beanBlank2Null(marketingMembersUpdateParam, MarketingMembersUpdateParam.class);
		marketingMembersService.updateMembers(marketingMembersUpdateParam);
        return new RestResult<String>(200, "??????", null);
    }
    
    @RequestMapping(value = "/getJwtToken",method = RequestMethod.GET)
    @ApiOperation(value = "??????jwt-token", notes = "")
    @ApiImplicitParams(value= {
    		@ApiImplicitParam(name = "memberId", paramType = "query", defaultValue = "1", value = "??????id")
    })
    public void getJwtToken(@RequestParam Long memberId, Byte memberType) throws Exception {
    	try {
            MemberWithWechat memberWithWechat = null;
    		if (memberType != null && memberType.intValue() == MemberTypeEnums.SALER.getType().intValue()) {
				UserWithWechat userWithWechat = marketingSaleMemberService.selectById(memberId);
                memberWithWechat = modelMapper.map(userWithWechat, MemberWithWechat.class);
			} else {
                memberWithWechat = marketingMembersService.selectById(memberId);
			}
			if (null == memberWithWechat) {
				throw new SuperCodeException("????????????", 500);
			}
			H5LoginVO hVo=new H5LoginVO();
			hVo.setMemberId(memberId);
			String userName=memberWithWechat.getUserName();
			hVo.setMemberName(userName==null?memberWithWechat.getWxName():userName);
			hVo.setMobile(memberWithWechat.getMobile());
			hVo.setRegistered(1);
			hVo.setMemberType(memberWithWechat.getMemberType());
			String orgnazationName="";
			try {
				orgnazationName=commonService.getOrgNameByOrgId(memberWithWechat.getOrganizationId());
			} catch (Exception e) {
				e.printStackTrace();
			}
			hVo.setOrganizationId(memberWithWechat.getOrganizationId());
			hVo.setOrganizationName(orgnazationName);
			hVo.setHaveIntegral(memberWithWechat.getHaveIntegral());
			hVo.setWechatHeadImgUrl(memberWithWechat.getWechatHeadImgUrl());
			hVo.setCustomerId(memberWithWechat.getCustomerId());
			hVo.setCustomerName(memberWithWechat.getCustomerName());
			String jwtToken=JWTUtil.createTokenWithClaim(hVo);
			Cookie jwtTokenCookie = new Cookie(CommonConstants.JWT_TOKEN,jwtToken);
			// jwt????????????2?????????????????????
			jwtTokenCookie.setMaxAge(60*60*2);
			// ???????????? ??????????????????????????????
			jwtTokenCookie.setPath("/");
			jwtTokenCookie.setDomain(cookieDomain);
			response.addCookie(jwtTokenCookie);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    @RequestMapping(value = "/userInfo",method = RequestMethod.GET)
    @ApiOperation(value = "??????????????????????????????", notes = "")
    @ApiImplicitParams(value= {
    		@ApiImplicitParam(paramType="query",value = "????????????",name="id"),
    		@ApiImplicitParam(name = "jwt-token", paramType = "header", defaultValue = "ldpfbsujjknla;s.lasufuafpioquw949gyobrljaugf89iweubjkrlnkqsufi.awi2f7ygihuoquiu", value = "jwt-token??????")
    })
    public RestResult<H5LoginVO> userInfo(@RequestParam Long id,@ApiIgnore H5LoginVO h5LoginVO) throws Exception {
    	RestResult<H5LoginVO> restResult=new RestResult<H5LoginVO>();
    	try {
    		Long memberId=h5LoginVO.getMemberId();
			if (id.intValue()!=memberId.intValue()) {
				restResult.setState(500);
				restResult.setMsg("?????????????????????id??????????????????????????????");
				return restResult;
			}

            MemberWithWechat memberWithWechat = marketingMembersService.selectById(id);
			if (null == memberWithWechat) {
				restResult.setState(500);
				restResult.setMsg("??????id="+id+"?????????????????????");
				return restResult;
			}
			
			H5LoginVO hVo=new H5LoginVO();
			hVo.setMemberId(id);
			String userName=memberWithWechat.getUserName();
			hVo.setMemberName(userName==null?memberWithWechat.getWxName():userName);
			hVo.setMobile(memberWithWechat.getMobile());
			hVo.setRegistered(1);
			String orgnazationName="";
			try {
				orgnazationName=commonService.getOrgNameByOrgId(memberWithWechat.getOrganizationId());
			} catch (Exception e) {
				e.printStackTrace();
			}
			hVo.setOrganizationName(orgnazationName);
			hVo.setHaveIntegral(memberWithWechat.getHaveIntegral());
			hVo.setWechatHeadImgUrl(memberWithWechat.getWechatHeadImgUrl());
			hVo.setCustomerId(memberWithWechat.getCustomerId());
			hVo.setCustomerName(memberWithWechat.getCustomerName());
			restResult.setState(200);
			restResult.setResults(hVo);
		} catch (Exception e) {
			restResult.setState(500);
			restResult.setMsg("???????????????????????????"+e.getMessage());
		}
        return restResult;
    }
}
