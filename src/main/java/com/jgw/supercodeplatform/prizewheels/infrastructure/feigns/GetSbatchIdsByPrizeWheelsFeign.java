package com.jgw.supercodeplatform.prizewheels.infrastructure.feigns;

import com.jgw.supercodeplatform.marketing.common.model.RestResult;
import com.jgw.supercodeplatform.marketingsaler.integral.interfaces.dto.OutCodeInfoDto;
import com.jgw.supercodeplatform.prizewheels.infrastructure.feigns.dto.GetBatchInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@FeignClient(name = "${platform.codemanager.mircosoft.name:platform-codemanager-supercode-dev}",fallback = GetSbatchIdsByPrizeWheelsFallbackFeign.class )
public interface GetSbatchIdsByPrizeWheelsFeign {
    // object 会返回2种类型
    @RequestMapping(value = "/code/relation/getBatchInfoWithoutType",method = RequestMethod.GET)
    RestResult<Object> getSbatchIds(List<GetBatchInfoDto> getBatchInfoDtoList);


    // todo
    RestResult<Object> removeOldProduct();
}
