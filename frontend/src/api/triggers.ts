import { apiFetch, postJson, putJson, deleteReq } from './client'
import type { TriggerConfig, TriggerConfigForm, TriggerFireResult } from '../types'

const BASE = '/api/triggers'

export const getTriggers = (): Promise<TriggerConfig[]> => apiFetch(BASE)
export const getTrigger = (id: number): Promise<TriggerConfig> => apiFetch(`${BASE}/${id}`)
export const createTrigger = (t: TriggerConfigForm): Promise<TriggerConfig> => postJson(BASE, t)
export const updateTrigger = (id: number, t: TriggerConfigForm): Promise<TriggerConfig> => putJson(`${BASE}/${id}`, t)
export const deleteTrigger = (id: number): Promise<void> => deleteReq(`${BASE}/${id}`)
export const fireTrigger = (id: number): Promise<TriggerFireResult> => apiFetch(`${BASE}/${id}/fire`, { method: 'POST' })
