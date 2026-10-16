import { formatPrice } from '../lib/duckForm.js';
import { DuckIcon } from './Icons.jsx';

export default function DuckTable({ ducks, loading, hasLoaded, error, disabled, onEdit, onDelete }) {
  if (loading && !hasLoaded) return <div className="empty-state" role="status"><span className="spinner" />Loading inventory…</div>;
  if (!hasLoaded && error) return <div className="empty-state"><DuckIcon /><h3>Inventory unavailable</h3><p>Check your connection, then try refreshing.</p></div>;
  if (!ducks.length) return <div className="empty-state"><DuckIcon /><h3>Your warehouse starts here</h3><p>Add your first duck to begin managing stock.</p></div>;
  return <div className="table-scroll" aria-busy={loading}>
    <table><caption className="sr-only">Active ducks sorted by quantity, lowest first</caption>
      <thead><tr><th scope="col">ID</th><th scope="col">Color</th><th scope="col">Size</th>
        <th scope="col" className="numeric">Price</th><th scope="col" className="numeric" aria-sort="ascending">Quantity <span aria-hidden="true">↑</span></th><th scope="col" className="actions-column">Actions</th></tr></thead>
      <tbody>{ducks.map(duck => <tr key={duck.id}>
        <td className="id-cell">#{duck.id}</td>
        <td><span className="color-label"><span className={`color-dot ${duck.color.toLowerCase()}`} aria-hidden="true" />{duck.color}</span></td>
        <td><span className="size-tag">{duck.size}</span></td>
        <td className="numeric money">{formatPrice(duck.price)} <span className="currency">USD</span></td>
        <td className="numeric stock">{duck.quantity.toLocaleString('en-US')}{duck.quantity === 0 && <span className="zero-stock">Out of stock</span>}</td>
        <td><div className="row-actions"><button disabled={disabled} className="text-button" aria-label={`Edit duck ${duck.id}`} onClick={() => onEdit(duck)}>Edit</button>
          <span className="action-divider" aria-hidden="true" /><button disabled={disabled} className="text-button delete" aria-label={`Delete duck ${duck.id}`} onClick={() => onDelete(duck)}>Delete</button></div></td>
      </tr>)}</tbody>
    </table>
  </div>;
}
