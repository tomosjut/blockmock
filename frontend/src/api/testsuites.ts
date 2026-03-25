import { apiFetch, postJson, putJson, deleteReq } from './client'
import type { TestSuite, TestScenario, TestRun } from '../types'

const BASE = '/api/test-suites'
const IE_BASE = '/api/import-export'

export const getTestSuites = (): Promise<TestSuite[]> => apiFetch(BASE)
export const getTestSuite = (id: number): Promise<TestSuite> => apiFetch(`${BASE}/${id}`)
export const createTestSuite = (ts: Partial<TestSuite>): Promise<TestSuite> => postJson(BASE, ts)
export const updateTestSuite = (id: number, ts: Partial<TestSuite>): Promise<TestSuite> => putJson(`${BASE}/${id}`, ts)
export const deleteTestSuite = (id: number): Promise<void> => deleteReq(`${BASE}/${id}`)

export const getScenarios = (suiteId: number): Promise<TestScenario[]> => apiFetch(`${BASE}/${suiteId}/scenarios`)
export const createScenario = (suiteId: number, s: Partial<TestScenario>): Promise<TestScenario> => postJson(`${BASE}/${suiteId}/scenarios`, s)
export const updateScenario = (suiteId: number, scenarioId: number, s: Partial<TestScenario>): Promise<TestScenario> => putJson(`${BASE}/${suiteId}/scenarios/${scenarioId}`, s)
export const deleteScenario = (suiteId: number, scenarioId: number): Promise<void> => deleteReq(`${BASE}/${suiteId}/scenarios/${scenarioId}`)

export const startRun = (suiteId: number, scenarioId: number): Promise<TestRun> =>
  apiFetch(`${BASE}/${suiteId}/scenarios/${scenarioId}/runs`, { method: 'POST' })
export const getRuns = (suiteId: number, scenarioId: number): Promise<TestRun[]> =>
  apiFetch(`${BASE}/${suiteId}/scenarios/${scenarioId}/runs`)
export const completeRun = (suiteId: number, scenarioId: number, runId: number): Promise<TestRun> =>
  apiFetch(`${BASE}/${suiteId}/scenarios/${scenarioId}/runs/${runId}/complete`, { method: 'POST' })
export const cancelRun = (suiteId: number, scenarioId: number, runId: number): Promise<void> =>
  deleteReq(`${BASE}/${suiteId}/scenarios/${scenarioId}/runs/${runId}`)
export const clearRuns = (suiteId: number, scenarioId: number): Promise<void> =>
  deleteReq(`${BASE}/${suiteId}/scenarios/${scenarioId}/runs`)

export async function getJUnitXml(suiteId: number, scenarioId: number, runId: number): Promise<string> {
  const response = await fetch(`${BASE}/${suiteId}/scenarios/${scenarioId}/runs/${runId}/junit`)
  if (!response.ok) throw new Error(`API ${response.status}: ${response.statusText}`)
  return response.text()
}

export async function exportSuite(id: number): Promise<{ blob: Blob; filename: string }> {
  const response = await fetch(`${IE_BASE}/suites/${id}`)
  if (!response.ok) throw new Error(`Export failed: ${response.statusText}`)
  const disposition = response.headers.get('Content-Disposition') ?? ''
  const match = disposition.match(/filename="([^"]+)"/)
  const filename = match ? match[1] : `suite-${id}.json`
  return { blob: await response.blob(), filename }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const importSuite = (data: unknown): Promise<any> => postJson(`${IE_BASE}/suites`, data)
