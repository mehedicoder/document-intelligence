package com.intelligence;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * com.intelligence.TextSimilarityRanker v3.5
 * Now fully integrated with SLF4J Logging.
 */
public class TextSimilarityRanker {

    private static final Logger log = LoggerFactory.getLogger(TextSimilarityRanker.class);

    private static final String DEFAULT_RESOURCES_DIR = "./src/main/resources/";
    private static final String OLLAMA_SERVICE_URL = "http://localhost:11434";
    private static final String EMBEDDING_MODEL_NAME = "nomic-embed-text:latest";
    private static final int DEFAULT_TOP_K_RESULTS = 5;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 100;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        log.info("Initializing Ollama Embedding Model: {}", EMBEDDING_MODEL_NAME);
        EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(OLLAMA_SERVICE_URL)
                .modelName(EMBEDDING_MODEL_NAME)
                .build();

        // UI Header stays as System.out for CLI clarity
        System.out.println("===========================================");
        System.out.println("   AI TEXT SIMILARITY RANKER LOADED");
        System.out.println("===========================================");

        while (true) {
            System.out.print("\nEnter data folder path (Enter for default & exit): ");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("bye")) break;

            boolean isDefault = userInput.isEmpty();
            String path = resolveDirectoryPath(userInput);

            System.out.print("\nWhat would you like to know (Ask your question or exit/bye to stop processing)? ");
            String query = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("bye")) break;

            if (!query.isBlank()) processDirectory(embeddingModel, path, query);
            else break;

            if (isDefault) break;
        }
        scanner.close();
        log.info("Application shut down by user.");
    }

    private static void processDirectory(EmbeddingModel model, String path, String query) {
        log.info("Starting processing for directory: {}", path);

        List<TextSegment> segments = fetchUniqueTextSegmentsFromDirectory(path);

        if (segments.isEmpty()) {
            log.warn("No content found or directory is empty at: {}", path);
            return;
        }

        log.info("Querying {} segments with text: '{}'", segments.size(), query);

        Map<TextSegment, Double> scores = rankSegments(model, query, segments);
        printTopRankedMatches(scores, DEFAULT_TOP_K_RESULTS);
    }

    static List<TextSegment> fetchUniqueTextSegmentsFromDirectory(String directoryPath) {
        log.debug("Scanning directory for supported files...");
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Path> files;
            try (Stream<Path> stream = Files.list(Paths.get(directoryPath))) {
                files = stream.filter(Files::isRegularFile)
                        .filter(TextSimilarityRanker::isSupportedFormat)
                        .toList();
            }

            if (files.isEmpty()) {
                log.debug("No supported files found in {}", directoryPath);
                return Collections.emptyList();
            }

            log.debug("Found {} files. Initializing Recursive Splitter.", files.size());
            DocumentSplitter splitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);

            List<Future<List<TextSegment>>> futures = files.stream()
                    .map(path -> executor.submit(() -> {
                        log.debug("Extracting and chunking: {}", path.getFileName());
                        String rawText = String.join(" ", extractContent(path));

                        Map<String, String> metaMap = new HashMap<>();
                        metaMap.put("file_name", path.getFileName().toString());
                        Metadata metadata = Metadata.from(metaMap);

                        Document doc = Document.from(rawText, metadata);
                        return splitter.split(doc);
                    }))
                    .toList();

            List<TextSegment> allSegments = new ArrayList<>();
            for (Future<List<TextSegment>> f : futures) {
                try {
                    allSegments.addAll(f.get());
                } catch (Exception e) {
                    log.error("Critical error processing a file task: {}", e.getMessage());
                }
            }
            log.info("Successfully created {} total segments from {} files.", allSegments.size(), files.size());
            return allSegments;
        } catch (IOException e) {
            log.error("IO Error while accessing directory {}: {}", directoryPath, e.getMessage());
            return Collections.emptyList();
        }
    }

    static Map<TextSegment, Double> rankSegments(EmbeddingModel model, String query, List<TextSegment> segments) {
        log.debug("Embedding query and segments via Ollama...");
        float[] queryVec = model.embed(query).content().vector();
        List<Embedding> docVecs = model.embedAll(segments).content();

        log.debug("Performing cosine similarity calculations...");
        Map<TextSegment, Double> scoringMap = new HashMap<>();
        for (int i = 0; i < segments.size(); i++) {
            double sim = calculateCosineSimilarity(queryVec, docVecs.get(i).vector());
            scoringMap.put(segments.get(i), sim);
        }
        return scoringMap;
    }

    static double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        return (normA == 0 || normB == 0) ? 0 : dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static boolean isSupportedFormat(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".pdf") ||
                name.endsWith(".docx") || name.endsWith(".csv") || name.endsWith(".json");
    }

    private static List<String> extractContent(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        try {
            if (name.endsWith(".pdf")) return PdfContentReader.read(path);
            if (name.endsWith(".docx")) return WordContentReader.read(path);
            if (name.endsWith(".csv")) return CsvContentReader.read(path);
            if (name.endsWith(".json")) return JsonContentReader.read(path);
            if (name.endsWith(".md") || name.endsWith(".markdown")) {
                return MarkdownContentReader.read(path);
            }
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Content Extraction Failed for {}: {}", name, e.getMessage());
            return Collections.emptyList();
        }
    }

    private static String resolveDirectoryPath(String input) {
        if (input.isEmpty()) return DEFAULT_RESOURCES_DIR;
        Path path = Paths.get(input);
        return (path.isAbsolute() && Files.isDirectory(path)) ? input : DEFAULT_RESOURCES_DIR;
    }

    private static void printTopRankedMatches(Map<TextSegment, Double> map, int limit) {
        // Result output remains System.out for the terminal user
        System.out.println("\n--- TOP RANKED MATCHES ---");
        map.entrySet().stream()
                .sorted(Map.Entry.<TextSegment, Double>comparingByValue().reversed())
                .limit(limit)
                .forEach(entry -> {
                    TextSegment segment = entry.getKey();
                    String fileName = segment.metadata().getString("file_name");
                    String snippet = segment.text().replace("\n", " ");
                    if (snippet.length() > 100) snippet = snippet.substring(0, 97) + "...";

                    System.out.printf("[Score: %.4f] (Source: %s) %s%n",
                            entry.getValue(),
                            fileName != null ? fileName : "Unknown",
                            snippet);
                });
        System.out.println("--------------------------");
    }
}