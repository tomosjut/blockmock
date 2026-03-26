export type ProtocolType = 'HTTP' | 'AMQP' | 'AMQPS'
export type PatternType = 'REQUEST_REPLY' | 'FIRE_FORGET' | 'PUB_SUB'
export type AmqpPattern = 'RECEIVE' | 'PUBLISH' | 'REQUEST_REPLY'
export type AmqpRoutingType = 'ANYCAST' | 'MULTICAST'
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'HEAD' | 'OPTIONS'
export type TestRunStatus = 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
export type TriggerType = 'HTTP' | 'CRON' | 'AMQP'

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

interface MockEndpointBase {
  id?: number
  name: string
  description?: string
  protocol: ProtocolType
  pattern: PatternType
  enabled: boolean
  totalRequests?: number
  matchedRequests?: number
  unmatchedRequests?: number
  lastRequestAt?: string
  averageResponseTimeMs?: number
  responses: MockResponse[]
  createdAt?: string
  updatedAt?: string
}

export interface HttpMockEndpoint extends MockEndpointBase {
  protocol: 'HTTP'
  httpMethod?: HttpMethod
  httpPath?: string
  httpPathRegex?: boolean
}

export interface AmqpMockEndpoint extends MockEndpointBase {
  protocol: 'AMQP' | 'AMQPS'
  amqpAddress?: string
  amqpPattern?: AmqpPattern
  amqpRoutingType?: AmqpRoutingType
}

export type MockEndpoint = HttpMockEndpoint | AmqpMockEndpoint

export function isHttpEndpoint(ep: MockEndpoint): ep is HttpMockEndpoint {
  return ep.protocol === 'HTTP'
}

export function isAmqpEndpoint(ep: MockEndpoint): ep is AmqpMockEndpoint {
  return ep.protocol === 'AMQP' || ep.protocol === 'AMQPS'
}

/** Flat form type for creating/editing endpoints — all protocol-specific fields optional */
export interface MockEndpointForm {
  id?: number
  name?: string
  description?: string
  protocol?: ProtocolType
  pattern?: PatternType
  enabled?: boolean
  httpMethod?: HttpMethod
  httpPath?: string
  httpPathRegex?: boolean
  amqpAddress?: string
  amqpPattern?: AmqpPattern
  amqpRoutingType?: AmqpRoutingType
  responses?: MockResponse[]
  totalRequests?: number
  matchedRequests?: number
  unmatchedRequests?: number
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

interface TriggerConfigBase {
  id?: number
  name: string
  description?: string
  type: TriggerType
  testScenario?: { id: number; name?: string }
  enabled?: boolean
  lastFiredAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface HttpTriggerConfig extends TriggerConfigBase {
  type: 'HTTP'
  httpUrl?: string
  httpMethod?: string
  httpBody?: string
  httpHeaders?: Record<string, string>
}

export interface CronTriggerConfig extends TriggerConfigBase {
  type: 'CRON'
  cronExpression?: string
}

export interface AmqpTriggerConfig extends TriggerConfigBase {
  type: 'AMQP'
  amqpAddress?: string
  amqpBody?: string
  amqpProperties?: Record<string, string>
  amqpRoutingType?: AmqpRoutingType
}

export type TriggerConfig = HttpTriggerConfig | CronTriggerConfig | AmqpTriggerConfig

export function isHttpTrigger(t: TriggerConfig): t is HttpTriggerConfig {
  return t.type === 'HTTP'
}

export function isCronTrigger(t: TriggerConfig): t is CronTriggerConfig {
  return t.type === 'CRON'
}

export function isAmqpTrigger(t: TriggerConfig): t is AmqpTriggerConfig {
  return t.type === 'AMQP'
}

/** Flat form type for creating/editing triggers — all type-specific fields optional */
export interface TriggerConfigForm {
  id?: number
  name?: string
  description?: string
  type?: TriggerType
  testScenario?: { id: number; name?: string }
  enabled?: boolean
  lastFiredAt?: string
  httpUrl?: string
  httpMethod?: string
  httpBody?: string
  httpHeaders?: Record<string, string>
  cronExpression?: string
  amqpAddress?: string
  amqpBody?: string
  amqpProperties?: Record<string, string>
  amqpRoutingType?: AmqpRoutingType
}

export interface DashboardScenarioSummary {
  id: number
  name: string
  lastRunStatus?: string
  lastRunAt?: string
  activeRun: boolean
  lastRunPassed: number
  lastRunTotal: number
}

export interface DashboardSuiteSummary {
  id: number
  name: string
  color?: string
  scenarios: DashboardScenarioSummary[]
}

export interface DashboardRecentRun {
  id: number
  suiteId: number
  suiteName: string
  suiteColor?: string
  scenarioId: number
  scenarioName: string
  status: string
  startedAt?: string
  completedAt?: string
  passed: number
  total: number
}

export interface DashboardRecentFire {
  id: number
  name: string
  type: string
  scenarioName?: string
  firedAt?: string
}

export interface DashboardStats {
  endpointCount: number
  activeEndpointCount: number
  matchedRequests: number
  unmatchedRequests: number
  suites: DashboardSuiteSummary[]
  recentRuns: DashboardRecentRun[]
  recentFires: DashboardRecentFire[]
}

export interface TriggerFireResult {
  responseStatus?: number
  responseBody?: string
  error?: string
  messageId?: string
  firedAt?: string
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
  amqpAddress?: string
  amqpSubject?: string
  amqpMessageId?: string
  amqpCorrelationId?: string
  amqpReplyTo?: string
  amqpProperties?: Record<string, string>
}
