package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class GitHubIngestionService {
    private final VectorStore vectorStore;

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git", ".idea", ".vscode", "node_modules", "target", "build",
            "dist", "out", "bin", "obj", "venv", ".next", "coverage"
    );

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            ".jar", ".class", ".dll", ".exe", ".so", ".dylib",
            ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg", ".pdf",
            ".zip", ".tar", ".gz", ".mp3", ".mp4", ".mov"
    );

    public String ingestRepo(String repo) {
        try{
            File tempDir = Files.createTempDirectory("repo-").toFile();
            tempDir.deleteOnExit();

            try(Git git = Git.cloneRepository().setURI(repo).setDirectory(tempDir).call()){
                List<Document> documents = traverseAndParse(tempDir.toPath(),repo);

                TokenTextSplitter splitter = TokenTextSplitter.builder()
                        .withChunkSize(500)
                        .withMinChunkSizeChars(100)
                        .withMinChunkLengthToEmbed(5)
                        .withMaxNumChunks(10000)
                        .withKeepSeparator(true)
                        .withPunctuationMarks(List.of('\n', '.', '?', '!', ';'))
                        .build();

                List<Document> chunks = splitter.split(documents);
                vectorStore.add(chunks);
                return "Successfully ingested " + chunks.size() + " code chunks into Qdrant!";
            }
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Document> traverseAndParse(Path path, String repo) {
        List<Document> documents = new ArrayList<>();

        try(Stream<Path> paths = Files.walk(path)){
            paths.filter(Files::isRegularFile)
                    .filter(this::isCodeFile)
                    .forEach(p -> {
                        try{
                            String content = Files.readString(p);
                            String relativePath = path.relativize(p).toString();

                            Map<String, Object> metadata = Map.of(
                                    "repo",repo,
                                "file_path",relativePath
                            );
                            documents.add(new Document(content, metadata));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return documents;
    }

    private boolean isCodeFile(Path path) {
        String p = path.toString().replace("\\", "/");
        String fileName = path.getFileName().toString();

        for (String ignored : IGNORED_DIRECTORIES) {
            if (p.contains("/" + ignored + "/")) {
                return false;
            }
        }

        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            String extension = fileName.substring(lastDotIndex).toLowerCase();
            if (BINARY_EXTENSIONS.contains(extension)) {
                return false;
            }
        }
        return isTextFile(path);
    }

    private boolean isTextFile(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            int lengthToCheck = Math.min(bytes.length, 512);

            for (int i = 0; i < lengthToCheck; i++) {
                if (bytes[i] == 0x00) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
