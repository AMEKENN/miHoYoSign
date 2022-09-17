package com.ame.mihoyosign;

import com.ame.mihoyosign.service.MiHoYoService;
import com.ame.mihoyosign.service.RoleService;
import com.ame.mihoyosign.service.SignService;
import com.ame.mihoyosign.service.UserService;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class MiHoYoSignApplicationTests {
    @Resource
    MiHoYoService miHoYoService;
    @Resource
    UserService userService;
    @Resource
    RoleService roleService;
    @Resource
    SignService signService;

}
