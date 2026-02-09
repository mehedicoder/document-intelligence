package com.intelligence;

import com.opencsv.CSVReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class CsvContentReader {
    /**
     * Reads CSV and joins columns into a single descriptive string per row.
     */
    public static List<String> read(Path path) throws Exception {
        List<String> rows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(Files.newBufferedReader(path))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                // Filter out empty columns and join with spaces
                String rowText = Arrays.stream(nextLine)
                        .filter(col -> !col.trim().isEmpty())
                        .collect(Collectors.joining(" "));
                if (!rowText.isEmpty()) rows.add(rowText);
            }
        }
        return rows;
    }
}