package com.yupi.yuoj.ai;

import com.yupi.yuoj.config.AiChatProperties;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

/**
 * AI 对话模型工厂
 */
public interface AiChatModelFactory {

    StreamingChatLanguageModel create(AiChatProperties properties);
}
