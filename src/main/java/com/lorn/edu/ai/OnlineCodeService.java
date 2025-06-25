package com.lorn.edu.ai;

import com.alibaba.fastjson.JSONObject;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineCodeService {
    private final ChatService chatService;
    private final ExecutorService executorService;

    public OnlineCodeService() {
        this.chatService = new ChatService();
        this.chatService.setModel(ChatService.ModelType.CODEGEEX4);
        this.executorService = Executors.newCachedThreadPool();
    }

    public String executeCode(String code, String language, String versionIndex) throws Exception {
        // 创建一个空的历史记录列表
        List<JSONObject> emptyHistory = new ArrayList<>();
        // 构建默认的分析消息
        String defaultMessage = "请执行这段代码并返回运行结果。如果有错误，请指出错误并给出修改建议。";
        return executeCode(code, language, defaultMessage, emptyHistory);
    }

    public String executeCode(String code, String language, String message, List<JSONObject> history) throws Exception {
        List<JSONObject> messages = new ArrayList<>();
        
        // 添加系统角色提示
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个代码执行助手。请执行用户提供的代码并返回运行结果。" +
                "如果代码有错误，请指出错误并给出修改建议。" +
                "请专注于代码分析和执行，提供专业的技术建议。");
        messages.add(systemMessage);

        // 添加历史消息
        if (history != null) {
            messages.addAll(history);
        }

        // 构建当前消息
        StringBuilder prompt = new StringBuilder();
        prompt.append("请执行以下").append(language).append("代码并返回运行结果：\n\n");
        prompt.append("```").append(language).append("\n");
        prompt.append(code).append("\n");
        prompt.append("```");

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt.toString());
        messages.add(userMessage);

        // 使用 CodeGeeX-4 生成回复
        String response = chatService.sendMessage(messages);
        
        // 格式化输出结果
        return formatExecutionResult(response);
    }

    private String formatExecutionResult(String aiResponse) {
        StringBuilder result = new StringBuilder();
        
        // 提取代码执行结果
        String[] parts = aiResponse.split("```");
        if (parts.length > 1) {
            // 提取实际的执行结果，去除多余的语言标记
            String output = parts[1].trim();
            if (output.startsWith("java") || output.startsWith("python") || output.startsWith("cpp")) {
                output = output.substring(output.indexOf("\n") + 1).trim();
            }
            result.append(output);
            
            // 如果有编译错误或警告，添加到结果中
            if (aiResponse.contains("错误") || aiResponse.contains("警告")) {
                String errorInfo = parts[0].trim();
                // 移除多余的提示信息
                errorInfo = errorInfo.replaceAll("代码中有语法错误，|以下是修改后的代码：", "");
                result.append("\n\n").append(errorInfo);
            }
        } else {
            result.append(aiResponse.trim());
        }
        
        return result.toString();
    }

    private double getCpuUsage() {
        com.sun.management.OperatingSystemMXBean osBean = 
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return Math.round(osBean.getProcessCpuLoad() * 1000.0) / 10.0;
    }

    private long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return (totalMemory - freeMemory) / (1024 * 1024);
    }
}