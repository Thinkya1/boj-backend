package com.yupi.yuoj.service.impl;

import com.yupi.yuoj.ai.DeepSeekChatClient;
import com.yupi.yuoj.common.ErrorCode;
import com.yupi.yuoj.config.AiChatProperties;
import com.yupi.yuoj.exception.BusinessException;
import com.yupi.yuoj.model.dto.ai.AiChatMessage;
import com.yupi.yuoj.model.dto.ai.AiChatRequest;
import com.yupi.yuoj.model.entity.Question;
import com.yupi.yuoj.model.entity.User;
import com.yupi.yuoj.service.AiChatService;
import com.yupi.yuoj.service.QuestionService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务实现
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是资深算法竞赛与后端工程师，负责帮助用户修正代码。",
            "请根据题目描述、当前代码与用户问题给出修改方案。",
            "输出要求：",
            "1) 必须给出统一 diff 补丁（unified diff），用 ```diff``` 包裹。",
            "2) 补丁必须基于“当前代码”。",
            "3) 如需澄清问题，请先提问，不要编造不存在的测试数据或隐藏用例。",
            "4) 可以在 diff 之外给出简要说明，中文输出。"
    );

    private final AiChatProperties properties;

    private final DeepSeekChatClient chatClient;

    private final QuestionService questionService;

    public AiChatServiceImpl(AiChatProperties properties,
                             DeepSeekChatClient chatClient,
                             QuestionService questionService) {
        this.properties = properties;
        this.chatClient = chatClient;
        this.questionService = questionService;
    }

    @Override
    public SseEmitter streamChat(AiChatRequest request, User loginUser) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 功能未开启");
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求为空");
        }
        Long questionId = request.getQuestionId();
        if (questionId == null || questionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目 id 非法");
        }
        if (StringUtils.isBlank(request.getLanguage())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "语言不能为空");
        }
        String code = request.getCode();
        if (StringUtils.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码不能为空");
        }
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        List<AiChatMessage> messages = buildMessages(request, question);
        SseEmitter emitter = new SseEmitter(properties.getStreamTimeoutMs());
        CompletableFuture.runAsync(() -> {
            try {
                chatClient.streamChat(messages, chunk -> sendChunk(emitter, chunk));
                sendChunk(emitter, "[DONE]");
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private List<AiChatMessage> buildMessages(AiChatRequest request, Question question) {
        List<AiChatMessage> messages = new ArrayList<>();
        AiChatMessage system = new AiChatMessage();
        system.setRole("system");
        system.setContent(SYSTEM_PROMPT);
        messages.add(system);

        List<AiChatMessage> history = trimHistory(request.getHistory());
        messages.addAll(history);

        AiChatMessage user = new AiChatMessage();
        user.setRole("user");
        user.setContent(buildUserPrompt(request, question));
        messages.add(user);
        return messages;
    }

    private List<AiChatMessage> trimHistory(List<AiChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }
        int maxHistory = properties.getMaxHistoryMessages();
        if (maxHistory <= 0) {
            maxHistory = 10;
        }
        List<AiChatMessage> filtered = history.stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.isNotBlank(item.getRole()))
                .filter(item -> StringUtils.isNotBlank(item.getContent()))
                .filter(item -> isRoleSupported(item.getRole()))
                .collect(Collectors.toList());
        if (filtered.size() <= maxHistory) {
            return filtered;
        }
        return filtered.subList(filtered.size() - maxHistory, filtered.size());
    }

    private boolean isRoleSupported(String role) {
        String normalized = role.toLowerCase(Locale.ROOT);
        return "user".equals(normalized) || "assistant".equals(normalized);
    }

    private String buildUserPrompt(AiChatRequest request, Question question) {
        StringBuilder builder = new StringBuilder();
        builder.append("题目标题：").append(nullToDash(question.getTitle())).append('\n');
        builder.append("题目描述：\n").append(clip(question.getContent())).append('\n');
        builder.append("语言：").append(nullToDash(request.getLanguage())).append('\n');
        builder.append("用户问题：").append(nullToDash(request.getPrompt())).append('\n');
        builder.append("当前代码：\n");
        builder.append("```").append(normalizeLang(request.getLanguage())).append('\n');
        builder.append(clip(request.getCode())).append('\n');
        builder.append("```");
        return builder.toString();
    }

    private String clip(String value) {
        if (StringUtils.isBlank(value)) {
            return "-";
        }
        String trimmed = value.trim();
        int maxLength = properties.getMaxInputChars();
        if (maxLength <= 0) {
            maxLength = 12000;
        }
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...(已截断)";
    }

    private String nullToDash(String value) {
        return StringUtils.isBlank(value) ? "-" : value.trim();
    }

    private String normalizeLang(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().data(chunk));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
