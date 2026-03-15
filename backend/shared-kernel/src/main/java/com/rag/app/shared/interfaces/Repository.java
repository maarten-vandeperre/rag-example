package com.rag.app.shared.interfaces;

import java.util.Optional;

public interface Repository<T, ID> {
    void save(T entity);

    Optional<T> findById(ID id);

    void delete(ID id);

    boolean exists(ID id);
}
