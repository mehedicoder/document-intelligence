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

    // --- Configuration Constants ---
    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";
    private static final String GROQ_MODEL_NAME = "llama-3.3-70b-versatile";
    private static final String GROQ_API_KEY_ENV = "GROQ_API_KEY";

    private static final String OLLAMA_SERVICE_URL = "http://localhost:11434";
    private static final String EMBEDDING_MODEL_NAME = "nomic-embed-text:latest";

    private static final String DATA_RESOURCES_PATH = "./src/main/resources/";
    private static final int CHAT_MEMORY_MAX_MESSAGES = 20;
    private static final int MODEL_TIMEOUT_SECONDS = 120;

    public static void main(String[] args) {
        // force UTF-8 character encoding
        System.setProperty("file.encoding", "UTF-8");

        //Initialize Components
        StreamingChatModel chatModel = createStreamingModel();
        EmbeddingModel embeddingModel = createEmbeddingModel();
        ChatMemoryStore store = new PersistentChatMemoryStore();
        Scanner scanner = new Scanner(System.in);
        String userId = "user-" + System.getProperty("user.name");

        System.out.println("\n==============================================");
        System.out.println("   DOCUMENT INTELLIGENCE AGENT (v2.0)");
        System.out.println("   Type 'exit', 'quit', or 'bye' to stop.");
        System.out.println("==============================================\n");

        while (true) {
            System.out.print("\nEnter data folder path (Enter for default & exit): ");
            String dataDir = scanner.nextLine().trim();
            ContextRetriever contextRetriever;

            if (dataDir.isBlank() || dataDir.isBlank() || dataDir.equals("default")) contextRetriever= new ContextRetriever(embeddingModel, dataDir);
            else contextRetriever= new ContextRetriever(embeddingModel, dataDir);

            //Build the Assistant
            DocumentAssistantAgent assistant = buildAssistant(chatModel, contextRetriever, store);
            System.out.print("Ask: ");
            String query = scanner.nextLine().trim();

            if (isExitCommand(query)) {
                System.out.println("AI: Goodbye! Have a productive day.");
                break;
            }

            if (query.isEmpty()) continue;

            executeStreamingChat(assistant, userId, query);
            System.out.println();
        }
        scanner.close();
    }

    private static boolean isExitCommand(String input) {
        return input.equalsIgnoreCase("exit") ||
                input.equalsIgnoreCase("quit") ||
                input.equalsIgnoreCase("bye");
    }

    // --- Builder Methods ---

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

        // 1. Show the "Thinking" state immediately
        System.out.print("AI: thinking...");
        System.out.flush();

        assistant.chatStreaming(userId, question)
                .onPartialResponse(token -> {
                    // 2. Clear "thinking..." when the first token arrives
                    if (isFirstToken.getAndSet(false)) {
                        // \r moves cursor to start of line, then we print spaces to clear the text
                        System.out.print("\rAI:               \rAI: ");
                        System.out.flush();
                    }

                    // 3. Print the token with your typing effect
                    stochasticPrint(token);
                })
                .onCompleteResponse(response -> {
                    System.out.println(); // Finish the line
                    future.complete(null);
                })
                .onError(err -> {
                    // Clear the thinking indicator if an error occurs
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