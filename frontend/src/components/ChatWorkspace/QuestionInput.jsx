import { useMemo } from 'react';

const MAX_QUESTION_LENGTH = 180;

function QuestionInput({ value, onChange, onSubmit, disabled }) {
  const remaining = useMemo(() => MAX_QUESTION_LENGTH - value.length, [value]);

  const handleSubmit = (event) => {
    event.preventDefault();
    onSubmit();
  };

  return (
    <form className="question-form" onSubmit={handleSubmit}>
      <label className="question-label" htmlFor="question-input">
        Ask about your uploaded documents
      </label>
      <div className="question-row">
        <textarea
          id="question-input"
          className="question-input"
          value={value}
          onChange={(event) => onChange(event.target.value.slice(0, MAX_QUESTION_LENGTH))}
          placeholder="What changed in the onboarding guide?"
          disabled={disabled}
          rows="3"
        />
        <button className="question-button" type="submit" disabled={disabled || !value.trim()}>
          {disabled ? 'Thinking...' : 'Ask'}
        </button>
      </div>
      <div className="question-footer">
        <span>Press Enter in the button or submit to ask your question.</span>
        <span>{remaining} characters left</span>
      </div>
    </form>
  );
}

export default QuestionInput;
