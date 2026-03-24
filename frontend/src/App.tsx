import { NavLink, Routes, Route, Navigate } from 'react-router-dom'
import './App.css'

import DashboardPage from './pages/DashboardPage'
import EndpointsPage from './pages/EndpointsPage'
import BlocksPage from './pages/BlocksPage'
import TriggersPage from './pages/TriggersPage'
import TestSuitesPage from './pages/TestSuitesPage'
import LogsPage from './pages/LogsPage'
import TemplatesPage from './pages/TemplatesPage'

const tabs = [
  { to: '/',            label: 'Dashboard'   },
  { to: '/endpoints',   label: 'Endpoints'   },
  { to: '/blocks',      label: 'Blocks'      },
  { to: '/triggers',    label: 'Triggers'    },
  { to: '/test-suites', label: 'Test Suites' },
  { to: '/logs',        label: 'Logs'        },
  { to: '/templates',   label: 'Templates'   },
]

function App() {
  return (
    <div className="app">
      <header className="app-header">
        <div className="app-logo">
          <img src="/blockmock.png" alt="BlockMock" height={36} />
        </div>
        <nav className="tab-nav">
          {tabs.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) => isActive ? 'tab active' : 'tab'}
            >
              {label}
            </NavLink>
          ))}
        </nav>
      </header>

      <main className="app-content">
        <Routes>
          <Route path="/"            element={<DashboardPage />} />
          <Route path="/endpoints"   element={<EndpointsPage />} />
          <Route path="/blocks"      element={<BlocksPage />} />
          <Route path="/triggers"    element={<TriggersPage />} />
          <Route path="/test-suites" element={<TestSuitesPage />} />
          <Route path="/logs"        element={<LogsPage />} />
          <Route path="/templates"   element={<TemplatesPage />} />
          <Route path="*"            element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
