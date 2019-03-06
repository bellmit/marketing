package com.jgw.supercodeplatform.marketing.dao.user;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.jgw.supercodeplatform.marketing.dto.members.MarketingMembersAddParam;
import com.jgw.supercodeplatform.marketing.pojo.MarketingMembers;

/**
 * 会员类
 */
@Mapper
public interface MarketingMembersMapper {
    String selectSql = " a.Id as id, a.WxName as wxName,a.Openid as openid,a.Mobile as mobile,"
            + " a.UserId as userId,a.UserName as userName,"
            + " a.Sex as sex,a.Birthday as birthday,a.ProvinceCode as provinceCode,"
            + " a.CountyCode as countyCode,a.CityCode as cityCode,a.ProvinceName as provinceName,"
            + " a.CountyName as countyName,a.CityName as cityName,"
            + " DATE_FORMAT(a.RegistDate,'%Y-%m-%d') as registDate,"
            + " a.State as state,a.OrganizationId as organizationId,"
            + " a.NewRegisterFlag as newRegisterFlag ,"
            + " DATE_FORMAT(a.CreateDate,'%Y-%m-%d') as createDate,DATE_FORMAT(a.UpdateDate,'%Y-%m-%d') as updateDate,"
            + "a.CustomerName as customerName,a.CustomerCode as customerCode,"
            + " a.BabyBirthday as babyBirthday ";


    /**
     * 会员注册
     * @param map
     * @return
     */
    @Insert(" INSERT INTO marketing_members(WxName,Openid,Mobile,UserId,UserName,"
            + " Sex,Birthday,ProvinceCode,CountyCode,CityCode,ProvinceName,CountyName,"
            + " CityName,RegistDate,OrganizationId,CustomerName,CustomerCode,BabyBirthday)"
            + " VALUES(#{wxName},#{openid},#{mobile},#{userId},#{userName},#{sex},#{birthday},#{provinceCode},#{countyCode},#{cityCode},"
            + " #{provinceName},#{countyName},#{cityName},NOW(),#{organizationId},"
            + " #{customerName},#{customerCode},#{babyBirthday} )")
    int addMembers(MarketingMembersAddParam marketingMembersAddParam);


    /**
     * 条件查询会员
     * @return
     */
    @Select(" <script>"
            + " SELECT  ${portraitsList}  FROM marketing_members "
            + " <where>"
            + "  OrganizationId = #{ organizationId }  "
            + " <if test='mobile != null and mobile != &apos;&apos;'> AND Mobile like &apos;%${mobile}%&apos; </if>"
            + " <if test='wxName != null and wxName != &apos;&apos;'> AND WxName like &apos;%${wxName}%&apos; </if>"
            + " <if test='openid != null and openid != &apos;&apos;'> AND Openid like &apos;%${openid}%&apos; </if>"
            + " <if test='userName != null and userName != &apos;&apos;'> AND UserName like &apos;%${userName}%&apos; </if>"
            + " <if test='sex != null and sex != &apos;&apos;'> AND Sex like &apos;%${sex}%&apos; </if>"
            + " <if test='birthday != null and birthday != &apos;&apos;'> AND Birthday like &apos;%${birthday}%&apos; </if>"
            + " <if test='provinceName != null and provinceName != &apos;&apos;'> AND ProvinceName like &apos;%${provinceName}%&apos; </if>"
            + " <if test='countyName != null and countyName != &apos;&apos;'> AND CountyName like &apos;%${countyName}%&apos; </if>"
            + " <if test='cityName != null and cityName != &apos;&apos;'> AND CityName like &apos;%${cityName}%&apos; </if>"
            + " <if test='customerName != null and customerName != &apos;&apos;'> AND CustomerName like &apos;%${customerName}%&apos; </if>"
            + " <if test='newRegisterFlag != null and newRegisterFlag != &apos;&apos;'> AND NewRegisterFlag like &apos;%${newRegisterFlag}%&apos; </if>"
            + " <if test='registDate != null and registDate != &apos;&apos;'> AND RegistDate like &apos;%${registDate}%&apos; </if>"
            + " <if test='babyBirthday != null and babyBirthday != &apos;&apos;'> AND BabyBirthday like &apos;%${babyBirthday}%&apos; </if>"
            + " <if test='state != null and state != &apos;&apos;'> AND State like &apos;%${state}%&apos; </if>"
            + " <if test='search !=null and search != &apos;&apos;'>"
            + " AND ("
            + " Mobile LIKE binary CONCAT('%',#{search},'%')  "
            + " OR WxName LIKE binary CONCAT('%',#{search},'%') "
            + " OR Openid LIKE binary CONCAT('%',#{search},'%') "
            + " OR UserName LIKE binary CONCAT('%',#{search},'%') "
            + " OR Sex LIKE binary CONCAT('%',#{search},'%') "
            + " OR Birthday LIKE binary CONCAT('%',#{search},'%') "
            + " OR ProvinceName LIKE binary CONCAT('%',#{search},'%') "
            + " OR CountyName LIKE binary CONCAT('%',#{search},'%') "
            + " OR CityName LIKE binary CONCAT('%',#{search},'%') "
            + " OR CustomerName LIKE binary CONCAT('%',#{search},'%') "
            + " OR NewRegisterFlag LIKE binary CONCAT('%',#{search},'%') "
            + " OR RegistDate LIKE binary CONCAT('%',#{search},'%') "
            + " OR BabyBirthday LIKE binary CONCAT('%',#{search},'%') "
            + " OR State LIKE binary CONCAT('%',#{search},'%') "
            + ")"
            + " </if>"
            + " <if test='startNumber != null and pageSize != null and pageSize != 0'> LIMIT #{startNumber},#{pageSize}</if>"
            + " </where>"
            + " </script>")
    List<MarketingMembers> getAllMarketingMembersLikeParams(Map<String,Object> map);

    /**
     * 条件查询会员数量
     * @return
     */
    @Select(" <script>"
            + " SELECT  COUNT(1)  FROM marketing_members "
            + " <where>"
            + "  OrganizationId = #{ organizationId} "
            + " <if test='mobile != null and mobile != &apos;&apos;'> AND Mobile like &apos;%${mobile}%&apos; </if>"
            + " <if test='wxName != null and wxName != &apos;&apos;'> AND WxName like &apos;%${wxName}%&apos; </if>"
            + " <if test='openid != null and openid != &apos;&apos;'> AND Openid like &apos;%${openid}%&apos; </if>"
            + " <if test='userName != null and userName != &apos;&apos;'> AND UserName like &apos;%${userName}%&apos; </if>"
            + " <if test='sex != null and sex != &apos;&apos;'> AND Sex like &apos;%${sex}%&apos; </if>"
            + " <if test='birthday != null and birthday != &apos;&apos;'> AND Birthday like &apos;%${birthday}%&apos; </if>"
            + " <if test='provinceName != null and provinceName != &apos;&apos;'> AND ProvinceName like &apos;%${provinceName}%&apos; </if>"
            + " <if test='countyName != null and countyName != &apos;&apos;'> AND CountyName like &apos;%${countyName}%&apos; </if>"
            + " <if test='cityName != null and cityName != &apos;&apos;'> AND CityName like &apos;%${cityName}%&apos; </if>"
            + " <if test='customerName != null and customerName != &apos;&apos;'> AND CustomerName like &apos;%${customerName}%&apos; </if>"
            + " <if test='newRegisterFlag != null and newRegisterFlag != &apos;&apos;'> AND NewRegisterFlag like &apos;%${newRegisterFlag}%&apos; </if>"
            + " <if test='registDate != null and registDate != &apos;&apos;'> AND RegistDate like &apos;%${registDate}%&apos; </if>"
            + " <if test='babyBirthday != null and babyBirthday != &apos;&apos;'> AND BabyBirthday like &apos;%${babyBirthday}%&apos; </if>"
            + " <if test='state != null and state != &apos;&apos;'> AND State like &apos;%${state}%&apos; </if>"
            + " <if test='search !=null and search != &apos;&apos;'>"
            + " AND ("
            + " Mobile LIKE binary CONCAT('%',#{search},'%')  "
            + " OR WxName LIKE binary CONCAT('%',#{search},'%') "
            + " OR Openid LIKE binary CONCAT('%',#{search},'%') "
            + " OR UserName LIKE binary CONCAT('%',#{search},'%') "
            + " OR Sex LIKE binary CONCAT('%',#{search},'%') "
            + " OR Birthday LIKE binary CONCAT('%',#{search},'%') "
            + " OR ProvinceName LIKE binary CONCAT('%',#{search},'%') "
            + " OR CountyName LIKE binary CONCAT('%',#{search},'%') "
            + " OR CityName LIKE binary CONCAT('%',#{search},'%') "
            + " OR CustomerName LIKE binary CONCAT('%',#{search},'%') "
            + " OR NewRegisterFlag LIKE binary CONCAT('%',#{search},'%') "
            + " OR RegistDate LIKE binary CONCAT('%',#{search},'%') "
            + " OR BabyBirthday LIKE binary CONCAT('%',#{search},'%') "
            + " OR State LIKE binary CONCAT('%',#{search},'%') "
            + ")"
            + " </if>"
            + " </where>"
            + " </script>")
    Integer getAllMarketingMembersCount(Map<String,Object> map);


    /**
     * 修改会员信息
     * @param map
     * @return
     */
    @Update(" <script>"
            + " UPDATE marketing_members "
            + " <set>"
            + " <if test='userName !=null and userName != &apos;&apos; '> UserName = #{userName} ,</if> "
            + " <if test='sex !=null and sex != &apos;&apos; '> Sex = #{sex} ,</if> "
            + " <if test='birthday !=null and birthday != &apos;&apos; '> Birthday = #{birthday} ,</if> "
            + " <if test='provinceCode !=null and provinceCode != &apos;&apos; '> ProvinceCode = #{provinceCode} ,</if> "
            + " <if test='countyCode !=null and countyCode != &apos;&apos; '> CountyCode = #{countyCode} ,</if> "
            + " <if test='cityCode !=null and cityCode != &apos;&apos; '> CityCode = #{cityCode} ,</if> "
            + " <if test='provinceName !=null and provinceName != &apos;&apos; '> ProvinceName = #{provinceName} ,</if> "
            + " <if test='countyName !=null and countyName != &apos;&apos; '> CountyName = #{countyName} ,</if> "
            + " <if test='cityName !=null and cityName != &apos;&apos; '> CityName = #{cityName} ,</if> "
            + " <if test='newRegisterFlag !=null and newRegisterFlag != &apos;&apos; '> NewRegisterFlag = #{newRegisterFlag} ,</if> "
            + " <if test='state !=null and state != &apos;&apos; '> State = #{state} ,</if> "
            + " <if test='customerName !=null and customerName != &apos;&apos; '> CustomerName = #{customerName} ,</if> "
            + " <if test='customerCode !=null and customerCode != &apos;&apos; '> CustomerCode = #{customerCode} ,</if> "
            + " <if test='babyBirthday !=null and babyBirthday != &apos;&apos; '> BabyBirthday = #{babyBirthday} ,</if> "
            + " <if test='mobile !=null and mobile != &apos;&apos; '> Mobile = #{mobile} ,</if> "
            + " <if test='wxName !=null and wxName != &apos;&apos; '> WxName = #{wxName} ,</if> "
            + " <if test='openid !=null and openid != &apos;&apos; '> Openid = #{openid} ,</if> "
            + " <if test='updateDate !=null and updateDate != &apos;&apos; '> UpdateDate = NOW() ,</if> "
            + " </set>"
            + " <where> "
            + " <if test='id !=null and id != &apos;&apos; '> and Id = #{id} </if>"
            + " <if test='userId !=null and userId != &apos;&apos; '> and UserId = #{userId} </if> "
            + " <if test='organizationId !=null and organizationId != &apos;&apos; '> and OrganizationId = #{organizationId} </if>"
            + " </where>"
            + " </script>")
    int updateMembers(Map<String,Object> map);


    /**
     * 根据id获取单个会员信息
     * @param id
     * @return
     */
    @Select(" SELECT "+selectSql+" FROM marketing_members a WHERE UserId = #{userId} AND OrganizationId = #{organizationId} ")
    MarketingMembers getMemberById(@Param("userId")String userId,@Param("organizationId")String  organizationId);

    @Select(" SELECT "+selectSql+" FROM marketing_members a WHERE a.Openid = #{openid} AND OrganizationId = #{organizationId} ")
	MarketingMembers selectByOpenIdAndOrgId(@Param("openid")String openid, @Param("organizationId")String  organizationId);

    @Select(" SELECT "+selectSql+" FROM marketing_members a WHERE a.Mobile = #{mobile} AND OrganizationId = #{organizationId} ")
	MarketingMembers selectByMobileAndOrgId(@Param("mobile")String mobile,  @Param("organizationId")String organizationId);

    @Delete("delete from marketing_members where Id=#{id}")
	void deleteById(@Param("id")Long id);




}