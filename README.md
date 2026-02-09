# 🧠 Document Intelligence
**A private, local-first AI assistant that actually knows your files.**

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![LangChain4j](https://img.shields.io/badge/Framework-LangChain4j-blue.svg)](https://github.com/langchain4j/langchain4j)
[![Ollama](https://img.shields.io/badge/Embeddings-Ollama-white.svg)](https://ollama.ai/)

I built **Document Intelligence** because I was tired of "Command+F-ing" through dozens of PDFs and notes just to find one specific detail. This tool transforms your local folders into a searchable, interactive knowledge base using Retrieval-Augmented Generation (RAG).

Unlike most AI tools, this doesn't require you to upload your sensitive data to the cloud. Everything—from indexing to searching—happens on your own hardware.

---

## 🚀 Key Features

* **100% Data Privacy:** We use local **Ollama** embeddings (`nomic-embed-text`). Your files stay where they belong: on your machine.
* **Virtual Thread Performance:** Built on **Java 21**, the system uses Virtual Threads (Project Loom) to process and index massive document sets in parallel.
* **Verified Citations:** No more AI "hallucinations." Every answer includes the specific source filename (e.g., `(Source: roadmap_2026.pdf)`).
* **Streaming & Thinking:** Features a professional "thinking" indicator and human-like streaming response for a smooth, Ollama-style CLI experience.
* **Persistent Memory:** It remembers your conversation context, so you can ask follow-up questions without repeating yourself.

---

## 📂 Supported Formats

| Category | File Types |
| :--- | :--- |
| **Documents** | `.pdf`, `.docx` |
| **Technical** | `.md`, `.markdown`, `.txt` |
| **Data** | `.csv`, `.json` |

---

## 🛠️ Setup & Installation

### 1. Prerequisites
* **Java 21+** (The Virtual Thread magic depends on it).
* **Ollama** installed and running on `localhost:11434`.
* **Groq API Key:** Set as an environment variable: `GROQ_API_KEY`.

### 2. Pull the Models
Open your terminal and grab the embedding model:
```bash
ollama pull nomic-embed-text