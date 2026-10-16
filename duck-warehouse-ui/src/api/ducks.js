export class ApiError extends Error {
  constructor(message, fields = {}, status = 0) {
    super(message);
    this.name = 'ApiError';
    this.fields = fields;
    this.status = status;
  }
}

async function request(path, { method = 'GET', body, signal } = {}) {
  let response;
  try {
    response = await fetch(`/api/ducks${path}`, {
      method, signal,
      headers: body ? { 'Content-Type': 'application/json', Accept: 'application/json' } : { Accept: 'application/json' },
      ...(body ? { body: JSON.stringify(body) } : {}),
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError(method === 'GET'
      ? 'Could not connect to the warehouse. Check that the backend is running.'
      : 'The connection was interrupted. Refresh the inventory to check whether the change completed before submitting again.');
  }
  if (response.status === 204) return null;
  let data;
  try { data = await response.json(); }
  catch {
    throw new ApiError(method === 'GET'
      ? 'The server returned an unexpected response. Check that the backend is running.'
      : 'The server returned an unexpected response. Refresh the inventory to verify the change before submitting again.', {}, response.status);
  }
  if (!response.ok) throw new ApiError(data.message || 'The request could not be completed.', data.fields || {}, response.status);
  return data;
}

export const ducksApi = {
  list: signal => request('', { signal }),
  add: body => request('', { method: 'POST', body }),
  edit: (id, body) => request(`/${id}`, { method: 'PUT', body }),
  remove: id => request(`/${id}`, { method: 'DELETE' }),
};
