package com.lorn.edu.ai;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CodeService {
    private static final String API_KEY = "替换成您的API Key";
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private static final String MODEL = "codegeex-4";

    private final OkHttpClient client;

    public CodeService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public String askCodingQuestion(List<JSONObject> messages) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("messages", messages);

        // CodeGeeX-4特定参数
        requestBody.put("temperature", 0.8);
        requestBody.put("top_p", 0.8);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 2048);
        requestBody.put("stop", new String[]{"<|user|>", "<|assistant|>"});

        System.out.println("代码问答请求体: " + requestBody.toString());

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败，状态码: " + response.code());
            }

            String responseBody = response.body().string();
            System.out.println("代码问答响应: " + responseBody);

            JSONObject jsonResponse = JSON.parseObject(responseBody);
            return jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        }
    }

    // 添加代码分析功能
    public String analyzeCode(String code) throws Exception {
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", "请分析以下代码并指出可能的改进点：\n\n" + code);

        List<JSONObject> messages = new ArrayList<>();
        messages.add(message);

        return askCodingQuestion(messages);
    }

    // 添加代码优化功能
    public String optimizeCode(String code) throws Exception {
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", "请优化以下代码，并解释优化原因：\n\n" + code);

        List<JSONObject> messages = new ArrayList<>();
        messages.add(message);

        return askCodingQuestion(messages);
    }
}