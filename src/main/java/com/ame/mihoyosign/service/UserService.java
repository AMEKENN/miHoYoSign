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
        User user = new User(qqId, cookie,
                UUID.randomUUID().toString(), ApiDelay, signDelay, null);
        List<Role> roles = miHoYoService.getRoles(user, null, null);
        try {
            unbind(qqId);
        } catch (NullUserException ignored) {
        }
        messageService.sendLog("新的用户绑定:" + qqId);
        userRepository.save(user);
        return "已成功绑定或更换账号\n「米游社中所有角色」\n" + Role.getRolesInfo(roles);
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
            if (delay == null || delay.equals("无")) {
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
