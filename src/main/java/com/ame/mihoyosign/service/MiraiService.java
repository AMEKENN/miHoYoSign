package com.ame.mihoyosign.service;

import com.alibaba.fastjson.JSON;
import com.ame.mihoyosign.entity.Role;
import com.ame.mihoyosign.entity.mirai.Message;
import com.ame.mihoyosign.exception.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;

@Log4j2
@Service
public class MiraiService {

    @Value("${app-config.admin-qq-id}")
    private long adminQqId;
    @Value("${app-config.help}")
    private String help;
    @Value("#{${app-config.commands}}")
    private Map<String, String> commands;
    @Resource
    private SignService signService;
    @Resource
    private RoleService roleService;
    @Resource
    private AdminService adminService;
    @Resource
    private UserService userService;
    @Resource
    private MiHoYoService miHoYoService;
    @Resource
    private MessageService messageService;


    public void onMessage(String messageStr) {
        //log.info(messageStr);
        Message message = JSON.parseObject(messageStr, Message.class);
        String msg = message.getMessage();

        if ("group".equals(message.getMessage_type()) && "帮助".equals(commands.get(msg))) {
            messageService.sendGroupMsg(help, message.getGroup_id());
        } else if ("private".equals(message.getMessage_type())) {
            long qqId = message.getUser_id();
            log.info("<--  QQ:[{}]  Message:[{}]", qqId, msg.replace("\n", " "));

            //检测是否使用管理员模式
            boolean adminMode = false;
            if (msg.startsWith("(QQ:") && qqId == adminQqId) {
                try {
                    qqId = Long.parseLong(msg.substring(4, msg.indexOf(')')));
                    msg = msg.substring(msg.indexOf(')') + 1);
                    adminMode = true;
                } catch (Exception e) {
                    messageService.sendPrivateMsg("管理员模式的格式错误", qqId, -1);
                    return;
                }
            }

            String[] s = msg.split(" ", 3);
            String re;

            if (commands.containsKey(s[0])) {
                String command = commands.get(s[0]);
                String[] args = Arrays.copyOfRange(s, 1, 3);
                if (args[0] != null && args[0].matches("[0-9]+")) {
                    re = function(qqId, msg, command, args[1], args[0]);
                } else {
                    re = function(qqId, msg, command, args[0], args[1]);
                }
            } else {
                return;
            }
            if (re != null) {
                messageService.sendPrivateMsg(re, adminMode ? adminQqId : qqId, message.getSender().getGroup_id());
            }
        }
    }

    private String function(long qqId, String msg, String command, String str, String num) {
        //所有功能
        String re = null;
        try {
            switch (command) {
                case "帮助":
                    re = help;
                    break;
                case "绑定":
                    re = userService.bind(qqId, msg.substring(3));
                    break;
                case "解绑":
                    re = userService.unbind(qqId);
                    break;
                case "修改UA":
                    re = userService.setUA(qqId, msg.substring(5));
                    break;
                case "修改api延迟":
                    re = userService.setApiDelay(qqId, str);
                    break;
                case "修改签到延迟":
                    re = userService.setSignDelay(qqId, str);
                    break;
                case "开启签到":
                    re = roleService.turnOnSign(qqId, str, num);
                    break;
                case "关闭签到":
                    re = roleService.turnOffSign(qqId, str, num);
                    break;
                case "签到":
                    re = signService.sign(qqId, str, num);
                    break;
                case "开启通知":
                    re = roleService.changeNotice(qqId, str, num, true);
                    break;
                case "关闭通知":
                    re = roleService.changeNotice(qqId, str, num, false);
                    break;
                case "所有角色":
                    re = "「米游社中所有角色」\n" + Role.getRolesInfo(miHoYoService.getRoles(userService.getUser(qqId), null, null));
                    break;
                case "已开启角色":
                    re = roleService.getSignOnRolesInfo(qqId);
                    break;
                case "通知":
                    re = adminService.sendNotice(qqId, str, num);
                    break;
                case "所有用户角色":
                    re = adminService.getAllRole(qqId);
                    break;
                case "更新奖励列表":
                    adminService.updateRewards(qqId);
                    re = "正在更新奖励表";
                    break;
                case "全部签到":
                    adminService.checkAdmin(qqId);
                    adminService.signAll();
                    re = "正在全部签到";
                    break;
            }
        } catch (IndexOutOfBoundsException e) {
            re = "指令格式错误";
        } catch (NullUserException e) {
            re = "未绑定米游社";
        } catch (NullRoleException e) {
            re = "没有角色";
        } catch (NoGameException e) {
            re = "游戏名错误";
        } catch (MiHoYoApiException e) {
            re = "调用米游社API错误\n米游社返回消息:\n\t" + e.getMessage();
        } catch (NotAdminException ignored) {
        } catch (Exception e) {
            re = "指令格式错误或执行过程中出错";
            log.warn(re);
            e.printStackTrace();
        }
        return re;
    }

}
