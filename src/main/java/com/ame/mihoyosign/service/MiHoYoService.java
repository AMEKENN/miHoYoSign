package com.ame.mihoyosign.service;

import com.alibaba.fastjson.JSONObject;
import com.ame.mihoyosign.dao.MiHoYoRepository;
import com.ame.mihoyosign.entity.Role;
import com.ame.mihoyosign.entity.User;
import com.ame.mihoyosign.entity.Reward;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class MiHoYoService {

    @Value("#{${ys.headers}}")
    private HttpHeaders ysHeaders;
    @Value("#{${bh3.headers}}")
    private HttpHeaders bh3Headers;
    @Value("${ys.salt}")
    private String salt;

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

    public HttpHeaders getYsHeaders(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(ysHeaders);
        headers.add("DS", getDS());
        headers.add("x-rpc-device_id", user.getDeviceId());
        headers.add(HttpHeaders.COOKIE, user.getCookie());
        return headers;
    }

    public HttpHeaders getBh3Headers(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(bh3Headers);
        headers.add("x-rpc-device_id", user.getDeviceId());
        headers.add(HttpHeaders.COOKIE, user.getCookie());
        return headers;
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
    public synchronized void updateYsRewards() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        Reward reward = rewardService.getReward("hk4e_cn", day);
        if (reward == null || month != reward.getMonth()) {
            JSONObject ysSignRewards = miHoYoRepository.getYsSignRewards();
            List<Reward> rewards = ysSignRewards.getJSONObject("data").getJSONArray("awards").toJavaList(Reward.class);
            rewardService.deleteRewards("hk4e_cn");
            month = ysSignRewards.getJSONObject("data").getIntValue("month");
            for (int i = 0; i < rewards.size(); i++) {
                reward = rewards.get(i);
                reward.setDay(i + 1);
                reward.setGameBiz("hk4e_cn");
                reward.setMonth(month);
            }
            rewardService.updateReward(rewards);
        }
    }

    public Reward ysSign(Role role, User user) throws MiHoYoApiException {
        String apiDelay = user.getApiDelay();
        Utils.delay(apiDelay);
        HttpHeaders headers = getYsHeaders(user);
        JSONObject sign = miHoYoRepository.ysSign(role, headers);
        if (sign.getIntValue("retcode") == 0) {
            Utils.delay(apiDelay);
            JSONObject signInInfo = miHoYoRepository.getYsSignInfo(role, headers);
            JSONObject data = signInInfo.getJSONObject("data");
            int day = data.getIntValue("total_sign_day");
            return rewardService.getReward("hk4e_cn", day);
        } else {
            throw new MiHoYoApiException(sign.getString("message"));
        }
    }

    public Reward bh3Sign(Role role, User user) throws ParseException, MiHoYoApiException {
        Utils.delay(user.getApiDelay());
        JSONObject sign = miHoYoRepository.bh3Sign(role, getBh3Headers(user));
        if (sign.getIntValue("retcode") == 0) {
            JSONObject data = sign.getJSONObject("data");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date beginDate = new Date();
            Date endDate = format.parse(data.getString("now"));
            int day = (int) (endDate.getTime() - beginDate.getTime()) / (24 * 60 * 60 * 1000);
            return data.getJSONArray("list").getObject(day, Reward.class);
        } else {
            throw new MiHoYoApiException(sign.getString("message"));
        }
    }
}
