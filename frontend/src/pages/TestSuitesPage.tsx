import React, { useEffect, useRef, useState } from 'react'
import type { TestSuite, TestScenario, TestExpectation, ScenarioResponseOverride, TestRun, TestExpectationResult, MockEndpoint, Block, TriggerConfig } from '../types'
import { isHttpEndpoint } from '../types'
import {
  getTestSuites, createTestSuite, updateTestSuite, deleteTestSuite,
  getScenarios, createScenario, updateScenario, deleteScenario,
  startRun, getRuns, completeRun, cancelRun, getJUnitXml,
  exportSuite, importSuite, clearRuns,
} from '../api/testsuites'
import { getEndpoints } from '../api/endpoints'
import { getBlocks } from '../api/blocks'
import { getTriggers, fireTrigger } from '../api/triggers'
import './TestSuitesPage.css'

const COLORS = ['#667eea', '#cba6f7', '#89b4fa', '#a6e3a1', '#f9e2af', '#fab387', '#f38ba8', '#74c7ec']

function endpointLabel(ep: MockEndpoint): string {
  if (isHttpEndpoint(ep)) return `${ep.httpMethod} ${ep.httpPath} (${ep.name})`
  return `AMQP ${ep.amqpAddress} (${ep.name})`
}

const emptyExpectation = (): TestExpectation => ({
  name: '',
  mockEndpoint: undefined,
  minCallCount: 1,
  maxCallCount: undefined,
  requiredBodyContains: '',
  expectationOrder: undefined,
})

const emptySuiteForm = (): Partial<TestSuite> => ({
  name: '',
  description: '',
  color: '#667eea',
  blocks: [],
})

const emptyScenarioForm = (): Partial<TestScenario> => ({
  name: '',
  description: '',
  expectations: [],
  responseOverrides: [],
})

const emptyOverride = (): ScenarioResponseOverride => ({
  mockEndpoint: undefined,
  mockResponse: undefined,
})

export default function TestSuitesPage() {
  const [suites, setSuites] = useState<TestSuite[]>([])
  const [scenarios, setScenarios] = useState<Record<number, TestScenario[]>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [allEndpoints, setAllEndpoints] = useState<MockEndpoint[]>([])
  const [allBlocks, setAllBlocks] = useState<Block[]>([])

  // Suite modal
  const [showSuiteModal, setShowSuiteModal] = useState(false)
  const [editingSuite, setEditingSuite] = useState<TestSuite | null>(null)
  const [suiteForm, setSuiteForm] = useState<Partial<TestSuite>>(emptySuiteForm())
  const [savingSuite, setSavingSuite] = useState(false)

  // Scenario modal
  const [showScenarioModal, setShowScenarioModal] = useState(false)
  const [editingScenario, setEditingScenario] = useState<TestScenario | null>(null)
  const [scenarioSuiteId, setScenarioSuiteId] = useState<number | null>(null)
  const [scenarioForm, setScenarioForm] = useState<Partial<TestScenario>>(emptyScenarioForm())
  const [savingScenario, setSavingScenario] = useState(false)

  // Runs modal
  const [showRunsModal, setShowRunsModal] = useState(false)
  const [selectedSuite, setSelectedSuite] = useState<TestSuite | null>(null)
  const [selectedScenario, setSelectedScenario] = useState<TestScenario | null>(null)
  const [runs, setRuns] = useState<TestRun[]>([])
  const [loadingRuns, setLoadingRuns] = useState(false)

  // Triggers for runs modal
  const [scenarioTriggers, setScenarioTriggers] = useState<TriggerConfig[]>([])
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
      // Load scenarios for each suite
      const scenarioMap: Record<number, TestScenario[]> = {}
      await Promise.all(s.map(async suite => {
        if (suite.id) {
          scenarioMap[suite.id] = await getScenarios(suite.id)
        }
      }))
      setScenarios(scenarioMap)
    } catch {
      setError('Failed to load test suites')
    } finally {
      setLoading(false)
    }
  }

  // ---- Suite modal ----

  function openCreateSuite() {
    setEditingSuite(null)
    setSuiteForm(emptySuiteForm())
    setShowSuiteModal(true)
  }

  function openEditSuite(s: TestSuite) {
    setEditingSuite(s)
    setSuiteForm({
      name: s.name,
      description: s.description ?? '',
      color: s.color ?? '#667eea',
      blocks: s.blocks ? [...s.blocks] : [],
    })
    setShowSuiteModal(true)
  }

  async function saveSuite() {
    setSavingSuite(true)
    try {
      const payload = {
        ...suiteForm,
        blocks: (suiteForm.blocks ?? []).map(b => ({ id: b.id })),
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
      setSavingSuite(false)
    }
  }

  async function handleDeleteSuite(s: TestSuite) {
    if (!confirm(`Delete test suite "${s.name}"?`)) return
    try {
      await deleteTestSuite(s.id!)
      await load()
    } catch {
      setError('Failed to delete test suite')
    }
  }

  function toggleBlock(block: Block) {
    setSuiteForm(f => {
      const current = f.blocks ?? []
      const exists = current.some(b => b.id === block.id)
      return {
        ...f,
        blocks: exists ? current.filter(b => b.id !== block.id) : [...current, { id: block.id!, name: block.name }],
      }
    })
  }

  // ---- Scenario modal ----

  function openCreateScenario(suite: TestSuite) {
    setEditingScenario(null)
    setScenarioSuiteId(suite.id!)
    setScenarioForm(emptyScenarioForm())
    setShowScenarioModal(true)
  }

  function openEditScenario(suite: TestSuite, scenario: TestScenario) {
    setEditingScenario(scenario)
    setScenarioSuiteId(suite.id!)
    setScenarioForm({
      name: scenario.name,
      description: scenario.description ?? '',
      expectations: scenario.expectations ? scenario.expectations.map(e => ({ ...e })) : [],
      responseOverrides: scenario.responseOverrides ? scenario.responseOverrides.map(o => ({ ...o })) : [],
    })
    setShowScenarioModal(true)
  }

  async function saveScenario() {
    setSavingScenario(true)
    try {
      const payload = {
        ...scenarioForm,
        expectations: (scenarioForm.expectations ?? []).map(e => ({
          ...e,
          mockEndpoint: e.mockEndpoint?.id ? { id: e.mockEndpoint.id } : undefined,
          requiredBodyContains: e.requiredBodyContains || undefined,
          maxCallCount: e.maxCallCount || undefined,
          expectationOrder: e.expectationOrder || undefined,
        })),
        responseOverrides: (scenarioForm.responseOverrides ?? [])
          .filter(o => o.mockEndpoint?.id && o.mockResponse?.id)
          .map(o => ({
            mockEndpoint: { id: o.mockEndpoint!.id },
            mockResponse: { id: o.mockResponse!.id },
          })),
      }
      if (editingScenario?.id) {
        await updateScenario(scenarioSuiteId!, editingScenario.id, payload)
      } else {
        await createScenario(scenarioSuiteId!, payload)
      }
      setShowScenarioModal(false)
      await load()
    } catch {
      setError('Failed to save scenario')
    } finally {
      setSavingScenario(false)
    }
  }

  async function handleDeleteScenario(suite: TestSuite, scenario: TestScenario) {
    if (!confirm(`Delete scenario "${scenario.name}"?`)) return
    try {
      await deleteScenario(suite.id!, scenario.id!)
      await load()
    } catch {
      setError('Failed to delete scenario')
    }
  }

  function addExpectation() {
    setScenarioForm(f => ({ ...f, expectations: [...(f.expectations ?? []), emptyExpectation()] }))
  }

  function updateExpectation(i: number, patch: Partial<TestExpectation>) {
    setScenarioForm(f => ({
      ...f,
      expectations: (f.expectations ?? []).map((e, idx) => idx === i ? { ...e, ...patch } : e),
    }))
  }

  function removeExpectation(i: number) {
    setScenarioForm(f => ({ ...f, expectations: (f.expectations ?? []).filter((_, idx) => idx !== i) }))
  }

  function addOverride() {
    setScenarioForm(f => ({ ...f, responseOverrides: [...(f.responseOverrides ?? []), emptyOverride()] }))
  }

  function updateOverride(i: number, patch: Partial<ScenarioResponseOverride>) {
    setScenarioForm(f => ({
      ...f,
      responseOverrides: (f.responseOverrides ?? []).map((o, idx) => idx === i ? { ...o, ...patch } : o),
    }))
  }

  function removeOverride(i: number) {
    setScenarioForm(f => ({ ...f, responseOverrides: (f.responseOverrides ?? []).filter((_, idx) => idx !== i) }))
  }

  // ---- Runs modal ----

  async function openRuns(suite: TestSuite, scenario: TestScenario) {
    setSelectedSuite(suite)
    setSelectedScenario(scenario)
    setShowRunsModal(true)
    setLoadingRuns(true)
    try {
      const [runsData, allTriggers] = await Promise.all([
        getRuns(suite.id!, scenario.id!),
        getTriggers(),
      ])
      setRuns(runsData)
      setScenarioTriggers(allTriggers.filter(t => t.testScenario?.id === scenario.id && t.enabled))
    } catch {
      setError('Failed to load runs')
    } finally {
      setLoadingRuns(false)
    }
  }

  async function handleStartRun(suite: TestSuite, scenario: TestScenario) {
    try {
      await startRun(suite.id!, scenario.id!)
      await openRuns(suite, scenario)
    } catch (e: unknown) {
      setError('Failed to start run: ' + (e instanceof Error ? e.message : String(e)))
    }
  }

  async function handleCompleteRun(run: TestRun) {
    try {
      const updated = await completeRun(selectedSuite!.id!, selectedScenario!.id!, run.id!)
      setRuns(prev => prev.map(r => r.id === updated.id ? updated : r))
    } catch (e: unknown) {
      setError('Failed to complete run: ' + (e instanceof Error ? e.message : String(e)))
    }
  }

  async function handleCancelRun(run: TestRun) {
    if (!confirm('Cancel this run?')) return
    try {
      await cancelRun(selectedSuite!.id!, selectedScenario!.id!, run.id!)
      setRuns(prev => prev.filter(r => r.id !== run.id))
    } catch {
      setError('Failed to cancel run')
    }
  }

  async function handleClearRuns() {
    if (!confirm('Delete all completed, failed and cancelled runs?')) return
    try {
      await clearRuns(selectedSuite!.id!, selectedScenario!.id!)
      setRuns(prev => prev.filter(r => r.status === 'RUNNING'))
    } catch {
      setError('Failed to clear runs')
    }
  }

  async function handleDownloadJUnit(run: TestRun) {
    try {
      const xml = await getJUnitXml(selectedSuite!.id!, selectedScenario!.id!, run.id!)
      const blob = new Blob([xml], { type: 'application/xml' })
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

  // ---- Import/export ----

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

  return (
    <div className="page">
      <div className="page-header">
        <h1>Test Suites</h1>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="btn" onClick={() => importInputRef.current?.click()}>↑ Import</button>
          <button className="btn btn-primary" onClick={openCreateSuite}>+ New Suite</button>
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
          <p className="muted">Create a suite to define test scenarios and run integration tests.</p>
        </div>
      ) : (
        <div className="suites-grid">
          {suites.map(s => {
            const suiteScenarios = scenarios[s.id!] ?? []
            return (
              <div key={s.id} className="suite-card">
                <div className="suite-card-accent" style={{ background: s.color ?? '#667eea' }} />
                <div className="suite-card-body">
                  <div className="suite-card-header">
                    <span className="suite-name">{s.name}</span>
                    <div className="suite-actions">
                      <button className="btn-link" onClick={() => openEditSuite(s)}>Edit</button>
                      <button className="btn-link" onClick={() => handleExport(s)}>↓ Export</button>
                      <button className="btn-link danger" onClick={() => handleDeleteSuite(s)}>Delete</button>
                    </div>
                  </div>

                  {s.description && <p className="suite-desc">{s.description}</p>}

                  {(s.blocks?.length ?? 0) > 0 && (
                    <div className="suite-blocks">
                      {s.blocks!.map(b => (
                        <span key={b.id} className="block-tag">{b.name}</span>
                      ))}
                    </div>
                  )}

                  <div className="scenarios-section">
                    <div className="scenarios-header">
                      <span className="scenarios-label">Scenarios</span>
                      <button className="btn-link" onClick={() => openCreateScenario(s)}>+ Add</button>
                    </div>
                    {suiteScenarios.length === 0 ? (
                      <p className="muted" style={{ margin: '0.25rem 0', fontSize: '0.85rem' }}>No scenarios yet.</p>
                    ) : (
                      suiteScenarios.map(sc => (
                        <div key={sc.id} className="scenario-row">
                          <div className="scenario-info">
                            <span className="scenario-name">{sc.name}</span>
                            <span className="scenario-count muted">
                              {sc.expectations?.length ?? 0} exp
                              {(sc.responseOverrides?.length ?? 0) > 0 && ` · ${sc.responseOverrides!.length} override${sc.responseOverrides!.length !== 1 ? 's' : ''}`}
                            </span>
                          </div>
                          <div className="scenario-actions">
                            <button className="btn btn-run btn-sm" onClick={() => handleStartRun(s, sc)}>▶ Run</button>
                            <button className="btn-link" onClick={() => openRuns(s, sc)}>Runs</button>
                            <button className="btn-link" onClick={() => openEditScenario(s, sc)}>Edit</button>
                            <button className="btn-link danger" onClick={() => handleDeleteScenario(s, sc)}>Delete</button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Suite modal */}
      {showSuiteModal && (
        <div className="modal-overlay" onClick={() => setShowSuiteModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
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
                      value={suiteForm.name ?? ''}
                      onChange={e => setSuiteForm(f => ({ ...f, name: e.target.value }))}
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
                          className={`color-swatch ${suiteForm.color === c ? 'selected' : ''}`}
                          style={{ background: c }}
                          onClick={() => setSuiteForm(f => ({ ...f, color: c }))}
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
                  value={suiteForm.description ?? ''}
                  onChange={e => setSuiteForm(f => ({ ...f, description: e.target.value }))}
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
                          checked={(suiteForm.blocks ?? []).some(fb => fb.id === b.id)}
                          onChange={() => toggleBlock(b)}
                        />
                        <span className="block-dot" style={{ background: b.color ?? '#667eea' }} />
                        {b.name}
                      </label>
                    ))}
                  </div>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn" onClick={() => setShowSuiteModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={saveSuite} disabled={savingSuite}>
                {savingSuite ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Scenario modal */}
      {showScenarioModal && (
        <div className="modal-overlay" onClick={() => setShowScenarioModal(false)}>
          <div className="modal modal-xl" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingScenario ? 'Edit Scenario' : 'New Scenario'}</h2>
              <button className="modal-close" onClick={() => setShowScenarioModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-row">
                <label>Name</label>
                <input
                  type="text"
                  value={scenarioForm.name ?? ''}
                  onChange={e => setScenarioForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="e.g. Happy Path"
                />
              </div>
              <div className="form-row">
                <label>Description</label>
                <input
                  type="text"
                  value={scenarioForm.description ?? ''}
                  onChange={e => setScenarioForm(f => ({ ...f, description: e.target.value }))}
                  placeholder="Optional"
                />
              </div>

              <div className="form-row">
                <div className="section-header">
                  <label>Expectations</label>
                  <button className="btn-link" onClick={addExpectation}>+ Add</button>
                </div>

                {(scenarioForm.expectations ?? []).length === 0 ? (
                  <p className="muted" style={{ margin: '0.5rem 0' }}>No expectations yet.</p>
                ) : (
                  <div className="expectations-list">
                    {(scenarioForm.expectations ?? []).map((exp, i) => (
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
                                mockEndpoint: ep ? {
                                  id: ep.id!,
                                  name: ep.name,
                                  httpMethod: isHttpEndpoint(ep) ? ep.httpMethod : undefined,
                                  httpPath: isHttpEndpoint(ep) ? ep.httpPath : undefined,
                                } : undefined,
                              })
                            }}
                          >
                            <option value="">— select —</option>
                            {allEndpoints.map(ep => (
                              <option key={ep.id} value={ep.id}>
                                {endpointLabel(ep)}
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

              <div className="form-row">
                <div className="section-header">
                  <label>Response Overrides <span className="label-hint">(verplicht een specifieke response voor dit scenario)</span></label>
                  <button className="btn-link" onClick={addOverride}>+ Add</button>
                </div>

                {(scenarioForm.responseOverrides ?? []).length === 0 ? (
                  <p className="muted" style={{ margin: '0.5rem 0', fontSize: '0.85rem' }}>Geen overrides — alle endpoints gebruiken hun normale responses.</p>
                ) : (
                  <div className="overrides-list">
                    {(scenarioForm.responseOverrides ?? []).map((ov, i) => {
                      const epResponses = ov.mockEndpoint?.id
                        ? (allEndpoints.find(ep => ep.id === ov.mockEndpoint!.id)?.responses ?? [])
                        : []
                      return (
                        <div key={i} className="override-row">
                          <div className="exp-field exp-field-endpoint">
                            <label>Endpoint</label>
                            <select
                              value={ov.mockEndpoint?.id ?? ''}
                              onChange={e => {
                                const ep = allEndpoints.find(ep => ep.id === Number(e.target.value))
                                updateOverride(i, {
                                  mockEndpoint: ep ? {
                                    id: ep.id!,
                                    name: ep.name,
                                    httpMethod: isHttpEndpoint(ep) ? ep.httpMethod : undefined,
                                    httpPath: isHttpEndpoint(ep) ? ep.httpPath : undefined,
                                  } : undefined,
                                  mockResponse: undefined,
                                })
                              }}
                            >
                              <option value="">— select endpoint —</option>
                              {allEndpoints.map(ep => (
                                <option key={ep.id} value={ep.id}>
                                  {endpointLabel(ep)}
                                </option>
                              ))}
                            </select>
                          </div>
                          <div className="exp-field exp-field-endpoint">
                            <label>Forceer response</label>
                            <select
                              value={ov.mockResponse?.id ?? ''}
                              disabled={epResponses.length === 0}
                              onChange={e => {
                                const resp = epResponses.find(r => r.id === Number(e.target.value))
                                updateOverride(i, {
                                  mockResponse: resp ? { id: resp.id!, name: resp.name, responseStatusCode: resp.responseStatusCode } : undefined,
                                })
                              }}
                            >
                              <option value="">— select response —</option>
                              {epResponses.map(r => (
                                <option key={r.id} value={r.id}>
                                  {r.name} ({r.responseStatusCode})
                                </option>
                              ))}
                            </select>
                          </div>
                          <button className="btn-remove" onClick={() => removeOverride(i)} title="Remove">✕</button>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn" onClick={() => setShowScenarioModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={saveScenario} disabled={savingScenario}>
                {savingScenario ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Runs modal */}
      {showRunsModal && selectedSuite && selectedScenario && (
        <div className="modal-overlay" onClick={() => setShowRunsModal(false)}>
          <div className="modal modal-wide" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Runs — {selectedSuite.name} / {selectedScenario.name}</h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                {scenarioTriggers.map(t => (
                  <button
                    key={t.id}
                    className="btn btn-fire"
                    onClick={() => handleFireTrigger(t)}
                    disabled={firingTrigger === t.id}
                    title={t.name}
                  >
                    {firingTrigger === t.id ? '...' : '▶ Trigger'}
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
                          <ResultRow key={result.id ?? i} result={result} scenario={selectedScenario} />
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

function ResultRow({ result, scenario }: { result: TestExpectationResult; scenario: TestScenario }) {
  const expectation = scenario.expectations?.find(e => e.id === result.testExpectation?.id)
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
