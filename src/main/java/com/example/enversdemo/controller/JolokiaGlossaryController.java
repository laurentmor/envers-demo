package com.example.enversdemo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class JolokiaGlossaryController {

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping(path = "/jolokia/glossary/save", consumes = "application/json")
    public ResponseEntity<?> saveGlossary(@RequestBody Object payload) {
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            Path out = Path.of("jolokia-glossary.json");
            Files.writeString(out, json);
            return ResponseEntity.ok().body(java.util.Map.of("path", out.toAbsolutePath().toString()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
