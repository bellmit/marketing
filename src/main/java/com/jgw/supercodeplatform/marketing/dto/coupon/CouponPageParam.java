package com.jgw.supercodeplatform.marketing.dto.coupon;

import com.jgw.supercodeplatform.marketing.common.page.DaoSearch;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("用户H5抵扣券分页查询")
public class CouponPageParam extends DaoSearch {

	@ApiModelProperty(hidden = true)
	private Long memberId;
	@ApiModelProperty("查询类型：1.未使用，2.已使用，3.已过期")
	private Integer useType;

	public Long getMemberId() {
		return memberId;
	}

	public void setMemberId(Long memberId) {
		this.memberId = memberId;
	}

	public Integer getUseType() {
		return useType;
	}

	public void setUseType(Integer useType) {
		this.useType = useType;
	}
	
}
