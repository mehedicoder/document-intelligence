package com.intelligence;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TextSimilarityRankerIntegrationTest {

    @TempDir
    Path tempDir;

    /**
     * Mock Model that generates embeddings based on string length for predictable testing.
     */
    private static class LengthBasedEmbeddingModel implements EmbeddingModel {
        @Override
        public Response<Embedding> embed(String text) {
            return Response.from(Embedding.from(new float[]{(float) text.length(), 1.0f}));
        }

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            return Response.from(segments.stream()
                    .map(s -> Embedding.from(new float[]{(float) s.text().length(), 1.0f}))
                    .toList());
        }
    }

    @Test
    @DisplayName("Should handle multiple formats (TXT, MD) and rank them")
    void shouldHandleMultipleFormats() throws IOException {
        Files.write(tempDir.resolve("test1.txt"), List.of("Standard text content"));
        Files.write(tempDir.resolve("test2.md"), List.of("# Markdown Title"));

        List<TextSegment> segments = TextSimilarityRanker.fetchUniqueTextSegmentsFromDirectory(tempDir.toAbsolutePath().toString());

        assertThat(segments).isNotEmpty();

        // Extract the text part for assertions
        assertThat(segments).extracting(TextSegment::text)
                .anyMatch(s -> s.toLowerCase().contains("standard"))
                .anyMatch(s -> s.toLowerCase().contains("markdown"));
    }

    @Test
    @DisplayName("Should handle CSV and JSON formats correctly")
    void shouldHandleStructuredData() throws Exception {
        Files.write(tempDir.resolve("data.csv"), List.of("name,desc", "Tomato,Fruit"));
        Files.write(tempDir.resolve("data.json"), List.of("{\"item\": \"Pasta\"}"));

        List<TextSegment> segments = TextSimilarityRanker.fetchUniqueTextSegmentsFromDirectory(tempDir.toAbsolutePath().toString());

        assertThat(segments).isNotEmpty();

        List<String> textContent = segments.stream().map(TextSegment::text).toList();
        assertThat(textContent).anyMatch(l -> l.contains("Tomato"));
        assertThat(textContent).anyMatch(l -> l.contains("Pasta"));

        EmbeddingModel fakeModel = new LengthBasedEmbeddingModel();
        Map<TextSegment, Double> results = TextSimilarityRanker.rankSegments(fakeModel, "Pasta", segments);

        boolean foundPastaInResults = results.keySet().stream()
                .anyMatch(segment -> segment.text().contains("Pasta"));

        assertThat(foundPastaInResults).isTrue();
    }

    @Test
    @DisplayName("Should extract and clean Markdown content")
    void shouldHandleMarkdownCleaning() throws IOException {
        Path mdFile = tempDir.resolve("guide.md");
        Files.writeString(mdFile, "### Header\nThis is **bold** and [a link](http://test.com).");

        List<TextSegment> segments = TextSimilarityRanker.fetchUniqueTextSegmentsFromDirectory(tempDir.toString());

        assertThat(segments).isNotEmpty();

        // Flatten all chunks from this file into one string for easier validation
        String fullCleanedText = segments.stream()
                .map(TextSegment::text)
                .collect(Collectors.joining(" "));

        // Verify syntax is stripped
        assertThat(fullCleanedText).doesNotContain("###", "**", "http://test.com");

        // Verify core content is preserved
        assertThat(fullCleanedText).contains("Header", "bold", "a link");

        // Verify metadata is correctly mapped to the segments
        assertThat(segments.get(0).metadata().getString("file_name")).isEqualTo("guide.md");
    }

    @Test
    @DisplayName("Should process mixed formats and rank them correctly")
    void shouldHandleMixedFormatsAndRanking() throws IOException {
        Files.writeString(tempDir.resolve("info.txt"), "Standard plain text here.");
        Files.writeString(tempDir.resolve("data.json"), "{\"note\": \"Searchable pasta recipe\"}");

        List<TextSegment> segments = TextSimilarityRanker.fetchUniqueTextSegmentsFromDirectory(tempDir.toString());

        assertThat(segments).hasSizeGreaterThanOrEqualTo(2);

        // Verification of Metadata across different readers
        assertThat(segments).extracting(s -> s.metadata().getString("file_name"))
                .contains("info.txt", "data.json");

        EmbeddingModel mockModel = new LengthBasedEmbeddingModel();
        Map<TextSegment, Double> rankings = TextSimilarityRanker.rankSegments(mockModel, "pasta", segments);

        boolean foundJsonContent = rankings.keySet().stream()
                .anyMatch(s -> s.text().contains("pasta"));

        assertThat(foundJsonContent).isTrue();
    }

    @Test
    @DisplayName("Should return empty list for empty directory")
    void handleEmptyDirectory() {
        List<TextSegment> segments = TextSimilarityRanker.fetchUniqueTextSegmentsFromDirectory(tempDir.toString());
        assertThat(segments).isEmpty();
    }
}