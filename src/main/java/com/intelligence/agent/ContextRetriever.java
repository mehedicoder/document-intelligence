package com.intelligence.agent;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import java.util.Map;

public class ContextRetriever implements ContentRetriever {
    private final EmbeddingModel embeddingModel;
    private final String directoryPath;

    public ContextRetriever(EmbeddingModel model, String path) {
        this.embeddingModel = model;
        this.directoryPath = path;
    }

    @Override
    public List<dev.langchain4j.rag.content.Content> retrieve(Query query) {
        // This calls your existing "fetch" and "rank" logic
        List<TextSegment> segments = TextSimilarityRanker.fetchUniqueTextSegmentsFromDirectory(directoryPath);
        Map<TextSegment, Double> ranked = TextSimilarityRanker.rankSegments(embeddingModel, query.text(), segments);

        return ranked.entrySet().stream()
                .sorted(Map.Entry.<TextSegment, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    TextSegment segment = entry.getKey();
                    String fileName = segment.metadata().getString("file_name");

                    // We format the text so the source is part of the content body
                    String enrichedText = String.format("Source File: %s\nContent: %s",
                            (fileName != null ? fileName : "Unknown"),
                            segment.text());

                    return dev.langchain4j.rag.content.Content.from(enrichedText);
                })
                .toList();
    }
}