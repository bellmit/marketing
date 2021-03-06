package com.jgw.supercodeplatform.marketing.service.activity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.DateUtil;
import com.jgw.supercodeplatform.marketing.common.util.RestTemplateUtil;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.dao.activity.MarketingMembersWinRecordMapper;
import com.jgw.supercodeplatform.marketing.dao.user.MarketingMembersMapper;
import com.jgw.supercodeplatform.marketing.dto.platform.ActivityDataParam;
import com.jgw.supercodeplatform.marketing.pojo.MarketingMembers;
import com.jgw.supercodeplatform.marketing.pojo.PieChartVo;
import com.jgw.supercodeplatform.marketing.service.es.activity.CodeEsService;
import com.jgw.supercodeplatform.marketing.vo.platform.ActivityOrganizationDataVo;
import com.jgw.supercodeplatform.marketing.vo.platform.DayActivityJoinQuantityVo;
import com.jgw.supercodeplatform.marketing.vo.platform.MemberAreaVo;
import com.jgw.supercodeplatform.marketing.vo.platform.MemberPortraitDataVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class PlatformStatisticsService {

    private final static long ONE_DAY_MILLS = 24 * 60 * 60 * 1000;
    @Autowired
    private CommonUtil commonUtil;
    @Autowired
    private CodeEsService codeEsService;
    @Autowired
    private MarketingMembersWinRecordMapper marketingMembersWinRecordMapper;
    @Autowired
    private MarketingMembersMapper marketingMembersMapper;
    @Autowired
    private RestTemplateUtil restTemplateUtil;
    @Value("${rest.codemanager.url}")
    private String restCodemanagerUrl;

    private final String[] provinces = {"??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????", "?????????", "??????",
            "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????", "?????????", "??????", "??????", "??????",
            "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????"};
    /**
     * ?????????
     * @param activityDataParam
     * @return
     */
    public List<PieChartVo> scanCodeRate(@Valid ActivityDataParam activityDataParam) {
        long startTime = activityDataParam.getStartDate().getTime();
        long endTime = activityDataParam.getEndDate().getTime() + ONE_DAY_MILLS;
        String startDateStr = DateFormatUtils.format(startTime, "yyyy-MM-dd");
        String endDateStr = DateFormatUtils.format(endTime, "yyyy-MM-dd");
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("start", startDateStr);
        paramMap.put("end", endDateStr);
        Map<String, String> headMap = new HashMap<>();
        headMap.put("super-token", commonUtil.getSuperToken());
        long produceCodeNum = 0; //???????????????????????????
        try {
            ResponseEntity<String> responseEntity = restTemplateUtil.getRequestAndReturnJosn(restCodemanagerUrl+ CommonConstants.CODE_GETCODETOTAL,paramMap,headMap);
            String resBody = responseEntity.getBody();
            if (responseEntity.getStatusCode().equals(HttpStatus.OK) && StringUtils.isNotBlank(resBody)) {
                JSONObject resJson = JSON.parseObject(resBody);
                if (resJson.getIntValue("state") == HttpStatus.OK.value()) {
                    Long totalCodeNum = resJson.getLong("results");
                    produceCodeNum = totalCodeNum == null?0:totalCodeNum;
                }
            }
        } catch (Exception e) {
            log.error("???????????????????????????????????????", e);
        }
        PieChartVo produceCodeVo = new PieChartVo("?????????", produceCodeNum);
        //?????????
        long scanCodeNum = codeEsService.countPlatformScanCodeRecordByTime(startTime, endTime, null);
        PieChartVo scanCodeVo = new PieChartVo("?????????", scanCodeNum);
        return Lists.newArrayList(produceCodeVo, scanCodeVo);
    }

    /**
     * ?????????
     * @param activityDataParam
     * @return
     */
    public List<PieChartVo> winningPrize(ActivityDataParam activityDataParam){
        Date startTime = activityDataParam.getStartDate();
        Date endTime = new Date(activityDataParam.getEndDate().getTime() + ONE_DAY_MILLS);
        long activityJoinNum = marketingMembersWinRecordMapper.countPlatformTotal(startTime,endTime);
        PieChartVo activityJoinVo = new PieChartVo("???????????????", activityJoinNum);
        long winningPrizeNum = marketingMembersWinRecordMapper.countPlatformWining(startTime,endTime);
        PieChartVo winningPrizeVo = new PieChartVo("???????????????", winningPrizeNum);
        return Lists.newArrayList(activityJoinVo, winningPrizeVo);
    }

    /**
     * ???????????????
     * @param activityDataParam
     * @return
     */
    public List<PieChartVo> activityJoin(@Valid ActivityDataParam activityDataParam){
        long startTime = activityDataParam.getStartDate().getTime();
        long endTime = activityDataParam.getEndDate().getTime() + ONE_DAY_MILLS;
        long scanCodeTotalNum = codeEsService.countPlatformScanCodeRecordByTime(startTime, endTime, null);
        PieChartVo scanCodeTotalVo = new PieChartVo("?????????", scanCodeTotalNum);
        long joinNum = marketingMembersWinRecordMapper.countPlatformTotal(new Date(startTime), new Date(endTime));
        PieChartVo joinVo = new PieChartVo("?????????", joinNum);
        return Lists.newArrayList(scanCodeTotalVo, joinVo);
    }

    /**
     * ???????????????????????????????????????
     * @param activityDataParam
     * @return
     */
    public List<ActivityOrganizationDataVo> activityOrganization(ActivityDataParam activityDataParam) {
        long startTime = activityDataParam.getStartDate().getTime();
        long endTime = activityDataParam.getEndDate().getTime() + ONE_DAY_MILLS;
        List<ActivityOrganizationDataVo> activityOrganizationDataVoList = codeEsService.scanOrganizationList(startTime, endTime);
        return activityOrganizationDataVoList;
    }

    /**
     *
     * @param activityDataParam
     * @param status 1?????????????????????0?????????????????????????????????,null??????0???1???????????????
     * @return
     */
    public DayActivityJoinQuantityVo statiticsDayActivity(ActivityDataParam activityDataParam, Integer status){
        long startTime = activityDataParam.getStartDate().getTime();
        long endTime = activityDataParam.getEndDate().getTime() + ONE_DAY_MILLS;
        List<PieChartVo> pieVoList = codeEsService.dayActivityStatistic(startTime, endTime, status);
        SortedSet<PieChartVo> dayPieSet = new TreeSet<>();
        dayPieSet.addAll(pieVoList);
        dayPieSet.addAll(DateUtil.dayFmt(activityDataParam.getStartDate(), activityDataParam.getEndDate()));
        DayActivityJoinQuantityVo dayActivityJoinQuantityVo = new DayActivityJoinQuantityVo();
        List<String> nameList = dayPieSet.stream().map(dayPie -> dayPie.getName()).collect(Collectors.toList());
        List<Long> valueList = dayPieSet.stream().map(dayPie -> dayPie.getValue()).collect(Collectors.toList());
        dayActivityJoinQuantityVo.setData(nameList);
        dayActivityJoinQuantityVo.setValue(valueList);
        long max = valueList.stream().max((v1, v2) -> v1.compareTo(v2)).get();
        long min = valueList.stream().min((v1, v2) -> v1.compareTo(v2)).get();
        dayActivityJoinQuantityVo.setMaxValue(max);
        dayActivityJoinQuantityVo.setMinValue(min);
        return dayActivityJoinQuantityVo;
    }

    /**
     * ????????????
     * @param activityDataParam
     * @return
     */
    public List<PieChartVo> scanCodeActMember(ActivityDataParam activityDataParam) {
        long startTime = activityDataParam.getStartDate().getTime();
        long endTime = activityDataParam.getEndDate().getTime() + ONE_DAY_MILLS;
        QueryWrapper<MarketingMembers> wrapper = Wrappers.<MarketingMembers>query().ge("RegistDate", new Date(startTime)).lt("RegistDate", new Date(endTime)).ne("State", 2);
        long allNum = marketingMembersMapper.selectCount(wrapper);
        PieChartVo allPie = new PieChartVo("?????????", allNum);
        long actNum = marketingMembersWinRecordMapper.countActUser(new Date(startTime), new Date(endTime));
        PieChartVo actPie = new PieChartVo("????????????", actNum);
        return Lists.newArrayList(allPie, actPie);
    }

    /**
     * ??????????????????
     * @return
     */
    public MemberPortraitDataVo memberPortrait(ActivityDataParam activityDataParam){
        long startTime = activityDataParam.getStartDate().getTime();
        long endTime = activityDataParam.getEndDate().getTime() + ONE_DAY_MILLS;
        //??????
        Map<String, Long> sexStatitics = marketingMembersMapper.statisticSex(new Date(startTime), new Date(endTime));
        PieChartVo malePieChartVo = new PieChartVo("???",sexStatitics.get("male"));
        PieChartVo femalePieChartVo = new PieChartVo("???", sexStatitics.get("female"));
        PieChartVo otherSexPieChartVo = new PieChartVo("??????", sexStatitics.get("other"));
        List<PieChartVo> sexList = Lists.newArrayList(malePieChartVo, femalePieChartVo, otherSexPieChartVo);
        //??????
        Map<String, Long> ageStatitics = marketingMembersMapper.statistcAge(new Date(startTime), new Date(endTime));
        PieChartVo tenPieChartVo = new PieChartVo("0-10",ageStatitics.get("ten"));
        PieChartVo twentyPieChartVo = new PieChartVo("10-20",ageStatitics.get("twenty"));
        PieChartVo thirtyPieChartVo = new PieChartVo("20-30",ageStatitics.get("thirty"));
        PieChartVo fortyPieChartVo = new PieChartVo("30-40",ageStatitics.get("forty"));
        PieChartVo fiftyPieChartVo = new PieChartVo("40-50",ageStatitics.get("fifty"));
        PieChartVo sixtyPieChartVo = new PieChartVo("50-60",ageStatitics.get("sixty"));
        PieChartVo seventyPieChartVo = new PieChartVo("60-70",ageStatitics.get("seventy"));
        PieChartVo eightyPieChartVo = new PieChartVo("70-80",ageStatitics.get("eighty"));
        PieChartVo ninetyPieChartVo = new PieChartVo("80-90",ageStatitics.get("ninety"));
        PieChartVo hundredPieChartVo = new PieChartVo("90-100",ageStatitics.get("hundred"));
        PieChartVo otherAgePieChartVo = new PieChartVo("??????",ageStatitics.get("other"));
        List<PieChartVo> ageList = Lists.newArrayList(tenPieChartVo, twentyPieChartVo, thirtyPieChartVo,fortyPieChartVo,
                fiftyPieChartVo,sixtyPieChartVo,seventyPieChartVo,eightyPieChartVo,ninetyPieChartVo,hundredPieChartVo,otherAgePieChartVo);
        //????????????
        Map<String, Long> browserStatitics = marketingMembersMapper.statisticBrowser(new Date(startTime), new Date(endTime));
        PieChartVo wxChartVo = new PieChartVo("??????",browserStatitics.get("wx"));
        PieChartVo zfbPieChartVo = new PieChartVo("?????????",browserStatitics.get("zzb"));
        PieChartVo ddPieChartVo = new PieChartVo("??????",browserStatitics.get("dd"));
        PieChartVo llqPieChartVo = new PieChartVo("?????????",browserStatitics.get("llq"));
        PieChartVo qqPieChartVo = new PieChartVo("QQ",browserStatitics.get("qq"));
        PieChartVo otherBrowserPieChartVo = new PieChartVo("??????",browserStatitics.get("other"));
        List<PieChartVo> browserLit = Lists.newArrayList(wxChartVo, zfbPieChartVo, ddPieChartVo, llqPieChartVo, qqPieChartVo, otherBrowserPieChartVo);
        MemberPortraitDataVo memberPortraitDataVo = new MemberPortraitDataVo();
        memberPortraitDataVo.setSex(sexList);
        memberPortraitDataVo.setScanSource(browserLit.stream().filter(browser -> browser.getValue() != null && browser.getValue().intValue() != 0).collect(Collectors.toList()));
        memberPortraitDataVo.setAge(ageList.stream().filter(age -> age.getValue() != null && age.getValue().intValue() != 0).collect(Collectors.toList()));
        return memberPortraitDataVo;
    }


    public MemberAreaVo memberRegion(ActivityDataParam activityDataParam){
        long startTime = activityDataParam.getStartDate().getTime();
        long endTime = activityDataParam.getEndDate().getTime() + ONE_DAY_MILLS;
        Set<PieChartVo> areaPieChartSet = marketingMembersMapper.statisticArea(new Date(startTime), new Date(endTime));
        areaPieChartSet.stream().forEach(area -> {
            String name = area.getName();
            if (name.contains("?????????") || name.contains("?????????")) {
                area.setName(name.substring(0, 3));
            } else {
                area.setName(name.substring(0, 2));
            }
        });
        Set<PieChartVo> pieChartVoSet = Stream.of(provinces).map(area -> new PieChartVo(area, 0L)).collect(Collectors.toSet());
        Set<PieChartVo> pieChartVoHashSet = new HashSet<>();
        pieChartVoHashSet.addAll(areaPieChartSet);
        pieChartVoHashSet.addAll(pieChartVoSet);
        long maxNum = pieChartVoHashSet.stream().max((p1, p2) -> p1.getValue().compareTo(p2.getValue())).get().getValue();
        long minNum = pieChartVoHashSet.stream().min((p1, p2) -> p1.getValue().compareTo(p2.getValue())).get().getValue();
        List<PieChartVo> pieChartVoList = new ArrayList<>(pieChartVoHashSet);
        //?????????????????????
        Collections.sort(pieChartVoList, (v1, v2) -> v2.getValue().compareTo(v1.getValue()));
        MemberAreaVo memberAreaVo = new MemberAreaVo();
        memberAreaVo.setMinNum(minNum);
        memberAreaVo.setMaxNum(maxNum);
        memberAreaVo.setRegionList(pieChartVoList);
        return memberAreaVo;
    }



}
