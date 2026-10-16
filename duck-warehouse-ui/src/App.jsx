import { useEffect, useRef, useState } from 'react';
import { useWarehouse } from './hooks/useWarehouse.js';
import { ducksApi } from './api/ducks.js';
import DuckForm from './components/DuckForm.jsx';
import DuckTable from './components/DuckTable.jsx';
import DeleteConfirmation from './components/DeleteConfirmation.jsx';
import { DuckIcon, PlusIcon, RefreshIcon } from './components/Icons.jsx';

export default function App() {
  const { ducks, loading, error, hasLoaded, reload } = useWarehouse();
  const [modal, setModal] = useState(null);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState('');
  const opener = useRef(null);
  const units = ducks.reduce((total, duck) => total + BigInt(duck.quantity), 0n);
  const stocked = ducks.filter(duck => duck.quantity > 0).length;

  function open(next) { opener.current = document.activeElement; setNotice(''); setModal(next); }
  function close() { setModal(null); }
  useEffect(() => {
    if (!modal && !saving) {
      opener.current?.focus();
      opener.current = null;
    }
  }, [modal, saving]);
  async function write(operation, success) {
    setSaving(true);
    try {
      await operation();
      close();
      setNotice(success);
      await reload();
    } finally { setSaving(false); }
  }

  return <>
    <header className="topbar"><div className="topbar-inner">
      <a href="/" className="brand" aria-label="Duck Warehouse home"><span className="brand-mark"><DuckIcon /></span><span>Duck<span className="brand-light"> / Warehouse</span></span></a>
      <span className="workspace-tag">Inventory workspace</span>
    </div></header>
    <main className="container">
      <section className="page-heading"><div><p className="eyebrow">WAREHOUSE MANAGEMENT</p><h1>Duck Warehouse<span className="heading-dot">.</span></h1><p className="subtitle">A clear view of every duck in stock.</p></div>
        <button className="button primary add-control" disabled={saving || loading || Boolean(error)} onClick={() => open({ type: 'add' })}><PlusIcon />Add duck</button>
      </section>
      <section className="stats" aria-label="Inventory summary">
        <div className="stat"><span className="stat-label">Active variants</span><strong>{hasLoaded ? ducks.length.toLocaleString('en-US') : '—'}</strong><span>Distinct color, size & price combinations</span></div>
        <div className="stat"><span className="stat-label">Total units</span><strong>{hasLoaded ? units.toLocaleString('en-US') : '—'}</strong><span>Ducks currently in your warehouse</span></div>
        <div className="stat"><span className="stat-label">Variants in stock</span><strong>{hasLoaded ? stocked.toLocaleString('en-US') : '—'}</strong><span>With at least one unit available</span></div>
      </section>
      {notice && <div className="alert success" role="status"><span>{notice}</span><button aria-label="Dismiss notification" onClick={() => setNotice('')}>×</button></div>}
      {error && <div className="alert error" role="alert"><span>{error}{hasLoaded && ' Showing the last loaded inventory; it may be out of date.'}</span><button className="text-button" disabled={saving || loading} onClick={reload}>Retry</button></div>}
      <section className="inventory-card" aria-labelledby="inventory-title">
        <div className="table-toolbar"><div><h2 id="inventory-title">Inventory <span className="count-pill">{hasLoaded ? ducks.length : '—'}</span></h2><p>Sorted by quantity, lowest first</p></div>
          <button className="button secondary refresh" disabled={loading || saving} onClick={reload}><RefreshIcon />{loading ? 'Refreshing…' : 'Refresh'}</button>
        </div>
        <DuckTable ducks={ducks} loading={loading} hasLoaded={hasLoaded} error={error} disabled={loading || saving || Boolean(error)} onEdit={duck => open({ type: 'edit', duck })} onDelete={duck => open({ type: 'delete', duck })} />
        <div className="table-footer"><span>{hasLoaded ? `${ducks.length} active ${ducks.length === 1 ? 'variant' : 'variants'}` : 'Waiting for inventory'}</span><span>All prices in USD</span></div>
      </section>
      <footer className="page-footer"><span>Duck Warehouse</span><span>Keep your ducks in a row.</span></footer>
    </main>
    {(modal?.type === 'add' || modal?.type === 'edit') && <DuckForm key={modal.duck?.id || 'add'} duck={modal.duck} onClose={close}
      onSave={payload => write(() => modal.type === 'edit' ? ducksApi.edit(modal.duck.id, payload) : ducksApi.add(payload), modal.type === 'edit' ? 'Duck updated.' : 'Duck added. Matching variants are merged automatically.')} />}
    {modal?.type === 'delete' && <DeleteConfirmation duck={modal.duck} onClose={close} onDelete={id => write(() => ducksApi.remove(id), 'Duck deleted from the inventory.')} />}
  </>;
}
