function extractModuleName(value) {
  if (!value) {
    return null;
  }

  const normalized = value.replace(/\\/g, '/');
  const match = normalized.match(/modules\/([^/]+)/);
  return match ? match[1] : null;
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'Disallow direct imports between product area modules'
    }
  },
  create(context) {
    return {
      ImportDeclaration(node) {
        const importModule = extractModuleName(node.source.value);
        const currentModule = extractModuleName(context.getFilename());

        if (!currentModule || !importModule) {
          return;
        }

        if (currentModule !== importModule && importModule !== 'shared') {
          context.report({
            node,
            message: `Module '${currentModule}' cannot directly import from '${importModule}'. Use shared interfaces instead.`
          });
        }
      }
    };
  }
};
