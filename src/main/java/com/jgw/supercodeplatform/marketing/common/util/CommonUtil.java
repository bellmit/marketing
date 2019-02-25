package com.jgw.supercodeplatform.marketing.common.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.page.NormalProperties;
import com.jgw.supercodeplatform.marketing.common.page.Page;
import com.jgw.supercodeplatform.user.UserInfoUtil;

/**
 * 基础工具类
 *
 * @author zcy
 */
@Component
public class CommonUtil extends UserInfoUtil {

    @Autowired
    private RestTemplate restTemplate;

    public void validateRequestParamAndValueNotNull(Map<String, Object> params, String... args) throws SuperCodeException {

        if (params.size() == 0) {
            throw new SuperCodeException("业务参数不能为空");
        }

        if (args.length == 0) {
            return;
        }

        List<String> list1 = new ArrayList<>();
        list1.addAll(params.keySet());

        for (String key : list1) {
            if (params.get(key) == null || params.get(key).toString().equals("") || params.get(key).toString().toLowerCase().equals("null")) {
                params.remove(key);
                continue;
            }
            if (params.get(key) instanceof Map) {
                Map map = (Map) params.get(key);
                if (map.size() == 0) {
                    params.remove(key);
                }
            }
        }

        List<String> list = new ArrayList<>();
        for (String arg : args) {
            if (!params.containsKey(arg)) {
                list.add(arg);
            }
        }

        if (list.size() > 0) {
            throw new SuperCodeException("Missing parameter {" + list.toString() + "}");
        }
    }


    /**
     * 验证参数是否为空,为空返回true
     *
     * @param obj
     * @return
     * @author liujianqiang
     * @data 2018年10月11日
     */
    public boolean isNull(Object obj) {
        if (obj == null || "".equals(obj.toString())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 根据入参和总记录数，返回页码实体类
     * @param params
     * @param total
     * @return
     * @throws Exception
     * @author liujianqiang
     * @data 2018年10月11日
     */
    public Page getPage(Map<String, Object> params, int total) throws SuperCodeException {
        Object pageCountObj = params.get("pageSize");//每页记录数
        Object currentPageObj = params.get("current");//当前页
        int pageCount;
        int currentPage;
        if (isNull(pageCountObj) || Integer.parseInt(pageCountObj.toString()) == 0) {//假如每页记录数为空,默认为10条
            pageCount = NormalProperties.DEFAULT_PAGE_COUNT;
        } else {
            pageCount = Integer.parseInt(pageCountObj.toString());
        }
        if (isNull(currentPageObj) || Integer.parseInt(currentPageObj.toString()) == 0) {//假如当前页为空,则当前页设置为默认值1
            currentPage = NormalProperties.DEFAULT_CURRENT_PAGE;
        } else {
            currentPage = Integer.parseInt(currentPageObj.toString());
        }
        return new Page(pageCount, currentPage, total);
    }
    
    /**
     * 统一返回结果list字段名
     *
     * @param returnMap
     * @param list
     * @return
     * @author liujianqiang
     * @data 2018年11月12日
     */
    public Map<String, Object> getRetunMap(Map<String, Object> returnMap, List<?> list) {
        returnMap.put("list", list);
        return returnMap;
    }


    /**
     * 功能描述：对象转换成对应的对象
     *
     * @return T
     * @Author corbett
     * @Description //TODO
     * @Date 14:07 2018/10/12
     * @Param [o-需要转换的对象, c--需要转换成的对象类型]
     **/
    public <T> T toJavaBean(Object o, Class<T> c) throws SuperCodeException {

        if (isNull(o) || isNull(c)) {
            throw new SuperCodeException("转换的对象或者class类都不能为null");
        }
        return JSONObject.parseObject(o instanceof String ? o.toString() : JSON.toJSONString(o), c);
    }


    /**
     * 转换date格式
     * @author liujianqiang
     * @data 2018年12月29日
     * @param date
     * @return
     */
    public String formatDateToString(Date date){
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	return sdf.format(date);
    }
    
	@Override
	public String getOrganizationId() throws SuperCodeException {
		try {
			return super.getOrganizationId();
		} catch (Exception e) {
			throw new SuperCodeException("无组织信息，请确认当前用户为普通角色用户", 500);
		}
	}
}
