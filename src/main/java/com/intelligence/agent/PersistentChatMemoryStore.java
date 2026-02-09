package com.intelligence.agent;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final Path storagePath = Paths.get("chat-memory.json");

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // Load messages from file
        if (!Files.exists(storagePath)) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(storagePath);
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // Save messages to file
        try {
            String json = ChatMessageSerializer.messagesToJson(messages);
            Files.writeString(storagePath, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        try {
            Files.deleteIfExists(storagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}