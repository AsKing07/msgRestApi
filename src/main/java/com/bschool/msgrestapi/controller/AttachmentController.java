package com.bschool.msgrestapi.controller;

import com.bschool.msgrestapi.dto.response.AttachmentDownload;
import com.bschool.msgrestapi.dto.response.AttachmentResponse;
import com.bschool.msgrestapi.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/conversations/{conversationId}/files")
@RequiredArgsConstructor
@Tag(name = "Fichiers", description = "US8, US9")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "US8 — Envoyer un fichier à un ami")
    public AttachmentResponse upload(
            @PathVariable Long conversationId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        return attachmentService.upload(conversationId, userId, file);
    }

    @GetMapping
    @Operation(summary = "US8 — Lister les fichiers d'une discussion")
    public List<AttachmentResponse> list(
            @PathVariable Long conversationId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return attachmentService.listByConversation(conversationId, userId);
    }

    @GetMapping("/{attachmentId}/download")
    @Operation(summary = "US8 — Télécharger un fichier disponible")
    public ResponseEntity<Resource> download(
            @PathVariable Long conversationId,
            @PathVariable Long attachmentId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        AttachmentDownload download = attachmentService.download(conversationId, attachmentId, userId);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(download.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                );

        if (download.sizeBytes() != null) {
            response.contentLength(download.sizeBytes());
        }

        return response.body(download.resource());
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "US8 — Supprimer un fichier envoyé")
    public void delete(
            @PathVariable Long conversationId,
            @PathVariable Long attachmentId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        attachmentService.delete(conversationId, attachmentId, userId);
    }

    @PostMapping("/{attachmentId}/cancel")
    @Operation(summary = "US8 — Annuler un fichier envoyé")
    public AttachmentResponse cancel(
            @PathVariable Long conversationId,
            @PathVariable Long attachmentId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return attachmentService.cancel(conversationId, attachmentId, userId);
    }

    @PostMapping("/{attachmentId}/decline")
    @Operation(summary = "US8 — Décliner un fichier reçu")
    public AttachmentResponse decline(
            @PathVariable Long conversationId,
            @PathVariable Long attachmentId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return attachmentService.decline(conversationId, attachmentId, userId);
    }
}
