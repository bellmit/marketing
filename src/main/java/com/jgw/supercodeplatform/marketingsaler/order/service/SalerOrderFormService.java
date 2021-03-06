package com.jgw.supercodeplatform.marketingsaler.order.service;


import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.page.AbstractPageService;
import com.jgw.supercodeplatform.marketing.common.page.DaoSearch;
import com.jgw.supercodeplatform.marketing.common.page.Page;
import com.jgw.supercodeplatform.marketing.exception.BizRuntimeException;
import com.jgw.supercodeplatform.marketing.exception.TableSaveException;
import com.jgw.supercodeplatform.marketing.vo.activity.H5LoginVO;
import com.jgw.supercodeplatform.marketingsaler.base.service.SalerCommonService;
import com.jgw.supercodeplatform.marketingsaler.dynamic.mapper.DynamicMapper;
import com.jgw.supercodeplatform.marketingsaler.integral.application.group.BaseCustomerService;
import com.jgw.supercodeplatform.marketingsaler.order.constants.FormType;
import com.jgw.supercodeplatform.marketingsaler.order.dto.ChangeColumDto;
import com.jgw.supercodeplatform.marketingsaler.order.dto.ColumnnameAndValueDto;
import com.jgw.supercodeplatform.marketingsaler.order.dto.SalerOrderFormDto;
import com.jgw.supercodeplatform.marketingsaler.order.dto.SalerOrderFormSettingDto;
import com.jgw.supercodeplatform.marketingsaler.order.mapper.SalerOrderFormMapper;
import com.jgw.supercodeplatform.marketingsaler.order.pojo.SalerOrderForm;
import com.jgw.supercodeplatform.marketingsaler.order.transfer.SalerOrderTransfer;
import com.jgw.supercodeplatform.marketingsaler.order.vo.H5SalerOrderFormVo;
import com.jgw.supercodeplatform.marketingsaler.order.vo.SalerPreFillInfoVo;
import com.jgw.supercodeplatform.marketingsaler.outservicegroup.dto.CustomerInfoView;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotEmpty;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * <p>
 * ????????????????????? ???????????????
 * </p>
 *
 * @author renxinlin
 * @since 2019-09-02
 */
@Service
@Slf4j
public class SalerOrderFormService extends SalerCommonService<SalerOrderFormMapper, SalerOrderForm> {


    @Autowired
    private DynamicMapper dynamicMapper;
    @Autowired
    private BaseCustomerService baseCustomerService;

    /**
     * ??????????????????
     * ????????????????????? ?????????????????? ???????????????????????????
     *  ?????????????????????
     *  ???????????????????????????????????????  ???????????????????????????
     * @param salerOrderForms
     */
    // TODO ?????????????????????
    @Transactional // ????????????????????????
    public void alterOrCreateTableAndUpdateMetadata(List<SalerOrderFormSettingDto> salerOrderForms) {
        Asserts.check(!CollectionUtils.isEmpty(salerOrderForms),"??????????????????");
        valid(salerOrderForms);
        List<SalerOrderFormSettingDto> updateOrderForms = new ArrayList<>();
        List<SalerOrderFormSettingDto> deleteOrAddForms = new ArrayList<>();
        log.info("??????????????????????????????{}",salerOrderForms);
        List updateids = new ArrayList();
        for(SalerOrderFormSettingDto salerOrderForm : salerOrderForms){
            if(salerOrderForm.getId() == null || salerOrderForm.getId() <= 0 ){
                deleteOrAddForms.add(salerOrderForm);
            }else {
                updateOrderForms.add(salerOrderForm);
                updateids.add(salerOrderForm.getId());
            }
        };

        deleteOrAddOrUpdate(deleteOrAddForms,updateids,updateOrderForms);

    }

    /**
     * ????????????????????????
     * @param salerOrderForms
     */
    private void valid(List<SalerOrderFormSettingDto> salerOrderForms) {

        List<SalerOrderFormDto> salerOrderFormDtos = SalerOrderTransfer.defaultForms(commonUtil.getOrganizationId(), commonUtil.getOrganizationName());
        List<SalerOrderFormDto> salerOrderFormDtos1 = SalerOrderTransfer.setFormsOtherField(salerOrderForms, commonUtil.getOrganizationId(), commonUtil.getOrganizationName());
        salerOrderFormDtos.addAll(salerOrderFormDtos1);
        Set<String> columnNames = new HashSet();
        salerOrderFormDtos.forEach(salerOrderFormDto ->{
            columnNames.add(salerOrderFormDto.getColumnName());
        });
        Asserts.check(columnNames.size() == salerOrderFormDtos.size(),"????????????????????????????????????????????????");
        salerOrderForms.forEach(salerOrderFormSettingDto -> {
            if(salerOrderFormSettingDto.getFormType().byteValue() == FormType.xiala){
                Asserts.check(!StringUtils.isEmpty(salerOrderFormSettingDto.getValue()),"??????????????????????????????");
            }
        });

    }


    /**
     * ??????????????????????????????????????????
     * @param salerOrderForms ??????
     * @param updateids ??????
     *
     * @param updateOrderForms ??????
     */
    private void deleteOrAddOrUpdate(List<SalerOrderFormSettingDto> salerOrderForms,List<Long> updateids,List<SalerOrderFormSettingDto> updateOrderForms) {
        if(CollectionUtils.isEmpty(salerOrderForms) && CollectionUtils.isEmpty(updateOrderForms)){
            return;
        }
        // ???????????????????????????

        List<SalerOrderForm> undeleteBecauseofUpdates = new ArrayList<>();
        if(!CollectionUtils.isEmpty(updateids)){
            undeleteBecauseofUpdates = baseMapper.selectBatchIds(updateids);
        }

        // ????????????  ??????????????????????????????????????????
        List<SalerOrderFormDto> withDefaultsalerOrderFormDtos = SalerOrderTransfer.setFormsOtherField(salerOrderForms, commonUtil.getOrganizationId(), commonUtil.getOrganizationName());
        // ??????????????????
        checkrepeat(withDefaultsalerOrderFormDtos);
        // ?????????????????????
        List<SalerOrderForm> createsMetadatas = baseMapper.selectList(query().eq("OrganizationId", commonUtil.getOrganizationId()).getWrapper());
        // ??????????????????
        List<SalerOrderForm> pojos = SalerOrderTransfer.modelMapper(modelMapper,withDefaultsalerOrderFormDtos);


        List<SalerOrderFormDto> defaultforms = SalerOrderTransfer.defaultForms(commonUtil.getOrganizationId(), commonUtil.getOrganizationName());
        if(CollectionUtils.isEmpty(createsMetadatas)){
            // ??????????????????
            withDefaultsalerOrderFormDtos.addAll(defaultforms);
            pojos = SalerOrderTransfer.modelMapper(modelMapper,withDefaultsalerOrderFormDtos);
            // ?????????????????????
            List<String> newColumns = withDefaultsalerOrderFormDtos.stream().map(dto -> dto.getColumnName()).collect(Collectors.toList());
            // ??????????????????
            newColumns.removeIf(column-> column.equalsIgnoreCase(SalerOrderTransfer.PrimaryKey));
            try {
                dynamicMapper.createTable(withDefaultsalerOrderFormDtos.get(0).getTableName(),newColumns);
            } catch (Exception e) {
                e.printStackTrace();
                // ????????????..........................................
                throw new BizRuntimeException("?????????????????????????????????????????????");
            }
            if(!CollectionUtils.isEmpty(pojos)){
                log.info("1?????????????????????POJO=>{}", JSONObject.toJSONString(pojos));
                this.saveBatch(pojos);
            }
        }else{
            // ??????????????????
            List<String> createsMetadatasColumnName = createsMetadatas.stream().map(createsMetadata -> createsMetadata.getColumnName()).collect(Collectors.toList());
            // ????????????
            List<String> withDefaultsalerOrderFormColumnNames = withDefaultsalerOrderFormDtos.stream().map(withDefaultsalerOrderFormDto -> withDefaultsalerOrderFormDto.getColumnName()).collect(Collectors.toList());

            // add column?????????????????????
            List<String> addColumns = modelMapper.map(withDefaultsalerOrderFormColumnNames,List.class);
            addColumns.removeIf(addColumn->createsMetadatasColumnName.contains(addColumn));
            List<SalerOrderForm> addColumnPojos = new ArrayList<>();
            if(!CollectionUtils.isEmpty(addColumns)){
                for(SalerOrderFormDto salerOrderFormDto:withDefaultsalerOrderFormDtos){
                    if(addColumns.contains(salerOrderFormDto.getColumnName())){
                        SalerOrderForm add = modelMapper.map(salerOrderFormDto, SalerOrderForm.class);
                        addColumnPojos.add(add);
                    }

                }
            }

            List<String> deleteColumns = modelMapper.map(createsMetadatasColumnName,List.class);
            // ??????????????????????????????
            removeDefaultAndUpdate(undeleteBecauseofUpdates, defaultforms, deleteColumns);
            // ???????????????????????????
            log(addColumns, deleteColumns);


            List<ChangeColumDto> updateColumns = null;
            if(!CollectionUtils.isEmpty(updateOrderForms)){
                List<Long> ids = updateOrderForms.stream().map(data -> data.getId()).collect(Collectors.toList());
                List<SalerOrderForm> oldSalerOrderForms = baseMapper.selectBatchIds(ids);
                Asserts.check(ids.size() == oldSalerOrderForms.size(),"??????id?????????");
                updateColumns  =  SalerOrderTransfer.setColumnInfoWhenUpdate(updateOrderForms, oldSalerOrderForms,commonUtil.getOrganizationId());

            }


            try {
                // ?????????????????????
                log.info("??????????????? tableName ==>{}???deleteColumnNames ==>{}???addcolumnNames ==>{}???updateColumnNames ==> {}"
                ,SalerOrderTransfer.initTableName(commonUtil.getOrganizationId())
                        ,deleteColumns
                        ,addColumns
                        ,updateColumns
                );
                dynamicMapper.alterTableAndDropOrAddOrUpdate(SalerOrderTransfer.initTableName(commonUtil.getOrganizationId()),deleteColumns,addColumns,updateColumns);

            } catch (Exception e) {
                e.printStackTrace();

                throw new BizRuntimeException("?????????????????????????????????????????????");
            }

            if(!CollectionUtils.isEmpty(deleteColumns)){
                log.info("2????????????????????????POJO=>{}", JSONObject.toJSONString(pojos));
                baseMapper.delete(query().eq("OrganizationId",commonUtil.getOrganizationId()).in("ColumnName",deleteColumns).notIn(!CollectionUtils.isEmpty(updateids),"id",updateids).getWrapper());
            }

            if(!CollectionUtils.isEmpty(updateColumns)){
                log.info("3????????????????????????POJO=>{}", JSONObject.toJSONString(pojos));
                this.updateBatchById(SalerOrderTransfer.initUpdateSalerOrderFormInfo(updateColumns));
            }

            if(!CollectionUtils.isEmpty(addColumnPojos)){
                log.info("4????????????????????????POJO=>{}", JSONObject.toJSONString(pojos));
                this.saveBatch(addColumnPojos);
            }
        }


    }

    private void log(List<String> addColumns, List<String> deleteColumns) {
        StringBuffer sbadd =new StringBuffer("");
        addColumns.forEach(data->sbadd.append(data).append("  "));
        log.info("add columns ??????{}" ,sbadd.toString());
        StringBuffer sb =new StringBuffer("");
        deleteColumns.forEach(data->sb.append(data).append("  "));
        log.info("delete columns ??????{}" ,sb.toString());
    }

    private void removeDefaultAndUpdate(List<SalerOrderForm> undeleteBecauseofUpdates, List<SalerOrderFormDto> defaultforms, List<String> deleteColumns) {
        List<String> undeleteBecauseofUpdateColumnNames = undeleteBecauseofUpdates.stream().map(undeleteBecauseofUpdate -> undeleteBecauseofUpdate.getColumnName()).collect(Collectors.toList());
        List<String> defaultformColumnNames = defaultforms.stream().map(defaultform -> defaultform.getColumnName()).collect(Collectors.toList());
        deleteColumns.removeIf(deleteColumn-> defaultformColumnNames.contains(deleteColumn)); // ????????????????????????
        deleteColumns.removeIf(deleteColumn->undeleteBecauseofUpdateColumnNames.contains(deleteColumn)); // ????????????????????????
    }

    private void checkrepeat(List<SalerOrderFormDto> withDefaultsalerOrderFormDtos) {
        Set<@NotEmpty(message = "????????????????????????") String> formNames = withDefaultsalerOrderFormDtos.stream().map(salerOrderForm -> salerOrderForm.getFormName()).collect(Collectors.toSet());
        Asserts.check(formNames.size() == withDefaultsalerOrderFormDtos.size(),"???????????????????????????????????????????????????????????????");
        for(SalerOrderFormDto salerOrderFormDto:withDefaultsalerOrderFormDtos){
            if(SalerOrderTransfer.deafultColumnNames.contains(salerOrderFormDto.getColumnName())){
                throw new BizRuntimeException("???????????????????????????");
            }
            if(SalerOrderTransfer.PrimaryKey.equalsIgnoreCase(salerOrderFormDto.getColumnName())){
                throw new BizRuntimeException("id????????????????????????????????????");
            }

        }
    }

    public List<SalerOrderForm> detail() {
        List<SalerOrderForm> salerOrderForms = baseMapper.selectList(query().eq("OrganizationId", commonUtil.getOrganizationId()).getWrapper());
        if(CollectionUtils.isEmpty(salerOrderForms)){
            return new ArrayList<>();
        }else{
            // ?????????????????????
            salerOrderForms.removeIf(salerOrderForm -> SalerOrderTransfer.deafultColumnNames.contains(salerOrderForm.getColumnName()));
        }
        return salerOrderForms;
    }

    /**
     * ???????????????
     * @param daoSearch
     */
    public  AbstractPageService.PageResults<List<Map<String,Object>>> selectPage(DaoSearch daoSearch) throws SuperCodeException {
        // ????????????
        String tableName = SalerOrderTransfer.initTableName(commonUtil.getOrganizationId());
        // ???????????????
        Integer pageSize = daoSearch.getPageSize();
        int current = (daoSearch.getCurrent() -1)*pageSize;
        List<SalerOrderForm> columnConfigs = getColumnNamesByOrganizationId(commonUtil.getOrganizationId());
        List<String> columnNames = columnConfigs.stream().map(columnconfig -> columnconfig.getColumnName()).collect(Collectors.toList());
        int count = dynamicMapper.selectCount(tableName,columnNames,daoSearch.getSearch());
        List<Map<String,Object>> pageData = dynamicMapper.selectPageData(tableName,current, pageSize,columnNames,daoSearch.getSearch());
        Page pageInfo = new Page(pageSize,daoSearch.getCurrent(),count);
        AbstractPageService.PageResults<List<Map<String,Object>>> pageResult =
                new AbstractPageService.PageResults<>(pageData,pageInfo);
        return pageResult;
    }

    private List<SalerOrderForm> getColumnNamesByOrganizationId(String organizationId) {
        List<SalerOrderForm> columnConfigs = baseMapper.selectList(query().eq("organizationId", organizationId).getWrapper());
        return  columnConfigs;
    }

    public  List<H5SalerOrderFormVo> showOrder(H5LoginVO user) {
        Asserts.check(!StringUtils.isEmpty(user.getOrganizationId()),"?????????????????????");
        // ?????????:?????? notin
        List<SalerOrderForm> salerOrderForms = baseMapper.selectList(query().eq("OrganizationId", user.getOrganizationId())
                .notIn("ColumnName", SalerOrderTransfer.deafultColumnNames).getWrapper());
        List<H5SalerOrderFormVo> vos = SalerOrderTransfer.modelMapperVo(modelMapper,salerOrderForms);
        return vos;
    }


    /**
     * TODO ?????????????????? ???????????????
     * @param columnnameAndValues
     * @param user
     */
    public void saveOrder(List<ColumnnameAndValueDto> columnnameAndValues, H5LoginVO user) {
        Asserts.check(!StringUtils.isEmpty(user.getOrganizationId()), "?????????????????????");
        Asserts.check(!CollectionUtils.isEmpty(columnnameAndValues), "?????????????????????");
       // validAllHaveColumn(columnnameAndValues,user.getOrganizationId());
        StringBuffer address = new StringBuffer("");
        if (!StringUtils.isEmpty(user.getCustomerId())) {
            CustomerInfoView customerInfo = baseCustomerService.getCustomerInfo(user.getCustomerId());
            log.info("?????????????????????????????????customerInfo {}",customerInfo);
            getAddress(address, customerInfo);
        }
        SalerOrderTransfer.initDefaultColumnValue(columnnameAndValues, user, address.toString());
        try {
            // ????????????????????????????????????????????????????????????
            dynamicMapper.saveOrder(columnnameAndValues, SalerOrderTransfer.initTableName(user.getOrganizationId()));
        } catch (RuntimeException e) {
            throw new BizRuntimeException("??????????????????????????????...");
        }
    }


    public void updateOrder(List<ColumnnameAndValueDto> columnnameAndValues) {
        Asserts.check(!StringUtils.isEmpty(commonUtil.getOrganizationId()), "?????????????????????");
        Asserts.check(!CollectionUtils.isEmpty(columnnameAndValues), "?????????????????????");
        //
        AtomicReference<String> id = new AtomicReference<String> ();
        columnnameAndValues.forEach(columnnameAndValueDto -> {
            if(columnnameAndValueDto.getColumnName().equalsIgnoreCase("id")){
                id.set(columnnameAndValueDto.getColumnValue());
            }
        });
        if(id.get() ==null || id.get() .equals("")){
            throw new BizRuntimeException("ID ?????????");
        }
        dynamicMapper.updateOrder(columnnameAndValues, SalerOrderTransfer.initTableName(commonUtil.getOrganizationId()),id.get());


    }

    public void saveOrder(List<ColumnnameAndValueDto> columnnameAndValues) {
        Asserts.check(!StringUtils.isEmpty(commonUtil.getOrganizationId()), "?????????????????????");
        Asserts.check(!CollectionUtils.isEmpty(columnnameAndValues), "?????????????????????");
        // validAllHaveColumn(columnnameAndValues,commonUtil.getOrganizationId());
        try {
            dynamicMapper.saveOrder(columnnameAndValues, SalerOrderTransfer.initTableName(commonUtil.getOrganizationId()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new TableSaveException(e.getMessage());
        }
    }




    private void validAllHaveColumn(List<ColumnnameAndValueDto> columnnameAndValues,String organizationId) {
        int orderSize = baseMapper.selectCount(query().eq("organizationId", organizationId).getWrapper());
        int size = orderSize-SalerOrderTransfer.deafultColumnNames.size();
        if(columnnameAndValues.size() != size){
            throw new BizRuntimeException("???????????????...");
        }

    }

    /**
     * ??????????????????
     * @param address
     * @param customerInfo
     */
    private void getAddress(StringBuffer address, CustomerInfoView customerInfo) {
        if (customerInfo != null) {
            if (!StringUtils.isEmpty(customerInfo.getProvinceName())) {
                address.append(customerInfo.getProvinceName());
            }
            if (!StringUtils.isEmpty(customerInfo.getCityName())) {
                address.append(customerInfo.getCityName());
            }
            if (!StringUtils.isEmpty(customerInfo.getCountyName())) {
                address.append(customerInfo.getCountyName());
            }
        }
    }


    public void updateStatus(Long id, byte status) {
        dynamicMapper.updateStatus(id, status, SalerOrderTransfer.initTableName(commonUtil.getOrganizationId()));

    }

    public void delete(Long id) {
        dynamicMapper.delete(id, SalerOrderTransfer.initTableName(commonUtil.getOrganizationId()));

    }

    public List<Map<String, Object>> detailbyId(Long id) {
        return dynamicMapper.selectById(id,SalerOrderTransfer.initTableName(commonUtil.getOrganizationId()));
    }

    /**
     * ???????????????????????????
     * @param jwtUser
     * @return
     */
    public SalerPreFillInfoVo getPreFill(H5LoginVO jwtUser){
        SalerPreFillInfoVo salerPreFillInfoVo= new SalerPreFillInfoVo();
        salerPreFillInfoVo.setDinghuoren(jwtUser.getMemberName());
        salerPreFillInfoVo.setDinghuorendianhua(jwtUser.getMobile());
        StringBuffer address = new StringBuffer("");
        if (org.apache.commons.lang.StringUtils.isNotBlank(jwtUser.getCustomerId())){
            CustomerInfoView customerInfoView=baseCustomerService.getCustomerInfo(jwtUser.getCustomerId());
            log.info("?????????????????????????????????customerInfoView-{}",customerInfoView);
            getAddress(address,customerInfoView);
            String detailedAddress = customerInfoView == null ? "" : customerInfoView.getDetailedAddress()  ;
            address.append(detailedAddress != null ? detailedAddress:"");
            salerPreFillInfoVo.setShouhuodizhi(address.toString());
        }
        return salerPreFillInfoVo;
    }
}
