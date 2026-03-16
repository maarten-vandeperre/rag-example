package com.rag.app.chat.interfaces;

import com.rag.app.chat.domain.entities.AnswerSourceReference;
import com.rag.app.chat.domain.exceptions.RepositoryException;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing answer source references.
 * Provides persistence operations for the relationship between chat answers
 * and the document chunks used to generate them.
 */
public interface AnswerSourceReferenceRepository {
    
    /**
     * Saves an answer source reference to persistent storage.
     * 
     * @param reference the answer source reference to save
     * @return the saved reference
     * @throws IllegalArgumentException if reference is null
     * @throws RepositoryException if persistence fails
     */
    AnswerSourceReference save(AnswerSourceReference reference);
    
    /**
     * Finds all source references for a given answer, ordered by source order.
     * 
     * @param answerId the ID of the answer
     * @return list of source references ordered by source order (ascending)
     * @throws IllegalArgumentException if answerId is null or blank
     */
    List<AnswerSourceReference> findByAnswerIdOrderBySourceOrder(String answerId);
    
    /**
     * Finds a source reference by its ID.
     * 
     * @param id the ID of the source reference
     * @return optional containing the reference if found, empty otherwise
     * @throws IllegalArgumentException if id is null or blank
     */
    Optional<AnswerSourceReference> findById(String id);
    
    /**
     * Deletes all source references for a given answer.
     * This is typically called when an answer is deleted.
     * 
     * @param answerId the ID of the answer
     * @throws IllegalArgumentException if answerId is null or blank
     */
    void deleteByAnswerId(String answerId);
    
    /**
     * Checks if any source references exist for a given answer.
     * 
     * @param answerId the ID of the answer
     * @return true if source references exist, false otherwise
     * @throws IllegalArgumentException if answerId is null or blank
     */
    boolean existsByAnswerId(String answerId);
    
    /**
     * Counts the number of source references for a given answer.
     * 
     * @param answerId the ID of the answer
     * @return the count of source references
     * @throws IllegalArgumentException if answerId is null or blank
     */
    long countByAnswerId(String answerId);
    
    /**
     * Finds source references by document ID.
     * Useful for cleanup when documents are deleted.
     * 
     * @param documentId the ID of the document
     * @return list of source references for the document
     * @throws IllegalArgumentException if documentId is null or blank
     */
    List<AnswerSourceReference> findByDocumentId(String documentId);
    
    /**
     * Updates the document ID to null for all references to a deleted document.
     * This maintains referential integrity while preserving the source content.
     * 
     * @param documentId the ID of the deleted document
     * @throws IllegalArgumentException if documentId is null or blank
     */
    void nullifyDocumentReferences(String documentId);
}