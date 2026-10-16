export const COLORS = ['Red', 'Green', 'Yellow', 'Black'];
export const SIZES = ['XLarge', 'Large', 'Medium', 'Small', 'XSmall'];
export const MAX_QUANTITY = 2147483647;

export function validateDuckForm(values, editing = false) {
  const errors = {};
  if (!editing && !COLORS.includes(values.color)) errors.color = 'Choose a valid color.';
  if (!editing && !SIZES.includes(values.size)) errors.size = 'Choose a valid size.';
  const price = String(values.price).trim();
  const quantity = String(values.quantity).trim();
  if (!/^\d{1,10}(\.\d{1,2})?$/.test(price)) {
    errors.price = 'Enter a price with up to 10 digits and 2 decimal places.';
  } else {
    const [whole, fraction = ''] = price.split('.');
    if (BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0')) < 1n) {
      errors.price = 'Price must be at least $0.01.';
    }
  }
  if (!/^\d+$/.test(quantity) || !Number.isSafeInteger(Number(quantity)) ||
      Number(quantity) < (editing ? 0 : 1) || Number(quantity) > MAX_QUANTITY) {
    errors.quantity = `Enter a whole number from ${editing ? 0 : 1} to 2,147,483,647.`;
  }
  if (Object.keys(errors).length) return { errors, payload: null };
  const payload = { price, quantity: Number(quantity) };
  if (!editing) Object.assign(payload, { color: values.color, size: values.size });
  return { errors, payload };
}

export function formatPrice(price) {
  // Presentation only. The form sends the exact decimal string to the backend.
  return Number(price).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
