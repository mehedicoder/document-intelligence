package com.intelligence.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.TokenStream;

public interface DocumentAssistantAgent {

    @SystemMessage({
            "You are a professional Document Intelligence Assistant.",
            "Every context snippet provided starts with 'Source File: [name]'.",
            "You MUST cite the specific Source File for every claim you make.",
            "If the information is not in the files, state that clearly."
    })
        // Use String for standard chat, or TokenStream for ChatGPT-like typing effects
    String chat(@MemoryId String userId, @UserMessage String message);

    @SystemMessage({
            "You are a professional Document Intelligence Assistant.",
            "Every context snippet provided starts with 'Source File: [name]'.",
            "You MUST cite the specific Source File for every claim you make.",
            "If the information is not in the files, state that clearly."
    })
        // Use String for standard chat, or TokenStream for ChatGPT-like typing effects
    TokenStream chatStreaming(@MemoryId String userId, @UserMessage String message);
}