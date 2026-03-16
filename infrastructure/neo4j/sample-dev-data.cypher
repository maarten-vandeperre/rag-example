MERGE (admin:User {userId: '22222222-2222-2222-2222-222222222222'})
SET admin.username = 'jane.admin', admin.role = 'ADMIN', admin.email = 'jane.admin@example.com';

MERGE (user:User {userId: '11111111-1111-1111-1111-111111111111'})
SET user.username = 'john.doe', user.role = 'STANDARD', user.email = 'john.doe@example.com';

MERGE (doc:Document {documentId: 'doc-001'})
SET doc.fileName = 'sample-guide.pdf', doc.status = 'READY', doc.fileType = 'PDF';

MERGE (chunk:Chunk {chunkId: 'chunk-001'})
SET chunk.documentId = 'doc-001', chunk.position = 1, chunk.text = 'This sample guide explains document upload and retrieval.';

MERGE (user)-[:UPLOADED]->(doc)
MERGE (doc)-[:HAS_CHUNK]->(chunk)
MERGE (admin)-[:CAN_ADMINISTER]->(doc);
