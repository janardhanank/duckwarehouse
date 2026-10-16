import { useCallback, useEffect, useRef, useState } from 'react';
import { ducksApi } from '../api/ducks.js';

export function useWarehouse() {
  const [ducks, setDucks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [hasLoaded, setHasLoaded] = useState(false);
  const activeRequest = useRef(null);

  const reload = useCallback(async () => {
    activeRequest.current?.abort();
    const controller = new AbortController();
    activeRequest.current = controller;
    setLoading(true);
    setError('');
    try {
      const rows = await ducksApi.list(controller.signal);
      if (!controller.signal.aborted) {
        // Keep the backend's authoritative quantity ordering; stable ID breaks ties.
        setDucks(rows);
        setHasLoaded(true);
      }
    } catch (err) {
      if (!controller.signal.aborted) setError(err.message);
    } finally {
      if (!controller.signal.aborted) setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
    return () => activeRequest.current?.abort();
  }, [reload]);

  return { ducks, loading, error, hasLoaded, reload };
}
