package com.jgw.supercodeplatform.prizewheels.application.service;


import com.jgw.supercodeplatform.marketing.common.page.AbstractPageService;
import com.jgw.supercodeplatform.marketing.common.page.DaoSearch;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.prizewheels.application.transfer.ProductTransfer;
import com.jgw.supercodeplatform.prizewheels.application.transfer.WheelsRewardTransfer;
import com.jgw.supercodeplatform.prizewheels.application.transfer.WheelsTransfer;
import com.jgw.supercodeplatform.prizewheels.domain.model.Product;
import com.jgw.supercodeplatform.prizewheels.domain.model.Publisher;
import com.jgw.supercodeplatform.prizewheels.domain.model.Wheels;
import com.jgw.supercodeplatform.prizewheels.domain.model.WheelsReward;
import com.jgw.supercodeplatform.prizewheels.domain.repository.ActivitySetRepository;
import com.jgw.supercodeplatform.prizewheels.domain.repository.ProductRepository;
import com.jgw.supercodeplatform.prizewheels.domain.repository.WheelsPublishRepository;
import com.jgw.supercodeplatform.prizewheels.domain.repository.WheelsRewardRepository;
import com.jgw.supercodeplatform.prizewheels.domain.service.ProcessActivityDomainService;
import com.jgw.supercodeplatform.prizewheels.domain.service.ProductDomainService;
import com.jgw.supercodeplatform.prizewheels.domain.service.WheelsRewardDomainService;
import com.jgw.supercodeplatform.prizewheels.infrastructure.mysql.mapper.WheelsMapper;
import com.jgw.supercodeplatform.prizewheels.infrastructure.mysql.pojo.ActivitySet;
import com.jgw.supercodeplatform.prizewheels.infrastructure.mysql.pojo.WheelsPojo;
import com.jgw.supercodeplatform.prizewheels.infrastructure.repository.WheelsPublishRepositoryImpl;
import com.jgw.supercodeplatform.prizewheels.interfaces.dto.*;
import com.jgw.supercodeplatform.prizewheels.interfaces.vo.WheelsDetailsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private WheelsTransfer wheelsTransfer;
    @Autowired
    private ProductTransfer productTransfer;


    @Autowired
    private WheelsRewardTransfer wheelsRewardTransfer;

    @Autowired
    private WheelsPublishRepository wheelsPublishRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WheelsRewardRepository wheelsRewardRepository;

    @Autowired
    private WheelsRewardDomainService wheelsRewardDomainService;

    @Autowired
    private ProductDomainService productDomainService;

    @Autowired
    private WheelsMapper wheelsMapper;

    @Autowired
    private ProcessActivityDomainService processActivityDomainService;

    @Autowired
    private  ActivitySetRepository activitySetRepository;

    @Autowired
    private   WheelsPublishRepositoryImpl wheelsPublishRepositoryImpl;

    /**
     * 新增大转盘活动
     * @param wheelsDto
     */
    @Transactional(rollbackFor = Exception.class)
    public void publish(WheelsDto wheelsDto){
        // 数据转换
        byte autoType = wheelsDto.getAutoType();
        Wheels wheels =  wheelsTransfer.tranferToDomainWhenAdd(wheelsDto);

        List<ProductDto> productDtos = wheelsDto.getProductDtos();
        List<WheelsRewardDto> wheelsRewardDtos = wheelsDto.getWheelsRewardDtos();
        List<Product> products = productTransfer.transferDtoToDomain(productDtos, autoType);
        List<WheelsReward> wheelsRewards = wheelsRewardTransfer.transferDtoToDomain(wheelsRewardDtos);
        // 1 业务处理
        // 大转盘
        Publisher publisher = new Publisher();
        publisher.initUserInfo(
                commonUtil.getUserLoginCache().getAccountId()
                ,commonUtil.getUserLoginCache().getUserName());
        wheels.initOrgInfo(commonUtil.getOrganizationId(),commonUtil.getOrganizationName());
        wheels.addPublisher(publisher);
        wheels.checkWhenAdd();
        // 持久化 返回主键
        wheelsPublishRepository.publish(wheels);
        Long prizeWheelsid = wheels.getId();

        // 2 奖励
        wheelsRewardDomainService.checkWhenAdd(wheelsRewards);
        // 持久化返回主键
        wheelsRewardRepository.batchSave(wheelsRewards);

        // 2-1 cdk 领域事件 奖品与cdk绑定
        wheelsRewardDomainService.cdkEventCommitedWhenNecessary(wheelsRewards);

        // 3 码管理业务
        // 3-1 获取生码批次
        products = productDomainService.initSbatchIds(products);

        // 3-2 将此活动涉及产品与码管理的信息解绑
        productDomainService.removeOldProduct(products);
        // 3-3 发送新的产品绑定请求
        productDomainService.executeBizWhichCodeManagerWant(products);
        // 4 修改活动聚合老表
        ActivitySet activitySet = processActivityDomainService.formPrizeWheelsToOldActivity(wheels, (int) autoType);
        // 持久化

        productRepository.saveButDeleteOld(products);

        activitySetRepository.addWhenWheelsAdd(activitySet);

    }


    @Transactional
    public void deletePrizeWheelsById(Long id) {
        wheelsPublishRepository.deletePrizeWheelsById(id);
        productRepository.deleteByPrizeWheelsId(id);
        wheelsRewardRepository.deleteByPrizeWheelsId(id);
        // TODO cdk后期删除
    }


    @Transactional(rollbackFor = Exception.class)
    public void update(WheelsUpdateDto wheelsUpdateDto) {
        // 数据转换
        Long prizeWheelsid = wheelsUpdateDto.getId();
        byte autoType = wheelsUpdateDto.getAutoType();
        Wheels wheels =  wheelsTransfer.tranferToDomain(wheelsUpdateDto);

        List<ProductUpdateDto> productUpdateDtos = wheelsUpdateDto.getProductUpdateDtos();
        List<WheelsRewardUpdateDto> wheelsRewardUpdateDtos = wheelsUpdateDto.getWheelsRewardUpdateDtos();
        List<Product> products = productTransfer.transferUpdateDtoToDomain(productUpdateDtos, prizeWheelsid, autoType);
        List<WheelsReward> wheelsRewards = wheelsRewardTransfer.transferUpdateDtoToDomain(wheelsRewardUpdateDtos, prizeWheelsid);
        // 1 业务处理
        // 大转盘
        Publisher publisher = new Publisher();
        publisher.initUserInfo(
                commonUtil.getUserLoginCache().getAccountId()
                ,commonUtil.getUserLoginCache().getUserName());
        wheels.initOrgInfo(commonUtil.getOrganizationId(),commonUtil.getOrganizationName());
        wheels.addPublisher(publisher);
        wheels.checkWhenUpdate();

        // 2 奖励
        wheelsRewardDomainService.checkWhenUpdate(wheelsRewards);
        // 2-1 cdk 领域事件 奖品与cdk绑定
        wheelsRewardDomainService.cdkEventCommitedWhenNecessary(wheelsRewards);

        // 3 码管理业务
        // 3-1 获取生码批次
        products = productDomainService.initSbatchIds(products);

        // 3-2 将此活动之前产品与码管理的信息解绑
        List<Product> oldPrizeWheelsProduct = productRepository.getByPrizeWheelsId(prizeWheelsid);
        productDomainService.removeOldProduct(oldPrizeWheelsProduct);
        // 将准备绑定的产品原来的绑定删除: 里面可能有部分产品之前属于其他活动需要解绑:
        productDomainService.removeOldProduct(products);
        // 3-3 发送新的产品绑定请求
        productDomainService.executeBizWhichCodeManagerWant(products);
        // 4 修改活动聚合老表
        ActivitySet activitySet = processActivityDomainService.formPrizeWheelsToOldActivity(wheels, (int) autoType);
        // 持久化
        wheelsPublishRepository.updatePrizeWheel(wheels);

        wheelsRewardRepository.deleteByPrizeWheelsId(prizeWheelsid);
        wheelsRewardRepository.batchSave(wheelsRewards);

        productRepository.saveButDeleteOld(products);

        activitySetRepository.updateWhenWheelsChanged(activitySet);

    }


    /**
     * B端 根据组织id和组织名称获取大转盘详情
     * @return
     */
    public WheelsDetailsVo getWheelsDetails(Long id ){
        // 组织数据获取
        // 获取大转盘

        WheelsPojo wheelsPojo=wheelsPublishRepositoryImpl.getWheels(id);
        WheelsDetailsVo wheelsDetailsVo=wheelsTransfer.tranferWheelsPojoToDomain(wheelsPojo);
        //获取产品
        List<Product> wheelsProducts = productRepository.getByPrizeWheelsId(id);
        List<ProductUpdateDto> productUpdateDtos=productTransfer.productToProductDto(wheelsProducts);
        wheelsDetailsVo.setProductUpdateDtos(productUpdateDtos);
        //获取奖励
        List<WheelsReward> wheelsRewards=wheelsRewardRepository.getByPrizeWheelsId(id);
        List<WheelsRewardUpdateDto> wheelsRewardUpdateDtos=wheelsRewardTransfer.transferRewardToDomain(wheelsRewards);
        wheelsDetailsVo.setWheelsRewardUpdateDtos(wheelsRewardUpdateDtos);
        wheelsDetailsVo.setAutoType(wheelsProducts.get(0).getAutoType());
        return null;
    }
    public AbstractPageService.PageResults<List<WheelsUpdateDto>> list(DaoSearch daoSearch) {
        return null;
    }

    /**
     * B端 根据组织id和组织名称获取大转盘详情
     * @return
     */
    public WheelsDetailsVo getWheelsDetails(){
        // 组织数据获取

        return null;
    }

    public String uploadExcel(MultipartFile uploadFile){

//        ExcelUtils
//        String fileName = uploadFile.getOriginalFilename();
//        String path = commonUtil.getRoot()+ File.separator;
//        File filePath = new File(path);
//        if (!filePath.isDirectory()){
//            filePath.mkdir();
//        }
//        File file = new File(path, commonUtil.getUUID() + fileName);
//        uploadFile.transferTo(file);
        return null;
    }
}
