import PDFViewer from './PDFViewer';
import TextViewer from './TextViewer';

function DocumentContentRenderer({ document, html }) {
  if (document?.fileType === 'PDF') {
    return <PDFViewer html={html} />;
  }

  if (document?.fileType === 'MARKDOWN' || document?.fileType === 'PLAIN_TEXT') {
    return <TextViewer fileType={document.fileType} html={html} />;
  }

  return <TextViewer fileType={document?.fileType || 'Document'} html={html} />;
}

export default DocumentContentRenderer;
