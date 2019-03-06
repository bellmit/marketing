package com.jgw.supercodeplatform.marketing.common.model.activity;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * 活动中奖奖次设置实体
 * @author czm
 *
 */
public class MarketingPrizeTypeMO implements Comparable<MarketingPrizeTypeMO>{

    private long id;//序列Id
    private long activitySetId;//活动设置Id
    private Integer prizeAmount;//金额数量,类型跟微信接口保持一致
    private Integer prizeProbability;//中奖几率
    private String prizeTypeName;//奖品类型名称
    private Byte randomAmount;//是否随机金额 1随机 0固定
    private long totalNum;//当前批次剩下的码数量
    private long winingNum;//已中奖数
    private Byte realPrize;//是否由用户创建的真实奖次
    
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getActivitySetId() {
		return activitySetId;
	}
	public void setActivitySetId(long activitySetId) {
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

	public long getTotalNum() {
		return totalNum;
	}
	public void setTotalNum(long totalNum) {
		this.totalNum = totalNum;
	}
	public long getWiningNum() {
		return winingNum;
	}
	public void setWiningNum(long winingNum) {
		this.winingNum = winingNum;
	}
	
	public Byte getRealPrize() {
		return realPrize;
	}
	public void setRealPrize(Byte realPrize) {
		this.realPrize = realPrize;
	}
	@Override
	public int compareTo(MarketingPrizeTypeMO o) {
		if (o==null) {
			return 1;
		}
		if (this.prizeProbability<o.getPrizeProbability()) {
			return 1;
		}
		return 0;
	}
	@Override
	public int hashCode() {
	     return new HashCodeBuilder(17, 37).
	       append(id).
	       append(activitySetId).
	       toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		   if (obj == null) { return false;}
		   if (obj == this) { return true; }
		   if (obj.getClass() != getClass()) {
		     return false;
		   }
		   MarketingPrizeTypeMO rhs = (MarketingPrizeTypeMO) obj;
		   return new EqualsBuilder()
		   //这里调用父类的equals()方法，一般情况下不需要使用
		                 .appendSuper(super.equals(obj))
		                 .append("id", rhs.id)
		                 .append("activitySetId", rhs.activitySetId)
		                 .isEquals();
	}
}