import { apiFetch, postJson, putJson, deleteReq } from './client'
import type { Scenario } from '../types'

const BASE = '/api/scenarios'

export const getScenarios = (): Promise<Scenario[]> => apiFetch(BASE)
export const getScenario = (id: number): Promise<Scenario> => apiFetch(`${BASE}/${id}`)
export const createScenario = (s: Partial<Scenario>): Promise<Scenario> => postJson(BASE, s)
export const updateScenario = (id: number, s: Partial<Scenario>): Promise<Scenario> => putJson(`${BASE}/${id}`, s)
export const deleteScenario = (id: number): Promise<void> => deleteReq(`${BASE}/${id}`)
