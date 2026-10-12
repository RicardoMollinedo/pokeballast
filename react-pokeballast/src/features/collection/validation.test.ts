import { parseTags, validateCustomization } from './validation';

describe('customization rules (mirror of the backend Customization)', () => {
  it('parses, normalizes and de-duplicates tags', () => {
    expect(parseTags(' Starter, kanto-01 ,, starter ')).toEqual(['starter', 'kanto-01']);
  });

  it('accepts valid values', () => {
    expect(validateCustomization({ localizedName: 'ピカチュウ', region: 'Kanto', tags: ['mascot'] })).toEqual({});
  });

  it('rejects malformed tags, too many tags and too long texts', () => {
    expect(validateCustomization({ localizedName: '', region: '', tags: ['has space'] })).toHaveProperty('tags');
    expect(validateCustomization({ localizedName: '', region: '', tags: ['double--hyphen'] })).toHaveProperty('tags');
    expect(validateCustomization({ localizedName: '', region: '', tags: Array.from({ length: 11 }, (_, i) => `t${i}`) }))
      .toHaveProperty('tags');
    expect(validateCustomization({ localizedName: 'x'.repeat(61), region: 'y'.repeat(61), tags: [] }))
      .toEqual({ localizedName: expect.any(String), region: expect.any(String) });
  });
});
