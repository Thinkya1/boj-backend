package biny.biny.ai;

import biny.biny.config.AiChatProperties;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

/**
 * AI 对话模型工厂
 */
public interface AiChatModelFactory {

    StreamingChatLanguageModel create(AiChatProperties properties);
}
