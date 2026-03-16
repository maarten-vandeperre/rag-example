function escapeHtml(content = '') {
  return content
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

export function highlightTextSnippet(content = '', snippet) {
  const safeContent = escapeHtml(content);
  if (!snippet?.content) {
    return safeContent;
  }

  const highlighted = escapeHtml(snippet.content);
  return safeContent.replace(highlighted, `<mark data-highlight="snippet">${highlighted}</mark>`);
}

export function findTextOccurrences(content = '', searchTerm = '') {
  const normalizedSearchTerm = searchTerm.trim().toLowerCase();
  if (!normalizedSearchTerm) {
    return [];
  }

  const matches = [];
  const normalizedContent = content.toLowerCase();
  let currentIndex = normalizedContent.indexOf(normalizedSearchTerm);

  while (currentIndex >= 0) {
    matches.push({
      start: currentIndex,
      end: currentIndex + normalizedSearchTerm.length,
      match: content.slice(currentIndex, currentIndex + normalizedSearchTerm.length)
    });
    currentIndex = normalizedContent.indexOf(normalizedSearchTerm, currentIndex + normalizedSearchTerm.length);
  }

  return matches;
}

export function highlightSearchResult(content = '', searchTerm = '', activeIndex = 0, snippet) {
  const occurrences = findTextOccurrences(content, searchTerm);
  const escaped = escapeHtml(content);

  let html = occurrences.length > 0
    ? occurrences.reduceRight((result, occurrence, index) => {
      const before = result.slice(0, occurrence.start);
      const match = result.slice(occurrence.start, occurrence.end);
      const after = result.slice(occurrence.end);
      const className = index === activeIndex ? 'search-highlight search-highlight-active' : 'search-highlight';
      return `${before}<mark class="${className}" data-highlight="search">${match}</mark>${after}`;
    }, escaped)
    : escaped;

  if (snippet?.content) {
    const escapedSnippet = escapeHtml(snippet.content);
    html = html.replace(escapedSnippet, `<mark data-highlight="snippet">${escapedSnippet}</mark>`);
  }

  return html;
}

const textHighlight = {
  findTextOccurrences,
  highlightSearchResult,
  highlightTextSnippet
};

export default textHighlight;
