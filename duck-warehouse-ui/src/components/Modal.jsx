import { useEffect, useRef } from 'react';

export default function Modal({ title, titleId, busy, onClose, children }) {
  const ref = useRef(null);
  useEffect(() => {
    const dialog = ref.current;
    dialog.showModal();
    return () => dialog.close();
  }, []);
  return <dialog ref={ref} className="modal" aria-labelledby={titleId}
    onCancel={event => { event.preventDefault(); if (!busy) onClose(); }}>
    <div className="modal-heading"><h2 id={titleId}>{title}</h2>
      <button type="button" className="close-button" aria-label="Close dialog" disabled={busy} onClick={onClose}>×</button>
    </div>
    {children}
  </dialog>;
}
