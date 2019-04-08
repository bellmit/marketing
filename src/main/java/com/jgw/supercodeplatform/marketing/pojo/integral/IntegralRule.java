package com.jgw.supercodeplatform.marketing.pojo.integral;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.List;

@ApiModel(value = "通用积分规则")
public class IntegralRule {
    /** 积分活动主表 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 积分有效期状态位0永久有效1存在有效期 */
    @ApiModelProperty(value = "积分有效期状态位0永久有效1存在有效期")
    private Byte timeLimitStatus;

    /** 从奖励开始多久后过期 */
    @ApiModelProperty(value = "从奖励开始多久后过期")
    private Date timeLimitDate;

    /** 积分上限状态位0无上限，1每人每日最多获得的积分上限目前0有效 */
    @ApiModelProperty(value = "积分上限状态位0无上限，1每人每日最多获得的积分上限目前0有效")
    private Boolean integralLimitStatus;

    /** 每人每日最多获得的积分上限 */
    @ApiModelProperty(value = "每人每日最多获得的积分上限")
    private Integer integralLimit;

    /** 每人每【2年1月0日】获取积分上限 */
    @ApiModelProperty(value = "每人每【2年1月0日】获取积分上限")
    private Byte integralLimitAge;

    /** 额外送:注册 */
    @ApiModelProperty(value = "额外送:注册")
    private Integer integralByRegister;

    /** 额外送:生日 */
    @ApiModelProperty(value = "额外送:生日")
    private Integer integralByBirthday;

    /** 历史首次 */
    @ApiModelProperty(value = "历史首次")
    private Integer integralByFirstTime;

    /** 额外送注册状态：0勾选有效1无效 */
    @ApiModelProperty(value = "额外送注册状态：0勾选有效1无效")
    private Byte integralByRegisterStatus;

    /** 额外送生日状态：0勾选有效1无效 */
    @ApiModelProperty(value = "额外送生日状态：0勾选有效1无效")
    private Byte integralByBirthdayStatus;

    /** 额外历史首次送状态：0勾选有效1无效 */
    @ApiModelProperty(value = "额外历史首次送状态：0勾选有效1无效")
    private Byte integralByFirstTimeStatus;


    /** 组织id */
    @ApiModelProperty(value = "组织id")
    private String organizationId;

    /** 组织名称 */
    @ApiModelProperty(value = "组织名称")
    private String organizationName;

    /** 积分通用规则0有效1无效 */
    @ApiModelProperty(value = " 积分通用规则0有效1无效 ")
    private Byte isEffective;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Byte getTimeLimitStatus() {
        return timeLimitStatus;
    }

    public void setTimeLimitStatus(Byte timeLimitStatus) {
        this.timeLimitStatus = timeLimitStatus;
    }

    public Date getTimeLimitDate() {
        return timeLimitDate;
    }

    public void setTimeLimitDate(Date timeLimitDate) {
        this.timeLimitDate = timeLimitDate;
    }

    public Boolean getIntegralLimitStatus() {
        return integralLimitStatus;
    }

    public void setIntegralLimitStatus(Boolean integralLimitStatus) {
        this.integralLimitStatus = integralLimitStatus;
    }

    public Integer getIntegralLimit() {
        return integralLimit;
    }

    public void setIntegralLimit(Integer integralLimit) {
        this.integralLimit = integralLimit;
    }

    public Byte getIntegralLimitAge() {
        return integralLimitAge;
    }

    public void setIntegralLimitAge(Byte integralLimitAge) {
        this.integralLimitAge = integralLimitAge;
    }

    public Integer getIntegralByRegister() {
        return integralByRegister;
    }

    public void setIntegralByRegister(Integer integralByRegister) {
        this.integralByRegister = integralByRegister;
    }

    public Integer getIntegralByBirthday() {
        return integralByBirthday;
    }

    public void setIntegralByBirthday(Integer integralByBirthday) {
        this.integralByBirthday = integralByBirthday;
    }

    public Integer getIntegralByFirstTime() {
        return integralByFirstTime;
    }

    public void setIntegralByFirstTime(Integer integralByFirstTime) {
        this.integralByFirstTime = integralByFirstTime;
    }

    public Byte getIntegralByRegisterStatus() {
        return integralByRegisterStatus;
    }

    public void setIntegralByRegisterStatus(Byte integralByRegisterStatus) {
        this.integralByRegisterStatus = integralByRegisterStatus;
    }

    public Byte getIntegralByBirthdayStatus() {
        return integralByBirthdayStatus;
    }

    public void setIntegralByBirthdayStatus(Byte integralByBirthdayStatus) {
        this.integralByBirthdayStatus = integralByBirthdayStatus;
    }

    public Byte getIntegralByFirstTimeStatus() {
        return integralByFirstTimeStatus;
    }

    public void setIntegralByFirstTimeStatus(Byte integralByFirstTimeStatus) {
        this.integralByFirstTimeStatus = integralByFirstTimeStatus;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public Byte getIsEffective() {
        return isEffective;
    }

    public void setIsEffective(Byte isEffective) {
        this.isEffective = isEffective;
    }
}