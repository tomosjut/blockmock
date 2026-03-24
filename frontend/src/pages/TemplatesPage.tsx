import { useEffect, useState } from 'react'
import { getTemplates, type Template } from '../api/templates'
import { createEndpoint } from '../api/endpoints'
import './TemplatesPage.css'

const PROTOCOL_ICONS: Record<string, string> = {
  HTTP: '🌐',
}

export default function TemplatesPage() {
  const [templates, setTemplates] = useState<Template[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [importing, setImporting] = useState<string | null>(null)
  const [imported, setImported] = useState<Set<string>>(new Set())

  useEffect(() => {
    getTemplates()
      .then(setTemplates)
      .catch(() => setError('Failed to load templates'))
      .finally(() => setLoading(false))
  }, [])

  async function handleImport(template: Template) {
    setImporting(template.id)
    try {
      await createEndpoint(template.endpoint)
      setImported(prev => new Set(prev).add(template.id))
    } catch {
      setError(`Failed to import "${template.name}"`)
    } finally {
      setImporting(null)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Templates</h1>
      </div>

      <p className="templates-intro muted">
        Ready-made endpoint configurations. Import one to add it to your endpoints.
      </p>

      {error && (
        <div className="alert alert-error">
          {error}
          <button onClick={() => setError(null)}>✕</button>
        </div>
      )}

      {loading ? (
        <p className="muted">Loading...</p>
      ) : (
        <div className="templates-grid">
          {templates.map(t => (
            <div key={t.id} className="template-card">
              <div className="template-card-header">
                <span className="template-icon">{PROTOCOL_ICONS[t.protocol] ?? '⚡'}</span>
                <div className="template-title">
                  <span className="template-name">{t.name}</span>
                  <span className="template-protocol">{t.protocol}</span>
                </div>
              </div>

              <p className="template-desc">{t.description}</p>

              <div className="template-preview">
                <span className={`method-badge method-${t.endpoint.httpMethod}`}>
                  {t.endpoint.httpMethod}
                </span>
                <code className="template-path">{t.endpoint.httpPath}</code>
              </div>

              {t.endpoint.responses?.length > 0 && (
                <div className="template-responses">
                  {t.endpoint.responses.map((r, i) => (
                    <div key={i} className="template-response">
                      <span className={`status-code s${Math.floor(r.responseStatusCode / 100)}xx`}>
                        {r.responseStatusCode}
                      </span>
                      <span className="template-response-name">{r.name}</span>
                    </div>
                  ))}
                </div>
              )}

              <div className="template-card-footer">
                {imported.has(t.id) ? (
                  <span className="imported-badge">✓ Imported</span>
                ) : (
                  <button
                    className="btn btn-primary"
                    onClick={() => handleImport(t)}
                    disabled={importing === t.id}
                  >
                    {importing === t.id ? 'Importing...' : '+ Import'}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
