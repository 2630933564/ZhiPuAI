package com.lorn.edu.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class VideoService {
    private static final String API_KEY = "替换成您的API Key";
    private static final String GENERATE_API_URL = "https://open.bigmodel.cn/api/paas/v4/videos/generations";
    private static final String QUERY_API_URL = "https://open.bigmodel.cn/api/paas/v4/async-result";
    private static final String VIDEO_SAVE_PATH = "E:\\WorkSpace\\cloud_resource\\static\\videos\\";//将所生成的视频保存到您的本地磁盘
    //文生视使用示例   如：视频 一只小狗在开心的奔跑

    private final OkHttpClient client;
    private static final long QUERY_INTERVAL = 10000; // 10秒
    private static final int MAX_QUERY_TIMES = 60; // 10分钟

    public VideoService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public String generateVideo(String prompt) throws Exception {
        return generateVideo(prompt, null);
    }

    public String generateVideo(String prompt, String imageUrl) throws Exception {
        // 生成请求ID
        String requestId = UUID.randomUUID().toString();
        System.out.println("开始生成视频，请求ID: " + requestId);

        // 准备请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "cogvideox");
        requestBody.put("prompt", prompt);
        requestBody.put("request_id", requestId);

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            requestBody.put("image_url", imageUrl);
        }

        System.out.println("生成请求体: " + requestBody.toString());

        // 发送生成请求
        Request request = new Request.Builder()
                .url(GENERATE_API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("视频生成请求失败，状态码: " + response.code());
            }

            JSONObject jsonResponse = JSON.parseObject(response.body().string());
            String taskId = jsonResponse.getString("id");

            // 轮询查询结果
            return pollVideoResult(taskId);
        }
    }

    private String pollVideoResult(String taskId) throws Exception {
        System.out.println("获取到任务ID: " + taskId);

        for (int i = 0; i < MAX_QUERY_TIMES; i++) {
            Thread.sleep(QUERY_INTERVAL);

            // 构建查询URL
            String queryUrl = QUERY_API_URL + "/" + taskId;
            System.out.println("第" + (i + 1) + "次查询，URL: " + queryUrl);

            Request request = new Request.Builder()
                    .url(queryUrl)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                System.out.println("查询响应: " + responseBody);

                if (!response.isSuccessful()) {
                    if (i < MAX_QUERY_TIMES - 1) {
                        System.out.println("查询失败，将在" + (QUERY_INTERVAL / 1000) + "秒后重试...");
                        continue;
                    }
                    throw new IOException("查询请求失败，状态码: " + response.code());
                }

                JSONObject jsonResponse = JSON.parseObject(responseBody);
                String status = jsonResponse.getString("task_status");
                System.out.println("任务状态: " + status);

                switch (status) {
                    case "SUCCESS":
                        // 从video_result数组中获取视频URL
                        JSONObject videoResult = jsonResponse.getJSONArray("video_result")
                                .getJSONObject(0);
                        String videoUrl = videoResult.getString("url");
                        String coverUrl = videoResult.getString("cover_image_url");

                        System.out.println("视频生成成功！");
                        System.out.println("视频URL: " + videoUrl);
                        System.out.println("封面URL: " + coverUrl);

                        return downloadAndSaveVideo(videoUrl);

                    case "FAIL":
                        String errorMessage = jsonResponse.containsKey("error") ?
                                jsonResponse.getString("error") : "未知错误";
                        throw new Exception("视频生成失败: " + errorMessage);

                    case "PROCESSING":
                        System.out.println("视频正在生成中，已等待" +
                                (i + 1) * (QUERY_INTERVAL / 1000) + "秒...");
                        continue;

                    default:
                        throw new Exception("未知的任务状态: " + status);
                }
            } catch (Exception e) {
                if (i == MAX_QUERY_TIMES - 1) {
                    throw e;
                }
                System.out.println("查询出错: " + e.getMessage());
            }
        }

        throw new Exception("视频生成超时，请稍后使用相同的请求ID重试");
    }

    private String downloadAndSaveVideo(String videoUrl) throws Exception {
        Request request = new Request.Builder().url(videoUrl).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("视频下载失败，状态码: " + response.code());
            }

            File directory = new File(VIDEO_SAVE_PATH);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new java.util.Date());
            String fileName = VIDEO_SAVE_PATH + "video_" + timestamp + ".mp4";

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(fileName)) {
                fos.write(response.body().bytes());
            }

            return fileName;
        }
    }
}