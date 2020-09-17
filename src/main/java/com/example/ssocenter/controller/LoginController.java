package com.example.ssocenter.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.ssocenter.constants.GlobalContants;
import com.example.ssocenter.model.SonSystemInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/sso")
@Data
@Slf4j
public class LoginController {

    @Autowired
    private GlobalContants globalContants;

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/isLogin")
    @ResponseBody
    public void isLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURL = request.getRequestURL().toString();
        log.info("isLogin getRequestURL: " + requestURL);
        String request_url = request.getParameter("service");
        String sysName = request.getParameter("sysName");
        String globalSessionId = request.getSession().getId();
        if (!globalContants.getSysNameList().contains(sysName)) {
            log.info("该系统不在sso管理中，请联系sso管理员");
            String redirectUrl = request.getContextPath() + "/error/403.html";
            response.sendRedirect(redirectUrl);
            return;
        }
        Map<String, String> sessionIdTokenMap = globalContants.getSessionIdTokenMap();
        String token = sessionIdTokenMap.get(globalSessionId);
        //检查全局会话sessionId是否存在，同一个浏览器是否已经登录过？只要全局会话存在，就说明已经有系统登录过
        if (StringUtils.isNotBlank(token)) {
            Map<String, List<SonSystemInfo>> tokenSonSysSessionInfoListMap = globalContants.getTokenSonSysSessionInfoListMap();
            List<SonSystemInfo> sonSystemInfos = tokenSonSysSessionInfoListMap.get(token);
            SonSystemInfo sonSystemInfoOld = sonSystemInfos.get(0);
            //将来校验的子系统加入全局缓存
            String hostAddress = request.getParameter("hostAddress");
            String exitUrl = request.getParameter("exitUrl");
            String sessionId = request.getParameter("sessionId");
            SonSystemInfo sonSystemInfo = new SonSystemInfo(hostAddress, exitUrl, sessionId, sonSystemInfoOld.getUsername(), sonSystemInfoOld.getPassword());
            sonSystemInfos.add(sonSystemInfo);

            request_url += "?isLogin=yes";
            request_url += "&isLoginToken=" + token;
            request_url += "&isLoginUsername=" + URLEncoder.encode(sonSystemInfo.getUsername(), "utf-8");
            request_url += "&isLoginPassword=" + URLEncoder.encode(sonSystemInfo.getPassword(), "utf-8");
            response.sendRedirect(request_url);
        } else {
            //没在任意一个子系统登录过
            String redirectUrl = request.getContextPath()+"/index.html";
            redirectUrl += "?service=" + request_url;
            log.info("isLogin redirectUrl: " + redirectUrl);
            response.sendRedirect(redirectUrl);
        }
    }

    @RequestMapping("/login")
    public void login(HttpServletRequest request, HttpServletResponse response, String username, String password, String request_url) throws IOException {
        //不校验
        if (true) {
        } else {
            response.sendRedirect(request.getContextPath());
        }
        //登录成功，设置全局会话，先放key ，后期改为redis
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        Map<String, String> sessionIdTokenMap = globalContants.getSessionIdTokenMap();
        String globalSessionId = request.getSession().getId();
        sessionIdTokenMap.put(globalSessionId, token);
        Map<String, List<SonSystemInfo>> tokenSonSysSessionInfoListMap = globalContants.getTokenSonSysSessionInfoListMap();
        tokenSonSysSessionInfoListMap.put(token, new ArrayList<>());
        log.info("设置全局会话:globalSessionId: {},token: {}", globalSessionId, token);
        String redirectUrl = request_url;
        redirectUrl += "?tokenFromSSO=" + token;
        redirectUrl += "&username=" + URLEncoder.encode(username, "utf-8");
        redirectUrl += "&password=" + URLEncoder.encode(password, "utf-8");
        log.info("login redirectUrl: " + redirectUrl);
        response.sendRedirect(redirectUrl);
        return;
    }

    @RequestMapping("/tokenCheck")
    @ResponseBody
    public void tokenCheck(HttpServletRequest request, HttpServletResponse response, @RequestBody JSONObject jsonObject) throws IOException {
        String service = jsonObject.getString("service");
        String token = jsonObject.getString("tokenCheck");
        String hostAddress = jsonObject.getString("hostAddress");
        String exitUrl = jsonObject.getString("exitUrl");
        String username = jsonObject.getString("username");
        String password = jsonObject.getString("password");
        String sessionId = jsonObject.getString("sessionId");
        //todo 暂用map存放，后期将token放进redis管理，不在需要全局会话map
        Map<String, List<SonSystemInfo>> tokenSonSysSessionInfoListMap = globalContants.getTokenSonSysSessionInfoListMap();
        if (tokenSonSysSessionInfoListMap.containsKey(token)) {
            SonSystemInfo sonSystemInfo = new SonSystemInfo(hostAddress, exitUrl, sessionId, username, password);
            List<SonSystemInfo> sonSystemInfos = tokenSonSysSessionInfoListMap.get(token);
            sonSystemInfos.add(sonSystemInfo);
            String redirectUrl = service + "?isChecked=yes";
            log.info("tokenCheck redirectUrl: " + redirectUrl);
            response.sendRedirect(redirectUrl);
        } else {
            String redirectUrlFailed = request.getContextPath() + "?service=" + service;
            log.info("tokenCheck redirectUrlFailed: " + redirectUrlFailed);
            response.sendRedirect(redirectUrlFailed);
        }
    }

    @RequestMapping("/getRequestUrl")
    @ResponseBody
    public String getRequestUrl(HttpServletRequest request) {
        Map<String, String> map = globalContants.getMap();
        String request_url = map.get("service_" + request.getSession().getId());
        return request_url;
    }

    @RequestMapping("/exit")
    @ResponseBody
    public void exit(@RequestBody JSONObject jsonObject) {
        String token = jsonObject.getString("token");
        String key = null;
        Map<String, String> sessionIdTokenMap = globalContants.getSessionIdTokenMap();
        for (Map.Entry<String, String> entry : sessionIdTokenMap.entrySet()) {
            if (token.equalsIgnoreCase(entry.getValue())) {
                key = entry.getKey();
            }
        }
        if (key != null) {
            sessionIdTokenMap.remove(key);
            log.info("sessionIdTokenMap.size():" + sessionIdTokenMap.size());
            log.info("sso单点注销，全局会话注销成功");
        }
        Map<String, List<SonSystemInfo>> tokenSonSysSessionInfoListMap = globalContants.getTokenSonSysSessionInfoListMap();
        List<SonSystemInfo> sonSystemInfos = tokenSonSysSessionInfoListMap.get(token);
        if (!CollectionUtils.isEmpty(sonSystemInfos)) {
            for (SonSystemInfo sonSystemInfo : sonSystemInfos) {
                try {
                    String sessionId = sonSystemInfo.getSessionId();
                    JSONObject json = new JSONObject();
                    json.put("sessionId", sessionId);
                    String requestParam = json.toJSONString();
                    URI uri = UriComponentsBuilder.fromHttpUrl(sonSystemInfo.getSonHostAddress()).path(sonSystemInfo.getSonExitUrl()).build(true).toUri();
                    RequestEntity<String> requestEntity = RequestEntity.post(uri).accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).body(requestParam);
                    restTemplate.exchange(requestEntity, JSONObject.class);
                    log.info("sso单点注销发送成功: " + uri.toString());
                } catch (RestClientException e) {
                    log.info(e.getMessage(), e);
                }
            }
        }
        tokenSonSysSessionInfoListMap.remove(token);
        log.info("tokenSonSysSessionInfoListMap.size():" + tokenSonSysSessionInfoListMap.size());
        log.info("sso全局会话注销成功");
    }


}
