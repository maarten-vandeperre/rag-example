package com.rag.app.chat.interfaces;

import com.rag.app.chat.usecases.models.DocumentChunk;
import com.rag.app.chat.usecases.models.GetChatHistoryInput;
import com.rag.app.chat.usecases.models.GetChatHistoryOutput;
import com.rag.app.chat.usecases.models.QueryDocumentsInput;
import com.rag.app.chat.usecases.models.QueryDocumentsOutput;

import java.util.List;

public interface ChatSystemFacade {
    QueryDocumentsOutput queryDocuments(QueryDocumentsInput input);

    GetChatHistoryOutput getChatHistory(GetChatHistoryInput input);

    void storeDocumentVectors(String documentId, String content);

    void removeDocumentVectors(String documentId);

    List<DocumentChunk> searchSimilarContent(String query, List<String> documentIds);
}
