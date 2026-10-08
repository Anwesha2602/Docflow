// In development, Vite proxies /api to localhost:8080. Set VITE_API_BASE_URL
// when the frontend is served separately from the API in another environment.
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, options)
  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try { const body = await response.json(); message = body.error || body.message || message } catch { /* response may be plain text */ }
    throw new Error(message)
  }
  if (options.raw) return response
  return response.json()
}

export const api = {
  documents: () => request('/api/documents'),
  document: id => request(`/api/documents/${id}`),
  uploadDocument: file => { const data = new FormData(); data.append('file', file); return request('/api/documents/upload', { method: 'POST', body: data }) },
  retryDocument: id => request(`/api/documents/${id}/retry`, { method: 'POST' }),
  history: id => request(`/api/documents/${id}/history`),
  download: id => request(`/api/documents/${id}/download`, { raw: true }),
  // BatchController binds MultipartFile[] to @RequestParam("file"). Use the same key for every file.
  uploadBatch: files => { const data = new FormData(); files.forEach(file => data.append('file', file)); return request('/api/batches/upload', { method: 'POST', body: data }) },
  batch: id => request(`/api/batches/${id}`),
}

export const apiBase = API_BASE
