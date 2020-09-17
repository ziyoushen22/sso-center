package com.example.ssocenter.interceptor;

import com.example.ssocenter.constants.GlobalContants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Slf4j
@Data
@Component
public class UserSecurityInterceptor implements HandlerInterceptor {


    @Autowired
    private GlobalContants globalContants;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String url = request.getRequestURL().toString();
        if (url.contains("index.html")) {
            String service = request.getParameter("service");
            if (StringUtils.isNotBlank(service)) {
                Map<String, String> map = globalContants.getMap();
                map.put("service_" + request.getSession().getId(), service);
            }
        }
        return true;
    }


}
