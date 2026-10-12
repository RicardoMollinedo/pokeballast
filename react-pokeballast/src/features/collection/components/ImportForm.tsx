import { useState, type FormEvent } from 'react';
import { formatName } from '../../../shared/utils/format';
import { useImportPokemon } from '../hooks/useCollection';
import styles from './Collection.module.css';

/** US03: replicate a pokemon from PokeAPI into the local database. */
export function ImportForm() {
  const [idOrName, setIdOrName] = useState('');
  const importPokemon = useImportPokemon();

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const value = idOrName.trim().toLowerCase();
    if (!value) return;
    importPokemon.mutate(value, { onSuccess: () => setIdOrName('') });
  };

  return (
    <form className={`card ${styles.importForm}`} onSubmit={handleSubmit}>
      <label htmlFor="import-id">Import from PokeAPI</label>
      <div className={styles.row}>
        <input id="import-id" value={idOrName} onChange={(event) => setIdOrName(event.target.value)}
          placeholder="Name or number, e.g. gengar or 94" maxLength={50} required />
        <button type="submit" className="primary" disabled={importPokemon.isPending}>
          {importPokemon.isPending ? 'Importing…' : 'Import'}
        </button>
      </div>
      {importPokemon.isSuccess && (
        <p className="success-text" role="status">{formatName(importPokemon.data.name)} was added to the collection.</p>
      )}
      {importPokemon.isError && <p className="error-text" role="alert">{importPokemon.error.message}</p>}
    </form>
  );
}
