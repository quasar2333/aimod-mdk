package com.yourname.aimod;

import com.yourname.aimod.ai.ModelProfile;
import net.minecraftforge.common.config.Configuration;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {

    public static Configuration config;
    public static final Map<String, ModelProfile> modelProfiles = new HashMap<>();

    // 全局配置项
    public static boolean persistPrivateSessions;
    public static final String GENERAL_CATEGORY = "general";


    public static void loadConfig(File configFile) {
        config = new Configuration(configFile);
        config.load();
        syncConfig();
    }

    public static void reloadConfig() {
        if (config != null) {
            config.load();
            syncConfig();
            AiMod.logger.info("AI Mod Config reloaded.");
        }
    }

    public static void syncConfig() {
        // 加载全局配置
        config.addCustomCategoryComment(GENERAL_CATEGORY, "General settings for the AI Mod");
        persistPrivateSessions = config.getBoolean("persistPrivateSessions", GENERAL_CATEGORY, false,
                "If true, private chat sessions will be restored when a player logs back in.\n" +
                        "If false, private sessions are ended when a player logs out.");

        modelProfiles.clear();
        AiMod.logger.info("Loading AI Model Profiles from config...");

        // 定义默认模型，如果不存在则创建
        defineDefaultModels();

        // 遍历所有模型配置
        for (String categoryName : config.getCategoryNames()) {
            if (categoryName.startsWith("model_")) {
                String profileName = categoryName.substring("model_".length());
                if (profileName.isEmpty()) continue;

                try {
                    boolean isPublic = config.getBoolean("isPublic", categoryName, false, "Set to true for public model, false for private model");
                    String baseUrl = config.getString("baseUrl", categoryName, "http://localhost:1234/v1/chat/completions", "API Base URL");
                    String apiKey = config.getString("apiKey", categoryName, "lm-studio", "API Key (use 'none' or empty if not required)");
                    String modelId = config.getString("modelId", categoryName, "model-id-from-provider", "Model ID string required by the provider");
                    float temperature = config.getFloat("temperature", categoryName, 0.7f, 0.0f, 2.0f, "Model temperature (creativity)");
                    int maxContext = config.getInt("maxContext", categoryName, 5, 1, 100, "Number of past messages (user+ai) to remember");
                    String systemPrompt = config.getString("systemPrompt", categoryName, "You are a helpful assistant in Minecraft.", "System prompt / Preset");

                    Map<String, List<String>> keywordActions = new HashMap<>();
                    String[] actionsArray = config.getStringList("keywordActions", categoryName, new String[]{},
                            "Keyword actions format: keyword::/command1|/command2. Each entry is one keyword mapping. Commands run via console.");
                    for (String actionEntry : actionsArray) {
                        String[] parts = actionEntry.split("::", 2);
                        if (parts.length == 2) {
                            String keyword = parts[0].trim();
                            String[] commands = parts[1].split("\\|");
                            List<String> commandList = new ArrayList<>();
                            for (String cmd : commands) {
                                if (!cmd.trim().isEmpty()) commandList.add(cmd.trim());
                            }
                            if (!keyword.isEmpty() && !commandList.isEmpty()) {
                                keywordActions.put(keyword.toLowerCase(), commandList);
                                AiMod.logger.info("Loaded keyword action for '" + profileName + "': [" + keyword + "] -> " + String.join(", ", commandList));
                            }
                        }
                    }

                    ModelProfile profile = new ModelProfile(
                            profileName, isPublic, baseUrl, apiKey, modelId,
                            temperature, maxContext, systemPrompt, keywordActions);
                    modelProfiles.put(profileName.toLowerCase(), profile);
                    AiMod.logger.info("Loaded " + (isPublic ? "PUBLIC" : "PRIVATE") + " profile: " + profileName);

                } catch (Exception e) {
                    AiMod.logger.error("Error loading model profile category: " + categoryName, e);
                }
            }
        }
        if (config.hasChanged()) {
            config.save();
        }
        AiMod.logger.info("Loaded " + modelProfiles.size() + " model profiles.");
    }

    private static void defineDefaultModels() {
        String catPub = "model_public_bot";
        config.getBoolean("isPublic", catPub, true, "Set to true for public model, false for private model");
        config.getString("baseUrl", catPub, "http://127.0.0.1:1234/v1/chat/completions", "API Base URL");
        config.getString("apiKey", catPub, "no-key", "API Key");
        config.getString("modelId", catPub, "default-model", "Model ID");
        config.getFloat("temperature", catPub, 0.8f, 0.0f, 2.0f, "Model temperature");
        config.getInt("maxContext", catPub, 6, 1, 100, "Context length");
        // --- FIX 1: 添加了第四个参数 (注释) ---
        config.getString("systemPrompt", catPub, "You are MineBot, a helpful chat bot in a Minecraft server. Keep responses brief, friendly and relevant. Always mention player name when replying.", "System prompt / Preset");
        config.getStringList("keywordActions", catPub, new String[]{"diamond time::/give @p diamond 1|/say Here is your diamond, @p!", "day please::/time set day|/weather clear"}, "Keyword Actions");

        String catPriv = "model_private_guide";
        config.getBoolean("isPublic", catPriv, false, "Set to true for public model, false for private model");
        config.getString("baseUrl", catPriv, "http://127.0.0.1:1234/v1/chat/completions", "API Base URL");
        config.getString("apiKey", catPriv, "no-key", "API Key");
        config.getString("modelId", catPriv, "private-model", "Model ID");
        config.getFloat("temperature", catPriv, 0.6f, 0.0f, 2.0f, "Model temperature");
        config.getInt("maxContext", catPriv, 10, 1, 100, "Context length");
        // --- FIX 2: 添加了第四个参数 (注释) ---
        config.getString("systemPrompt", catPriv, "You are a private Minecraft guide, assisting one player. Be detailed and roleplay slightly.", "System prompt / Preset");
        config.getStringList("keywordActions", catPriv, new String[]{}, "Keyword Actions");
    }
}