import { useRef, useState } from 'react';
import Modal from './Modal.jsx';

export default function DeleteConfirmation({ duck, onDelete, onClose }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const submission = useRef(false);
  async function confirm() {
    if (submission.current) return;
    submission.current = true;
    setBusy(true);
    setError('');
    try { await onDelete(duck.id); }
    catch (err) { setError(err.message); }
    finally { submission.current = false; setBusy(false); }
  }
  return <Modal title="Delete this duck?" titleId="delete-title" busy={busy} onClose={onClose}>
    <p className="modal-description">Duck <strong>#{duck.id} · {duck.color} · {duck.size}</strong> will be removed from the inventory listing. Its record will be retained.</p>
    {error && <div className="alert error" role="alert">{error}</div>}
    <div className="modal-actions"><button className="button secondary" disabled={busy} onClick={onClose} autoFocus>Cancel</button>
      <button className="button danger" disabled={busy} onClick={confirm}>{busy ? 'Deleting…' : 'Delete duck'}</button>
    </div>
  </Modal>;
}
