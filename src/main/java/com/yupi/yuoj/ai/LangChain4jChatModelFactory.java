package com.yupi.yuoj.ai;

import com.yupi.yuoj.config.AiChatProperties;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * LangChain4j 模型工厂
 */
@Component
public class LangChain4jChatModelFactory implements AiChatModelFactory {

    @Override
    public StreamingChatLanguageModel create(AiChatProperties properties) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxOutputTokens())
                .tokenizer(new OpenAiTokenizer("gpt-3.5-turbo"))
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .build();
    }
}
