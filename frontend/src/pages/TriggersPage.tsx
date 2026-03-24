import { useEffect, useState } from 'react'
import type { TriggerConfig, TriggerFireResult, TriggerType } from '../types'
import type { TestSuite } from '../types'
import { getTriggers, createTrigger, updateTrigger, deleteTrigger, fireTrigger } from '../api/triggers'
import { getTestSuites } from '../api/testsuites'
import './TriggersPage.css'

const emptyForm = (): Partial<TriggerConfig> => ({
  name: '',
  description: '',
  type: 'HTTP',
  httpUrl: '',
  httpMethod: 'POST',
  httpBody: '',
  enabled: true,
})

export default function TriggersPage() {
  const [triggers, setTriggers] = useState<TriggerConfig[]>([])
  const [suites, setSuites] = useState<TestSuite[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState<TriggerConfig | null>(null)
  const [form, setForm] = useState<Partial<TriggerConfig>>(emptyForm())
  const [saving, setSaving] = useState(false)

  const [fireResult, setFireResult] = useState<{ trigger: TriggerConfig; result: TriggerFireResult } | null>(null)
  const [firing, setFiring] = useState<number | null>(null)

  useEffect(() => { load() }, [])

  async function load() {
    try {
      setLoading(true)
      const [t, s] = await Promise.all([getTriggers(), getTestSuites()])
      setTriggers(t)
      setSuites(s)
    } catch {
      setError('Failed to load triggers')
    } finally {
      setLoading(false)
    }
  }

  function openCreate() {
    setEditing(null)
    setForm(emptyForm())
    setShowModal(true)
  }

  function openEdit(t: TriggerConfig) {
    setEditing(t)
    setForm({ ...t, testSuite: t.testSuite })
    setShowModal(true)
  }

  async function save() {
    setSaving(true)
    try {
      const payload = {
        ...form,
        testSuite: form.testSuite?.id ? { id: form.testSuite.id } : undefined,
      }
      if (editing?.id) {
        await updateTrigger(editing.id, payload)
      } else {
        await createTrigger(payload)
      }
      setShowModal(false)
      await load()
    } catch {
      setError('Failed to save trigger')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(t: TriggerConfig) {
    if (!confirm(`Delete trigger "${t.name}"?`)) return
    try {
      await deleteTrigger(t.id!)
      await load()
    } catch {
      setError('Failed to delete trigger')
    }
  }

  async function handleFire(t: TriggerConfig) {
    setFiring(t.id!)
    setFireResult(null)
    try {
      const result = await fireTrigger(t.id!)
      setFireResult({ trigger: t, result })
      await load()
    } catch (e: unknown) {
      setError('Failed to fire trigger: ' + (e instanceof Error ? e.message : String(e)))
    } finally {
      setFiring(null)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Triggers</h1>
        <button className="btn btn-primary" onClick={openCreate}>+ New Trigger</button>
      </div>

      <p className="muted triggers-intro">
        Triggers kick off an integration test run. An HTTP trigger makes an outbound call to your application; a Cron trigger fires on a schedule.
      </p>

      {error && (
        <div className="alert alert-error">
          {error}
          <button onClick={() => setError(null)}>✕</button>
        </div>
      )}

      {fireResult && (
        <div className={`alert ${fireResult.result.error ? 'alert-error' : 'alert-success'}`}>
          <div>
            <strong>{fireResult.trigger.name}</strong> fired —{' '}
            {fireResult.result.error
              ? `Error: ${fireResult.result.error}`
              : `HTTP ${fireResult.result.responseStatus}`}
          </div>
          <button onClick={() => setFireResult(null)}>✕</button>
        </div>
      )}

      {loading ? (
        <p className="muted">Loading...</p>
      ) : triggers.length === 0 ? (
        <div className="empty-state">
          <p>No triggers yet.</p>
          <p className="muted">Create a trigger to automatically start integration test runs.</p>
        </div>
      ) : (
        <div className="triggers-list">
          {triggers.map(t => (
            <div key={t.id} className={`trigger-card ${t.enabled ? '' : 'trigger-disabled'}`}>
              <div className="trigger-card-left">
                <span className={`trigger-type-badge trigger-type-${t.type.toLowerCase()}`}>{t.type}</span>
                <div className="trigger-info">
                  <span className="trigger-name">{t.name}</span>
                  {t.description && <span className="trigger-desc">{t.description}</span>}
                  <div className="trigger-detail">
                    {t.type === 'HTTP' && t.httpUrl && (
                      <code className="trigger-url">
                        <span className={`method-badge method-${t.httpMethod}`}>{t.httpMethod}</span>
                        {' '}{t.httpUrl}
                      </code>
                    )}
                    {t.type === 'CRON' && t.cronExpression && (
                      <code className="trigger-cron">{t.cronExpression}</code>
                    )}
                    {t.testSuite && (
                      <span className="trigger-suite">→ {t.testSuite.name ?? `Suite #${t.testSuite.id}`}</span>
                    )}
                  </div>
                  {t.lastFiredAt && (
                    <span className="trigger-last-fired">
                      Last fired: {new Date(t.lastFiredAt).toLocaleString('nl-NL')}
                    </span>
                  )}
                </div>
              </div>
              <div className="trigger-card-right">
                <button
                  className="btn btn-fire"
                  onClick={() => handleFire(t)}
                  disabled={firing === t.id || !t.enabled}
                >
                  {firing === t.id ? '...' : '▶ Fire'}
                </button>
                <button className="btn-link" onClick={() => openEdit(t)}>Edit</button>
                <button className="btn-link danger" onClick={() => handleDelete(t)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal modal-wide" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editing ? 'Edit Trigger' : 'New Trigger'}</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-row-inline">
                <div style={{ flex: 1 }}>
                  <div className="form-row">
                    <label>Name</label>
                    <input
                      type="text"
                      value={form.name ?? ''}
                      onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                      placeholder="e.g. Fire Order"
                    />
                  </div>
                </div>
                <div>
                  <div className="form-row">
                    <label>Type</label>
                    <select
                      value={form.type ?? 'HTTP'}
                      onChange={e => setForm(f => ({ ...f, type: e.target.value as TriggerType }))}
                    >
                      <option value="HTTP">HTTP</option>
                      <option value="CRON">CRON</option>
                    </select>
                  </div>
                </div>
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
                <label>Test Suite <span className="label-hint">(run will be started when trigger fires)</span></label>
                <select
                  value={form.testSuite?.id ?? ''}
                  onChange={e => {
                    const suite = suites.find(s => s.id === Number(e.target.value))
                    setForm(f => ({ ...f, testSuite: suite ? { id: suite.id!, name: suite.name } : undefined }))
                  }}
                >
                  <option value="">— none —</option>
                  {suites.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>

              {form.type === 'HTTP' && (
                <>
                  <div className="form-row-inline">
                    <div>
                      <div className="form-row">
                        <label>Method</label>
                        <select
                          value={form.httpMethod ?? 'POST'}
                          onChange={e => setForm(f => ({ ...f, httpMethod: e.target.value }))}
                        >
                          {['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].map(m => (
                            <option key={m}>{m}</option>
                          ))}
                        </select>
                      </div>
                    </div>
                    <div style={{ flex: 1 }}>
                      <div className="form-row">
                        <label>URL</label>
                        <input
                          type="text"
                          value={form.httpUrl ?? ''}
                          onChange={e => setForm(f => ({ ...f, httpUrl: e.target.value }))}
                          placeholder="http://localhost:3000/orders"
                        />
                      </div>
                    </div>
                  </div>

                  <div className="form-row">
                    <label>Request Body <span className="label-hint">(optional)</span></label>
                    <textarea
                      value={form.httpBody ?? ''}
                      onChange={e => setForm(f => ({ ...f, httpBody: e.target.value }))}
                      rows={4}
                      placeholder={'{\n  "customerId": "cust-1",\n  "items": [{"sku": "BOOK-01", "qty": 2}],\n  "totalAmount": 29.99\n}'}
                    />
                  </div>
                </>
              )}

              {form.type === 'CRON' && (
                <div className="form-row">
                  <label>Cron Expression <span className="label-hint">(Quartz format, e.g. 0 * * * * ?)</span></label>
                  <input
                    type="text"
                    value={form.cronExpression ?? ''}
                    onChange={e => setForm(f => ({ ...f, cronExpression: e.target.value }))}
                    placeholder="0 0/5 * * * ?"
                  />
                </div>
              )}

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
              <button className="btn" onClick={() => setShowModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={save} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
