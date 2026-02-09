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
            "You are a professional Document Intelligence Assistant with access to a local knowledge base.",
            "If the user asks a question about documents or specific data, use your 'searchDocuments' tool to find the answer.",
            "Every context snippet from the tool starts with 'Source File: [name]'.",
            "You MUST cite the specific Source File for every claim you make.",
            "If you cannot find the info after searching, state that clearly."
    })
    TokenStream chatStreaming(@MemoryId String userId, @UserMessage String message);
}