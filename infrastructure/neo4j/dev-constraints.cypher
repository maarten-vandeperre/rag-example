CREATE CONSTRAINT document_id_unique IF NOT EXISTS
FOR (d:Document)
REQUIRE d.documentId IS UNIQUE;

CREATE CONSTRAINT user_id_unique IF NOT EXISTS
FOR (u:User)
REQUIRE u.userId IS UNIQUE;

CREATE CONSTRAINT chunk_id_unique IF NOT EXISTS
FOR (c:Chunk)
REQUIRE c.chunkId IS UNIQUE;

CREATE INDEX document_status_index IF NOT EXISTS
FOR (d:Document)
ON (d.status);

CREATE INDEX document_file_name_index IF NOT EXISTS
FOR (d:Document)
ON (d.fileName);

CREATE INDEX user_username_index IF NOT EXISTS
FOR (u:User)
ON (u.username);

CREATE INDEX chunk_document_id_index IF NOT EXISTS
FOR (c:Chunk)
ON (c.documentId);
