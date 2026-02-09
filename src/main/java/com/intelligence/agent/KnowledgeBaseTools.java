package com.intelligence.agent;

import com.intelligence.reader.*;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class KnowledgeBaseTools {
    private final ContextRetriever retriever;
    private final String currentFolderPath; // Pass the folder path here

    public KnowledgeBaseTools(ContextRetriever retriever, String currentFolderPath) {
        this.retriever = retriever;
        this.currentFolderPath = currentFolderPath;
    }

    @Tool("Summarizes a specific document by its filename. Use this when the user says 'summarize' or 'summarise'.")
    public String summarizeDocument(@P("The exact filename to summarize (e.g., roadmap.pdf)") String fileName) {
        try {
            Path path = Paths.get(currentFolderPath, fileName);
            List<String> lines;

            // Reusing your existing readers based on extension
            if (fileName.endsWith(".pdf")) lines = PdfContentReader.read(path);
            else if (fileName.endsWith(".docx")) lines = WordContentReader.read(path);
            else if (fileName.endsWith(".md")) lines = MarkdownContentReader.read(path);
            else lines = List.of(java.nio.file.Files.readString(path));

            String fullText = String.join("\n", lines);

            // Limit text if it's massive to avoid token overflow
            if (fullText.length() > 20000) {
                fullText = fullText.substring(0, 20000) + "... [Text truncated for brevity]";
            }

            return "Full Content of " + fileName + ":\n" + fullText;
        } catch (Exception e) {
            return "Error: Could not find or read " + fileName + ". Ensure the name is correct.";
        }
    }

    @Tool("Searches for snippets across all documents.")
    public String searchDocuments(@P("Query") String query) {
        return retriever.retrieve(dev.langchain4j.rag.query.Query.from(query))
                .stream().map(c -> c.textSegment().text()).collect(java.util.stream.Collectors.joining("\n---\n"));
    }
}