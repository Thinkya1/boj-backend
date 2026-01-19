package biny.biny.controller;

import biny.biny.model.dto.ai.AiChatRequest;
import biny.biny.model.entity.User;
import biny.biny.service.AiChatService;
import biny.biny.service.UserService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话接口
 */
@RestController
@RequestMapping("/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    private final UserService userService;

    public AiChatController(AiChatService aiChatService, UserService userService) {
        this.aiChatService = aiChatService;
        this.userService = userService;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody AiChatRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return aiChatService.streamChat(request, loginUser);
    }
}
