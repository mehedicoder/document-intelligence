package com.intelligence;

import com.intelligence.agent.DocumentAssistantAgent;
import com.intelligence.agent.ContextRetriever;
import com.intelligence.agent.PersistentChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class DocumentIntelligenceApp {
    private static final Logger log = LoggerFactory.getLogger(DocumentIntelligenceApp.class);

    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";
    private static final String GROQ_MODEL_NAME = "llama-3.3-70b-versatile";
    private static final String GROQ_API_KEY_ENV = "GROQ_API_KEY";
    private static final String OLLAMA_SERVICE_URL = "http://localhost:11434";
    private static final String EMBEDDING_MODEL_NAME = "nomic-embed-text:latest";

    private static final String DEFAULT_DATA_PATH = "./src/main/resources/";
    private static final int CHAT_MEMORY_MAX_MESSAGES = 20;
    private static final int MODEL_TIMEOUT_SECONDS = 120;

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");

        StreamingChatModel chatModel = createStreamingModel();
        EmbeddingModel embeddingModel = createEmbeddingModel();
        ChatMemoryStore store = new PersistentChatMemoryStore();
        Scanner scanner = new Scanner(System.in);
        String userId = "user-" + System.getProperty("user.name");

        System.out.println("\n==============================================");
        System.out.println("   DOCUMENT INTELLIGENCE AGENT (v2.0)");
        System.out.println("==============================================\n");

        outerLoop:
        while (true) {
            // 1. Data Folder Selection
            System.out.println("\n[STEP 1: Load Data]");
            System.out.print("Enter data folder path (or press Enter for 'default'): ");
            String inputDir = scanner.nextLine().trim();

            if (isExitCommand(inputDir)) break;

            String resolvedPath = (inputDir.isEmpty() || inputDir.equalsIgnoreCase("default"))
                    ? DEFAULT_DATA_PATH : inputDir;

            System.out.println(">> Loading context from: " + resolvedPath);
            ContextRetriever contextRetriever = new ContextRetriever(embeddingModel, resolvedPath);
            DocumentAssistantAgent assistant = buildAssistant(chatModel, contextRetriever, store);

            // 2. Question Loop for the current folder
            while (true) {
                System.out.print("\nAsk your question: ");
                String query = scanner.nextLine().trim();

                if (isExitCommand(query)) break outerLoop;
                if (query.isEmpty()) continue;

                executeStreamingChat(assistant, userId, query);

                // 3. Post-Answer Prompt
                System.out.print("\n\nContinue with this folder? (yes/continue) or enter 'new' for another folder: ");
                String choice = scanner.nextLine().trim().toLowerCase();

                if (isExitCommand(choice)) break outerLoop;

                if (choice.equals("new") || choice.equals("another")) {
                    break; // Breaks inner loop to ask for a new folder in outer loop
                }
                // If they type 'yes' or 'continue', the inner loop repeats
            }
        }

        System.out.println("\nAI: Goodbye! Have a productive day.");
        scanner.close();
    }

    private static boolean isExitCommand(String input) {
        return input.equalsIgnoreCase("exit") ||
                input.equalsIgnoreCase("quit") ||
                input.equalsIgnoreCase("bye");
    }

    // --- Helper & Builder Methods ---

    private static StreamingChatModel createStreamingModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(System.getenv(GROQ_API_KEY_ENV))
                .baseUrl(GROQ_BASE_URL)
                .modelName(GROQ_MODEL_NAME)
                .timeout(Duration.ofSeconds(MODEL_TIMEOUT_SECONDS))
                .build();
    }

    private static EmbeddingModel createEmbeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(OLLAMA_SERVICE_URL)
                .modelName(EMBEDDING_MODEL_NAME)
                .build();
    }

    private static DocumentAssistantAgent buildAssistant(StreamingChatModel chatModel, ContextRetriever retriever, ChatMemoryStore store) {
        return AiServices.builder(DocumentAssistantAgent.class)
                .streamingChatModel(chatModel)
                .contentRetriever(retriever)
                .chatMemoryProvider(chatId -> MessageWindowChatMemory.builder()
                        .id(chatId)
                        .maxMessages(CHAT_MEMORY_MAX_MESSAGES)
                        .chatMemoryStore(store)
                        .build())
                .build();
    }

    private static void executeStreamingChat(DocumentAssistantAgent assistant, String userId, String question) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicBoolean isFirstToken = new AtomicBoolean(true);

        System.out.print("AI: thinking...");
        System.out.flush();

        assistant.chatStreaming(userId, question)
                .onPartialResponse(token -> {
                    if (isFirstToken.getAndSet(false)) {
                        System.out.print("\rAI:               \rAI: ");
                        System.out.flush();
                    }
                    stochasticPrint(token);
                })
                .onCompleteResponse(response -> future.complete(null))
                .onError(err -> {
                    System.out.print("\rAI: [ERROR]       \n");
                    System.err.println(err.getMessage());
                    future.complete(null);
                })
                .start();

        future.join();
    }

    private static void stochasticPrint(String chunk) {
        Random random = ThreadLocalRandom.current();
        for (char c : chunk.toCharArray()) {
            System.out.print(c);
            System.out.flush();
            try {
                int delay = 35 + random.nextInt(35);
                if (".?!".indexOf(c) != -1) delay += 300;
                else if (",:;".indexOf(c) != -1) delay += 100;
                if (random.nextDouble() > 0.92) delay = 8;
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}