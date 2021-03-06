package com.jgw.supercodeplatform.marketing.service.user;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.exception.SuperCodeExtException;
import com.jgw.supercodeplatform.marketing.common.constants.MechanismTypeConstants;
import com.jgw.supercodeplatform.marketing.common.constants.PcccodeConstants;
import com.jgw.supercodeplatform.marketing.common.constants.StateConstants;
import com.jgw.supercodeplatform.marketing.common.constants.UserSourceConstants;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.page.AbstractPageService;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingUserMapperExt;
import com.jgw.supercodeplatform.marketing.dao.activity.generator.mapper.MarketingUserMapper;
import com.jgw.supercodeplatform.marketing.dao.user.MarketingWxMemberMapper;
import com.jgw.supercodeplatform.marketing.dao.weixin.MarketingWxMerchantsMapper;
import com.jgw.supercodeplatform.marketing.dto.MarketingSaleMembersAddParam;
import com.jgw.supercodeplatform.marketing.dto.MarketingSaleMembersUpdateParam;
import com.jgw.supercodeplatform.marketing.dto.SaleMemberBatchStatusParam;
import com.jgw.supercodeplatform.marketing.dto.SalerLoginParam;
import com.jgw.supercodeplatform.marketing.dto.members.MarketingMembersListParam;
import com.jgw.supercodeplatform.marketing.enums.market.MemberTypeEnums;
import com.jgw.supercodeplatform.marketing.enums.market.SaleUserStatus;
import com.jgw.supercodeplatform.marketing.pojo.MarketingUser;
import com.jgw.supercodeplatform.marketing.pojo.MarketingWxMember;
import com.jgw.supercodeplatform.marketing.pojo.MarketingWxMerchants;
import com.jgw.supercodeplatform.marketing.pojo.UserWithWechat;
import com.jgw.supercodeplatform.marketing.service.common.CommonService;
import com.jgw.supercodeplatform.marketingsaler.outservicegroup.dto.CustomerInfoView;
import com.jgw.supercodeplatform.marketingsaler.outservicegroup.feigns.BaseCustomerFeignService;
import com.jgw.supercodeplatform.marketingsaler.outservicegroup.feigns.CustomerIdDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
 import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MarketingSaleMemberService extends AbstractPageService<MarketingMembersListParam> {
	/**
	 * ????????????URI
	 */
	@Value("#/sales/index?organizationId=")
	private  String WEB_SALER_CENTER_URI_FOR_SHORT_MSG ;


	@Value("${marketing.activity.h5page.url}")
	private  String WEB_SALER_CENTER_DOMAIN;

	@Autowired
	private BaseCustomerFeignService baseCustomerFeignService;


 	@Value("?????????{{user}}????????????????????????????????????????????????{{url}}")
	private String SHORT_MSG ;
	@Autowired
	private MarketingUserMapperExt mapper;

	@Autowired
	private ModelMapper modelMapper;


	@Autowired
	private CommonService commonService;

	@Autowired
	private MarketingMembersService membersService;

	@Autowired
	private MarketingWxMerchantsMapper mWxMerchantsMapper;

	@Autowired
	private MarketingWxMemberMapper marketingWxMemberMapper;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private MarketingUserMapper marketingUserMapper;

	@Override
	protected List<MarketingUser> searchResult(MarketingMembersListParam searchParams) throws SuperCodeException{
		if(StringUtils.isBlank(searchParams.getOrganizationId())){
			throw new SuperCodeException("????????????...");
		}
		return mapper.list(searchParams);
	}

	@Override
	protected int count(MarketingMembersListParam searchParams) throws SuperCodeException{
		if(StringUtils.isBlank(searchParams.getOrganizationId())){
			throw new SuperCodeException("????????????...");
		}
		Integer count=mapper.count(searchParams);
		return count;
	}



	/**
	 * ????????????????????????
	 * @param
	 * @param
	 * @param organizationId
	 * @throws SuperCodeException
	 */
	public void updateMembersBatchStatus(SaleMemberBatchStatusParam ids, String organizationId) throws SuperCodeException {
		if(ids == null ){
			throw new SuperCodeException("???????????????");
		}
		if(StringUtils.isBlank(organizationId)){
			throw new SuperCodeException("?????????????????????");
		}
		if(CollectionUtils.isEmpty(ids.getIds())){
			throw new SuperCodeException("id?????????...");
		}
		if(ids.getState() > SaleUserStatus.Max.getStatus().intValue() || ids.getState() < SaleUserStatus.Min.getStatus().intValue()){
			throw new SuperCodeException("???????????????...");

		}

		mapper.updateBatch(ids, organizationId);
	}


	/**
	 * ??????????????????
	 * @param id
	 * @param state
	 * @param organizationId
	 * @throws SuperCodeException
	 */
	public void updateMembersStatus(Long id, int state, String organizationId) throws SuperCodeException{
		if(StringUtils.isBlank(organizationId)){
			throw new SuperCodeException("?????????????????????");
		}
		if(id == null || id<=0){
			throw new SuperCodeException("id?????????...");
		}
		if(state > SaleUserStatus.Max.getStatus().intValue() || state < SaleUserStatus.Min.getStatus().intValue()){
			throw new SuperCodeException("???????????????...");

		}

		MarketingUser marketingUser = mapper.selectByPrimaryKey(id);
		if(!organizationId.equals(marketingUser.getOrganizationId())){
			throw new SuperCodeException("????????????...");
		}
		// ?????????????????????
		if(marketingUser.getState().intValue() == SaleUserStatus.AUDITED.getStatus().intValue()
				&& state == SaleUserStatus.ENABLE.getStatus().intValue()  ){
			String msg = msgTimplate(marketingUser.getUserName()==null ? "???":marketingUser.getUserName()
					, WEB_SALER_CENTER_DOMAIN+WEB_SALER_CENTER_URI_FOR_SHORT_MSG ,organizationId);
			try {
				checkPhoneFormat(marketingUser.getMobile());

				membersService.sendRegisterMessage(marketingUser.getMobile(),msg);
			} catch (SuperCodeException e) {
				e.printStackTrace();
				log.error("?????????????????????????????????:??????:{},??????:{}",marketingUser.getMobile(),msg);
			}
		}
		MarketingUser dto = new MarketingUser();
		dto.setId(id);
		dto.setState((byte)state);
		mapper.updateByPrimaryKeySelective(dto);

	}



	/**
	 * ????????????????????????
	 * @param userName
	 * @param url
	 * @return
	 */
	private String msgTimplate(String userName, String url,String organizationId) {
		url = url+organizationId;
		return  SHORT_MSG.replace("{{user}}",userName).replace("{{url}}",url);

	}

	public void deleteSalerByOrg(Long id, String organizationId) throws SuperCodeException{
		if(StringUtils.isBlank(organizationId)){
			throw new SuperCodeException("?????????????????????");
		}
		if(id == null || id<=0){
			throw new SuperCodeException("id?????????...");
		}
		MarketingUser marketingUser = mapper.selectByPrimaryKey(id);
		if(marketingUser != null && organizationId.equals( marketingUser.getOrganizationId())){
			int i = mapper.deleteByPrimaryKey(id);
			if(i != 1){
				throw  new SuperCodeException("????????????...");
			}
		}else {
			throw  new SuperCodeException("????????????...");
		}

	}

	public MarketingUser getMemberById(Long id) throws SuperCodeException{
		// ???????????????
		if(id == null || id <= 0){
			throw new SuperCodeException("id????????????...");
		}
		MarketingUser marketingUser = mapper.selectByPrimaryKey(id);
		return  marketingUser;
	}



	public void updateMembers(MarketingSaleMembersUpdateParam marketingMembersUpdateParam, String organizationId) throws SuperCodeException{
		// ????????????
		if(marketingMembersUpdateParam == null){
			throw new SuperCodeException("?????????????????????...");
		}
		if(marketingMembersUpdateParam.getId() == null ){
			throw new SuperCodeException("id?????????...");

		}
		if(StringUtils.isBlank(organizationId)){
			throw new SuperCodeException("???????????????...");
		}
		if(!StringUtils.isEmpty(marketingMembersUpdateParam.getMobile())){
			checkPhoneFormat(marketingMembersUpdateParam.getMobile());
			MarketingUser marketingUser = mapper.selectByPhone(marketingMembersUpdateParam.getMobile());
			if(marketingUser != null && marketingUser.getId()!=null){
				Long id = marketingUser.getId().longValue();
				if(marketingMembersUpdateParam.getId().longValue() != id ){
					throw new RuntimeException("?????????????????????");
				}
			}
		}

//		if(StringUtils.isBlank(marketingMembersUpdateParam.getCustomerId())){
//			throw new SuperCodeException("??????id?????????...");
//
//		}
//		if(StringUtils.isBlank(marketingMembersUpdateParam.getCustomerName())){
//			throw new SuperCodeException("?????????????????????...");
//
//		}

		// ????????????
		MarketingUser marketingUser = mapper.selectByPrimaryKey(marketingMembersUpdateParam.getId());
		if(marketingUser == null || !organizationId.equals(marketingUser.getOrganizationId())){
			throw new SuperCodeException("????????????...");
		}
		if(StringUtils.isBlank(organizationId)){
			throw new SuperCodeException("????????????????????????...");
		}

		MarketingUser dto = modelMapper.map(marketingMembersUpdateParam,MarketingUser.class);


		// ????????????
		// ????????????
//		List<CustomerInfo> customers = marketingMembersUpdateParam.getCustomer();
//		if(!CollectionUtils.isEmpty(customers)){
//			StringBuffer customerIds =new StringBuffer("");
//			StringBuffer customerNames =new StringBuffer("");
//			for(CustomerInfo customer: customers){
//				if(StringUtils.isEmpty(customer.getCustomerId()) || (StringUtils.isEmpty(customer.getCustomerName()))){
//					throw new SuperCodeException("??????????????????...");
//				}
//				customerIds.append(",").append(customer.getCustomerId());
//				customerNames.append(",").append(customer.getCustomerName());
//			}
//			// ?????????????????????

//		}

		// ??????????????????

		String pcccode = marketingMembersUpdateParam.getPCCcode();
		List<JSONObject> objects = JSONObject.parseArray(pcccode,JSONObject.class);
		int size = objects.size();
		JSONObject province = size > 0 ? objects.get(0)  : new JSONObject()  ;
		JSONObject city = size > 1  ? objects.get(1) : new JSONObject() ;
		JSONObject country = size > 2 ? objects.get(2) : new JSONObject();
		// ???????????????

		dto.setProvinceCode(province.getString(PcccodeConstants.areaCode));
		dto.setCityCode(city.getString(PcccodeConstants.areaCode));
		dto.setCountyCode(country.getString(PcccodeConstants.areaCode));
		dto.setProvinceName(province.getString(PcccodeConstants.areaName));
		dto.setCityName(city.getString(PcccodeConstants.areaName));
		dto.setCountyName(country.getString(PcccodeConstants.areaName));
		dto.setpCCcode(pcccode);
		dto.setMechanismType(marketingMembersUpdateParam.getMechanismType());
		dto.setCustomerId(marketingMembersUpdateParam.getCustomerId());
		dto.setCustomerName(marketingMembersUpdateParam.getCustomerName());
		// ???????????? pcccode???????????????????????????????????????
		int i = mapper.updateByPrimaryKeySelectiveWithBiz(dto);
		if(i!=1){
			throw new SuperCodeException("????????????...");
		}
	}


	public MarketingUser selectBylogin(SalerLoginParam loginUser) throws SuperCodeException{

		if(StringUtils.isBlank(loginUser.getMobile())){
			throw new SuperCodeException("???????????????");
		}
		if(StringUtils.isBlank(loginUser.getOrganizationId())){
			throw new SuperCodeException("???????????????");
		}
		if(StringUtils.isBlank(loginUser.getVerificationCode())){
			throw new SuperCodeException("??????????????????");
		}
		boolean success = commonService.validateMobileCode(loginUser.getMobile(), loginUser.getVerificationCode());
		if(!success){
			throw new SuperCodeException("?????????????????????");
		}
		MarketingUser marketingUser = mapper.selectByPhone(loginUser.getMobile());
		if(marketingUser == null){
			throw new SuperCodeException("???????????????");

		}
		if(!loginUser.getOrganizationId().equals(marketingUser.getOrganizationId())){
			throw new SuperCodeException("??????????????????");

		}
		// ?????????????????????????????? ????????????
		return  marketingUser;
	}

	/**
	 * ??????????????????????????????
	 * @param userInfo
	 * @return
	 * @throws SuperCodeException
	 */
	@Transactional(rollbackFor = Exception.class)
	public UserWithWechat saveRegisterUser(MarketingSaleMembersAddParam userInfo) throws SuperCodeException{
		// 1????????????
		validateBasicByRegisterUser(userInfo);

		// 2.1????????????????????????
		boolean success = commonService.validateMobileCode(userInfo.getMobile(), userInfo.getVerificationCode());
		if(!success){
			throw new SuperCodeException("?????????????????????...");
		}
		// 2.1????????????: ????????????????????????,???????????????????????????
		MarketingUser userDto = mapper.selectByPhone(userInfo.getMobile());
		if(userDto != null){
			throw new SuperCodeException("??????????????????...");
		}

		// 3?????????????????????
		UserWithWechat userDo = changeToDo(userInfo);
		if (StringUtils.isNotBlank(userDo.getMobile())) {
			MarketingUser marketingUser = new MarketingUser();
			BeanUtils.copyProperties(userDo, marketingUser);
			marketingUser.setMechanismType(userInfo.getMechanismType() ==null ?null:userInfo.getMechanismType().byteValue());
			marketingUser.setSource(SourceType.H5);
//			setMechanismType(userInfo, marketingUser);
			mapper.insertSelective(marketingUser);
		}

		return userDo;

	}

	/**
	 * ?????????????????????
	 * @param userInfo
	 * @param marketingUser
	 */
	private void setMechanismType(MarketingSaleMembersAddParam userInfo, MarketingUser marketingUser) {
		RestResult<CustomerInfoView> customerInfo = baseCustomerFeignService.getCustomerInfo(new CustomerIdDto(userInfo.getCustomerId()));
		if(customerInfo != null && customerInfo.getState() == 200 && customerInfo.getResults() != null){
			Integer customerType = customerInfo.getResults().getCustomerType();
			marketingUser.setMechanismType(customerType == null ? null : customerType.byteValue());
		}
	}

	/**
	 * ???????????????????????????????????????
	 * @param userInfo
	 * @throws SuperCodeException
	 */
	private void validateBasicByRegisterUser(MarketingSaleMembersAddParam userInfo) throws SuperCodeException{
		if(userInfo == null){
			throw new SuperCodeException("??????????????????001...");
		}

		if(StringUtils.isBlank(userInfo.getOrganizationId())){
			throw new SuperCodeException("????????????????????????...");
		}
		if(StringUtils.isBlank(userInfo.getMobile())){
			throw new SuperCodeException("??????????????????...");
		}
		if(StringUtils.isBlank(userInfo.getUserName())){
			throw new SuperCodeException("??????????????????...");
		}
		if(StringUtils.isBlank(userInfo.getVerificationCode())){
			throw new SuperCodeException("??????????????????...");
		}
		if(StringUtils.isBlank(userInfo.getpCCcode())){
			throw new SuperCodeException("????????????????????????...");
		}
		// ??????????????????:??????
		if(StringUtils.isBlank(userInfo.getCustomerId())){
			throw new SuperCodeException("???????????????ID??????...");
		}

		if(StringUtils.isBlank(userInfo.getCustomerName())){
			throw new SuperCodeException("???????????????????????????...");
		}


	}

	/**
	 * ????????????????????????
	 * 	vo	to do
	 * @param userInfo
	 * @return
	 */
	private UserWithWechat changeToDo(MarketingSaleMembersAddParam userInfo) throws SuperCodeException {
		UserWithWechat userDtoToDb = modelMapper.map(userInfo,UserWithWechat.class);

		// ??????????????????
//		List<CustomerInfo> customers = userInfo.getCustomer();
//		if(!CollectionUtils.isEmpty(customers)){
//			StringBuffer ids = new StringBuffer();
//			StringBuffer names = new StringBuffer();
//			int i = 0;
//			for(CustomerInfo  customer:customers){
//				if(StringUtils.isEmpty(customer.getCustomerId()) || (StringUtils.isEmpty(customer.getCustomerName()))){
//					throw new SuperCodeException("??????????????????...");
//				}
//				i++;
//				if(i == customers.size()){
//					ids.append(customer.getCustomerId());
//					names.append(customer.getCustomerName());
//				}else {
//					ids.append(customer.getCustomerId()).append(",");
//					names.append(customer.getCustomerName()).append(",");
//
//				}
//			}
//			userDtoToDb.setCustomerId(ids.toString());
//			userDtoToDb.setCustomerName(names.toString());
//		}

		// pcccode??????
		// ???????????????
		String pcccode = userInfo.getpCCcode();
		List<JSONObject> objects = JSONObject.parseArray(pcccode,JSONObject.class);
		int size = objects.size();
		JSONObject province = size > 0 ? objects.get(0)  : new JSONObject()  ;
		JSONObject city = size > 1  ? objects.get(1) : new JSONObject() ;
		JSONObject country = size > 2 ? objects.get(2) : new JSONObject();
		userDtoToDb.setProvinceCode(province.getString(PcccodeConstants.areaCode));
		userDtoToDb.setCityCode(city.getString(PcccodeConstants.areaCode));
		userDtoToDb.setCountyCode(country.getString(PcccodeConstants.areaCode));
		userDtoToDb.setProvinceName(province.getString(PcccodeConstants.areaName));
		userDtoToDb.setCityName(city.getString(PcccodeConstants.areaName));
		userDtoToDb.setCountyName(country.getString(PcccodeConstants.areaName));
		// ?????????
		userDtoToDb.setMemberType(MemberTypeEnums.SALER.getType());
		// USER ID
		userDtoToDb.setUserId(UUID.randomUUID().toString().replaceAll("-",""));
		if(!StringUtils.isBlank(userInfo.getOpenId())){
			userDtoToDb.setOpenid(userInfo.getOpenId());
		}
		return userDtoToDb;
	}

	public void addUserOpenId(MarketingUser user, String openid) {
		String organizationId = user.getOrganizationId();
		QueryWrapper<MarketingWxMember> queryWrapper = Wrappers.query();
		queryWrapper.eq("MemberId",user.getId()).eq("CurrentUse", 1).eq("MemberType", MemberTypeEnums.SALER.getType());
		MarketingWxMember addMarketingWxMember = new MarketingWxMember();
		MarketingWxMember marketingWxMember = marketingWxMemberMapper.selectOne(queryWrapper);
		if (marketingWxMember != null) {
			UpdateWrapper nouseUpdateWrapper = Wrappers.<MarketingWxMember>update().set("CurrentUse",(byte)0).eq("OrganizationId", organizationId).eq("CurrentUse", (byte)1).eq("MemberType", MemberTypeEnums.SALER.getType());
			marketingWxMemberMapper.update(null, nouseUpdateWrapper);
			BeanUtils.copyProperties(marketingWxMember, addMarketingWxMember);
		}
		MarketingWxMerchants marketingWxMerchants = mWxMerchantsMapper.get(user.getOrganizationId());
		String organizationName = marketingWxMerchants.getOrganizatioIdlName();
		if (marketingWxMerchants.getMerchantType() == (byte)1) {
			organizationId = marketingWxMerchants.getOrganizationId();
			if (marketingWxMerchants.getJgwId() != null) {
				marketingWxMerchants = mWxMerchantsMapper.getJgw(marketingWxMerchants.getJgwId());
			} else {
				marketingWxMerchants = mWxMerchantsMapper.getDefaultJgw();
			}
			addMarketingWxMember.setJgwType((byte)1);
		} else {
			addMarketingWxMember.setJgwType((byte)0);
		}
		addMarketingWxMember.setOrganizationId(organizationId);
		addMarketingWxMember.setOrganizationFullName(organizationName);
		addMarketingWxMember.setAppid(marketingWxMerchants.getMchAppid());
		addMarketingWxMember.setOpenid(openid);
		addMarketingWxMember.setCreateTime(new Date());
		addMarketingWxMember.setUpdateTime(new Date());
		addMarketingWxMember.setMemberId(user.getId());
		addMarketingWxMember.setMemberType(MemberTypeEnums.SALER.getType());
		addMarketingWxMember.setCurrentUse((byte)1);
		marketingWxMemberMapper.insert(addMarketingWxMember);
	}


//	public MarketingUser selectByOpenid(String openid) throws SuperCodeException {
//		if(openid == null){
//			throw new SuperCodeException("??????????????????");
//		}
//		return mapper.selectByOpenid(openid);
//
//
//	}

	@Transactional
	public UserWithWechat selectByOpenidAndOrgId(String openid, String organizationId) {
		MarketingWxMember marketingWxMember = marketingWxMemberMapper.selectOne(Wrappers.<MarketingWxMember>query().eq("Openid", openid).eq("OrganizationId", organizationId).eq("MemberType", MemberTypeEnums.SALER.getType()));
		if (marketingWxMember == null) {
			return null;
		}
		if(marketingWxMember.getCurrentUse() == 0) {
			UpdateWrapper nouseUpdateWrapper = Wrappers.<MarketingWxMember>update().set("CurrentUse",(byte)0).eq("OrganizationId", organizationId).eq("CurrentUse", (byte)1).eq("MemberType", MemberTypeEnums.SALER.getType());
			marketingWxMemberMapper.update(null, nouseUpdateWrapper);
			UpdateWrapper currentUpdateWrapper = Wrappers.<MarketingWxMember>update().set("CurrentUse",(byte)1).eq("Openid", openid).eq("OrganizationId", organizationId).eq("MemberType", MemberTypeEnums.SALER.getType());
			marketingWxMemberMapper.update(null, currentUpdateWrapper);
		}
		UserWithWechat userWithWechat = new UserWithWechat();
		MarketingUser marketingUser = mapper.selectByPrimaryKey(marketingWxMember.getMemberId());
		BeanUtils.copyProperties(marketingUser, userWithWechat);
		BeanUtils.copyProperties(marketingWxMember, userWithWechat);
		return userWithWechat;
	}

	public UserWithWechat selectById(Long memberId) {
		MarketingUser marketingUser = mapper.selectByPrimaryKey(memberId);
		if (marketingUser == null) {
			return null;
		}
		MarketingWxMember marketingWxMember = marketingWxMemberMapper.selectOne(Wrappers.<MarketingWxMember>query().eq("MemberId",memberId).eq("CurrentUse", 1).eq("MemberType", MemberTypeEnums.SALER.getType()));
		UserWithWechat userWithWechat = new UserWithWechat();
		BeanUtils.copyProperties(marketingUser, userWithWechat);
		userWithWechat.setMemberId(marketingUser.getId());
		if (marketingWxMember != null) {
			userWithWechat.setWxName(marketingWxMember.getWxName());
			userWithWechat.setOpenid(marketingWxMember.getOpenid());
			userWithWechat.setWechatHeadImgUrl(marketingWxMember.getWechatHeadImgUrl());
		}
		return userWithWechat;
	}

	/**
	 * ??????openid ????????????
	 * @param marketingWxMember
	 * @return
	 */
	public int updateWxInfo(MarketingWxMember marketingWxMember) {
		if (marketingWxMember == null) {
			throw new SuperCodeExtException("??????????????????????????????");
		}
		String openid = marketingWxMember.getOpenid();
		if (StringUtils.isBlank(openid)){
			throw new SuperCodeExtException("openid????????????");
		}
		String organizationId = marketingWxMember.getOrganizationId();
		if (StringUtils.isBlank(organizationId)) {
			throw new SuperCodeExtException("??????ID????????????");
		}
		UpdateWrapper<MarketingWxMember> updateWrapper = Wrappers.<MarketingWxMember>update().eq("Openid", openid).eq("OrganizationId", organizationId).eq("MemberType", MemberTypeEnums.SALER.getType());
		return marketingWxMemberMapper.update(marketingWxMember, updateWrapper);
	}

	/**
	 * ???????????????
	 */
	public List<MarketingUser> getSalerInfoList() throws SuperCodeException {
		String organoizationId= commonUtil.getOrganizationId();
		if(StringUtils.isBlank(organoizationId)){
			throw new SuperCodeException("???????????????...");
		}
		QueryWrapper queryWrapper=new QueryWrapper();
		queryWrapper.eq("OrganizationId",organoizationId);
		//??????0?????????1?????????,???????????????
		queryWrapper.eq("MemberType",1);
		List<MarketingUser> list= marketingUserMapper.selectList(queryWrapper);
		if (list == null){
			throw new SuperCodeException("????????????????????????");
		}
		list.forEach(user->{
			String p = user.getProvinceName()== null ?"":user.getCountyName();
			String city = user.getCityName()== null ?"":user.getCountyName();
			String c =user.getCountyName() == null? "":user.getCountyName();
			user.setAddress(p+city+c);
		});
		return list;
	}

	public List<MarketingUser> changeList(List<MarketingUser> list){
		list.stream().filter(marketingUser -> {

			if (UserSourceConstants.H5.equals(marketingUser.getSource())){
				marketingUser.setSourceStr("H5");
			}else {
				marketingUser.setSourceStr("????????????");
			}

			if (StateConstants.TO_EXAMINE_ING.equals(marketingUser.getState())){
				marketingUser.setStateStr("??????");
			}else if (StateConstants.PROHIBIT.equals(marketingUser.getState())){
				marketingUser.setStateStr("??????");
			}else {
				marketingUser.setStateStr("??????");
			}

			if (MechanismTypeConstants.HEADQUARTER.equals(marketingUser.getMechanismType())){
				marketingUser.setMechanismTypeStr("????????????");
			}else if (MechanismTypeConstants.SUB_SIDIARY.equals(marketingUser.getMechanismType())){
				marketingUser.setMechanismTypeStr("??????");
			}else if (MechanismTypeConstants.DISTRIBUTOR.equals(marketingUser.getMechanismType())){
				marketingUser.setMechanismTypeStr("??????");
			} else {
				marketingUser.setMechanismTypeStr("??????");
			}

			return true;
		}).collect(Collectors.toList());
		return list;
	}
}



