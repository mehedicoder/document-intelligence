package com.intelligence.reader;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MarkdownContentReader {
    private static final Parser parser = Parser.builder().build();
    private static final TextContentRenderer renderer = TextContentRenderer.builder().build();

    public static List<String> read(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        Node document = parser.parse(content);

        // This visitor finds every Link node and wipes the URL destination
        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Link link) {
                link.setDestination(""); // Clear the URL
                visitChildren(link);      // Keep the text inside the link
            }

            @Override
            public void visit(Image image) {
                image.setDestination(""); // Clear image URLs/paths too
                visitChildren(image);
            }
        });

        return List.of(renderer.render(document).trim());
    }
}