import { useEffect, useState } from 'react'
import { getEndpoints } from '../api/endpoints'
import { getLogStats } from '../api/logs'
import type { MockEndpoint } from '../types'

export default function DashboardPage() {
  const [endpoints, setEndpoints] = useState<MockEndpoint[]>([])
  const [stats, setStats] = useState({ matched: 0, unmatched: 0 })

  useEffect(() => {
    getEndpoints().then(setEndpoints).catch(console.error)
    getLogStats().then(setStats).catch(console.error)
  }, [])

  const activeEndpoints = endpoints.filter(e => e.enabled).length

  return (
    <div className="page">
      <h1>Dashboard</h1>
      <div className="stat-cards">
        <div className="stat-card">
          <div className="stat-value">{endpoints.length}</div>
          <div className="stat-label">Total Endpoints</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{activeEndpoints}</div>
          <div className="stat-label">Active Endpoints</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats.matched}</div>
          <div className="stat-label">Matched Requests</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats.unmatched}</div>
          <div className="stat-label">Unmatched Requests</div>
        </div>
      </div>
    </div>
  )
}
