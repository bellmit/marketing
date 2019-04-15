package com.jgw.supercodeplatform.marketing.config.web.mvc;

import com.jgw.supercodeplatform.exception.SuperCodeException;
import com.jgw.supercodeplatform.marketing.common.util.JWTUtil;
import com.jgw.supercodeplatform.marketing.constants.CommonConstants;
import com.jgw.supercodeplatform.marketing.dto.integral.JwtUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * mvc参数额外解析：JwtUser.class
 * 注意：凡是CONTROLLER接口参数注入JWTUSER全部会在这里被解析
 */
@Component
public class SecurityParamResolver implements HandlerMethodArgumentResolver {
    private static Logger logger = LoggerFactory.getLogger(SecurityParamResolver.class);


    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        Class<?> clazz=methodParameter.getParameterType();
        // 指定什么样的对象需要被解析
        return clazz== JwtUser.class;
    }

    /**
     * controller 方法参数包含 JwtUser则进行解析;当解析失败返回null
     * @param methodParameter
     * @param modelAndViewContainer
     * @param nativeWebRequest
     * @param webDataBinderFactory
     * @return
     * @throws Exception
     */
    @Override
    public JwtUser resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws SuperCodeException {
        String token = null;
        try {
            HttpServletResponse response = nativeWebRequest.getNativeResponse(HttpServletResponse.class);
            HttpServletRequest request = nativeWebRequest.getNativeRequest(HttpServletRequest.class);
            token = request.getHeader(CommonConstants.JWT_TOKEN);
            if (token == null) {
                throw new SuperCodeException("用户获取失败...");
            }
            JwtUser jwtUser = JWTUtil.verifyToken(token);
            if (jwtUser == null || jwtUser.getMemberId() == null) {
                logger.error("jwt信息不全" + jwtUser);
                throw new SuperCodeException("用户信息不存在...");
            }
            return jwtUser;
        } catch (Exception e) {
            logger.error("解析jwt异常" + token);
            e.printStackTrace();
            throw new SuperCodeException("用户信息解析异常...");
        }
    }
}