function DocumentReference({ reference }) {
  const score = Math.round(reference.relevanceScore * 100);

  return (
    <a className="reference-card" href={reference.href || '#source'}>
      <span className="reference-name">{reference.documentName}</span>
      <span className="reference-meta">{reference.paragraphReference}</span>
      <span className="reference-score">{score}% match</span>
    </a>
  );
}

export default DocumentReference;
