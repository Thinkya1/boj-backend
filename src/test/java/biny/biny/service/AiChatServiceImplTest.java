package biny.biny.service;

import biny.biny.ai.AiChatModelFactory;
import biny.biny.config.AiChatProperties;
import biny.biny.model.dto.ai.AiChatMessage;
import biny.biny.model.dto.ai.AiChatRequest;
import biny.biny.model.entity.Question;
import biny.biny.model.entity.User;
import biny.biny.service.impl.AiChatServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
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
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.deepseek.com/v1");
        properties.setModel("deepseek-chat");

        StreamingChatLanguageModel chatModel = Mockito.mock(StreamingChatLanguageModel.class);
        AiChatModelFactory modelFactory = Mockito.mock(AiChatModelFactory.class);
        Mockito.when(modelFactory.create(Mockito.any())).thenReturn(chatModel);
        QuestionService questionService = Mockito.mock(QuestionService.class);
        Question question = new Question();
        question.setId(1L);
        question.setTitle("A + B");
        question.setContent("输入两个整数，输出它们的和。");
        Mockito.when(questionService.getById(1L)).thenReturn(question);

        AiChatServiceImpl service = new AiChatServiceImpl(properties, modelFactory, questionService);

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
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.doAnswer(invocation -> {
            StreamingResponseHandler<AiMessage> handler = invocation.getArgument(1);
            handler.onNext("ok");
            handler.onComplete(Response.from(AiMessage.from("done")));
            latch.countDown();
            return null;
        }).when(chatModel).generate(captor.capture(), Mockito.any());

        service.streamChat(request, loginUser);

        boolean called = latch.await(1, TimeUnit.SECONDS);
        Assertions.assertTrue(called);
        List<ChatMessage> messages = captor.getValue();
        Assertions.assertFalse(messages.isEmpty());
        Assertions.assertEquals(ChatMessageType.SYSTEM, messages.get(0).type());
        Assertions.assertEquals(ChatMessageType.USER, messages.get(messages.size() - 1).type());
        Assertions.assertTrue(messages.get(messages.size() - 1).text().contains("当前代码"));
    }
}
