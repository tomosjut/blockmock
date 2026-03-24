import React, { useEffect, useRef, useState } from 'react'
import type { TestSuite, TestExpectation, TestRun, TestExpectationResult, MockEndpoint, Block, TriggerConfig } from '../types'
import {
  getTestSuites, createTestSuite, updateTestSuite, deleteTestSuite,
  startRun, getRuns, completeRun, cancelRun, getJUnitXml,
  exportSuite, importSuite, clearRuns,
} from '../api/testsuites'
import { getEndpoints } from '../api/endpoints'
import { getBlocks } from '../api/blocks'
import { getTriggers, fireTrigger } from '../api/triggers'
import './TestSuitesPage.css'

const COLORS = ['#667eea', '#cba6f7', '#89b4fa', '#a6e3a1', '#f9e2af', '#fab387', '#f38ba8', '#74c7ec']

const emptyExpectation = (): TestExpectation => ({
  name: '',
  mockEndpoint: undefined,
  minCallCount: 1,
  maxCallCount: undefined,
  requiredBodyContains: '',
  expectationOrder: undefined,
})

const emptyForm = (): Partial<TestSuite> => ({
  name: '',
  description: '',
  color: '#667eea',
  blocks: [],
  expectations: [],
})

export default function TestSuitesPage() {
  const [suites, setSuites] = useState<TestSuite[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [allEndpoints, setAllEndpoints] = useState<MockEndpoint[]>([])
  const [allBlocks, setAllBlocks] = useState<Block[]>([])

  // Suite modal
  const [showSuiteModal, setShowSuiteModal] = useState(false)
  const [editingSuite, setEditingSuite] = useState<TestSuite | null>(null)
  const [form, setForm] = useState<Partial<TestSuite>>(emptyForm())
  const [saving, setSaving] = useState(false)

  // Runs modal
  const [showRunsModal, setShowRunsModal] = useState(false)
  const [selectedSuite, setSelectedSuite] = useState<TestSuite | null>(null)
  const [runs, setRuns] = useState<TestRun[]>([])
  const [loadingRuns, setLoadingRuns] = useState(false)

  // Triggers for runs modal
  const [suiteTriggers, setSuiteTriggers] = useState<TriggerConfig[]>([])
  const [firingTrigger, setFiringTrigger] = useState<number | null>(null)

  // Import/export
  const [importResult, setImportResult] = useState<string | null>(null)
  const importInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => { load() }, [])

  async function load() {
    try {
      setLoading(true)
      const [s, eps, blocks] = await Promise.all([getTestSuites(), getEndpoints(), getBlocks()])
      setSuites(s)
      setAllEndpoints(eps)
      setAllBlocks(blocks)
    } catch {
      setError('Failed to load test suites')
    } finally {
      setLoading(false)
    }
  }

  function openCreate() {
    setEditingSuite(null)
    setForm(emptyForm())
    setShowSuiteModal(true)
  }

  function openEdit(s: TestSuite) {
    setEditingSuite(s)
    setForm({
      name: s.name,
      description: s.description ?? '',
      color: s.color ?? '#667eea',
      blocks: s.blocks ? [...s.blocks] : [],
      expectations: s.expectations ? s.expectations.map(e => ({ ...e })) : [],
    })
    setShowSuiteModal(true)
  }

  async function saveSuite() {
    setSaving(true)
    try {
      const payload = {
        ...form,
        expectations: (form.expectations ?? []).map(e => ({
          ...e,
          mockEndpoint: e.mockEndpoint?.id ? { id: e.mockEndpoint.id } : undefined,
          requiredBodyContains: e.requiredBodyContains || undefined,
          maxCallCount: e.maxCallCount || undefined,
          expectationOrder: e.expectationOrder || undefined,
        })),
        blocks: (form.blocks ?? []).map(b => ({ id: b.id })),
      }
      if (editingSuite?.id) {
        await updateTestSuite(editingSuite.id, payload)
      } else {
        await createTestSuite(payload)
      }
      setShowSuiteModal(false)
      await load()
    } catch {
      setError('Failed to save test suite')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(s: TestSuite) {
    if (!confirm(`Delete test suite "${s.name}"?`)) return
    try {
      await deleteTestSuite(s.id!)
      await load()
    } catch {
      setError('Failed to delete test suite')
    }
  }

  async function handleStartRun(s: TestSuite) {
    try {
      await startRun(s.id!)
      await openRuns(s)
    } catch (e: unknown) {
      setError('Failed to start run: ' + (e instanceof Error ? e.message : String(e)))
    }
  }

  async function openRuns(s: TestSuite) {
    setSelectedSuite(s)
    setShowRunsModal(true)
    setLoadingRuns(true)
    try {
      const [runsData, allTriggers] = await Promise.all([getRuns(s.id!), getTriggers()])
      setRuns(runsData)
      setSuiteTriggers(allTriggers.filter(t => t.testSuite?.id === s.id && t.enabled))
    } catch {
      setError('Failed to load runs')
    } finally {
      setLoadingRuns(false)
    }
  }

  async function handleCompleteRun(run: TestRun) {
    try {
      const updated = await completeRun(selectedSuite!.id!, run.id!)
      setRuns(prev => prev.map(r => r.id === updated.id ? updated : r))
      await load()
    } catch (e: unknown) {
      setError('Failed to complete run: ' + (e instanceof Error ? e.message : String(e)))
    }
  }

  async function handleFireTrigger(trigger: TriggerConfig) {
    setFiringTrigger(trigger.id!)
    try {
      await fireTrigger(trigger.id!)
    } catch (e: unknown) {
      setError('Failed to fire trigger: ' + (e instanceof Error ? e.message : String(e)))
    } finally {
      setFiringTrigger(null)
    }
  }

  async function handleClearRuns() {
    if (!confirm('Delete all completed, failed and cancelled runs?')) return
    try {
      await clearRuns(selectedSuite!.id!)
      setRuns(prev => prev.filter(r => r.status === 'RUNNING'))
    } catch {
      setError('Failed to clear runs')
    }
  }

  async function handleCancelRun(run: TestRun) {
    if (!confirm('Cancel this run?')) return
    try {
      await cancelRun(selectedSuite!.id!, run.id!)
      setRuns(prev => prev.filter(r => r.id !== run.id))
    } catch {
      setError('Failed to cancel run')
    }
  }

  async function handleDownloadJUnit(run: TestRun) {
    try {
      const xml = await getJUnitXml(selectedSuite!.id!, run.id!)
      const blob = new Blob([xml as unknown as string], { type: 'application/xml' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `test-results-${run.id}.xml`
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setError('Failed to download JUnit XML')
    }
  }

  async function handleExport(s: TestSuite) {
    try {
      const { blob, filename } = await exportSuite(s.id!)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setError('Failed to export suite')
    }
  }

  async function handleImport(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    e.target.value = ''
    try {
      const text = await file.text()
      const data = JSON.parse(text)
      const result = await importSuite(data)
      const parts = [`Suite "${result.suiteName}" ${result.suiteCreated ? 'created' : 'updated'}`]
      if (result.endpointsCreated) parts.push(`${result.endpointsCreated} endpoint(s) created`)
      if (result.endpointsLinked) parts.push(`${result.endpointsLinked} linked`)
      if (result.blocksCreated) parts.push(`${result.blocksCreated} block(s) created`)
      if (result.triggersCreated) parts.push(`${result.triggersCreated} trigger(s) created`)
      if (result.triggersSkipped) parts.push(`${result.triggersSkipped} trigger(s) skipped`)
      setImportResult(parts.join(' · '))
      await load()
    } catch (err: unknown) {
      setError('Import failed: ' + (err instanceof Error ? err.message : String(err)))
    }
  }

  function addExpectation() {
    setForm(f => ({ ...f, expectations: [...(f.expectations ?? []), emptyExpectation()] }))
  }

  function updateExpectation(i: number, patch: Partial<TestExpectation>) {
    setForm(f => ({
      ...f,
      expectations: (f.expectations ?? []).map((e, idx) => idx === i ? { ...e, ...patch } : e),
    }))
  }

  function removeExpectation(i: number) {
    setForm(f => ({ ...f, expectations: (f.expectations ?? []).filter((_, idx) => idx !== i) }))
  }

  function toggleBlock(block: Block) {
    setForm(f => {
      const current = f.blocks ?? []
      const exists = current.some(b => b.id === block.id)
      return {
        ...f,
        blocks: exists ? current.filter(b => b.id !== block.id) : [...current, { id: block.id!, name: block.name }],
      }
    })
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Test Suites</h1>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="btn" onClick={() => importInputRef.current?.click()}>↑ Import</button>
          <button className="btn btn-primary" onClick={openCreate}>+ New Suite</button>
        </div>
      </div>
      <input ref={importInputRef} type="file" accept=".json" style={{ display: 'none' }} onChange={handleImport} />

      {error && (
        <div className="alert alert-error">
          {error}
          <button onClick={() => setError(null)}>✕</button>
        </div>
      )}

      {importResult && (
        <div className="alert alert-success">
          {importResult}
          <button onClick={() => setImportResult(null)}>✕</button>
        </div>
      )}

      {loading ? (
        <p className="muted">Loading...</p>
      ) : suites.length === 0 ? (
        <div className="empty-state">
          <p>No test suites yet.</p>
          <p className="muted">Create a suite to define expectations and run integration tests.</p>
        </div>
      ) : (
        <div className="suites-grid">
          {suites.map(s => (
            <div key={s.id} className="suite-card">
              <div className="suite-card-accent" style={{ background: s.color ?? '#667eea' }} />
              <div className="suite-card-body">
                <div className="suite-card-header">
                  <span className="suite-name">{s.name}</span>
                  <div className="suite-actions">
                    <button className="btn-link" onClick={() => openRuns(s)}>Runs</button>
                    <button className="btn-link" onClick={() => openEdit(s)}>Edit</button>
                    <button className="btn-link" onClick={() => handleExport(s)}>↓ Export</button>
                    <button className="btn-link danger" onClick={() => handleDelete(s)}>Delete</button>
                  </div>
                </div>

                {s.description && <p className="suite-desc">{s.description}</p>}

                <div className="suite-meta">
                  <span className="suite-stat">
                    <span className="suite-stat-value">{s.expectations?.length ?? 0}</span> expectations
                  </span>
                  {(s.blocks?.length ?? 0) > 0 && (
                    <span className="suite-stat">
                      <span className="suite-stat-value">{s.blocks!.length}</span> blocks
                    </span>
                  )}
                </div>

                <div className="suite-card-footer">
                  <button className="btn btn-run" onClick={() => handleStartRun(s)}>▶ Start Run</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Suite modal */}
      {showSuiteModal && (
        <div className="modal-overlay" onClick={() => setShowSuiteModal(false)}>
          <div className="modal modal-xl" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingSuite ? 'Edit Suite' : 'New Test Suite'}</h2>
              <button className="modal-close" onClick={() => setShowSuiteModal(false)}>✕</button>
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
                      placeholder="e.g. Order Flow Suite"
                    />
                  </div>
                </div>
                <div>
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

              {allBlocks.length > 0 && (
                <div className="form-row">
                  <label>Blocks <span className="label-hint">(started/stopped with this suite)</span></label>
                  <div className="blocks-checkboxes">
                    {allBlocks.map(b => (
                      <label key={b.id} className="block-checkbox">
                        <input
                          type="checkbox"
                          checked={(form.blocks ?? []).some(fb => fb.id === b.id)}
                          onChange={() => toggleBlock(b)}
                        />
                        <span className="block-dot" style={{ background: b.color ?? '#667eea' }} />
                        {b.name}
                      </label>
                    ))}
                  </div>
                </div>
              )}

              <div className="form-row">
                <div className="section-header">
                  <label>Expectations</label>
                  <button className="btn-link" onClick={addExpectation}>+ Add</button>
                </div>

                {(form.expectations ?? []).length === 0 ? (
                  <p className="muted" style={{ margin: '0.5rem 0' }}>No expectations yet.</p>
                ) : (
                  <div className="expectations-list">
                    {(form.expectations ?? []).map((exp, i) => (
                      <div key={i} className="expectation-row">
                        <div className="exp-field">
                          <label>Name</label>
                          <input
                            type="text"
                            value={exp.name}
                            onChange={e => updateExpectation(i, { name: e.target.value })}
                            placeholder="e.g. Payment called"
                          />
                        </div>
                        <div className="exp-field exp-field-endpoint">
                          <label>Endpoint</label>
                          <select
                            value={exp.mockEndpoint?.id ?? ''}
                            onChange={e => {
                              const ep = allEndpoints.find(ep => ep.id === Number(e.target.value))
                              updateExpectation(i, {
                                mockEndpoint: ep
                                  ? { id: ep.id!, name: ep.name, httpMethod: ep.httpMethod, httpPath: ep.httpPath }
                                  : undefined,
                              })
                            }}
                          >
                            <option value="">— select —</option>
                            {allEndpoints.map(ep => (
                              <option key={ep.id} value={ep.id}>
                                {ep.httpMethod} {ep.httpPath} ({ep.name})
                              </option>
                            ))}
                          </select>
                        </div>
                        <div className="exp-field exp-field-small">
                          <label>Min</label>
                          <input
                            type="number"
                            min={0}
                            value={exp.minCallCount ?? 1}
                            onChange={e => updateExpectation(i, { minCallCount: Number(e.target.value) })}
                          />
                        </div>
                        <div className="exp-field exp-field-small">
                          <label>Max</label>
                          <input
                            type="number"
                            min={0}
                            value={exp.maxCallCount ?? ''}
                            placeholder="∞"
                            onChange={e => updateExpectation(i, { maxCallCount: e.target.value ? Number(e.target.value) : undefined })}
                          />
                        </div>
                        <div className="exp-field exp-field-small">
                          <label>Order</label>
                          <input
                            type="number"
                            min={1}
                            value={exp.expectationOrder ?? ''}
                            placeholder="—"
                            onChange={e => updateExpectation(i, { expectationOrder: e.target.value ? Number(e.target.value) : undefined })}
                          />
                        </div>
                        <div className="exp-field" style={{ flex: 2 }}>
                          <label>Body contains <span className="label-hint">(optional)</span></label>
                          <input
                            type="text"
                            value={exp.requiredBodyContains ?? ''}
                            onChange={e => updateExpectation(i, { requiredBodyContains: e.target.value })}
                            placeholder='"status":"approved"'
                          />
                        </div>
                        <button className="btn-remove" onClick={() => removeExpectation(i)} title="Remove">✕</button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn" onClick={() => setShowSuiteModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={saveSuite} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Runs modal */}
      {showRunsModal && selectedSuite && (
        <div className="modal-overlay" onClick={() => setShowRunsModal(false)}>
          <div className="modal modal-wide" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Runs — {selectedSuite.name}</h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                {suiteTriggers.map(t => (
                  <button
                    key={t.id}
                    className="btn btn-fire"
                    onClick={() => handleFireTrigger(t)}
                    disabled={firingTrigger === t.id}
                    title={`Fire trigger: ${t.name}`}
                  >
                    {firingTrigger === t.id ? '...' : `▶ ${t.name}`}
                  </button>
                ))}
                <button className="modal-close" onClick={() => setShowRunsModal(false)}>✕</button>
              </div>
            </div>
            <div className="modal-body">
              {loadingRuns ? (
                <p className="muted">Loading...</p>
              ) : runs.length === 0 ? (
                <p className="muted">No runs yet.</p>
              ) : (
                runs.slice().reverse().map(run => (
                  <div key={run.id} className="run-card">
                    <div className="run-card-header">
                      <div className="run-info">
                        <span className={`run-status run-status-${run.status.toLowerCase()}`}>{run.status}</span>
                        <span className="run-time">{formatDateTime(run.startedAt)}</span>
                        {run.completedAt && (
                          <span className="run-duration">{formatDuration(run.startedAt, run.completedAt)}</span>
                        )}
                      </div>
                      <div className="run-actions">
                        {run.status === 'RUNNING' && (
                          <>
                            <button className="btn btn-complete" onClick={() => handleCompleteRun(run)}>✓ Complete</button>
                            <button className="btn-link danger" onClick={() => handleCancelRun(run)}>Cancel</button>
                          </>
                        )}
                        {(run.status === 'COMPLETED' || run.status === 'FAILED') && (
                          <button className="btn-link" onClick={() => handleDownloadJUnit(run)}>↓ JUnit XML</button>
                        )}
                      </div>
                    </div>

                    {run.results && run.results.length > 0 && (
                      <div className="run-results">
                        {run.results.map((result, i) => (
                          <ResultRow key={result.id ?? i} result={result} suite={selectedSuite} />
                        ))}
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
            <div className="modal-footer">
              <button className="btn-link danger" onClick={handleClearRuns}
                style={{ marginRight: 'auto' }}
                disabled={runs.every(r => r.status === 'RUNNING')}>
                Clear completed
              </button>
              <button className="btn btn-primary" onClick={() => setShowRunsModal(false)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function ResultRow({ result, suite }: { result: TestExpectationResult; suite: TestSuite }) {
  const expectation = suite.expectations?.find(e => e.id === result.testExpectation?.id)
  const name = expectation?.name ?? `Expectation #${result.testExpectation?.id}`

  return (
    <div className={`result-row ${result.passed ? 'passed' : 'failed'}`}>
      <span className="result-icon">{result.passed ? '✓' : '✗'}</span>
      <span className="result-name">{name}</span>
      <span className="result-count">
        {result.actualCallCount} call{result.actualCallCount !== 1 ? 's' : ''}
        {expectation && (
          <span className="result-expected">
            {' '}(min {expectation.minCallCount ?? 1}
            {expectation.maxCallCount ? `, max ${expectation.maxCallCount}` : ''})
          </span>
        )}
      </span>
      {!result.passed && result.failureReason && (
        <span className="result-reason">{result.failureReason}</span>
      )}
    </div>
  )
}

function formatDateTime(dt?: string) {
  if (!dt) return '—'
  return new Date(dt).toLocaleString('nl-NL', { dateStyle: 'short', timeStyle: 'medium' })
}

function formatDuration(start?: string, end?: string) {
  if (!start || !end) return ''
  const ms = new Date(end).getTime() - new Date(start).getTime()
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}
