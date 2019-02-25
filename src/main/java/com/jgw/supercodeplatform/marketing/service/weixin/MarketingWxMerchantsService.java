package com.jgw.supercodeplatform.marketing.service.weixin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.dao.weixin.MarketingWxMerchantsMapper;
import com.jgw.supercodeplatform.marketing.pojo.MarketingWxMerchants;

@Service
public class MarketingWxMerchantsService {

	@Autowired
	private CommonUtil commonUtil;
	
	@Autowired
	private MarketingWxMerchantsMapper dao;
	
	public RestResult<MarketingWxMerchants> get() throws SuperCodeException {
		RestResult<MarketingWxMerchants> restResult=new RestResult<MarketingWxMerchants>();
		String organizationId=commonUtil.getOrganizationId();
		MarketingWxMerchants merchants=dao.get(organizationId);
		restResult.setState(200);
		restResult.setMsg("成功");
		restResult.setResults(merchants);
		return restResult;
	}

}
