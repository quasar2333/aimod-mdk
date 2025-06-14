package com.yourname.aimod.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yourname.aimod.AiMod;
import com.yourname.aimod.ai.api.ApiMessage;
import com.yourname.aimod.ai.api.ApiRequest;
import com.yourname.aimod.ai.api.ApiResponse;
import com.yourname.aimod.ai.api.Choice;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// 处理 HTTP 请求和 JSON 解析
public class AIClient {

    private static final Gson GSON = new GsonBuilder().create();
    private static final int TIMEOUT_MS = 20000; // 20 seconds timeout

    // 异步调用AI
    public static void callAI(final ModelProfile profile, final List<ApiMessage> messages, final AIResponseCallback callback) {

        CompletableFuture.supplyAsync(() -> {
            // --- 这部分在后台线程执行 ---
            HttpURLConnection connection = null;
            try {
                URL url = new URL(profile.baseUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                if (profile.apiKey != null) {
                    connection.setRequestProperty("Authorization", "Bearer " + profile.apiKey);
                }
                connection.setDoOutput(true);

                ApiRequest requestBody = new ApiRequest(profile.modelId, messages, profile.temperature);
                String jsonInputString = GSON.toJson(requestBody);
                // AiMod.logger.info("AI Request: " + jsonInputString);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder responseBuilder = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            responseBuilder.append(responseLine.trim());
                        }
                        String jsonResponse = responseBuilder.toString();
                        //  AiMod.logger.info("AI Response: " + jsonResponse);

                        ApiResponse response = GSON.fromJson(jsonResponse, ApiResponse.class);
                        if (response != null && response.choices != null && !response.choices.isEmpty()) {
                            Choice firstChoice = response.choices.get(0);
                            if (firstChoice != null && firstChoice.message != null && firstChoice.message.content != null) {
                                return AIResponse.success(firstChoice.message.content);
                            }
                        }
                        return AIResponse.error("Invalid or empty response structure from API.");
                    }
                } else {
                    // 读取错误流
                    StringBuilder errorResponse = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            errorResponse.append(responseLine.trim());
                        }
                    } catch(Exception ignore){}
                    return AIResponse.error("API Error: HTTP " + responseCode + " - "+ connection.getResponseMessage() + " | " + errorResponse.toString());
                }

            } catch (Exception e) {
                AiMod.logger.error("Error during AI API call for model " + profile.profileName, e);
                return AIResponse.error("Exception during API call: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            // --- 后台线程结束 ---
        }).thenAccept(response -> {
            // 将回调函数调度回主服务器线程执行
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                server.addScheduledTask(() -> callback.onResponse(response));
            } else {
                // 如果服务器不在，至少记录错误
                AiMod.logger.error("Could not get server instance to schedule AI response callback!");
                if(!response.isSuccess()) AiMod.logger.error("Original AI Error: " + response.getError());
            }
        });
    }

    // 回调接口
    public interface AIResponseCallback {
        void onResponse(AIResponse response);
    }

    // 响应封装类
    public static class AIResponse {
        private final boolean success;
        private final String content; // null if error
        private final String error;   // null if success

        private AIResponse(boolean success, String content, String error) {
            this.success = success;
            this.content = content;
            this.error = error;
        }
        public static AIResponse success(String content) { return new AIResponse(true, content, null); }
        public static AIResponse error(String error) { return new AIResponse(false, null, error); }
        public boolean isSuccess() { return success; }
        public String getContent() { return content; }
        public String getError() { return error; }
    }
}