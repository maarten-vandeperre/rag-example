package com.rag.app.usecases;

import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.GetAdminProgressInput;
import com.rag.app.usecases.models.GetAdminProgressOutput;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class GetAdminProgress {
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public GetAdminProgress(DocumentRepository documentRepository, UserRepository userRepository) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    public GetAdminProgressOutput execute(GetAdminProgressInput input) {
        Objects.requireNonNull(input, "input must not be null");

        if (input.adminUserId() == null || input.adminUserId().isBlank()) {
            throw new IllegalArgumentException("adminUserId must not be null or empty");
        }

        UUID adminUserId;
        try {
            adminUserId = UUID.fromString(input.adminUserId());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("adminUserId must be a valid UUID", exception);
        }

        User user = userRepository.findById(adminUserId)
            .orElseThrow(() -> new IllegalArgumentException("adminUserId user must exist"));

        if (user.role() != UserRole.ADMIN) {
            throw new IllegalArgumentException("adminUserId user must be an admin");
        }

        List<FailedDocumentInfo> failedDocuments = documentRepository.findFailedDocuments().stream()
            .sorted(Comparator.comparing(FailedDocumentInfo::uploadedAt).reversed())
            .toList();

        List<ProcessingDocumentInfo> processingDocuments = documentRepository.findProcessingDocuments().stream()
            .sorted(Comparator.comparing(ProcessingDocumentInfo::processingStartedAt))
            .toList();

        return new GetAdminProgressOutput(
            documentRepository.getProcessingStatistics(),
            failedDocuments,
            processingDocuments
        );
    }
}
