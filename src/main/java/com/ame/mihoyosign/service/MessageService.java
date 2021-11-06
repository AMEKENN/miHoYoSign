package com.ame.mihoyosign.service;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Log4j2
@Service
public class MessageService {
    @Resource
    private RestTemplate restTemplate;
    @Value("${app-config.url}")
    private String url;
    @Value("${app-config.log-group-id}")
    private long LogGroupId;
    @Value("${app-config.admin-qq-id}")
    private long adminQQId;

    public void sendMsg(String msg, String type, long userId, long groupId) {
        try {
            JSONObject json = new JSONObject();
            json.put("message", msg);
            if (groupId != -1) {
                json.put("group_id", groupId);
            }
            if (userId != -1) {
                json.put("user_id", userId);
            }
            JSONObject re = restTemplate.postForObject(url + "/send_" + type + "_msg", json, JSONObject.class);
            if (re == null || re.getIntValue("retcode") != 0) {
                log.warn("发送消息失败");
            }
            log.info("-->  QQ[{}]  Message:[{}]", userId, msg.replace("\n", " "));
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("发送消息失败");
        }
    }

    public void sendPrivateMsg(String msg, long userId, long groupId) {
        sendMsg(msg, "private", userId, groupId);
    }

    public void sendGroupMsg(String msg, long groupId) {
        sendMsg(msg, "group", -1, groupId);
    }

    @Async
    public void sendLog(String msg) {
        log.info(msg.replace("\n", " "));
        if (LogGroupId == -2) {
            sendPrivateMsg(msg, adminQQId, -1);
        } else if (LogGroupId != -1) {
            sendGroupMsg(msg, LogGroupId);
        }
    }

}
