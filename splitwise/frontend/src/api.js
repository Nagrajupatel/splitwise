export const money = (n) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(n ?? 0);

async function handle(res) {
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error || `Something went wrong (${res.status})`);
  }
  return res.json();
}

const call = (path, method = 'GET', body) =>
  fetch('/api' + path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  }).then(handle);

export const api = {
  trips: () => call('/trips'),
  createTrip: (b) => call('/trips', 'POST', b),
  trip: (id) => call(`/trips/${id}`),
  addMember: (id, name) => call(`/trips/${id}/members`, 'POST', { name }),
  addExpense: (id, b) => call(`/trips/${id}/expenses`, 'POST', b),
  deleteExpense: (id, expenseId) => call(`/trips/${id}/expenses/${expenseId}`, 'DELETE'),
  settle: (id, b) => call(`/trips/${id}/settlements`, 'POST', b),
  scan: (file) => {
    const form = new FormData();
    form.append('file', file);
    return fetch('/api/ocr/scan', { method: 'POST', body: form }).then(handle);
  },
};
