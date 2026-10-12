import { formatId, formatName } from './format';

it('formats names and ids for display', () => {
  expect(formatName('mr-mime')).toBe('Mr Mime');
  expect(formatId(25)).toBe('#025');
  expect(formatId(1025)).toBe('#1025');
});
