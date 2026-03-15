package com.rag.app.chat.usecases.models;

import com.rag.app.chat.domain.entities.ChatMessage;

import java.util.List;

public record GetChatHistoryOutput(List<ChatMessage> messages) {
}
