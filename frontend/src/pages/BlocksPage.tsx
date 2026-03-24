import { useEffect, useState } from 'react'
import type { Block, MockEndpoint } from '../types'
import {
  getBlocks,
  createBlock,
  updateBlock,
  deleteBlock,
  getBlockEndpoints,
  addEndpointToBlock,
  removeEndpointFromBlock,
  startBlock,
  stopBlock,
} from '../api/blocks'
import { getEndpoints } from '../api/endpoints'
import './BlocksPage.css'

const COLORS = ['#667eea', '#cba6f7', '#89b4fa', '#a6e3a1', '#f9e2af', '#fab387', '#f38ba8', '#74c7ec']

const emptyBlock = (): Partial<Block> => ({
  name: '',
  description: '',
  color: '#667eea',
})

export default function BlocksPage() {
  const [blocks, setBlocks] = useState<Block[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Block modal
  const [showBlockModal, setShowBlockModal] = useState(false)
  const [editingBlock, setEditingBlock] = useState<Block | null>(null)
  const [form, setForm] = useState<Partial<Block>>(emptyBlock())
  const [saving, setSaving] = useState(false)

  // Endpoints modal
  const [showEndpointsModal, setShowEndpointsModal] = useState(false)
  const [selectedBlock, setSelectedBlock] = useState<Block | null>(null)
  const [blockEndpoints, setBlockEndpoints] = useState<MockEndpoint[]>([])
  const [allEndpoints, setAllEndpoints] = useState<MockEndpoint[]>([])
  const [loadingEndpoints, setLoadingEndpoints] = useState(false)

  useEffect(() => { load() }, [])

  async function load() {
    try {
      setLoading(true)
      setBlocks(await getBlocks())
    } catch {
      setError('Failed to load blocks')
    } finally {
      setLoading(false)
    }
  }

  function openCreate() {
    setEditingBlock(null)
    setForm(emptyBlock())
    setShowBlockModal(true)
  }

  function openEdit(b: Block) {
    setEditingBlock(b)
    setForm({ ...b })
    setShowBlockModal(true)
  }

  async function saveBlock() {
    setSaving(true)
    try {
      if (editingBlock?.id) {
        await updateBlock(editingBlock.id, form)
      } else {
        await createBlock(form)
      }
      setShowBlockModal(false)
      await load()
    } catch {
      setError('Failed to save block')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(b: Block) {
    if (!confirm(`Delete block "${b.name}"?`)) return
    try {
      await deleteBlock(b.id!)
      await load()
    } catch {
      setError('Failed to delete block')
    }
  }

  async function handleStart(b: Block) {
    try {
      await startBlock(b.id!)
      await load()
    } catch {
      setError('Failed to start block')
    }
  }

  async function handleStop(b: Block) {
    try {
      await stopBlock(b.id!)
      await load()
    } catch {
      setError('Failed to stop block')
    }
  }

  async function openEndpoints(b: Block) {
    setSelectedBlock(b)
    setShowEndpointsModal(true)
    setLoadingEndpoints(true)
    try {
      const [eps, all] = await Promise.all([
        getBlockEndpoints(b.id!),
        getEndpoints(),
      ])
      setBlockEndpoints(eps)
      setAllEndpoints(all)
    } catch {
      setError('Failed to load endpoints')
    } finally {
      setLoadingEndpoints(false)
    }
  }

  async function handleAddEndpoint(endpointId: number) {
    try {
      await addEndpointToBlock(selectedBlock!.id!, endpointId)
      const eps = await getBlockEndpoints(selectedBlock!.id!)
      setBlockEndpoints(eps)
      await load()
    } catch {
      setError('Failed to add endpoint')
    }
  }

  async function handleRemoveEndpoint(endpointId: number) {
    try {
      await removeEndpointFromBlock(selectedBlock!.id!, endpointId)
      const eps = await getBlockEndpoints(selectedBlock!.id!)
      setBlockEndpoints(eps)
      await load()
    } catch {
      setError('Failed to remove endpoint')
    }
  }

  const blockEndpointIds = new Set(blockEndpoints.map(e => e.id))
  const availableEndpoints = allEndpoints.filter(e => !blockEndpointIds.has(e.id))

  return (
    <div className="page">
      <div className="page-header">
        <h1>Blocks</h1>
        <button className="btn btn-primary" onClick={openCreate}>+ New Block</button>
      </div>

      {error && (
        <div className="alert alert-error">
          {error}
          <button onClick={() => setError(null)}>✕</button>
        </div>
      )}

      {loading ? (
        <p className="muted">Loading...</p>
      ) : blocks.length === 0 ? (
        <div className="empty-state">
          <p>No blocks yet.</p>
          <p className="muted">Group endpoints into blocks to use them in test suites.</p>
        </div>
      ) : (
        <div className="blocks-grid">
          {blocks.map(b => (
            <div key={b.id} className="block-card">
              <div className="block-card-accent" style={{ background: b.color ?? '#667eea' }} />
              <div className="block-card-body">
                <div className="block-card-header">
                  <span className="block-name">{b.name}</span>
                  <div className="block-actions">
                    <button className="btn-link" onClick={() => openEndpoints(b)}>Endpoints</button>
                    <button className="btn-link" onClick={() => openEdit(b)}>Edit</button>
                    <button className="btn-link danger" onClick={() => handleDelete(b)}>Delete</button>
                  </div>
                </div>

                {b.description && <p className="block-desc">{b.description}</p>}

                <div className="block-meta">
                  <span className="block-stat">
                    <span className="block-stat-value">{b.endpointCount ?? 0}</span> endpoints
                  </span>
                  {(b.endpointCount ?? 0) > 0 && (
                    <span className="block-stat">
                      <span className="block-stat-value">{b.activeEndpointCount ?? 0}</span> active
                    </span>
                  )}
                </div>

                <div className="block-card-footer">
                  <button className="btn btn-start" onClick={() => handleStart(b)}>▶ Start</button>
                  <button className="btn btn-stop" onClick={() => handleStop(b)}>■ Stop</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Block modal */}
      {showBlockModal && (
        <div className="modal-overlay" onClick={() => setShowBlockModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingBlock ? 'Edit Block' : 'New Block'}</h2>
              <button className="modal-close" onClick={() => setShowBlockModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-row">
                <label>Name</label>
                <input
                  type="text"
                  value={form.name ?? ''}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="e.g. Order Flow"
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
              <div className="form-row">
                <label>Color</label>
                <div className="color-picker">
                  {COLORS.map(c => (
                    <button
                      key={c}
                      className={`color-swatch ${form.color === c ? 'selected' : ''}`}
                      style={{ background: c }}
                      onClick={() => setForm(f => ({ ...f, color: c }))}
                    />
                  ))}
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn" onClick={() => setShowBlockModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={saveBlock} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Endpoints modal */}
      {showEndpointsModal && selectedBlock && (
        <div className="modal-overlay" onClick={() => setShowEndpointsModal(false)}>
          <div className="modal modal-wide" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Endpoints — {selectedBlock.name}</h2>
              <button className="modal-close" onClick={() => setShowEndpointsModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              {loadingEndpoints ? (
                <p className="muted">Loading...</p>
              ) : (
                <>
                  <div className="endpoints-section">
                    <h3>In this block</h3>
                    {blockEndpoints.length === 0 ? (
                      <p className="muted">No endpoints added yet.</p>
                    ) : (
                      <ul className="endpoint-list">
                        {blockEndpoints.map(ep => (
                          <li key={ep.id} className="endpoint-list-item">
                            <span className={`method-badge method-${ep.httpMethod}`}>{ep.httpMethod}</span>
                            <span className="ep-path">{ep.httpPath}</span>
                            <span className="ep-label">{ep.name}</span>
                            <button className="btn-link danger" onClick={() => handleRemoveEndpoint(ep.id!)}>Remove</button>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>

                  {availableEndpoints.length > 0 && (
                    <div className="endpoints-section">
                      <h3>Add endpoint</h3>
                      <ul className="endpoint-list">
                        {availableEndpoints.map(ep => (
                          <li key={ep.id} className="endpoint-list-item">
                            <span className={`method-badge method-${ep.httpMethod}`}>{ep.httpMethod}</span>
                            <span className="ep-path">{ep.httpPath}</span>
                            <span className="ep-label">{ep.name}</span>
                            <button className="btn-link" onClick={() => handleAddEndpoint(ep.id!)}>+ Add</button>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {availableEndpoints.length === 0 && blockEndpoints.length > 0 && (
                    <p className="muted" style={{ marginTop: '1rem' }}>All endpoints are already in this block.</p>
                  )}
                </>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn btn-primary" onClick={() => setShowEndpointsModal(false)}>Done</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
