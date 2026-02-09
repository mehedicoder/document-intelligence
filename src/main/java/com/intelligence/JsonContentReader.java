package com.intelligence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class JsonContentReader {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Reads JSON and extracts all text values recursively.
     */
    public static List<String> read(Path path) throws IOException {
        JsonNode rootNode = mapper.readTree(path.toFile());
        List<String> values = new ArrayList<>();
        extractTextValues(rootNode, values);
        return values;
    }

    private static void extractTextValues(JsonNode node, List<String> collector) {
        if (node.isTextual()) {
            collector.add(node.asText());
        } else if (node.isArray() || node.isObject()) {
            node.forEach(child -> extractTextValues(child, collector));
        }
    }
}