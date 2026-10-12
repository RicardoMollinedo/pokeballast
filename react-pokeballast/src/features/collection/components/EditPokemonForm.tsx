import { useState, type FormEvent, type ReactNode } from 'react';
import type { LocalPokemon, UpdateLocalPokemon } from '../../../api/types';
import { parseTags, validateCustomization } from '../validation';

interface EditPokemonFormProps {
  pokemon: LocalPokemon;
  onSubmit: (values: UpdateLocalPokemon) => void;
  submitting: boolean;
  serverErrors: Record<string, string>;
}

/** US04: the proprietary fields only. Full replacement: an empty field clears it. */
export function EditPokemonForm({ pokemon, onSubmit, submitting, serverErrors }: EditPokemonFormProps) {
  const [localizedName, setLocalizedName] = useState(pokemon.localizedName ?? '');
  const [region, setRegion] = useState(pokemon.region ?? '');
  const [tagsInput, setTagsInput] = useState(pokemon.tags.join(', '));
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});
  const errors = { ...serverErrors, ...clientErrors };

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const tags = parseTags(tagsInput);
    const found = validateCustomization({ localizedName, region, tags });
    setClientErrors(found);
    if (Object.keys(found).length > 0) return;
    onSubmit({ localizedName: localizedName.trim() || null, region: region.trim() || null, tags, version: pokemon.version });
  };

  return (
    <form className="card form" onSubmit={handleSubmit} noValidate>
      <Field id="localizedName" label="Localized name" error={errors.localizedName}>
        <input id="localizedName" value={localizedName} onChange={(event) => setLocalizedName(event.target.value)}
          aria-invalid={Boolean(errors.localizedName)}
          aria-describedby={errors.localizedName ? 'localizedName-error' : undefined} />
      </Field>
      <Field id="region" label="Region" error={errors.region}>
        <input id="region" value={region} onChange={(event) => setRegion(event.target.value)}
          aria-invalid={Boolean(errors.region)} aria-describedby={errors.region ? 'region-error' : undefined} />
      </Field>
      <Field id="tags" label="Tags (comma separated)" error={errors.tags}>
        <input id="tags" value={tagsInput} onChange={(event) => setTagsInput(event.target.value)}
          placeholder="starter, kanto-favorite" aria-invalid={Boolean(errors.tags)}
          aria-describedby={errors.tags ? 'tags-error' : 'tags-hint'} />
        <p id="tags-hint" className="hint">Up to 10 tags; lowercase letters, digits and single hyphens.</p>
      </Field>
      <button type="submit" className="primary" disabled={submitting}>
        {submitting ? 'Saving…' : 'Save changes'}
      </button>
    </form>
  );
}

function Field({ id, label, error, children }: { id: string; label: string; error?: string; children: ReactNode }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children}
      {error && <p id={`${id}-error`} className="error-text">{error}</p>}
    </div>
  );
}
