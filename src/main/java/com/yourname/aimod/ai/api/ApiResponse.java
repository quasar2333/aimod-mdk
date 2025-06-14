package com.yourname.aimod.ai.api;
import java.util.List;

// OpenAI 响应体
public class ApiResponse {
    public String id;
    public String object;
    public long created;
    public String model;
    public List<Choice> choices;
    // usage not mapped
}