package com.ame.mihoyosign.service;

import com.ame.mihoyosign.entity.Reward;
import com.ame.mihoyosign.entity.Role;
import com.ame.mihoyosign.entity.User;
import com.ame.mihoyosign.exception.NoGameException;
import com.ame.mihoyosign.exception.NullRoleException;
import com.ame.mihoyosign.exception.NullUserException;
import com.ame.mihoyosign.util.Utils;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Log4j2
@Service
public class SignService {

    @Resource
    private MiHoYoService miHoYoService;
    @Resource
    private RoleService roleService;
    @Resource
    private UserService userService;
    @Resource
    private MessageService messageService;

    @Async
    public void sign(User user) {
        try {
            long delay = Utils.delay(user.getSignDelay());
            roleService.updateNicknameAndLevel(user);
            List<Role> roles = roleService.getRoles(user.getId(), null, null);
            String sign = sign(user, roles, false);
            if (sign != null) {
                messageService.sendPrivateMsg(sign + "\n\n延迟签到:" + delay + "秒", user.getId(), -1);
            }
        } catch (NullRoleException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String sign(long qqId, String gameName, String gameUid) throws NullRoleException, NullUserException, NoGameException {
        User user = userService.getUser(qqId);
        List<Role> roles = roleService.getRoles(qqId, gameName, gameUid);
        String sign = sign(user, roles, true);
        return sign == null ? "没有角色开启签到" : sign;
    }

    public String sign(User user, List<Role> roles, boolean isCommand) {
        StringBuilder s = new StringBuilder();
        for (Role role : roles) {
            String gameBiz = role.getGameBiz();
            try {
                Reward reward = null;
                switch (gameBiz) {
                    case "hk4e_cn":
                        reward = miHoYoService.ysSign(role, user, isCommand);
                        break;
                    case "bh3_cn":
                        reward = miHoYoService.bh3Sign(role, user, isCommand);
                        break;
                }
                if (reward != null && (isCommand || role.isNotice())) {
                    s.append("「签到 成功」\n").append(Reward.getInfo(reward)).append(role.getInfo()).append("\n\n");
                }
                log.info("[签到结果:成功]  QQ:{}  游戏:{}  UID:{}",
                        user.getId(),
                        Role.getGameName(role.getGameBiz()),
                        role.getGameUid());
            } catch (Exception e) {
                s.append("「签到 失败」\n" + "原因:").append(e.getMessage()).append("\n")
                        .append(role.getInfo()).append("\n\n");
                log.info("[签到结果:失败]  QQ:{}  游戏:{}  UID:{}  原因:{}",
                        user.getId(),
                        Role.getGameName(role.getGameBiz()),
                        role.getGameUid(),
                        e.getMessage());
            }
        }
        return s.length() > 3 ? s.substring(0, s.length() - 2) : null;
    }

}
