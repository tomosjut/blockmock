import { apiFetch, postJson, putJson, deleteReq } from './client'
import type { Block, MockEndpoint } from '../types'

const BASE = '/api/blocks'

export const getBlocks = (): Promise<Block[]> => apiFetch(BASE)
export const getBlock = (id: number): Promise<Block> => apiFetch(`${BASE}/${id}`)
export const createBlock = (b: Partial<Block>): Promise<Block> => postJson(BASE, b)
export const updateBlock = (id: number, b: Partial<Block>): Promise<Block> => putJson(`${BASE}/${id}`, b)
export const deleteBlock = (id: number): Promise<void> => deleteReq(`${BASE}/${id}`)
export const getBlockEndpoints = (id: number): Promise<MockEndpoint[]> => apiFetch(`${BASE}/${id}/endpoints`)
export const addEndpointToBlock = (id: number, endpointId: number): Promise<void> => apiFetch(`${BASE}/${id}/endpoints/${endpointId}`, { method: 'POST' })
export const removeEndpointFromBlock = (id: number, endpointId: number): Promise<void> => deleteReq(`${BASE}/${id}/endpoints/${endpointId}`)
export const startBlock = (id: number): Promise<void> => apiFetch(`${BASE}/${id}/start`, { method: 'POST' })
export const stopBlock = (id: number): Promise<void> => apiFetch(`${BASE}/${id}/stop`, { method: 'POST' })
