package com.intelligence;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TextSimilarityRankerTest {

    @Test
    @DisplayName("Cosine Similarity should return 1.0 for identical vectors")
    void similarityOfIdenticalVectors() {
        float[] vector = {1.0f, 0.0f, 0.0f};
        double result = TextSimilarityRanker.calculateCosineSimilarity(vector, vector);
        assertThat(result).isCloseTo(1.0, within(0.0001));
    }

    @Test
    @DisplayName("Cosine Similarity should return 0.0 for orthogonal vectors")
    void similarityOfOrthogonalVectors() {
        float[] vectorA = {1.0f, 0.0f};
        float[] vectorB = {0.0f, 1.0f};
        double result = TextSimilarityRanker.calculateCosineSimilarity(vectorA, vectorB);
        assertThat(result).isCloseTo(0.0, within(0.0001));
    }

    @Test
    @DisplayName("Calculated similarity matches known geometric results ([3,4] and [5,12])")
    void similarityOfKnownVectors() {
        float[] vectorA = {3.0f, 4.0f};
        float[] vectorB = {5.0f, 12.0f};

        double result = TextSimilarityRanker.calculateCosineSimilarity(vectorA, vectorB);
        assertThat(result).isCloseTo(0.9692, within(0.0001));
    }

    @Test
    @DisplayName("Should handle zero vectors safely without crashing")
    void handleZeroVectors() {
        float[] vectorA = {0.0f, 0.0f};
        float[] vectorB = {1.0f, 2.0f};

        double result = TextSimilarityRanker.calculateCosineSimilarity(vectorA, vectorB);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Integration check: Batch embedding works with Fake Model and TextSegments")
    void mockModelIntegrationTest() {
        EmbeddingModel fakeModel = new FakeEmbeddingModel();
        String query = "test";

        // Setup: Create TextSegments instead of raw Strings
        List<TextSegment> segments = List.of(
                TextSegment.from("apple"),
                TextSegment.from("orange")
        );

        // Updated method call: Uses 'rankSegments' which is the v3.4 standard
        Map<TextSegment, Double> results = TextSimilarityRanker.rankSegments(fakeModel, query, segments);

        assertThat(results).hasSize(2);

        // Verify keys by extracting their text
        assertThat(results.keySet()).extracting(TextSegment::text)
                .containsExactlyInAnyOrder("apple", "orange");
    }

    /**
     * A simple Fake Model for testing purposes.
     */
    private static class FakeEmbeddingModel implements EmbeddingModel {
        @Override
        public Response<Embedding> embed(String text) {
            return Response.from(Embedding.from(new float[]{text.length(), 1.0f}));
        }

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            List<Embedding> embeddings = segments.stream()
                    .map(s -> Embedding.from(new float[]{s.text().length(), 1.0f}))
                    .toList();
            return Response.from(embeddings);
        }
    }
}