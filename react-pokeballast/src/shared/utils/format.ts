/** "mr-mime" -> "Mr Mime" */
export function formatName(slug: string): string {
  return slug.split('-').map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(' ');
}

/** 25 -> "#025" */
export function formatId(id: number): string {
  return `#${String(id).padStart(3, '0')}`;
}
