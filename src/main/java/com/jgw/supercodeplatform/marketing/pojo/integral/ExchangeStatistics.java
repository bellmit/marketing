package com.jgw.supercodeplatform.marketing.pojo.integral;

public class ExchangeStatistics {
    /**  */
    private Long id;

    /**  */
    private String organizationId;

    /** ��֯id */
    private String productId;

    /** ��Ʒid */
    private Long memberId;

    /** �û�id */
    private Integer exchangeNum;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Integer getExchangeNum() {
        return exchangeNum;
    }

    public void setExchangeNum(Integer exchangeNum) {
        this.exchangeNum = exchangeNum;
    }
}