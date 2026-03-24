import React, { useEffect, useRef, useState } from 'react'
import { getLogs, getLogStats, clearLogs } from '../api/logs'
import type { RequestLog } from '../types'
import './LogsPage.css'

type Filter = 'all' | 'matched' | 'unmatched'

export default function LogsPage() {
  const [logs, setLogs] = useState<RequestLog[]>([])
  const [stats, setStats] = useState<{ matched: number; unmatched: number } | null>(null)
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<Filter>('all')
  const [limit, setLimit] = useState(100)
  const [autoRefresh, setAutoRefresh] = useState(false)
  const [expanded, setExpanded] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => { load() }, [limit])

  useEffect(() => {
    if (autoRefresh) {
      intervalRef.current = setInterval(load, 3000)
    } else {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
    return () => { if (intervalRef.current) clearInterval(intervalRef.current) }
  }, [autoRefresh, limit])

  async function load() {
    try {
      const [l, s] = await Promise.all([getLogs(limit), getLogStats()])
      setLogs(l)
      setStats(s)
    } catch {
      setError('Failed to load logs')
    } finally {
      setLoading(false)
    }
  }

  async function handleClear() {
    if (!confirm('Clear all logs?')) return
    try {
      await clearLogs()
      setLogs([])
      setStats({ matched: 0, unmatched: 0 })
      setExpanded(null)
    } catch {
      setError('Failed to clear logs')
    }
  }

  const filtered = logs.filter(l => {
    if (filter === 'matched') return l.matched
    if (filter === 'unmatched') return !l.matched
    return true
  })

  return (
    <div className="page">
      <div className="page-header">
        <h1>Logs</h1>
        <div className="logs-toolbar">
          <label className="auto-refresh-toggle">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={e => setAutoRefresh(e.target.checked)}
            />
            Auto-refresh
          </label>
          <button className="btn" onClick={load}>↻ Refresh</button>
          <button className="btn danger" onClick={handleClear}>Clear</button>
        </div>
      </div>

      {error && (
        <div className="alert alert-error">
          {error}
          <button onClick={() => setError(null)}>✕</button>
        </div>
      )}

      {stats && (
        <div className="logs-stats">
          <button
            className={`stat-pill ${filter === 'all' ? 'active' : ''}`}
            onClick={() => setFilter('all')}
          >
            <span className="stat-pill-value">{stats.matched + stats.unmatched}</span> total
          </button>
          <button
            className={`stat-pill stat-pill-matched ${filter === 'matched' ? 'active' : ''}`}
            onClick={() => setFilter(f => f === 'matched' ? 'all' : 'matched')}
          >
            <span className="stat-pill-value">{stats.matched}</span> matched
          </button>
          <button
            className={`stat-pill stat-pill-unmatched ${filter === 'unmatched' ? 'active' : ''}`}
            onClick={() => setFilter(f => f === 'unmatched' ? 'all' : 'unmatched')}
          >
            <span className="stat-pill-value">{stats.unmatched}</span> unmatched
          </button>
        </div>
      )}

      {loading ? (
        <p className="muted">Loading...</p>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <p>{filter !== 'all' ? `No ${filter} requests.` : 'No logs yet.'}</p>
          <p className="muted">Requests to /mock/* will appear here.</p>
        </div>
      ) : (
        <>
          <table className="data-table logs-table">
            <thead>
              <tr>
                <th style={{ width: 140 }}>Time</th>
                <th style={{ width: 70 }}>Method</th>
                <th>Path</th>
                <th style={{ width: 70 }}>Status</th>
                <th style={{ width: 80 }}>Match</th>
                <th style={{ width: 80 }}>Delay</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(log => (
                <React.Fragment key={log.id}>
                  <tr
                    className={`log-row ${log.matched ? '' : 'log-unmatched'} ${expanded === log.id ? 'log-expanded' : ''}`}
                    onClick={() => setExpanded(expanded === log.id ? null : log.id!)}
                  >
                    <td className="log-time">{formatTime(log.receivedAt)}</td>
                    <td><span className={`method-badge method-${log.requestMethod}`}>{log.requestMethod}</span></td>
                    <td className="log-path">{log.requestPath}</td>
                    <td>
                      {log.responseStatusCode
                        ? <span className={`status-code s${Math.floor(log.responseStatusCode / 100)}xx`}>{log.responseStatusCode}</span>
                        : <span className="muted">—</span>}
                    </td>
                    <td>
                      <span className={`match-badge ${log.matched ? 'matched' : 'unmatched'}`}>
                        {log.matched ? '✓' : '✗'}
                      </span>
                    </td>
                    <td className="muted">{log.responseDelayMs ? `${log.responseDelayMs}ms` : '—'}</td>
                  </tr>
                  {expanded === log.id && (
                    <tr className="log-detail-row">
                      <td colSpan={6}>
                        <LogDetail log={log} />
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>

          <div className="logs-footer">
            <span className="muted">Showing {filtered.length} of {logs.length} logs</span>
            {logs.length >= limit && (
              <button className="btn-link" onClick={() => setLimit(l => l + 100)}>
                Load more
              </button>
            )}
          </div>
        </>
      )}
    </div>
  )
}

function LogDetail({ log }: { log: RequestLog }) {
  return (
    <div className="log-detail">
      <div className="log-detail-cols">
        <div className="log-detail-col">
          <div className="log-detail-section">
            <h4>Request</h4>
            {log.requestHeaders && Object.keys(log.requestHeaders).length > 0 && (
              <div className="detail-block">
                <span className="detail-label">Headers</span>
                <pre>{formatHeaders(log.requestHeaders)}</pre>
              </div>
            )}
            {log.requestQueryParams && Object.keys(log.requestQueryParams).length > 0 && (
              <div className="detail-block">
                <span className="detail-label">Query params</span>
                <pre>{formatHeaders(log.requestQueryParams)}</pre>
              </div>
            )}
            {log.requestBody ? (
              <div className="detail-block">
                <span className="detail-label">Body</span>
                <pre>{tryPrettyJson(log.requestBody)}</pre>
              </div>
            ) : (
              <span className="muted">No body</span>
            )}
          </div>
        </div>

        <div className="log-detail-col">
          <div className="log-detail-section">
            <h4>Response</h4>
            {log.responseHeaders && Object.keys(log.responseHeaders).length > 0 && (
              <div className="detail-block">
                <span className="detail-label">Headers</span>
                <pre>{formatHeaders(log.responseHeaders)}</pre>
              </div>
            )}
            {log.responseBody ? (
              <div className="detail-block">
                <span className="detail-label">Body</span>
                <pre>{tryPrettyJson(log.responseBody)}</pre>
              </div>
            ) : (
              <span className="muted">No body</span>
            )}
          </div>
        </div>
      </div>

      {log.clientIp && (
        <div className="detail-meta">
          <span className="detail-label">Client IP:</span> {log.clientIp}
        </div>
      )}
    </div>
  )
}

function formatTime(dt?: string) {
  if (!dt) return '—'
  const d = new Date(dt)
  const ms = String(d.getMilliseconds()).padStart(3, '0')
  return d.toLocaleTimeString('nl-NL', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) + '.' + ms
}

function formatHeaders(headers: Record<string, string>) {
  return Object.entries(headers).map(([k, v]) => `${k}: ${v}`).join('\n')
}

function tryPrettyJson(body: string) {
  try { return JSON.stringify(JSON.parse(body), null, 2) } catch { return body }
}
