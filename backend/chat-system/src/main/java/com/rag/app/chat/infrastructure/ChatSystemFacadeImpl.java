package com.rag.app.chat.infrastructure;

import com.rag.app.chat.interfaces.ChatSystemFacade;
import com.rag.app.chat.interfaces.SemanticSearch;
import com.rag.app.chat.interfaces.VectorStore;
import com.rag.app.chat.usecases.GetAnswerSourceDetails;
import com.rag.app.chat.usecases.GetChatHistory;
import com.rag.app.chat.usecases.QueryDocuments;
import com.rag.app.chat.usecases.models.DocumentChunk;
import com.rag.app.chat.usecases.models.GetAnswerSourceDetailsInput;
import com.rag.app.chat.usecases.models.GetAnswerSourceDetailsOutput;
import com.rag.app.chat.usecases.models.GetChatHistoryInput;
import com.rag.app.chat.usecases.models.GetChatHistoryOutput;
import com.rag.app.chat.usecases.models.QueryDocumentsInput;
import com.rag.app.chat.usecases.models.QueryDocumentsOutput;

import java.util.List;
import java.util.Objects;

public final class ChatSystemFacadeImpl implements ChatSystemFacade {
    private final QueryDocuments queryDocuments;
    private final GetChatHistory getChatHistory;
    private final GetAnswerSourceDetails getAnswerSourceDetails;
    private final VectorStore vectorStore;
    private final SemanticSearch semanticSearch;

    public ChatSystemFacadeImpl(QueryDocuments queryDocuments,
                                GetChatHistory getChatHistory,
                                GetAnswerSourceDetails getAnswerSourceDetails,
                                VectorStore vectorStore,
                                SemanticSearch semanticSearch) {
        this.queryDocuments = Objects.requireNonNull(queryDocuments, "queryDocuments must not be null");
        this.getChatHistory = Objects.requireNonNull(getChatHistory, "getChatHistory must not be null");
        this.getAnswerSourceDetails = Objects.requireNonNull(getAnswerSourceDetails, "getAnswerSourceDetails must not be null");
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
        this.semanticSearch = Objects.requireNonNull(semanticSearch, "semanticSearch must not be null");
    }

    @Override
    public QueryDocumentsOutput queryDocuments(QueryDocumentsInput input) {
        return queryDocuments.execute(input);
    }

    @Override
    public GetChatHistoryOutput getChatHistory(GetChatHistoryInput input) {
        return getChatHistory.execute(input);
    }
    
    @Override
    public GetAnswerSourceDetailsOutput getAnswerSourceDetails(GetAnswerSourceDetailsInput input) {
        return getAnswerSourceDetails.execute(input);
    }

    @Override
    public void storeDocumentVectors(String documentId, String content) {
        vectorStore.storeDocumentVectors(documentId, content);
    }

    @Override
    public void removeDocumentVectors(String documentId) {
        vectorStore.removeDocumentVectors(documentId);
    }

    @Override
    public List<DocumentChunk> searchSimilarContent(String query, List<String> documentIds) {
        return semanticSearch.searchDocuments(query, documentIds);
    }
}
