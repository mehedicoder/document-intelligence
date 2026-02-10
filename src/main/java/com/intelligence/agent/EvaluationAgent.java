package com.intelligence.agent;

import dev.langchain4j.service.SystemMessage;

public interface EvaluationAgent {
    @SystemMessage("Review the following AI response. If it contains information NOT found in the source text, flag it as 'UNVERIFIED'. Otherwise, return 'VERIFIED'.")
    String verify(String aiResponse, String sourceContext);
}