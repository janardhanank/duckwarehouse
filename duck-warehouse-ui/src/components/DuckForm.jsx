import { useRef, useState } from 'react';
import Modal from './Modal.jsx';
import { COLORS, SIZES, validateDuckForm } from '../lib/duckForm.js';

function Field({ name, label, error, children }) {
  return <div className="field"><label htmlFor={name}>{label}</label>{children}
    {error && <p className="field-error" id={`${name}-error`}>{error}</p>}
  </div>;
}

export default function DuckForm({ duck, onSave, onClose }) {
  const editing = Boolean(duck);
  const [values, setValues] = useState({ color: duck?.color || 'Red', size: duck?.size || 'Medium',
    price: duck ? Number(duck.price).toFixed(2) : '', quantity: duck ? String(duck.quantity) : '' });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const submission = useRef(false);
  const update = event => {
    const { name, value } = event.target;
    setValues(previous => ({ ...previous, [name]: value }));
    setErrors(previous => ({ ...previous, [name]: '' }));
    setMessage('');
  };
  const attributes = name => ({ id: name, name, value: values[name], onChange: update,
    disabled: busy, 'aria-invalid': Boolean(errors[name]),
    'aria-describedby': errors[name] ? `${name}-error` : undefined });

  async function submit(event) {
    event.preventDefault();
    if (submission.current) return;
    const validated = validateDuckForm(values, editing);
    setErrors(validated.errors);
    if (!validated.payload) {
      document.getElementById(Object.keys(validated.errors)[0])?.focus();
      return;
    }
    submission.current = true;
    setBusy(true);
    setMessage('');
    try { await onSave(validated.payload); }
    catch (error) { setMessage(error.message); setErrors(error.fields || {}); }
    finally { submission.current = false; setBusy(false); }
  }

  return <Modal title={editing ? `Edit duck #${duck.id}` : 'Add a duck'} titleId="duck-form-title" busy={busy} onClose={onClose}>
    <p className="modal-description">{editing ? 'Update the price and units in stock.' : 'Choose a variant and add it to your warehouse.'}</p>
    <form onSubmit={submit} noValidate>
      {message && <div className="alert error" role="alert">{message}</div>}
      <div className="form-grid">
        <Field name="color" label="Color" error={errors.color}>
          {editing ? <input id="color" value={values.color} readOnly className="readonly" /> :
            <select {...attributes('color')} autoFocus>{COLORS.map(color => <option key={color}>{color}</option>)}</select>}
        </Field>
        <Field name="size" label="Size" error={errors.size}>
          {editing ? <input id="size" value={values.size} readOnly className="readonly" /> :
            <select {...attributes('size')}>{SIZES.map(size => <option key={size}>{size}</option>)}</select>}
        </Field>
        <Field name="price" label="Unit price (USD)" error={errors.price}>
          <input {...attributes('price')} inputMode="decimal" placeholder="0.00" autoFocus={editing} autoComplete="off" />
        </Field>
        <Field name="quantity" label={editing ? 'Units in stock' : 'Units to add'} error={errors.quantity}>
          <input {...attributes('quantity')} inputMode="numeric" placeholder="e.g. 100" autoComplete="off" />
        </Field>
      </div>
      <p className="form-note">{editing ? 'Color and size are fixed. Stock quantity replaces the current value.' :
        'If this color, size, and price already exist, these units are added to the existing stock.'}</p>
      <div className="modal-actions"><button type="button" className="button secondary" disabled={busy} onClick={onClose}>Cancel</button>
        <button type="submit" className="button primary" disabled={busy}>{busy ? 'Saving…' : editing ? 'Save changes' : 'Add duck'}</button>
      </div>
    </form>
  </Modal>;
}
