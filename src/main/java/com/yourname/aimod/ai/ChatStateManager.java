package com.yourname.aimod.ai;

import com.google.common.collect.EvictingQueue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yourname.aimod.AiMod;
import com.yourname.aimod.ModConfig;
import com.yourname.aimod.ai.api.ApiMessage;
import com.yourname.aimod.util.ChatUtil; // <--- FIX: 添加了这行 import
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatStateManager {

    public static final ChatStateManager INSTANCE = new ChatStateManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private volatile String activePublicModelName = null;
    private final Map<UUID, String> privateChatSessions = new ConcurrentHashMap<>();
    private final Map<String, Queue<ApiMessage>> chatHistories = new ConcurrentHashMap<>();
    private final Set<String> processingKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ChatStateManager() {}

    @Nullable
    public ModelProfile getActivePublicModel() {
        if (activePublicModelName == null) return null;
        return ModConfig.modelProfiles.get(activePublicModelName);
    }

    public String getActivePublicModelName() {
        return activePublicModelName;
    }

    public boolean setPublicModel(@Nullable String modelName) {
        if (modelName == null) {
            if (activePublicModelName != null) {
                chatHistories.remove(activePublicModelName);
                processingKeys.remove(activePublicModelName);
            }
            activePublicModelName = null;
            return true;
        }
        ModelProfile profile = ModConfig.modelProfiles.get(modelName.toLowerCase());
        if (profile != null && profile.isPublic) {
            if (activePublicModelName != null && !activePublicModelName.equals(modelName.toLowerCase())) {
                chatHistories.remove(activePublicModelName);
                processingKeys.remove(activePublicModelName);
            }
            activePublicModelName = modelName.toLowerCase();
            chatHistories.computeIfAbsent(activePublicModelName, k -> EvictingQueue.create(profile.maxContext));
            return true;
        }
        return false;
    }

    @Nullable
    public ModelProfile getPrivateSessionModel(UUID playerUUID) {
        String modelName = privateChatSessions.get(playerUUID);
        if (modelName == null) return null;
        return ModConfig.modelProfiles.get(modelName);
    }

    public boolean isPlayerInPrivateSession(UUID playerUUID) {
        return privateChatSessions.containsKey(playerUUID);
    }

    public boolean startPrivateSession(UUID playerUUID, String modelName) {
        ModelProfile profile = ModConfig.modelProfiles.get(modelName.toLowerCase());
        if (profile != null && !profile.isPublic && !privateChatSessions.containsKey(playerUUID)) {
            privateChatSessions.put(playerUUID, modelName.toLowerCase());
            chatHistories.computeIfAbsent(playerUUID.toString(), k -> EvictingQueue.create(profile.maxContext));
            return true;
        }
        return false;
    }

    public boolean endPrivateSession(UUID playerUUID) {
        if (privateChatSessions.remove(playerUUID) != null) {
            chatHistories.remove(playerUUID.toString());
            processingKeys.remove(playerUUID.toString());
            return true;
        }
        return false;
    }

    public void onPlayerLogout(UUID playerUUID) {
        if (!ModConfig.persistPrivateSessions) {
            endPrivateSession(playerUUID);
        }
    }

    public void addMessage(String key, ApiMessage message, int maxContext) {
        Queue<ApiMessage> history = chatHistories.computeIfAbsent(key, k -> EvictingQueue.create(maxContext));
        history.add(message);
    }

    public void processUserMessageAndTriggerAI(String key, ModelProfile profile, EntityPlayerMP player, String content, AIClient.AIResponseCallback callback) {
        if (isProcessing(key)) {
            AiMod.logger.warn("AI is still processing for key: " + key + ". Ignoring new message.");
            ChatUtil.sendInfo(player, "AI is still thinking, please wait...");
            return;
        }
        setProcessing(key, true);
        String playerName = player.getName();
        addMessage(key, new ApiMessage("user", content, playerName), profile.maxContext);
        List<ApiMessage> context = getContext(key, profile);
        AIClient.callAI(profile, context, response -> {
            setProcessing(key, false);
            if (response.isSuccess()) {
                addMessage(key, new ApiMessage("assistant", response.getContent()), profile.maxContext);
            }
            callback.onResponse(response);
        });
    }

    public List<ApiMessage> getContext(String key, ModelProfile profile) {
        List<ApiMessage> context = new ArrayList<>();
        if (profile.systemPrompt != null && !profile.systemPrompt.isEmpty()) {
            context.add(new ApiMessage("system", profile.systemPrompt));
        }
        Queue<ApiMessage> history = chatHistories.get(key);
        if (history != null) {
            context.addAll(history);
        }
        return context;
    }

    public boolean isProcessing(String key) {
        return processingKeys.contains(key);
    }

    public void setProcessing(String key, boolean processing) {
        if (processing) {
            processingKeys.add(key);
        } else {
            processingKeys.remove(key);
        }
    }

    public void clearAll() {
        activePublicModelName = null;
        privateChatSessions.clear();
        chatHistories.clear();
        processingKeys.clear();
    }

    public void saveSessions(File file) {
        if (privateChatSessions.isEmpty()) {
            if (file.exists()) {
                file.delete();
            }
            return;
        }
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new FileWriter(file, false)) {
                GSON.toJson(privateChatSessions, writer);
                AiMod.logger.info("Saved " + privateChatSessions.size() + " private AI sessions to " + file.getName());
            }
        } catch (IOException e) {
            AiMod.logger.error("Could not save AI sessions to file: " + file.getAbsolutePath(), e);
        }
    }

    public void loadSessions(File file) {
        if (!file.exists()) {
            return;
        }
        try {
            try (Reader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<UUID, String>>() {}.getType();
                Map<UUID, String> loadedSessions = GSON.fromJson(reader, type);

                if (loadedSessions != null && !loadedSessions.isEmpty()) {
                    privateChatSessions.putAll(loadedSessions);
                    AiMod.logger.info("Loaded " + loadedSessions.size() + " private AI sessions from " + file.getName());

                    for (Map.Entry<UUID, String> entry : loadedSessions.entrySet()) {
                        ModelProfile profile = ModConfig.modelProfiles.get(entry.getValue());
                        if (profile != null) {
                            chatHistories.computeIfAbsent(entry.getKey().toString(), k -> EvictingQueue.create(profile.maxContext));
                        }
                    }
                }
            }
        } catch (Exception e) {
            AiMod.logger.error("Could not load AI sessions from file: " + file.getAbsolutePath(), e);
        }
    }
}