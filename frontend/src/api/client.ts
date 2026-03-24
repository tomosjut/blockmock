const JSON_HEADERS = { 'Content-Type': 'application/json' }

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(path, options)
  if (!response.ok) {
    const text = await response.text().catch(() => '')
    throw new Error(`API ${response.status}: ${text || response.statusText}`)
  }
  if (response.status === 204) return undefined as T
  return response.json()
}

export function postJson<T>(path: string, body: unknown): Promise<T> {
  return apiFetch(path, { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(body) })
}

export function putJson<T>(path: string, body: unknown): Promise<T> {
  return apiFetch(path, { method: 'PUT', headers: JSON_HEADERS, body: JSON.stringify(body) })
}

export function deleteReq(path: string): Promise<void> {
  return apiFetch(path, { method: 'DELETE' })
}
