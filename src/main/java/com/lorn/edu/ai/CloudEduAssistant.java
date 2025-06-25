package com.lorn.edu.ai;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CloudEduAssistant {
    private static final String ASSISTANT_NAME = "替换为您的xxx助手";
    private static final String WELCOME_MESSAGE =
            "欢迎使用xxx智能助手！我是您的专属学习伙伴，可以为您提供以下服务：\n" +
                    "1. 编程技术指导和代码优化\n" +
                    "2. 算法讲解和数据结构分析\n" +
                    "3. 学习方法和规划建议\n" +
                    "4. 知识解答和概念讲解\n" +
                    "输入'帮助'可以查看更多功能，输入'退出'结束对话。";

    private static final boolean IMAGE_GENERATION_ENABLED = true;
    private static final boolean VIDEO_GENERATION_ENABLED = true;

    private final ChatService chatService;
    private final ImageService imageService;
    private final VideoService videoService;
    private final CodeService codeService;
    private final OnlineCodeService onlineCodeService;
    private final List<JSONObject> messages;
    private boolean isCodeMode = false;
    private boolean isInteractiveMode = false;
    private final Scanner scanner;

    public CloudEduAssistant() {
        this.chatService = new ChatService();
        this.imageService = new ImageService();
        this.videoService = new VideoService();
        this.codeService = new CodeService();
        this.onlineCodeService = new OnlineCodeService();
        this.messages = new ArrayList<>();
        this.scanner = new Scanner(System.in);
    }

    public void start() throws Exception {
        System.out.println("\n=== " + ASSISTANT_NAME + " ===");
        System.out.println(WELCOME_MESSAGE);
        System.out.println("============================\n");

        while (true) {
            System.out.print("你: ");
            String userInput = scanner.nextLine();

            if (userInput == null || userInput.trim().isEmpty()) {
                System.out.println("输入不能为空，请重新输入！");
                continue;
            }

            if (handleSpecialCommands(userInput)) {
                continue;
            }

            processUserInput(userInput);
        }
    }

    private boolean handleSpecialCommands(String input) throws Exception {
        if (input.toLowerCase().startsWith("画图 ") ||
                input.toLowerCase().startsWith("生成图片 ") ||
                input.toLowerCase().startsWith("图片 ") ||
                input.toLowerCase().startsWith("图 ")) {
            String prompt = input.substring(input.indexOf(" ")).trim();
            handleImageGeneration(prompt);
            return true;
        }

        if (input.equalsIgnoreCase("图片")) {
            System.out.println(ASSISTANT_NAME + ": 请在'图片'后面添加描述，例如：");
            System.out.println("- 图片 一只可爱的小狗");
            System.out.println("- 图 蓝天白云下的草原");
            System.out.println("- 画图 夕阳下的海滩");
            return true;
        }

        if (input.toLowerCase().startsWith("视频 ")) {
            handleVideoGeneration(input.substring(3).trim());
            return true;
        }

        switch (input.toLowerCase()) {
            case "退出":
                System.out.println("感谢使用" + ASSISTANT_NAME + "，再见！");
                System.exit(0);
                return true;
            case "清空":
                messages.clear();
                System.out.println("对话历史已清空！");
                return true;
            case "帮助":
                showHelp();
                return true;
            case "切换代码模式":
                isCodeMode = true;
                System.out.println(ASSISTANT_NAME + ": 已切换到代码问答模式 (CodeGeeX-4)");
                return true;
            case "切换聊天模式":
                isCodeMode = false;
                System.out.println(ASSISTANT_NAME + ": 已切换到通用聊天模式 (GLM-4)");
                return true;
            case "分析代码":
                handleCodeAnalysis();
                return true;
            case "优化代码":
                handleCodeOptimization();
                return true;
            case "运行代码":
                handleCodeExecution();
                return true;
            case "交互模式":
                handleInteractiveMode();
                return true;
            default:
                return false;
        }
    }

    private void processUserInput(String input) {
        try {
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", input);
            messages.add(userMessage);

            String response;
            if (isCodeMode) {
                response = codeService.askCodingQuestion(messages);
            } else {
                response = chatService.sendMessage(messages);
            }

            System.out.println(ASSISTANT_NAME + ": " + response);

            JSONObject assistantMessage = new JSONObject();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", response);
            messages.add(assistantMessage);
        } catch (Exception e) {
            System.out.println("发送消息失败: " + e.getMessage());
        }
    }

    private void handleImageGeneration(String prompt) {
        if (!IMAGE_GENERATION_ENABLED) {
            System.out.println(ASSISTANT_NAME + ": 抱歉，图片生成功能当前不可用。");
            System.out.println(ASSISTANT_NAME + ": 原因：需要开通专门的图片生成服务额度。");
            System.out.println(ASSISTANT_NAME + ": 您可以：");
            System.out.println("1. 访问智谱AI官网开通图片生成服务");
            System.out.println("2. 使用其他功能，如聊天、代码分析等");
            return;
        }

        try {
            System.out.println(ASSISTANT_NAME + ": 开始生成图片...");
            System.out.println(ASSISTANT_NAME + ": 正在处理提示词: " + prompt);
            String imagePath = imageService.generateImage(prompt);
            System.out.println(ASSISTANT_NAME + ": 图片已生生成并保存到: " + imagePath);
        } catch (Exception e) {
            System.out.println("图片生成失败: " + e.getMessage());
            if (e.getMessage().contains("欠费") || e.getMessage().contains("1113")) {
                System.out.println(ASSISTANT_NAME + ": 图片生成服务需要单独开通和计费。");
                System.out.println(ASSISTANT_NAME + ": 请访问智谱AI官网开通相关服务。");
            }
            e.printStackTrace();
        }
    }

    private void handleVideoGeneration(String prompt) {
        if (!VIDEO_GENERATION_ENABLED) {
            System.out.println(ASSISTANT_NAME + ": 抱歉，视频生成功能当前不可用。");
            System.out.println(ASSISTANT_NAME + ": 原因：需要开通专门的视频生成服务额度。");
            System.out.println(ASSISTANT_NAME + ": 您可以：");
            System.out.println("1. 访问智谱AI官网开通视频生成服务");
            System.out.println("2. 使用其他功能，如聊天、代码分析等");
            return;
        }

        try {
            System.out.println(ASSISTANT_NAME + ": 开始生成视频，这可能需要几分钟时间...");
            System.out.println(ASSISTANT_NAME + ": 视频生成过程中请勿退出程序，生成完成后会自动保存");
            String videoPath = videoService.generateVideo(prompt);
            System.out.println(ASSISTANT_NAME + ": 视频已生成并保存到: " + videoPath);
        } catch (Exception e) {
            System.out.println("视频生成失败: " + e.getMessage());
            System.out.println("如果是超时错误，视频可能仍在生成中，请稍后再试。");
        }
    }

    private void handleCodeAnalysis() throws Exception {
        System.out.println(ASSISTANT_NAME + ": 请输入要分析的代码（输入'完成'结束）：");
        StringBuilder code = new StringBuilder();
        while (true) {
            String line = scanner.nextLine();
            if (line.equals("完成")) break;
            code.append(line).append("\n");
        }
        String result = codeService.analyzeCode(code.toString());
        System.out.println(ASSISTANT_NAME + ": " + result);
    }

    private void handleCodeOptimization() throws Exception {
        System.out.println(ASSISTANT_NAME + ": 请输入要优化的代码（输入'完成'结束）：");
        StringBuilder code = new StringBuilder();
        while (true) {
            String line = scanner.nextLine();
            if (line.equals("完成")) break;
            code.append(line).append("\n");
        }
        String result = codeService.optimizeCode(code.toString());
        System.out.println(ASSISTANT_NAME + ": " + result);
    }

    private void handleCodeExecution() throws Exception {
        System.out.println(ASSISTANT_NAME + ": 请选择编程语言：");
        System.out.println("1. Java");
        System.out.println("2. Python");
        System.out.println("3. C++");

        String language = scanner.nextLine().toLowerCase();
        String langCode = "";

        switch (language) {
            case "1":
            case "java":
                langCode = "java";
                break;
            case "2":
            case "python":
                langCode = "python3";
                break;
            case "3":
            case "c++":
                langCode = "cpp";
                break;
            default:
                System.out.println("不支持的语言！");
                return;
        }

        System.out.println(ASSISTANT_NAME + ": 请输入代码（输入'完成'结束）：");
        StringBuilder code = new StringBuilder();
        while (true) {
            String line = scanner.nextLine();
            if (line.equals("完成")) break;
            code.append(line).append("\n");
        }

        try {
            String result = onlineCodeService.executeCode(code.toString(), langCode, "0");
            System.out.println(result);
        } catch (Exception e) {
            System.out.println("代码执行失败: " + e.getMessage());
        }
    }

    private void handleInteractiveMode() {
        System.out.println(ASSISTANT_NAME + ": 进入代码执行模式");
        System.out.println("请选择编程语言：");
        System.out.println("1. Java");
        System.out.println("2. Python");
        System.out.println("3. C++");

        String language = scanner.nextLine().toLowerCase();
        String langCode = "";

        switch (language) {
            case "1":
            case "java":
                langCode = "java";
                break;
            case "2":
            case "python":
                langCode = "python3";
                break;
            case "3":
            case "c++":
                langCode = "cpp";
                break;
            default:
                System.out.println("不支持的语言！");
                return;
        }

        while (true) {
            System.out.println(ASSISTANT_NAME + ": 请输入代码（输入'退出'结束，输入'运行'执行）：");
            StringBuilder code = new StringBuilder();

            while (true) {
                String line = scanner.nextLine();
                if (line.equals("退出")) {
                    return;
                }
                if (line.equals("运行")) {
                    break;
                }
                code.append(line).append("\n");
            }

            try {
                String result = onlineCodeService.executeCode(code.toString(), langCode, "0");
                System.out.println(result);
            } catch (Exception e) {
                System.out.println("代码执行失败: " + e.getMessage());
            }
        }
    }

    private void showHelp() {
        System.out.println("\n=== " + ASSISTANT_NAME + " 功能说明 ===");
        System.out.println("1. 基础对话：直接输入问题即可");
        System.out.println("2. 图片生成：" + (IMAGE_GENERATION_ENABLED ? "可用" : "暂不可用"));
        System.out.println("   - 使用方法：");
        System.out.println("     图片 [描述]  (例如：图片 一只可爱的小狗)");
        System.out.println("     图 [描述]    (例如：图 蓝天白云)");
        System.out.println("     画图 [描述]  (例如：画图 夕阳海滩)");
        System.out.println("3. 视频生成：" + (VIDEO_GENERATION_ENABLED ? "可用" : "暂不可用"));
        System.out.println("4. 代码功能：");
        System.out.println("   - 切换代码模式：使用CodeGeeX-4进行代码问答");
        System.out.println("   - 分析代码：分析代码并提供改进建议");
        System.out.println("   - 优化代码：优化代码并解释原因");
        System.out.println("   - 运行代码：在线运行代码");
        System.out.println("   - 交互模式：交互式编程环境");
        System.out.println("5. 其他命令：");
        System.out.println("   - 帮助：显示本帮助信息");
        System.out.println("   - 清空：清除对话历史");
        System.out.println("   - 退出：结束对话");
        System.out.println("============================\n");
    }

    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }

    public static void main(String[] args) throws Exception {
        CloudEduAssistant assistant = new CloudEduAssistant();
        try {
            assistant.start();
        } finally {
            assistant.close();
        }
    }
}