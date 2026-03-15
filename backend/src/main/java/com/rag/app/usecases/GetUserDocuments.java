package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.models.DocumentSummary;
import com.rag.app.usecases.models.GetUserDocumentsInput;
import com.rag.app.usecases.models.GetUserDocumentsOutput;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public final class GetUserDocuments {
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Inject
    public GetUserDocuments(DocumentRepository documentRepository, UserRepository userRepository) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    public GetUserDocumentsOutput execute(GetUserDocumentsInput input) {
        Objects.requireNonNull(input, "input must not be null");

        if (input.userId() == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        User user = userRepository.findById(input.userId())
            .orElseThrow(() -> new IllegalArgumentException("userId user must exist"));

        List<DocumentSummary> documents = resolveDocuments(user, input.includeAllDocuments()).stream()
            .sorted(Comparator.comparing(Document::uploadedAt).reversed())
            .map(document -> new DocumentSummary(
                document.documentId(),
                document.fileName(),
                document.fileSize(),
                document.fileType(),
                document.status(),
                document.uploadedBy(),
                document.uploadedAt(),
                document.uploadedAt()
            ))
            .toList();

        return new GetUserDocumentsOutput(documents, documents.size());
    }

    private List<Document> resolveDocuments(User user, boolean includeAllDocuments) {
        if (user.role() == UserRole.ADMIN && includeAllDocuments) {
            return documentRepository.findAll();
        }

        return documentRepository.findByUploadedBy(user.userId().toString());
    }
}
