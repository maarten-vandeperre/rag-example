FROM postgres:15-alpine

RUN apk add --no-cache git build-base clang19 llvm19-dev

RUN git clone --branch v0.5.1 --depth 1 https://github.com/pgvector/pgvector.git /tmp/pgvector \
    && make -C /tmp/pgvector PG_CONFIG=/usr/local/bin/pg_config \
    && make -C /tmp/pgvector PG_CONFIG=/usr/local/bin/pg_config install \
    && rm -rf /tmp/pgvector

COPY init.sql /docker-entrypoint-initdb.d/10-init.sql
COPY sample-data.sql /docker-entrypoint-initdb.d/20-sample-data.sql
