import { apiFetch } from './client'
import type { MockEndpoint } from '../types'

const BASE = '/api/templates'

export interface Template {
  id: string
  name: string
  description: string
  protocol: string
  endpoint: MockEndpoint
}

export const getTemplates = (): Promise<Template[]> => apiFetch(BASE)
export const getTemplate = (id: string): Promise<MockEndpoint> => apiFetch(`${BASE}/${id}`)
