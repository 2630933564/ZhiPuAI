package com.lorn.edu.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GLMChatService {
    private static final String API_KEY = "替换成您的API Key";
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private final OkHttpClient client;

    public GLMChatService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public String chat(List<JSONObject> messages) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "glm-4-plus");
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("stream", false);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), 
                    requestBody.toString()
                ))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败，状态码: " + response.code());
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            return jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        }
    }

    public String getSystemPrompt() {
        return "你是一个xxx团队精心打造的xxx助手，专注于提供专业、友好的对话服务。" +
               "你的回答应该简洁明了，直接回应用户的问题。" +
               "如果遇到不明确的问题，请礼貌地请求用户提供更多信息。" +
               "你应该表现出对话的连贯性和上下文理解能力。";
    }
} 