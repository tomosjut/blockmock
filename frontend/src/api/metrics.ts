import { apiFetch, deleteReq } from './client'
import type { MockEndpoint } from '../types'

export const getMetrics = (): Promise<MockEndpoint[]> => apiFetch('/api/endpoints')
export const resetMetrics = (): Promise<void> => deleteReq('/api/metrics')
