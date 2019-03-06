package com.jgw.supercodeplatform.marketing.pojo;
/**
 * 活动中奖奖次设置实体
 * @author czm
 *
 */
public class MarketingPrizeType {

    private Long id;//序列Id
    private Long activitySetId;//活动设置Id
    private Integer prizeAmount;//金额数量,类型跟微信接口保持一致
    private Integer prizeProbability;//中奖几率
    private String prizeTypeName;//奖品类型名称
    private Byte randomAmount;//是否随机金额 1随机 0固定
    private Long winingNum;//已中奖数
    private Byte realPrize;//是否由用户创建的真实奖次
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getActivitySetId() {
		return activitySetId;
	}
	public void setActivitySetId(Long activitySetId) {
		this.activitySetId = activitySetId;
	}
	public Integer getPrizeAmount() {
		return prizeAmount;
	}
	public void setPrizeAmount(Integer prizeAmount) {
		this.prizeAmount = prizeAmount;
	}
	public Integer getPrizeProbability() {
		return prizeProbability;
	}
	public void setPrizeProbability(Integer prizeProbability) {
		this.prizeProbability = prizeProbability;
	}
	public String getPrizeTypeName() {
		return prizeTypeName;
	}
	public void setPrizeTypeName(String prizeTypeName) {
		this.prizeTypeName = prizeTypeName;
	}
	public Byte getRandomAmount() {
		return randomAmount;
	}
	public void setRandomAmount(Byte randomAmount) {
		this.randomAmount = randomAmount;
	}
	public Long getWiningNum() {
		return winingNum;
	}
	public void setWiningNum(Long winingNum) {
		this.winingNum = winingNum;
	}
	public Byte getRealPrize() {
		return realPrize;
	}
	public void setRealPrize(Byte realPrize) {
		this.realPrize = realPrize;
	}
   
}