package com.rag.app.shared.domain.exceptions;

public final class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(String rule, String message) {
        super("BUSINESS_RULE_VIOLATION", String.format("Rule '%s' violated: %s", rule, message));
    }
}
