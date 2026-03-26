import { apiFetch, postJson, putJson, deleteReq } from './client'
import type { MockEndpoint, MockEndpointForm, MockResponse } from '../types'

const BASE = '/api/endpoints'

export const getEndpoints = (): Promise<MockEndpoint[]> => apiFetch(BASE)
export const getEndpoint = (id: number): Promise<MockEndpoint> => apiFetch(`${BASE}/${id}`)
export const createEndpoint = (ep: MockEndpointForm): Promise<MockEndpoint> => postJson(BASE, ep)
export const updateEndpoint = (id: number, ep: MockEndpointForm): Promise<MockEndpoint> => putJson(`${BASE}/${id}`, ep)
export const deleteEndpoint = (id: number): Promise<void> => deleteReq(`${BASE}/${id}`)
export const toggleEndpoint = (id: number): Promise<void> => apiFetch(`${BASE}/${id}/toggle`, { method: 'POST' })
export const addResponse = (id: number, r: Partial<MockResponse>): Promise<MockResponse> => postJson(`${BASE}/${id}/responses`, r)
export const deleteResponse = (responseId: number): Promise<void> => deleteReq(`${BASE}/responses/${responseId}`)
