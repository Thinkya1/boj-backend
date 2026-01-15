package com.yupi.yuoj.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.yuoj.common.ErrorCode;
import com.yupi.yuoj.config.AiChatProperties;
import com.yupi.yuoj.exception.BusinessException;
import com.yupi.yuoj.model.dto.ai.AiChatMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 流式对话客户端
 */
@Component
public class DeepSeekChatClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final AiChatProperties properties;

    private final OkHttpClient httpClient;

    public DeepSeekChatClient(AiChatProperties properties) {
        this.properties = properties;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
    }

    public void streamChat(List<AiChatMessage> messages, Consumer<String> onDelta) {
        validateConfig();
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", properties.getModel());
        payload.put("temperature", properties.getTemperature());
        payload.put("max_tokens", properties.getMaxOutputTokens());
        payload.put("stream", true);
        payload.put("messages", buildMessagePayload(messages));
        RequestBody requestBody = RequestBody.create(JSON, JSONUtil.toJsonStr(payload));
        Request request = new Request.Builder()
                .url(buildEndpoint())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .post(requestBody)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() == null) {
                throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "AI 响应为空");
            }
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                throw new BusinessException(ErrorCode.API_REQUEST_ERROR,
                        "AI 请求失败，status=" + response.code() + ", message=" + truncate(errorBody));
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || !trimmed.startsWith("data:")) {
                        continue;
                    }
                    String data = trimmed.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    String content = parseDeltaContent(data);
                    if (StringUtils.isNotBlank(content)) {
                        onDelta.accept(content);
                    }
                }
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "AI 请求异常");
        }
    }

    private List<Map<String, Object>> buildMessagePayload(List<AiChatMessage> messages) {
        return messages.stream()
                .filter(Objects::nonNull)
                .map(message -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("role", message.getRole());
                    map.put("content", message.getContent());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private String parseDeltaContent(String data) {
        try {
            JSONObject jsonObject = JSONUtil.parseObj(data);
            JSONArray choices = jsonObject.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject choice = choices.getJSONObject(0);
            JSONObject delta = choice == null ? null : choice.getJSONObject("delta");
            if (delta == null) {
                return null;
            }
            return delta.getStr("content");
        } catch (Exception ignored) {
            return null;
        }
    }

    private void validateConfig() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 功能未开启");
        }
        if (StringUtils.isBlank(properties.getApiKey())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI Key 未配置");
        }
        if (StringUtils.isBlank(properties.getBaseUrl())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI BaseUrl 未配置");
        }
    }

    private String buildEndpoint() {
        String baseUrl = StringUtils.trimToEmpty(properties.getBaseUrl());
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/chat/completions";
    }

    private String truncate(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        int maxLength = 500;
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }
}
