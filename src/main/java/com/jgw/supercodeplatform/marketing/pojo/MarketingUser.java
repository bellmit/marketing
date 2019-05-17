package com.jgw.supercodeplatform.marketing.pojo;

import java.util.Date;

public class MarketingUser {
    /** 序号 */
    private Long id;

    /** 微信昵称 */
    private String wxName;

    /** 微信id号 */
    private String openid;

    /** 手机 */
    private String mobile;

    /** 用户Id */
    private String userId;

    /** 用户姓名 */
    private String userName;

    /** 性别1女0男 */
    private String sex;

    /** 生日 */
    private Date birthday;

    /** 省编码 */
    private String provinceCode;

    /** 县编码 */
    private String countyCode;

    /** 市编码 */
    private String cityCode;

    /** 省名称 */
    private String provinceName;

    /** 县名称 */
    private String countyName;

    /** 市名称 */
    private String cityName;

    /** 组织Id */
    private String organizationId;

    /** 建立日期 */
    private Date createDate;

    /** 修改日期 */
    private Date updateDate;

    /** 门店名称 */
    private String customerName;

    /** 门店编码 */
    private String customerId;

    /** 省市区前端编码 */
    private String pCCcode;

    /** 微信头像/个人头像 */
    private String wechatHeadImgUrl;

    /** 默认1导购员,其他员工等 */
    private Byte memberType;

    /** 用户状态(1、 表示正常，0 表示下线)导购员状态 */
    private Byte state;

    /** 扫码设备类型 */
    private Byte deviceType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWxName() {
        return wxName;
    }

    public void setWxName(String wxName) {
        this.wxName = wxName;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getCountyCode() {
        return countyCode;
    }

    public void setCountyCode(String countyCode) {
        this.countyCode = countyCode;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getpCCcode() {
        return pCCcode;
    }

    public void setpCCcode(String pCCcode) {
        this.pCCcode = pCCcode;
    }

    public String getWechatHeadImgUrl() {
        return wechatHeadImgUrl;
    }

    public void setWechatHeadImgUrl(String wechatHeadImgUrl) {
        this.wechatHeadImgUrl = wechatHeadImgUrl;
    }

    public Byte getMemberType() {
        return memberType;
    }

    public void setMemberType(Byte memberType) {
        this.memberType = memberType;
    }

    public Byte getState() {
        return state;
    }

    public void setState(Byte state) {
        this.state = state;
    }

    public Byte getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Byte deviceType) {
        this.deviceType = deviceType;
    }
}