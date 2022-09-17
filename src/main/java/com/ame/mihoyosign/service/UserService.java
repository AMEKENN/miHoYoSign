package com.ame.mihoyosign.service;

import com.ame.mihoyosign.dao.UserRepository;
import com.ame.mihoyosign.entity.Role;
import com.ame.mihoyosign.entity.User;
import com.ame.mihoyosign.exception.MiHoYoApiException;
import com.ame.mihoyosign.exception.NullUserException;
import com.ame.mihoyosign.util.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    @Resource
    private UserRepository userRepository;
    @Resource
    private MiHoYoService miHoYoService;
    @Resource
    private MessageService messageService;
    @Value("${app-config.api-delay}")
    private String ApiDelay;
    @Value("${app-config.api-delay-max}")
    private String ApiDelayMax;
    @Value("${app-config.sign-delay}")
    private String signDelay;
    @Value("${app-config.sign-delay-max}")
    private String signDelayMax;


    public String bind(long qqId, String cookie) throws MiHoYoApiException {
        User user = userRepository.findUserById(qqId);
        String r;
        if (user == null) {
            user = new User(qqId, cookie, null, UUID.randomUUID().toString(), ApiDelay, signDelay, null);
            r = "新用户绑定";
        } else {
            user.setCookie(cookie);
            r = "旧用户更新绑定";
        }
        List<Role> roles = miHoYoService.getRoles(user, null, null);
        messageService.sendLog(r + qqId);
        userRepository.save(user);
        return "已成功绑定或更新账号\n「米游社中所有角色」\n" + Role.getRolesInfo(roles);
    }


    public String unbind(long qqId) throws NullUserException {
        User user = getUser(qqId);
        userRepository.delete(user);
        return "已解除绑定";
    }

    public String setApiDelay(long qqId, String delay) throws NullUserException {
        User user = getUser(qqId);
        try {
            if (delay == null || delay.equals("无")) {
                delay = null;
            } else {
                delay = Utils.parseRange(delay, ApiDelayMax);
            }
            user.setApiDelay(delay);
            userRepository.save(user);
            return "已修改延迟:" + (delay == null ? "关" : delay);
        } catch (Exception e) {
            return "修改失败:" + e.getMessage();
        }
    }

    public String setSignDelay(long qqId, String delay) throws NullUserException {
        User user = getUser(qqId);
        try {
            if (delay == null || delay.equals("关")) {
                delay = null;
            } else {
                delay = Utils.parseRange(delay, signDelayMax);
            }
            user.setSignDelay(delay);
            userRepository.save(user);
            return "已修改延迟:" + (delay == null ? "关" : delay);
        } catch (Exception e) {
            return "修改失败:" + e.getMessage();
        }
    }

    public String setUA(long qqId, String ua) throws NullUserException {
        if (ua == null || "".equals(ua)) {
            return "UA错误,用米游社扫描此二维码获取 User-Agent\n" +
                    "[CQ:image,file=https://i.postimg.cc/FKxHmRxX/image.png]";
        }
        User user = getUser(qqId);
        user.setUa(ua);
        user = userRepository.save(user);
        return "已修改User-Agent:\n" + user.getUa();
    }

    public User getUser(long qqId) throws NullUserException {
        User user = userRepository.findUserById(qqId);
        if (user == null) {
            throw new NullUserException();
        }
        return user;
    }

    public List<User> getAllUser() {
        return userRepository.findAll();
    }
}
