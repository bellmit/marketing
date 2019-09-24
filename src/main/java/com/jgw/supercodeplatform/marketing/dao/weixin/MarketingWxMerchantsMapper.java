package com.jgw.supercodeplatform.marketing.dao.weixin;

import com.jgw.supercodeplatform.marketing.dto.activity.MarketingWxMerchantsParam;
import com.jgw.supercodeplatform.marketing.pojo.MarketingWxMerchants;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MarketingWxMerchantsMapper {
	static String allFields="Id id,Mchid mchid,MchAppid mchAppid,MerchantName merchantName,MerchantKey merchantKey,CertificateAddress certificateAddress,"
			+ "OrganizationId organizationId,OrganizatioIdlName organizatioIdlName,MerchantSecret merchantSecret, MerchantType merchantType, BelongToJgw belongToJgw";
	
    @Select("select "+allFields+" from marketing_wx_merchants where OrganizationId=#{organizationId}")
	MarketingWxMerchants get(@Param("organizationId") String organizationId);

	@Select("select "+allFields+" from marketing_wx_merchants where BelongToJgw = 1")
	MarketingWxMerchants getJgw();

	@Insert(" INSERT INTO marketing_wx_merchants(Mchid,MchAppid,MerchantName,MerchantKey,"
			+ " CertificateAddress,CertificatePassword,OrganizationId,OrganizatioIdlName,MerchantSecret) "
			+ " VALUES(#{mchid},#{mchAppid},#{merchantName},#{merchantKey},#{certificateAddress},"
			+ "#{certificatePassword},#{organizationId},#{organizatioIdlName},#{merchantSecret}"
			+ ")")
	int addWxMerchants(MarketingWxMerchantsParam marketingWxMerchantsParam);

	@Update(" <script>"
			+ " UPDATE marketing_wx_merchants "
			+ " <set>"
			+ " <if test='mchid !=null and mchid != &apos;&apos; '> Mchid= #{mchid} ,</if> "
			+ " <if test='mchAppid !=null and mchAppid != &apos;&apos; '> MchAppid = #{mchAppid} ,</if> "
			+ " <if test='merchantName !=null and merchantName != &apos;&apos; '> MerchantName = #{merchantName} ,</if> "
			+ " <if test='merchantKey !=null and merchantKey != &apos;&apos; '> MerchantKey = #{merchantKey} ,</if> "
			+ " <if test='certificateAddress !=null and certificateAddress != &apos;&apos; '> CertificateAddress = #{certificateAddress} ,</if> "
			+ " <if test='certificatePassword !=null and certificatePassword != &apos;&apos; '> CertificatePassword = #{certificatePassword} ,</if> "
			+ " <if test='merchantSecret !=null and merchantSecret != &apos;&apos; '> MerchantSecret = #{merchantSecret} ,</if> "
			//+ " <if test='organizationId !=null and organizationId != &apos;&apos; '> OrganizationId = #{organizationId} ,</if> "
			+ " <if test='organizatioIdlName !=null and organizatioIdlName != &apos;&apos; '> OrganizatioIdlName = #{organizatioIdlName} ,</if> "
			+ " </set>"
			+ " <where> "
 			+ "   OrganizationId = #{organizationId}  "
			+ " </where>"
			+ " </script>")
	int updateWxMerchants(MarketingWxMerchantsParam marketingWxMerchantsParam);

	@Select("select "+allFields+" from marketing_wx_merchants where OrganizationId=#{organizationId}")
	MarketingWxMerchants selectByOrganizationId(@Param("organizationId")String organizationId);

	@Select("select "+allFields+" from marketing_wx_merchants where MerchantName='甲骨文'")
	MarketingWxMerchants selectDefault();

	@Insert({"INSERT INTO marketing_wx_merchants(OrganizationId,OrganizatioIdlName,MerchantType) ",
			 "VALUES (#{organizationId},#{organizatioIdlName},1)"})
	int insertNoMerchant(@Param("organizationId") String organizationId, @Param("organizatioIdlName") String organizatioIdlName);

	@Update("UPDATE marketing_wx_merchants SET MerchantType = #{merchantType} WHERE OrganizationId = #{organizationId}")
	int updateNoMerchant(@Param("organizationId") String organizationId, @Param("merchantType") byte merchantType);

	@Insert({"INSERT INTO marketing_wx_merchants(OrganizationId,OrganizatioIdlName,MchAppid,MerchantSecret) ",
			"VALUES (#{organizationId},#{organizatioIdlName},#{mchAppid},#{merchantSecret})"})
	int insertAppidAndSecret(@Param("mchAppid")String mchAppid, @Param("merchantSecret")String merchantSecret, @Param("organizationId") String organizationId, @Param("organizatioIdlName") String organizatioIdlName);

	@Update("UPDATE marketing_wx_merchants SET MchAppid = #{mchAppid}, MerchantSecret = #{merchantSecret} WHERE OrganizationId = #{organizationId}")
	int updateAppidAndSecret(@Param("mchAppid")String mchAppid, @Param("merchantSecret")String merchantSecret, @Param("organizationId") String organizationId);

}
