package com.yupi.yuoj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 对话配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.chat")
public class AiChatProperties {

    /**
     * 是否启用
     */
    private boolean enabled = false;

    /**
     * DeepSeek BaseUrl
     */
    private String baseUrl = "https://api.deepseek.com/v1";

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型
     */
    private String model = "deepseek-chat";

    /**
     * 请求超时（毫秒）
     */
    private int timeoutMs = 15000;

    /**
     * 流式超时（毫秒）
     */
    private long streamTimeoutMs = 60000;

    /**
     * 最大输出 tokens
     */
    private int maxOutputTokens = 800;

    /**
     * 最大输入字符数
     */
    private int maxInputChars = 12000;

    /**
     * 历史消息数上限
     */
    private int maxHistoryMessages = 10;

    /**
     * 温度
     */
    private double temperature = 0.2;
}
