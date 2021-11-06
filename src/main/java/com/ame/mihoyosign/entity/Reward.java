package com.ame.mihoyosign.entity;


import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class Reward {

    @Id
    @GeneratedValue
    private int id;
    private int month;
    private int day;
    private String gameBiz;
    private String icon;
    private String name;
    private int cnt;

    public static String getInfo(Reward reward) {
        if (reward == null) {
            return "奖励" + "暂未更新奖励";
        } else {
            return "奖励:" + reward.getName() + "×" + reward.getCnt() +
                    "\n[CQ:image,file=" + reward.getIcon() + "]";
        }
    }

}
