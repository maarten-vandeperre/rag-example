package com.rag.app.shared.interfaces;

public interface UseCase<INPUT, OUTPUT> {
    OUTPUT execute(INPUT input);
}
