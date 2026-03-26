import { useEffect, useState } from 'react'
import type { TriggerConfig, TriggerConfigForm, TriggerFireResult, TriggerType, AmqpRoutingType, TestScenario } from '../types'
import { isHttpTrigger, isCronTrigger, isAmqpTrigger } from '../types'
import { getTriggers, createTrigger, updateTrigger, deleteTrigger, fireTrigger } from '../api/triggers'
import { getTestSuites, getScenarios } from '../api/testsuites'
import './TriggersPage.css'

const emptyForm = (): TriggerConfigForm => ({
  name: '',
  description: '',
  type: 'HTTP',
  httpUrl: '',
  httpMethod: 'POST',
  httpBody: '',
  httpHeaders: undefined,
  enabled: true,
})

type HeaderRow = { key: string; value: string }

const headersToRows = (h?: Record<string, string>): HeaderRow[] =>
  Object.entries(h ?? {}).map(([key, value]) => ({ key, value }))

const rowsToHeaders = (rows: HeaderRow[]): Record<string, string> | undefined => {
  const filled = rows.filter(r => r.key.trim())
  return filled.length > 0 ? Object.fromEntries(filled.map(r => [r.key, r.value])) : undefined
}

interface ScenarioOption {
  id: number
  label: string
}

export default function TriggersPage() {
  const [triggers, setTriggers] = useState<TriggerConfig[]>([])
  const [scenarioOptions, setScenarioOptions] = useState<ScenarioOption[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState<TriggerConfig | null>(null)
  const [form, setForm] = useState<TriggerConfigForm>(emptyForm())
  const [saving, setSaving] = useState(false)

  const [httpHeaderRows, setHttpHeaderRows] = useState<HeaderRow[]>([])

  const [fireResult, setFireResult] = useState<{ trigger: TriggerConfig; result: TriggerFireResult } | null>(null)
  const [firing, setFiring] = useState<number | null>(null)

  useEffect(() => { load() }, [])

  async function load() {
    try {
      setLoading(true)
      const [t, suites] = await Promise.all([getTriggers(), getTestSuites()])
      setTriggers(t)
      const opts: ScenarioOption[] = []
      await Promise.all(suites.map(async s => {
        const scens = await getScenarios(s.id!)
        scens.forEach((sc: TestScenario) => opts.push({ id: sc.id!, label: `${s.name} / ${sc.name}` }))
      }))
      setScenarioOptions(opts)
    } catch {
      setError('Failed to load triggers')
    } finally {
      setLoading(false)
    }
  }

  function openCreate() {
    setEditing(null)
    setForm(emptyForm())
    setHttpHeaderRows([])
    setShowModal(true)
  }

  function openEdit(t: TriggerConfig) {
    setEditing(t)
    const base: TriggerConfigForm = {
      name: t.name,
      description: t.description,
      type: t.type,
      testScenario: t.testScenario,
      enabled: t.enabled,
      lastFiredAt: t.lastFiredAt,
    }
    if (isHttpTrigger(t)) {
      base.httpUrl = t.httpUrl
      base.httpMethod = t.httpMethod
      base.httpBody = t.httpBody
      base.httpHeaders = t.httpHeaders
      setHttpHeaderRows(headersToRows(t.httpHeaders))
    } else if (isCronTrigger(t)) {
      base.cronExpression = t.cronExpression
      setHttpHeaderRows([])
    } else if (isAmqpTrigger(t)) {
      base.amqpAddress = t.amqpAddress
      base.amqpBody = t.amqpBody
      base.amqpProperties = t.amqpProperties
      base.amqpRoutingType = t.amqpRoutingType
      setHttpHeaderRows([])
    }
    setForm(base)
    setShowModal(true)
  }

  async function save() {
    setSaving(true)
    try {
      const payload: TriggerConfigForm = {
        ...form,
        testScenario: form.testScenario?.id ? { id: form.testScenario.id } : undefined,
        httpHeaders: form.type === 'HTTP' ? rowsToHeaders(httpHeaderRows) : undefined,
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

  function triggerDetail(t: TriggerConfig): React.ReactNode {
    if (isHttpTrigger(t) && t.httpUrl) {
      return (
        <code className="trigger-url">
          <span className={`method-badge method-${t.httpMethod}`}>{t.httpMethod}</span>
          {' '}{t.httpUrl}
        </code>
      )
    }
    if (isCronTrigger(t) && t.cronExpression) {
      return <code className="trigger-cron">{t.cronExpression}</code>
    }
    if (isAmqpTrigger(t) && t.amqpAddress) {
      return <code className="trigger-url">→ {t.amqpAddress}</code>
    }
    return null
  }

  function fireResultSummary(result: TriggerFireResult, type: string): string {
    if (result.error) return `Error: ${result.error}`
    if (type === 'AMQP') return `AMQP message sent${result.messageId ? ` (id: ${result.messageId})` : ''}`
    return `HTTP ${result.responseStatus}`
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Triggers</h1>
        <button className="btn btn-primary" onClick={openCreate}>+ New Trigger</button>
      </div>

      <p className="muted triggers-intro">
        Triggers kick off an integration test run. An HTTP trigger makes an outbound call to your application; a Cron trigger fires on a schedule; an AMQP trigger publishes a message to the broker.
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
            {fireResultSummary(fireResult.result, fireResult.trigger.type)}
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
                    {triggerDetail(t)}
                    {t.testScenario && (
                      <span className="trigger-suite">
                        → {scenarioOptions.find(o => o.id === t.testScenario!.id)?.label ?? `Scenario #${t.testScenario.id}`}
                      </span>
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
                      disabled={!!editing}
                    >
                      <option value="HTTP">HTTP</option>
                      <option value="CRON">CRON</option>
                      <option value="AMQP">AMQP</option>
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
                <label>Scenario <span className="label-hint">(optional — shown in Runs modal)</span></label>
                <select
                  value={form.testScenario?.id ?? ''}
                  onChange={e => {
                    const opt = scenarioOptions.find(o => o.id === Number(e.target.value))
                    setForm(f => ({ ...f, testScenario: opt ? { id: opt.id } : undefined }))
                  }}
                >
                  <option value="">— none —</option>
                  {scenarioOptions.map(o => <option key={o.id} value={o.id}>{o.label}</option>)}
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

                  <div className="form-row">
                    <label>Request Headers <span className="label-hint">(optional)</span></label>
                    <div className="header-rows">
                      {httpHeaderRows.map((row, i) => (
                        <div key={i} className="header-row">
                          <input
                            type="text"
                            placeholder="Header name"
                            value={row.key}
                            onChange={e => setHttpHeaderRows(rows => rows.map((r, j) => j === i ? { ...r, key: e.target.value } : r))}
                          />
                          <input
                            type="text"
                            placeholder="Value"
                            value={row.value}
                            onChange={e => setHttpHeaderRows(rows => rows.map((r, j) => j === i ? { ...r, value: e.target.value } : r))}
                          />
                          <button
                            type="button"
                            className="btn-link danger"
                            onClick={() => setHttpHeaderRows(rows => rows.filter((_, j) => j !== i))}
                          >✕</button>
                        </div>
                      ))}
                      <button
                        type="button"
                        className="btn btn-sm"
                        onClick={() => setHttpHeaderRows(rows => [...rows, { key: '', value: '' }])}
                      >+ Add Header</button>
                    </div>
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

              {form.type === 'AMQP' && (
                <>
                  <div className="form-row-inline">
                    <div style={{ flex: 1 }}>
                      <div className="form-row">
                        <label>Address</label>
                        <input
                          type="text"
                          value={form.amqpAddress ?? ''}
                          onChange={e => setForm(f => ({ ...f, amqpAddress: e.target.value }))}
                          placeholder="e.g. orders.commands"
                        />
                      </div>
                    </div>
                    <div>
                      <div className="form-row">
                        <label>Routing type</label>
                        <select
                          value={form.amqpRoutingType ?? 'ANYCAST'}
                          onChange={e => setForm(f => ({ ...f, amqpRoutingType: e.target.value as AmqpRoutingType }))}
                        >
                          <option value="ANYCAST">Anycast (queue)</option>
                          <option value="MULTICAST">Multicast (topic)</option>
                        </select>
                      </div>
                    </div>
                  </div>
                  <div className="form-row">
                    <label>Message Body <span className="label-hint">(optional)</span></label>
                    <textarea
                      value={form.amqpBody ?? ''}
                      onChange={e => setForm(f => ({ ...f, amqpBody: e.target.value }))}
                      rows={4}
                      placeholder={'{\n  "customerId": "cust-1",\n  "action": "place-order"\n}'}
                    />
                  </div>
                </>
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
