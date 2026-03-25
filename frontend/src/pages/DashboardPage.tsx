import { useEffect, useState } from 'react'
import { getDashboardStats } from '../api/testsuites'
import type { DashboardStats } from '../types'
import './DashboardPage.css'

const STATUS_LABEL: Record<string, string> = {
  COMPLETED: 'Passed',
  FAILED: 'Failed',
  RUNNING: 'Running',
  CANCELLED: 'Cancelled',
}

function RunStatusBadge({ status }: { status: string }) {
  return <span className={`run-badge run-badge-${status.toLowerCase()}`}>{STATUS_LABEL[status] ?? status}</span>
}

function fmtTime(iso?: string) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('nl-NL', { dateStyle: 'short', timeStyle: 'short' })
}

function PassRatio({ passed, total, status }: { passed: number; total: number; status?: string }) {
  if (!status) return <span className="muted">—</span>
  const allPassed = passed === total && status === 'COMPLETED'
  return (
    <span className={allPassed ? 'pass-ratio pass-ratio-ok' : 'pass-ratio pass-ratio-fail'}>
      {passed}/{total}
    </span>
  )
}

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="page"><p className="muted">Loading...</p></div>
  if (!stats) return <div className="page"><p className="muted">Failed to load dashboard.</p></div>

  const activeRuns = stats.suites.flatMap(s => s.scenarios.filter(sc => sc.activeRun))

  return (
    <div className="page">
      <h1>Dashboard</h1>

      {/* Stat cards */}
      <div className="stat-cards">
        <div className="stat-card">
          <div className="stat-value">{stats.endpointCount}</div>
          <div className="stat-label">Total Endpoints</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats.activeEndpointCount}</div>
          <div className="stat-label">Active Endpoints</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats.matchedRequests}</div>
          <div className="stat-label">Matched Requests</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats.unmatchedRequests}</div>
          <div className="stat-label">Unmatched Requests</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats.suites.length}</div>
          <div className="stat-label">Test Suites</div>
        </div>
        <div className="stat-card stat-card-accent">
          <div className="stat-value">{activeRuns.length}</div>
          <div className="stat-label">Active Runs</div>
        </div>
      </div>

      {/* Active runs banner */}
      {activeRuns.length > 0 && (
        <div className="dashboard-active-runs">
          <span className="pulse-dot" />
          <strong>Runs in progress:</strong>{' '}
          {activeRuns.map((sc, i) => (
            <span key={sc.id}>
              {i > 0 && ', '}
              {stats.suites.find(s => s.scenarios.some(x => x.id === sc.id))?.name} / {sc.name}
            </span>
          ))}
        </div>
      )}

      <div className="dashboard-columns">
        {/* Suite overview */}
        <section className="dashboard-section">
          <h2>Suite Overview</h2>
          {stats.suites.length === 0 ? (
            <p className="muted">No test suites yet.</p>
          ) : (
            <div className="suite-overview">
              {stats.suites.map(suite => (
                <div key={suite.id} className="suite-overview-card">
                  <div className="suite-overview-header">
                    <span className="suite-color-dot" style={{ background: suite.color ?? '#667eea' }} />
                    <strong>{suite.name}</strong>
                  </div>
                  {suite.scenarios.length === 0 ? (
                    <p className="muted" style={{ fontSize: '0.8rem', margin: '0.3rem 0 0' }}>No scenarios</p>
                  ) : (
                    <table className="suite-scenario-table">
                      <tbody>
                        {suite.scenarios.map(sc => (
                          <tr key={sc.id}>
                            <td className="scenario-name-cell">
                              {sc.activeRun && <span className="pulse-dot pulse-dot-sm" />}
                              {sc.name}
                            </td>
                            <td>
                              {sc.lastRunStatus
                                ? <RunStatusBadge status={sc.lastRunStatus} />
                                : <span className="muted" style={{ fontSize: '0.75rem' }}>no runs</span>
                              }
                            </td>
                            <td>
                              <PassRatio passed={sc.lastRunPassed} total={sc.lastRunTotal} status={sc.lastRunStatus} />
                            </td>
                            <td className="muted" style={{ fontSize: '0.75rem', textAlign: 'right' }}>
                              {fmtTime(sc.lastRunAt)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Recent runs */}
        <section className="dashboard-section">
          <h2>Recent Runs</h2>
          {stats.recentRuns.length === 0 ? (
            <p className="muted">No runs yet.</p>
          ) : (
            <table className="data-table dashboard-runs-table">
              <thead>
                <tr>
                  <th>Suite / Scenario</th>
                  <th>Status</th>
                  <th>Result</th>
                  <th>At</th>
                </tr>
              </thead>
              <tbody>
                {stats.recentRuns.map(run => (
                  <tr key={run.id}>
                    <td>
                      <span className="suite-color-dot" style={{ background: run.suiteColor ?? '#667eea' }} />
                      <span className="run-suite-name">{run.suiteName}</span>
                      <span className="muted"> / {run.scenarioName}</span>
                    </td>
                    <td><RunStatusBadge status={run.status} /></td>
                    <td><PassRatio passed={run.passed} total={run.total} status={run.status} /></td>
                    <td className="muted" style={{ fontSize: '0.8rem', whiteSpace: 'nowrap' }}>
                      {fmtTime(run.completedAt ?? run.startedAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>

      {/* Recent trigger fires */}
      {stats.recentFires.length > 0 && (
        <section className="dashboard-section dashboard-section-fires">
          <h2>Recent Trigger Fires</h2>
          <table className="data-table">
            <thead>
              <tr>
                <th>Trigger</th>
                <th>Type</th>
                <th>Scenario</th>
                <th>Fired at</th>
              </tr>
            </thead>
            <tbody>
              {stats.recentFires.map(f => (
                <tr key={f.id}>
                  <td>{f.name}</td>
                  <td><span className="muted" style={{ fontSize: '0.8rem' }}>{f.type}</span></td>
                  <td className="muted">{f.scenarioName ?? '—'}</td>
                  <td className="muted" style={{ fontSize: '0.8rem', whiteSpace: 'nowrap' }}>{fmtTime(f.firedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </div>
  )
}
