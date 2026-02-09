package com.intelligence.reader;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WordContentReader {
    public static List<String> read(Path path) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(path))) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .toList();
        }
    }
}