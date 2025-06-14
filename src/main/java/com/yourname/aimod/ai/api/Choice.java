package com.yourname.aimod.ai.api;

// OpenAI 响应中的 choice 部分
public class Choice {
    public int index;
    public ApiMessage message;
    public String finish_reason;
}