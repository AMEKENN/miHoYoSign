package com.ame.mihoyosign.service;

import com.ame.mihoyosign.dao.RewardRepository;
import com.ame.mihoyosign.entity.Reward;
import com.ame.mihoyosign.entity.Role;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RewardService {

    @Resource
    private RewardRepository rewardRepository;
    @Resource
    private MessageService messageService;

    public Reward getReward(String gameBiz, int day) {
        return rewardRepository.findRewardByGameBizAndDay(gameBiz, day);
    }

    public void updateReward(List<Reward> rewards) {
        rewardRepository.saveAll(rewards);
        messageService.sendLog(Role.getGameName(rewards.get(0).getGameBiz()) + " 签到奖励列表已更新");
    }

    public void deleteRewards(String gameBiz) {
        rewardRepository.deleteRewardsByGameBiz(gameBiz);
    }
}