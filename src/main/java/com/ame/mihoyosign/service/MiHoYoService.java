package com.ame.mihoyosign.service;

import com.alibaba.fastjson.JSONObject;
import com.ame.mihoyosign.dao.MiHoYoRepository;
import com.ame.mihoyosign.entity.Reward;
import com.ame.mihoyosign.entity.Role;
import com.ame.mihoyosign.entity.User;
import com.ame.mihoyosign.exception.MiHoYoApiException;
import com.ame.mihoyosign.util.Utils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class MiHoYoService {

    @Value("#{${app-config.headers}}")
    private HttpHeaders headers;
    @Value("${app-config.salt}")
    private String salt;
    @Value("${app-config.version}")
    private String version;
    @Value("${app-config.ua}")
    private String UA;
    @Resource
    private MiHoYoRepository miHoYoRepository;
    @Resource
    private RewardService rewardService;

    /**
     * 获取DS
     */
    private String getDS() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        String t = Integer.toString((int) ((ts.getTime()) / 1000));
        String r = Utils.getRandomFromArray(null, 6);
        String c = DigestUtils.md5DigestAsHex(("salt=" + salt + "&t=" + t + "&r=" + r).getBytes());
        return t + "," + r + "," + c;
    }

    public HttpHeaders getHeaders(User user) {
        HttpHeaders h = new HttpHeaders();
        h.addAll(headers);
        h.add("DS", getDS());
        h.add("x-rpc-device_id", user.getDeviceId());
        h.add("x-rpc-app_version", version);
        h.add(HttpHeaders.COOKIE, user.getCookie());
        if (user.getUa() == null) {
            h.add(HttpHeaders.USER_AGENT, UA.replace("{version}", version));
        } else {
            h.add(HttpHeaders.USER_AGENT, user.getUa().
                    replaceAll("miHoYoBBS/\\d+(.\\d+)*", "miHoYoBBS/" + version));
        }
        return h;
    }


    public List<Role> getRoles(User user, String gameBiz, String gameUid) throws MiHoYoApiException {
        JSONObject rolesByCookie = miHoYoRepository.getRolesByCookie(user.getCookie(), gameBiz);
        if (rolesByCookie.getIntValue("retcode") != 0) {
            throw new MiHoYoApiException(rolesByCookie.getString("message"));
        }

        List<Role> roles = rolesByCookie.getJSONObject("data").getJSONArray("list").toJavaList(Role.class);
        if (gameUid == null) {
            return roles;
        } else {
            return roles.stream()
                    .filter(role -> role.getGameUid().equals(gameUid))
                    .collect(Collectors.toList());
        }
    }

    @Async
    @Scheduled(cron = "${app-config.sign-reward-cron}")
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void updateRewards() {
        updateRewards("hk4e_cn", miHoYoRepository.getYsSignRewards());
        updateRewards("bh3_cn", miHoYoRepository.getBh3SignRewards());
    }

    public void updateRewards(String gameBiz, JSONObject signRewards) {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        Reward reward = rewardService.getReward(gameBiz, day);
        if (reward == null || month != reward.getMonth()) {
            List<Reward> rewards = signRewards.getJSONObject("data").getJSONArray("awards").toJavaList(Reward.class);
            rewardService.deleteRewards(gameBiz);
            month = signRewards.getJSONObject("data").getIntValue("month");
            for (int i = 0; i < rewards.size(); i++) {
                reward = rewards.get(i);
                reward.setDay(i + 1);
                reward.setGameBiz(gameBiz);
                reward.setMonth(month);
            }
            rewardService.updateReward(rewards);
        }
    }


    public Reward ysSign(Role role, User user, boolean isCommand) throws MiHoYoApiException {
        String apiDelay = user.getApiDelay();
        Utils.delay(apiDelay);
        HttpHeaders headers = getHeaders(user);
        JSONObject signInInfo = miHoYoRepository.getYsSignInfo(role, headers);
        if (signInInfo.getJSONObject("data").getBooleanValue("is_sign") && isCommand) {
            int day = signInInfo.getJSONObject("data").getIntValue("total_sign_day");
            return rewardService.getReward("hk4e_cn", day);
        } else if (signInInfo.getJSONObject("data").getBooleanValue("is_sign")) {
            return null;
        }
        Utils.delay(apiDelay);
        JSONObject sign = miHoYoRepository.ysSign(role, headers);
        if (sign.getIntValue("retcode") == 0) {
            Utils.delay(apiDelay);
            signInInfo = miHoYoRepository.getYsSignInfo(role, headers);
            if (signInInfo.getJSONObject("data").getBooleanValue("is_sign")) {
                int day = signInInfo.getJSONObject("data").getIntValue("total_sign_day");
                return rewardService.getReward("hk4e_cn", day);
            } else {
                throw new MiHoYoApiException("需要验证码,建议修改为常用设备的User-Agent");
            }
        } else {
            throw new MiHoYoApiException(sign.getString("message"));
        }
    }

    public Reward bh3Sign(Role role, User user, boolean isCommand) throws MiHoYoApiException {
        String apiDelay = user.getApiDelay();
        Utils.delay(apiDelay);
        HttpHeaders headers = getHeaders(user);
        JSONObject signInInfo = miHoYoRepository.getBh3SignInfo(role, headers);
        if (signInInfo.getJSONObject("data").getBooleanValue("is_sign") && isCommand) {
            int day = signInInfo.getJSONObject("data").getIntValue("total_sign_day");
            return rewardService.getReward("bh3_cn", day);
        } else if (signInInfo.getJSONObject("data").getBooleanValue("is_sign")) {
            return null;
        }
        Utils.delay(apiDelay);
        JSONObject sign = miHoYoRepository.bh3Sign(role, headers);
        if (sign.getIntValue("retcode") == 0) {
            Utils.delay(apiDelay);
            signInInfo = miHoYoRepository.getBh3SignInfo(role, headers);
            if (signInInfo.getJSONObject("data").getBooleanValue("is_sign")) {
                int day = signInInfo.getJSONObject("data").getIntValue("total_sign_day");
                return rewardService.getReward("bh3_cn", day);
            } else {
                throw new MiHoYoApiException("需要验证码,建议修改为常用设备的User-Agent");
            }
        } else {
            throw new MiHoYoApiException(sign.getString("message"));
        }
    }
}
