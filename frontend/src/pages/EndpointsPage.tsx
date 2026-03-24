import React, { useEffect, useState } from 'react'
import type { MockEndpoint, MockResponse, HttpMethod } from '../types'
import {
  getEndpoints,
  createEndpoint,
  updateEndpoint,
  deleteEndpoint,
  toggleEndpoint,
  addResponse,
  deleteResponse,
} from '../api/endpoints'
import './EndpointsPage.css'

const HTTP_METHODS: HttpMethod[] = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']

const emptyEndpoint = (): Partial<MockEndpoint> => ({
  name: '',
  description: '',
  protocol: 'HTTP',
  pattern: 'REQUEST_REPLY',
  enabled: true,
  httpMethod: 'GET',
  httpPath: '',
  httpPathRegex: false,
})

const emptyResponse = (): Partial<MockResponse> => ({
  name: '',
  priority: 0,
  responseStatusCode: 200,
  responseBody: '',
  responseDelayMs: 0,
})

export default function EndpointsPage() {
  const [endpoints, setEndpoints] = useState<MockEndpoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Endpoint modal
  const [showEndpointModal, setShowEndpointModal] = useState(false)
  const [editingEndpoint, setEditingEndpoint] = useState<MockEndpoint | null>(null)
  const [form, setForm] = useState<Partial<MockEndpoint>>(emptyEndpoint())
  const [saving, setSaving] = useState(false)

  // Response modal
  const [showResponseModal, setShowResponseModal] = useState(false)
  const [selectedEndpoint, setSelectedEndpoint] = useState<MockEndpoint | null>(null)
  const [responseForm, setResponseForm] = useState<Partial<MockResponse>>(emptyResponse())
  const [savingResponse, setSavingResponse] = useState(false)

  // Expanded rows
  const [expanded, setExpanded] = useState<Set<number>>(new Set())

  useEffect(() => { load() }, [])

  async function load() {
    try {
      setLoading(true)
      setEndpoints(await getEndpoints())
    } catch {
      setError('Failed to load endpoints')
    } finally {
      setLoading(false)
    }
  }

  function openCreate() {
    setEditingEndpoint(null)
    setForm(emptyEndpoint())
    setShowEndpointModal(true)
  }

  function openEdit(ep: MockEndpoint) {
    setEditingEndpoint(ep)
    setForm({ ...ep })
    setShowEndpointModal(true)
  }

  async function saveEndpoint() {
    setSaving(true)
    try {
      if (editingEndpoint?.id) {
        await updateEndpoint(editingEndpoint.id, form)
      } else {
        await createEndpoint(form)
      }
      setShowEndpointModal(false)
      await load()
    } catch {
      setError('Failed to save endpoint')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(ep: MockEndpoint) {
    if (!confirm(`Delete endpoint "${ep.name}"?`)) return
    try {
      await deleteEndpoint(ep.id!)
      await load()
    } catch {
      setError('Failed to delete endpoint')
    }
  }

  async function handleToggle(ep: MockEndpoint) {
    try {
      await toggleEndpoint(ep.id!)
      await load()
    } catch {
      setError('Failed to toggle endpoint')
    }
  }

  function toggleExpand(id: number) {
    setExpanded(prev => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  }

  function openAddResponse(ep: MockEndpoint) {
    setSelectedEndpoint(ep)
    setResponseForm(emptyResponse())
    setShowResponseModal(true)
  }

  async function saveResponse() {
    if (!selectedEndpoint?.id) return
    setSavingResponse(true)
    try {
      await addResponse(selectedEndpoint.id, responseForm)
      setShowResponseModal(false)
      await load()
    } catch {
      setError('Failed to save response')
    } finally {
      setSavingResponse(false)
    }
  }

  async function handleDeleteResponse(responseId: number) {
    if (!confirm('Delete this response?')) return
    try {
      await deleteResponse(responseId)
      await load()
    } catch {
      setError('Failed to delete response')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Endpoints</h1>
        <button className="btn btn-primary" onClick={openCreate}>+ New Endpoint</button>
      </div>

      {error && <div className="alert alert-error">{error}<button onClick={() => setError(null)}>✕</button></div>}

      {loading ? (
        <p className="muted">Loading...</p>
      ) : endpoints.length === 0 ? (
        <div className="empty-state">
          <p>No endpoints yet.</p>
          <p className="muted">Create an endpoint to start mocking HTTP calls.</p>
        </div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: 32 }}></th>
              <th>Name</th>
              <th>Method</th>
              <th>Path</th>
              <th>Responses</th>
              <th>Requests</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {endpoints.map(ep => (
              <React.Fragment key={ep.id}>
                <tr className={ep.enabled ? '' : 'row-disabled'}>
                  <td>
                    <button
                      className="btn-icon"
                      title={expanded.has(ep.id!) ? 'Collapse' : 'Expand'}
                      onClick={() => toggleExpand(ep.id!)}
                    >
                      {expanded.has(ep.id!) ? '▾' : '▸'}
                    </button>
                  </td>
                  <td>
                    <span className="ep-name">{ep.name}</span>
                    {ep.description && <span className="ep-desc">{ep.description}</span>}
                  </td>
                  <td><span className={`method-badge method-${ep.httpMethod}`}>{ep.httpMethod}</span></td>
                  <td><code className="path">{ep.httpPath}{ep.httpPathRegex && <span className="regex-tag">regex</span>}</code></td>
                  <td>{ep.responses?.length ?? 0}</td>
                  <td>{ep.totalRequests ?? 0}</td>
                  <td>
                    <button
                      className={`status-toggle ${ep.enabled ? 'enabled' : 'disabled'}`}
                      onClick={() => handleToggle(ep)}
                      title={ep.enabled ? 'Enabled — click to disable' : 'Disabled — click to enable'}
                    >
                      {ep.enabled ? 'ON' : 'OFF'}
                    </button>
                  </td>
                  <td className="actions">
                    <button className="btn-link" onClick={() => openAddResponse(ep)}>+ Response</button>
                    <button className="btn-link" onClick={() => openEdit(ep)}>Edit</button>
                    <button className="btn-link danger" onClick={() => handleDelete(ep)}>Delete</button>
                  </td>
                </tr>
                {expanded.has(ep.id!) && (
                  <tr className="responses-row">
                    <td colSpan={8}>
                      <div className="responses-panel">
                        {ep.responses?.length === 0 ? (
                          <p className="muted">No responses configured. Add one to start returning mock data.</p>
                        ) : (
                          <table className="responses-table">
                            <thead>
                              <tr>
                                <th>Name</th>
                                <th>Status</th>
                                <th>Priority</th>
                                <th>Delay</th>
                                <th>Body (preview)</th>
                                <th></th>
                              </tr>
                            </thead>
                            <tbody>
                              {ep.responses?.sort((a, b) => b.priority - a.priority).map(r => (
                                <tr key={r.id}>
                                  <td>{r.name}</td>
                                  <td><span className={`status-code s${Math.floor(r.responseStatusCode / 100)}xx`}>{r.responseStatusCode}</span></td>
                                  <td>{r.priority}</td>
                                  <td>{r.responseDelayMs ? `${r.responseDelayMs}ms` : '—'}</td>
                                  <td><code className="body-preview">{r.responseBody?.slice(0, 60) || '—'}</code></td>
                                  <td>
                                    <button className="btn-link danger" onClick={() => handleDeleteResponse(r.id!)}>Delete</button>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        )}
                      </div>
                    </td>
                  </tr>
                )}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      )}

      {/* Endpoint modal */}
      {showEndpointModal && (
        <div className="modal-overlay" onClick={() => setShowEndpointModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingEndpoint ? 'Edit Endpoint' : 'New Endpoint'}</h2>
              <button className="modal-close" onClick={() => setShowEndpointModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-row">
                <label>Name</label>
                <input
                  type="text"
                  value={form.name ?? ''}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="e.g. Payment Charge"
                />
              </div>
              <div className="form-row">
                <label>Description</label>
                <input
                  type="text"
                  value={form.description ?? ''}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  placeholder="Optional"
                />
              </div>
              <div className="form-row form-row-inline">
                <div>
                  <label>Method</label>
                  <select
                    value={form.httpMethod ?? 'GET'}
                    onChange={e => setForm(f => ({ ...f, httpMethod: e.target.value as HttpMethod }))}
                  >
                    {HTTP_METHODS.map(m => <option key={m}>{m}</option>)}
                  </select>
                </div>
                <div style={{ flex: 1 }}>
                  <label>Path</label>
                  <input
                    type="text"
                    value={form.httpPath ?? ''}
                    onChange={e => setForm(f => ({ ...f, httpPath: e.target.value }))}
                    placeholder="/api/payment/charge"
                  />
                </div>
              </div>
              <div className="form-row form-row-checkbox">
                <label>
                  <input
                    type="checkbox"
                    checked={form.httpPathRegex ?? false}
                    onChange={e => setForm(f => ({ ...f, httpPathRegex: e.target.checked }))}
                  />
                  Path is regex
                </label>
              </div>
              <div className="form-row form-row-checkbox">
                <label>
                  <input
                    type="checkbox"
                    checked={form.enabled ?? true}
                    onChange={e => setForm(f => ({ ...f, enabled: e.target.checked }))}
                  />
                  Enabled
                </label>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn" onClick={() => setShowEndpointModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={saveEndpoint} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Response modal */}
      {showResponseModal && (
        <div className="modal-overlay" onClick={() => setShowResponseModal(false)}>
          <div className="modal modal-wide" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Add Response — {selectedEndpoint?.name}</h2>
              <button className="modal-close" onClick={() => setShowResponseModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-row form-row-inline">
                <div style={{ flex: 1 }}>
                  <label>Name</label>
                  <input
                    type="text"
                    value={responseForm.name ?? ''}
                    onChange={e => setResponseForm(f => ({ ...f, name: e.target.value }))}
                    placeholder="e.g. Success"
                  />
                </div>
                <div>
                  <label>Status Code</label>
                  <input
                    type="number"
                    value={responseForm.responseStatusCode ?? 200}
                    onChange={e => setResponseForm(f => ({ ...f, responseStatusCode: Number(e.target.value) }))}
                    style={{ width: 80 }}
                  />
                </div>
                <div>
                  <label>Priority</label>
                  <input
                    type="number"
                    value={responseForm.priority ?? 0}
                    onChange={e => setResponseForm(f => ({ ...f, priority: Number(e.target.value) }))}
                    style={{ width: 70 }}
                    title="Higher = matched first"
                  />
                </div>
                <div>
                  <label>Delay (ms)</label>
                  <input
                    type="number"
                    value={responseForm.responseDelayMs ?? 0}
                    onChange={e => setResponseForm(f => ({ ...f, responseDelayMs: Number(e.target.value) }))}
                    style={{ width: 80 }}
                  />
                </div>
              </div>
              <div className="form-row">
                <label>Response Body</label>
                <textarea
                  value={responseForm.responseBody ?? ''}
                  onChange={e => setResponseForm(f => ({ ...f, responseBody: e.target.value }))}
                  rows={6}
                  placeholder={'{\n  "transactionId": "tx-123",\n  "status": "approved"\n}'}
                />
              </div>
              <div className="form-row">
                <label>Match Body Contains <span className="label-hint">(optional — only match requests containing this text)</span></label>
                <input
                  type="text"
                  value={responseForm.matchBody ?? ''}
                  onChange={e => setResponseForm(f => ({ ...f, matchBody: e.target.value }))}
                  placeholder='e.g. "customerId":"cust-1"'
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn" onClick={() => setShowResponseModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={saveResponse} disabled={savingResponse}>
                {savingResponse ? 'Saving...' : 'Add Response'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
