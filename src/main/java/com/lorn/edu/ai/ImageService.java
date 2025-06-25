package com.lorn.edu.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ImageService {
    private static final String API_KEY = "替换成您的API Key";
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/images/generations";
    private static final String IMAGE_SAVE_PATH = "E:\\WorkSpace\\cloud_resource\\static\\images\\";//将所生成的图片保存到您的本地磁盘
    // 文生图使用示例   如：图片 一条哈士奇睡在沙发上

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_INTERVAL = 5000; // 5秒

    // 修改默认模型为cogview-3-plus
    private static final String DEFAULT_MODEL = "cogview-3-plus";

    // 支持的图片尺寸
    public enum ImageSize {
        DEFAULT("1024x1024"),
        PORTRAIT_LARGE("768x1344"),
        PORTRAIT_MEDIUM("864x1152"),
        LANDSCAPE_LARGE("1344x768"),
        LANDSCAPE_MEDIUM("1152x864"),
        LANDSCAPE_WIDE("1440x720"),
        PORTRAIT_WIDE("720x1440");

        private final String value;

        ImageSize(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final OkHttpClient client;

    public ImageService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public String generateImage(String prompt) throws Exception {
        // 使用cogview-3-plus作为默认模型
        return generateImage(prompt, DEFAULT_MODEL, ImageSize.DEFAULT);
    }

    public String generateImage(String prompt, String model, ImageSize size) throws Exception {
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    System.out.println("等待" + (RETRY_INTERVAL/1000) + "秒后进行第" + (attempt + 1) + "次尝试...");
                    Thread.sleep(RETRY_INTERVAL);
                }

                // 准备请求体
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", model);
                requestBody.put("prompt", prompt);

                // cogview-3-plus总是支持size参数
                requestBody.put("size", size.getValue());

                System.out.println("生成图片请求体: " + requestBody.toString());

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    System.out.println("服务器响应: " + responseBody);

                    if (!response.isSuccessful()) {
                        JSONObject errorJson = JSON.parseObject(responseBody);
                        if (errorJson.containsKey("error")) {
                            JSONObject error = errorJson.getJSONObject("error");
                            String errorCode = error.getString("code");
                            String errorMessage = error.getString("message");

                            // 处理特定错误码
                            switch (errorCode) {
                                case "1113": // 欠费错误
                                    throw new Exception("API账户状态异常: " + errorMessage);
                                case "429": // 频率限制
                                    if (attempt < MAX_RETRIES - 1) {
                                        System.out.println("触发频率限制，将在" + (RETRY_INTERVAL/1000) + "秒后重试...");
                                        continue;
                                    }
                                    throw new Exception("请求频率过高，请稍后再试");
                                default:
                                    throw new Exception("API错误: " + errorMessage);
                            }
                        }
                        throw new IOException("请求失败，状态码: " + response.code());
                    }

                    JSONObject jsonResponse = JSON.parseObject(responseBody);
                    String imageUrl = jsonResponse.getJSONArray("data")
                            .getJSONObject(0)
                            .getString("url");

                    System.out.println("获取到图片URL: " + imageUrl);
                    return downloadAndSaveImage(imageUrl);
                }
            } catch (Exception e) {
                lastException = e;
                if (attempt == MAX_RETRIES - 1) {
                    throw new Exception("图片生成失败（尝试" + MAX_RETRIES + "次后）: " + e.getMessage(), e);
                }
                System.out.println("第" + (attempt + 1) + "次尝试失败: " + e.getMessage());
            }
        }

        throw lastException;
    }

    private String downloadAndSaveImage(String imageUrl) throws Exception {
        Request request = new Request.Builder().url(imageUrl).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载失败，状态码: " + response.code());
            }

            File directory = new File(IMAGE_SAVE_PATH);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new java.util.Date());
            String fileName = IMAGE_SAVE_PATH + "image_" + timestamp + ".png";

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(fileName)) {
                fos.write(response.body().bytes());
            }

            System.out.println("图片已保存到: " + fileName);
            return fileName;
        }
    }
}