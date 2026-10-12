/**
 * Mirror of the backend rules (bl.model.Customization) for instant feedback. The backend stays the
 * source of truth: its field errors (code 1004) are shown in the same place.
 */
export const MAX_TEXT = 60;
const MAX_TAGS = 10;
const MAX_TAG_LENGTH = 30;
const TAG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;

export function parseTags(raw: string): string[] {
  return [...new Set(raw.split(',').map((tag) => tag.trim().toLowerCase()).filter(Boolean))];
}

export function validateCustomization(values: { localizedName: string; region: string; tags: string[] }) {
  const errors: Record<string, string> = {};
  if (values.localizedName.trim().length > MAX_TEXT) errors.localizedName = `must be at most ${MAX_TEXT} characters`;
  if (values.region.trim().length > MAX_TEXT) errors.region = `must be at most ${MAX_TEXT} characters`;
  if (values.tags.length > MAX_TAGS) {
    errors.tags = `at most ${MAX_TAGS} tags are allowed`;
  } else if (values.tags.some((tag) => tag.length > MAX_TAG_LENGTH || !TAG_PATTERN.test(tag))) {
    errors.tags = 'use lowercase letters, digits and single hyphens, e.g. kanto-starter';
  }
  return errors;
}
