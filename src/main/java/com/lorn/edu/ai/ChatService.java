package com.lorn.edu.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChatService {
    private static final String API_KEY = "替换成您的API Key";
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private ModelType currentModel;
    private final OkHttpClient client;

    public enum ModelType {
        GLM4("glm-4"),
        GLM4PLUS("glm-4-plus"),
        CODEGEEX4("codegeex-4");

        private final String value;

        ModelType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public ChatService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        this.currentModel = ModelType.CODEGEEX4; // 默认使用 CodeGeeX-4
    }

    public void setModel(ModelType model) {
        this.currentModel = model;
    }

    public String sendMessage(List<JSONObject> messages) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", currentModel.getValue());
        
        // 处理消息编码
        List<JSONObject> encodedMessages = new ArrayList<>();
        for (JSONObject msg : messages) {
            JSONObject encodedMsg = new JSONObject();
            encodedMsg.put("role", msg.getString("role"));
            String content = msg.getString("content");
            // 确保内容使用 UTF-8 编码
            encodedMsg.put("content", new String(content.getBytes("UTF-8"), "UTF-8"));
            encodedMessages.add(encodedMsg);
        }
        requestBody.put("messages", encodedMessages);

        // 设置模型参数
        if (currentModel == ModelType.CODEGEEX4) {
            requestBody.put("temperature", 0.8);
            requestBody.put("top_p", 0.8);
            requestBody.put("stream", false);
            requestBody.put("max_tokens", 2048);
            requestBody.put("stop", new String[]{"<|user|>", "<|assistant|>"});
        } else {
            // GLM4 和 GLM4PLUS 的参数
            requestBody.put("temperature", 0.7);
            requestBody.put("stream", false);
        }

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
            String content = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
                    
            // 确保返回内容使用正确的编码
            return new String(content.getBytes("UTF-8"), "UTF-8");
        }
    }
}