package com.yupi.yuoj.service;

import com.yupi.yuoj.ai.DeepSeekChatClient;
import com.yupi.yuoj.config.AiChatProperties;
import com.yupi.yuoj.model.dto.ai.AiChatMessage;
import com.yupi.yuoj.model.dto.ai.AiChatRequest;
import com.yupi.yuoj.model.entity.Question;
import com.yupi.yuoj.model.entity.User;
import com.yupi.yuoj.service.impl.AiChatServiceImpl;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Test
    void streamChat_shouldBuildMessagesAndInvokeClient() throws Exception {
        AiChatProperties properties = new AiChatProperties();
        properties.setEnabled(true);
        properties.setStreamTimeoutMs(1000);
        properties.setMaxHistoryMessages(2);

        DeepSeekChatClient chatClient = Mockito.mock(DeepSeekChatClient.class);
        QuestionService questionService = Mockito.mock(QuestionService.class);
        Question question = new Question();
        question.setId(1L);
        question.setTitle("A + B");
        question.setContent("输入两个整数，输出它们的和。");
        Mockito.when(questionService.getById(1L)).thenReturn(question);

        AiChatServiceImpl service = new AiChatServiceImpl(properties, chatClient, questionService);

        AiChatMessage historyUser = new AiChatMessage();
        historyUser.setRole("user");
        historyUser.setContent("之前的问题");
        AiChatMessage historyAssistant = new AiChatMessage();
        historyAssistant.setRole("assistant");
        historyAssistant.setContent("之前的回复");

        AiChatRequest request = new AiChatRequest();
        request.setQuestionId(1L);
        request.setLanguage("java");
        request.setCode("public class Main {}");
        request.setPrompt("请修正代码");
        request.setHistory(Arrays.asList(historyUser, historyAssistant));

        User loginUser = new User();
        loginUser.setId(1L);

        CountDownLatch latch = new CountDownLatch(1);
        ArgumentCaptor<List<AiChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(chatClient).streamChat(captor.capture(), Mockito.any());

        service.streamChat(request, loginUser);

        boolean called = latch.await(1, TimeUnit.SECONDS);
        Assertions.assertTrue(called);
        List<AiChatMessage> messages = captor.getValue();
        Assertions.assertFalse(messages.isEmpty());
        Assertions.assertEquals("system", messages.get(0).getRole());
        Assertions.assertTrue(messages.get(messages.size() - 1).getContent().contains("当前代码"));
    }
}
