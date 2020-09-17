package com.example.ssocenter.constants;

import com.example.ssocenter.model.SonSystemInfo;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Data
public class GlobalContants {

    //用户存放外界跳转的request_url
    private Map<String,String> map=new ConcurrentHashMap<>();

    // todo 后期用redis代替，可设置超时时间
    //每个令牌对应的【子系统会话id，子系统根路径，子系统注销接口】列表，后期可改为本地读取，不需要传过来
    private Map<String, List<SonSystemInfo>> tokenSonSysSessionInfoListMap=new ConcurrentHashMap<>();

    //sessionId token
    private Map<String,String> sessionIdTokenMap=new ConcurrentHashMap<>();

    private List<String> sysNameList=new ArrayList<>();

    public GlobalContants() {
        sysNameList.add("ssoa");
        sysNameList.add("ssob");
    }
}
