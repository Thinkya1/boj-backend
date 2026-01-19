package biny.biny.config;

import biny.biny.constant.MqConstant;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue judgeQueue() {
        return new Queue(MqConstant.JUDGE_QUEUE, true);
    }
}
