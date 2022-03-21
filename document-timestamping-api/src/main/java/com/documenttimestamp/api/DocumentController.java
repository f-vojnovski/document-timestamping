package com.documenttimestamp.api;

import java.io.IOException;
import java.util.stream.Collectors;

import com.documenttimestamp.model.Document;
import com.documenttimestamp.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;


@CrossOrigin
@RestController
@RequestMapping(path = "api/v1/documents")
public class DocumentController {
    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/")
    public Document onDocumentUpload(@RequestParam("title") String title, @RequestParam("file") MultipartFile file) {
        try {
            return documentService.hashAndStoreDocument(title, file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/verify")
    public Document onDocumentVerify(@RequestParam("file") MultipartFile file) {
        try {
            return documentService.verifyDocument(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
