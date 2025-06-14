package com.yourname.aimod.ai.api;
import javax.annotation.Nullable;

// OpenAI 消息格式
public class ApiMessage {
    public String role; // "system", "user", "assistant"
    public String content;
    @Nullable
    public String name; // Optional: name of the user

    public ApiMessage(String role, String content) {
        this(role, content, null);
    }
    public ApiMessage(String role, String content, @Nullable String name) {
        this.role = role;
        this.content = content;
        this.name = name; // only useful for role=user usually
    }
}