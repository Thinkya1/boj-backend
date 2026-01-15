package com.yupi.yuoj.service;

import com.yupi.yuoj.model.dto.ai.AiChatRequest;
import com.yupi.yuoj.model.entity.User;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务
 */
public interface AiChatService {

    /**
     * 流式对话
     *
     * @param request 对话请求
     * @param loginUser 登录用户
     * @return SseEmitter
     */
    SseEmitter streamChat(AiChatRequest request, User loginUser);
}
