import DocumentLibrary from './components/DocumentLibrary';
import FileUpload from './components/FileUpload';
import DocumentList from './components/DocumentList';
import { useDocumentUpload } from './hooks/useDocumentUpload';
import { useDocumentList } from './hooks/useDocumentList';

export { DocumentLibrary, FileUpload, DocumentList, useDocumentUpload, useDocumentList };

export const DocumentManagementModule = {
  name: 'document-management',
  version: '1.0.0',
  components: { DocumentLibrary, FileUpload, DocumentList },
  hooks: { useDocumentUpload, useDocumentList }
};
