package com.jgw.supercodeplatform.marketing.common.model.activity;

public class ScanCodeInfoMO {
	private String codeId;//外码,跳转到营销扫码接口时获取
	private String codeTypeId;//码值id,跳转到营销扫码接口时获取
	private String productId;//产品id,跳转到营销扫码接口时获取
	private String productBatchId;//码平台传的产品批次id,跳转到营销扫码接口时获取
    private String openId;//当前扫码用户openid授权接口获取
    private Long activitySetId;//当前扫码的码参与的活动设置id,跳转到营销扫码接口时获取
    private String organizationId;//当前扫码所属企业id,跳转到营销扫码接口时获取
    
	public String getCodeId() {
		return codeId;
	}

	public void setCodeId(String codeId) {
		this.codeId = codeId;
	}

	public String getCodeTypeId() {
		return codeTypeId;
	}

	public void setCodeTypeId(String codeTypeId) {
		this.codeTypeId = codeTypeId;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductBatchId() {
		return productBatchId;
	}

	public void setProductBatchId(String productBatchId) {
		this.productBatchId = productBatchId;
	}

	public String getOpenId() {
		return openId;
	}

	public void setOpenId(String openId) {
		this.openId = openId;
	}

	public Long getActivitySetId() {
		return activitySetId;
	}

	public void setActivitySetId(Long activitySetId) {
		this.activitySetId = activitySetId;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

}
