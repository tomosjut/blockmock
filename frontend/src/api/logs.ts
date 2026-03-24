import { apiFetch, deleteReq } from './client'
import type { RequestLog } from '../types'

const BASE = '/api/logs'

export const getLogs = (limit = 100): Promise<RequestLog[]> => apiFetch(`${BASE}/recent?limit=${limit}`)
export const getLogStats = (): Promise<{ matched: number; unmatched: number }> => apiFetch(`${BASE}/stats`)
export const clearLogs = (): Promise<void> => deleteReq(BASE)
