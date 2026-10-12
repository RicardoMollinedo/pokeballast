import { useState, type FormEvent } from 'react';
import styles from './Collection.module.css';

interface CollectionFiltersProps {
  name?: string;
  tag?: string;
  onApply: (filter: { name?: string; tag?: string }) => void;
}

export function CollectionFilters({ name = '', tag = '', onApply }: CollectionFiltersProps) {
  const [nameInput, setNameInput] = useState(name);
  const [tagInput, setTagInput] = useState(tag);

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    onApply({ name: nameInput.trim() || undefined, tag: tagInput.trim().toLowerCase() || undefined });
  };

  const clear = () => {
    setNameInput('');
    setTagInput('');
    onApply({});
  };

  return (
    <form className={styles.filters} onSubmit={handleSubmit} role="search">
      <input aria-label="Search by name" placeholder="Name or localized name" value={nameInput}
        onChange={(event) => setNameInput(event.target.value)} />
      <input aria-label="Filter by tag" placeholder="Tag, e.g. starter" value={tagInput}
        onChange={(event) => setTagInput(event.target.value)} />
      <button type="submit">Search</button>
      {(name || tag) && (
        <button type="button" className="button-link" onClick={clear}>Clear</button>
      )}
    </form>
  );
}
