package com.yupi.yuoj.mq;

import com.yupi.yuoj.constant.MqConstant;
import com.yupi.yuoj.judge.JudgeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JudgeMessageConsumer {

    private final JudgeService judgeService;

    public JudgeMessageConsumer(@Lazy JudgeService judgeService) {
        this.judgeService = judgeService;
    }

    @RabbitListener(queues = MqConstant.JUDGE_QUEUE)
    public void onMessage(String message) {
        if (StringUtils.isBlank(message)) {
            log.warn("判题消息为空，已跳过");
            return;
        }
        long questionSubmitId;
        try {
            questionSubmitId = Long.parseLong(message.trim());
        } catch (NumberFormatException e) {
            log.error("判题消息格式错误，message={}", message);
            return;
        }
        try {
            judgeService.doJudge(questionSubmitId);
        } catch (Exception e) {
            log.error("判题消息消费异常，submitId={}", questionSubmitId, e);
        }
    }
}
