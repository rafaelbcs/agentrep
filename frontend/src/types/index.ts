export type AgentTier = 'UNKNOWN' | 'EMERGING' | 'TRUSTED' | 'ELITE'

export interface CategoryScore {
  score: number
  count: number
}

export interface Agent {
  id: string
  walletAddress: string
  name: string
  description?: string
  tier: AgentTier
  score: number
  totalOutcomes: number
  successRate: number
  onChainSynced: boolean
  createdAt: string
  updatedAt: string
}

export interface ReputationResponse {
  agentAddress: string
  score: number
  tier: AgentTier
  totalOutcomes: number
  successRate: number
  totalValueTransacted: string
  firstSeenAt: string
  lastUpdatedAt: string
  categories: Record<string, CategoryScore>
  onChainVerified: boolean
  chainTxUrl: string
}

export interface OutcomeResponse {
  outcomeId: string
  status: 'EVALUATING' | 'RESOLVED' | 'DISPUTED'
  verdict?: 'SUCCESS' | 'FAILURE' | 'DISPUTED'
  llmJudgeReasoning?: string
  llmConfidence?: number
  scoreImpact?: string
  onChainTx?: string
  estimatedResolutionSeconds?: number
}

export interface DisputeResponse {
  disputeId: string
  outcomeId: string
  status: 'OPEN' | 'EVIDENCE_SUBMITTED' | 'RESOLVED' | 'EXPIRED'
  resolvedVerdict?: 'REQUESTER_WINS' | 'CONTRACTOR_WINS'
  resolvedReason?: string
  requiredCounterpartyStakeUsdc: string
  deadline?: string
  resolvedAt?: string
}

export interface AgentRegisterResponse {
  agentId: string
  walletAddress: string
  apiKey: string
  moltbookSkillSnippet: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
