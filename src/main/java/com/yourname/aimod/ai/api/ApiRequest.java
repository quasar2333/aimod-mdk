package com.yourname.aimod.ai.api;
import java.util.List;

// OpenAI 请求体
public class ApiRequest {
    public String model;
    public List<ApiMessage> messages;
    public float temperature;
    public boolean stream = false; // We don't support streaming here

    public ApiRequest(String model, List<ApiMessage> messages, float temperature) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
    }
}