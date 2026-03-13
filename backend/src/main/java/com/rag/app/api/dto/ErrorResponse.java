package com.rag.app.api.dto;

import java.time.Instant;

public record ErrorResponse(String error, String message, Instant timestamp) {
}
