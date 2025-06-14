package com.yourname.aimod.ai;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// 存储单个模型的配置信息
public class ModelProfile {
    public final String profileName;
    public final boolean isPublic;
    public final String baseUrl;
    public final String apiKey;
    public final String modelId;
    public final float temperature;
    public final int maxContext;
    public final String systemPrompt;
    public final Map<String, List<String>> keywordActions;

    public ModelProfile(String profileName, boolean isPublic, String baseUrl, String apiKey, String modelId, float temperature, int maxContext, String systemPrompt,  Map<String, List<String>> keywordActions) {
        this.profileName = profileName;
        this.isPublic = isPublic;
        this.baseUrl = baseUrl;
        // 如果apikey是 "none" 或空，则设为 null
        this.apiKey = (apiKey == null || apiKey.isEmpty() || apiKey.equalsIgnoreCase("none") || apiKey.equalsIgnoreCase("no-key")) ? null : apiKey;
        this.modelId = modelId;
        this.temperature = temperature;
        this.maxContext = Math.max(1, maxContext); // 至少保留1条
        this.systemPrompt = systemPrompt;
        this.keywordActions = keywordActions != null ? keywordActions : Collections.emptyMap();
    }
}