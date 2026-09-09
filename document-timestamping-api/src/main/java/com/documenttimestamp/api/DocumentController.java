package com.documenttimestamp.api;

import com.documenttimestamp.model.Document;
import com.documenttimestamp.model.ProofResponse;
import com.documenttimestamp.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "api/v1/documents")
public class DocumentController {
    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/")
    public ResponseEntity<ProofResponse> onDocumentUpload(@RequestParam("title") String title,
                                                         @RequestParam("file") MultipartFile file) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A document title is required");
        }
        if (title.length() > Document.TITLE_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A document title must be " + Document.TITLE_MAX_LENGTH + " characters or fewer");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A non-empty file is required");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.hashAndStoreDocument(title, file));
    }

    @PostMapping("/verify")
    public ResponseEntity<ProofResponse> onDocumentVerify(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A non-empty file is required");
        }
        return ResponseEntity.ok(documentService.verifyDocument(file));
    }
}
