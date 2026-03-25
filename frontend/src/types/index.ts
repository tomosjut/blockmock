export type ProtocolType = 'HTTP'
export type PatternType = 'REQUEST_REPLY' | 'FIRE_FORGET' | 'PUB_SUB'
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'HEAD' | 'OPTIONS'
export type TestRunStatus = 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

export interface MockResponse {
  id?: number
  name: string
  priority: number
  responseStatusCode: number
  responseHeaders?: Record<string, string>
  responseBody?: string
  responseDelayMs?: number
  matchHeaders?: Record<string, string>
  matchQueryParams?: Record<string, string>
  matchBody?: string
}

export interface MockEndpoint {
  id?: number
  name: string
  description?: string
  protocol: ProtocolType
  pattern: PatternType
  enabled: boolean
  httpMethod?: HttpMethod
  httpPath?: string
  httpPathRegex?: boolean
  totalRequests?: number
  matchedRequests?: number
  unmatchedRequests?: number
  lastRequestAt?: string
  averageResponseTimeMs?: number
  responses: MockResponse[]
  createdAt?: string
  updatedAt?: string
}

export interface Block {
  id?: number
  name: string
  description?: string
  color?: string
  endpointCount?: number
  activeEndpointCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface TestExpectation {
  id?: number
  name: string
  mockEndpoint?: { id: number; name?: string; httpMethod?: string; httpPath?: string }
  minCallCount?: number
  maxCallCount?: number
  requiredBodyContains?: string
  requiredHeaders?: Record<string, string>
  expectationOrder?: number
}

export interface TestExpectationResult {
  id?: number
  testExpectation: { id: number }
  passed: boolean
  actualCallCount: number
  failureReason?: string
}

export interface TestRun {
  id?: number
  status: TestRunStatus
  startedAt?: string
  completedAt?: string
  results?: TestExpectationResult[]
}

export interface ScenarioResponseOverride {
  id?: number
  mockEndpoint?: { id: number; name?: string; httpMethod?: string; httpPath?: string }
  mockResponse?: { id: number; name?: string; responseStatusCode?: number }
}

export interface TestScenario {
  id?: number
  name: string
  description?: string
  expectations?: TestExpectation[]
  responseOverrides?: ScenarioResponseOverride[]
  createdAt?: string
  updatedAt?: string
}

export interface TestSuite {
  id?: number
  name: string
  description?: string
  color?: string
  blocks?: { id: number; name?: string }[]
  scenarios?: TestScenario[]
  createdAt?: string
  updatedAt?: string
}

export type TriggerType = 'HTTP' | 'CRON'

export interface TriggerConfig {
  id?: number
  name: string
  description?: string
  type: TriggerType
  testScenario?: { id: number; name?: string }
  httpUrl?: string
  httpMethod?: string
  httpBody?: string
  httpHeaders?: Record<string, string>
  cronExpression?: string
  enabled?: boolean
  lastFiredAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface TriggerFireResult {
  responseStatus?: number
  responseBody?: string
  error?: string
}

export interface RequestLog {
  id?: number
  protocol: ProtocolType
  requestMethod?: string
  requestPath?: string
  requestHeaders?: Record<string, string>
  requestQueryParams?: Record<string, string>
  requestBody?: string
  responseStatusCode?: number
  responseHeaders?: Record<string, string>
  responseBody?: string
  responseDelayMs?: number
  matched: boolean
  clientIp?: string
  receivedAt?: string
}
