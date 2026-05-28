package com.xiao.stockproject.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xiaohuijia
 * @date 2025/11/27 9:31
 */
public class DingDingMessageUtils {
    public static String url = "https://oapi.dingtalk.com/robot/send?access_token=e18782f2b99bfb0cb9452a7859ded1975f1b1d4d994eb1c71d56e62a1aaebe9d";
    //public static String url = "https://oapi.dingtalk.com/robot/send?access_token=627f31d7f733e0e4033a5a1d3c7bb97e89154f7f6f9d224f28d2deee52ac8500";
    //public static String keyWords = "提醒:";
    public static String keyWords = "提醒:";
    public static String sendMessage(String json) {
        for (int i = 0; i < 3; i++) {
            try {
                HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
                httpRequestFactory.setConnectionRequestTimeout(500);
                httpRequestFactory.setConnectTimeout(500);
                httpRequestFactory.setReadTimeout(500);
                RestTemplate template = new RestTemplate(httpRequestFactory);
                //ClientHttpRequestFactory clientFactory = new HttpComponentsClientHttpRequestFactory();
                //template.setRequestFactory(clientFactory);
                URI uri = UriComponentsBuilder.fromUriString(url) //
                        .build().encode().toUri();
                HttpHeaders requestHeaders = new HttpHeaders();
                requestHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
                requestHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON_UTF8));
                @SuppressWarnings({ "rawtypes", "unchecked" })
                HttpEntity<String> requestEntity = new HttpEntity(json,requestHeaders);
                ResponseEntity<String> responseEntity = template.exchange(uri, HttpMethod.POST, requestEntity,
                        String.class);
                JSONObject jsonObject = JSON.parseObject(responseEntity.getBody());
                if(jsonObject.getInteger("errcode") == 0){
                    break;
                }
                return responseEntity.getBody();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String sendTextMsg(String text,String iphone) {
        JSONObject json = new JSONObject();
        json.put("msgtype", "text");
        Map<String,String> map = new HashMap<>();
        map.put("content", keyWords + text);
        json.put("text",map);
        if(iphone != null){
            Map<String, List<String>> atMap = new HashMap<>();
            atMap.put("atMobiles", Arrays.asList(iphone));
            json.put("at",atMap);
        }
        return sendMessage(json.toString());
    }

    public static String sendLinkMsg(String title,String text,String picUrl,String messageUrl) {
        JSONObject json = new JSONObject();
        json.put("msgtype", "link");
        Map<String,String> map = new HashMap<>();
        map.put("text", keyWords + text);
        map.put("title", title);
        map.put("picUrl", picUrl);
        map.put("messageUrl", messageUrl);
        json.put("link",map);
        return sendMessage(json.toString());
    }

//    public static void main(String[] args) {
//        sendTextMsg("今日没有消息发送！");
//    }
}
