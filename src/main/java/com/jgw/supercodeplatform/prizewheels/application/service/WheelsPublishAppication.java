package com.jgw.supercodeplatform.prizewheels.application.service;


import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.ExcelUtils;
import com.jgw.supercodeplatform.pojo.cache.OrganizationCache;
import com.jgw.supercodeplatform.prizewheels.application.transfer.ProductTransfer;
import com.jgw.supercodeplatform.prizewheels.domain.model.Product;
import com.jgw.supercodeplatform.prizewheels.domain.model.Publisher;
import com.jgw.supercodeplatform.prizewheels.domain.model.Wheels;
import com.jgw.supercodeplatform.prizewheels.domain.repository.ProductRepository;
import com.jgw.supercodeplatform.prizewheels.domain.repository.WheelsPublishRepository;
import com.jgw.supercodeplatform.prizewheels.interfaces.dto.ProductDto;
import com.jgw.supercodeplatform.prizewheels.interfaces.dto.WheelsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 发布服务涉及多个聚合的处理
 *
 * 此服务采用实现类实现
 *
 * 创建方式：通过工厂解耦 [no]
 * 创建方式: 依赖注入 [yes]
 */
@Service
public class WheelsPublishAppication {

    @Autowired
    private CommonUtil commonUtil;
    @Autowired
    private ProductTransfer productTransfer;

    @Autowired
    private WheelsPublishRepository wheelsPublishRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 新增大转盘活动
     * @param wheelsDto
     */
    @Transactional(rollbackFor = Exception.class)
    public void publish(WheelsDto wheelsDto){
        // 数据获取
        String organizationId = commonUtil.getOrganizationId();
        String organization = commonUtil.getOrganizationName();
        String accountId = commonUtil.getUserLoginCache().getAccountId();
        String userName = commonUtil.getUserLoginCache().getUserName();


        // 创建发布大转盘相关领域并完成业务

        // 设置中奖产品 剔除存在的产品批次活动

        // 设置奖励信息
        // 异步事件 获取导入的cdk_key 发布cdk关联产品事件 并给cdk关联产品事件绑定消费者

        Publisher publisher = new Publisher();
        publisher.initUserInfoWhenFirstPublish(accountId,userName);

        List<ProductDto> productDtos = wheelsDto.getProductDtos();
        List<Product> products = productTransfer.dtoToProduct(productDtos);


        Wheels wheels = new Wheels(organizationId,organization);
        wheels.addPublisher(publisher);

        // 持久化业务
        productRepository.saveButDeleteOld(products);

        wheelsPublishRepository.publish(wheels);

    }// 单次操作:100万CDK 的更新 3000/s => 300s 管理系统上线活动允许

}