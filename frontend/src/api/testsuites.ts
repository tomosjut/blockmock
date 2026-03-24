import { apiFetch, postJson, putJson, deleteReq } from './client'
import type { TestSuite, TestRun } from '../types'

const BASE = '/api/test-suites'
const IE_BASE = '/api/import-export'

export const getTestSuites = (): Promise<TestSuite[]> => apiFetch(BASE)
export const getTestSuite = (id: number): Promise<TestSuite> => apiFetch(`${BASE}/${id}`)
export const createTestSuite = (ts: Partial<TestSuite>): Promise<TestSuite> => postJson(BASE, ts)
export const updateTestSuite = (id: number, ts: Partial<TestSuite>): Promise<TestSuite> => putJson(`${BASE}/${id}`, ts)
export const deleteTestSuite = (id: number): Promise<void> => deleteReq(`${BASE}/${id}`)

export const startRun = (id: number): Promise<TestRun> => apiFetch(`${BASE}/${id}/runs`, { method: 'POST' })
export const getRuns = (id: number): Promise<TestRun[]> => apiFetch(`${BASE}/${id}/runs`)
export const completeRun = (id: number, runId: number): Promise<TestRun> => apiFetch(`${BASE}/${id}/runs/${runId}/complete`, { method: 'POST' })
export const cancelRun = (id: number, runId: number): Promise<void> => deleteReq(`${BASE}/${id}/runs/${runId}`)
export const clearRuns = (id: number): Promise<void> => deleteReq(`${BASE}/${id}/runs`)
export async function getJUnitXml(id: number, runId: number): Promise<string> {
  const response = await fetch(`${BASE}/${id}/runs/${runId}/junit`)
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
