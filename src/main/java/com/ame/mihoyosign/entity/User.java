package com.ame.mihoyosign.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class User {
    @Id
    private Long id;
    @Column(length = 2048)
    private String cookie;
    @Column(length = 2048)
    private String ua;
    private String deviceId;
    @Column(columnDefinition = "varchar(255) default '[2,10]'")
    private String apiDelay;
    @Column(columnDefinition = "varchar(255) default '[0,1200]'")
    private String signDelay;
    //级联保存、更新、删除、刷新;延迟加载。当删除用户，会级联删除该用户的所有角色
    //拥有mappedBy注解的实体类为关系被维护端
    //mappedBy="user"是role中的user属性
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Role> roleList;//角色列表

}



