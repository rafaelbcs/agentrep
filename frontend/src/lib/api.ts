import axios from 'axios'
import type {
  Agent,
  AgentRegisterResponse,
  DisputeResponse,
  OutcomeResponse,
  PageResponse,
  ReputationResponse,
} from '@/types'

const client = axios.create({
  baseURL: (import.meta as any).env?.VITE_API_URL || '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config) => {
  const apiKey = localStorage.getItem('agentrep_api_key')
  if (apiKey) config.headers['X-API-Key'] = apiKey
  return config
})

export const api = {
  agents: {
    register: (data: {
      agentAddress: string
      name: string
      description?: string
      ownerEmail?: string
      categories?: string[]
    }) => client.post<AgentRegisterResponse>('/agents/register', data).then((r) => r.data),
  },

  reputation: {
    get: (address: string) =>
      client.get<ReputationResponse>(`/reputation/${address}`).then((r) => r.data),
    bulk: (addresses: string[]) =>
      client.post<ReputationResponse[]>('/reputation/bulk', addresses).then((r) => r.data),
  },

  explore: {
    list: (params: { category?: string; minScore?: number; page?: number; size?: number }) =>
      client.get<PageResponse<Agent>>('/explore', { params }).then((r) => r.data),
    leaderboard: (params?: { page?: number; size?: number }) =>
      client.get<PageResponse<Agent>>('/explore/leaderboard', { params }).then((r) => r.data),
    search: (q: string, params?: { page?: number; size?: number }) =>
      client.get<PageResponse<Agent>>('/explore/search', { params: { q, ...params } }).then((r) => r.data),
  },

  outcomes: {
    register: (data: {
      contractorAgentAddress: string
      requesterAgentAddress: string
      taskDescription: string
      taskCategory: string
      deliverableUrl?: string
      deliverableHash?: string
      valueUsdc: number
      txHash?: string
    }) => client.post<OutcomeResponse>('/outcome', data).then((r) => r.data),
    get: (id: string) => client.get<OutcomeResponse>(`/outcome/${id}`).then((r) => r.data),
  },

  disputes: {
    open: (data: {
      outcomeId: string
      reason: string
      evidenceUrl?: string
      stakePaymentTxHash: string
    }) => client.post<DisputeResponse>('/disputes', data).then((r) => r.data),
    get: (id: string) => client.get<DisputeResponse>(`/disputes/${id}`).then((r) => r.data),
    resolve: (id: string, data: { verdict: 'REQUESTER_WINS' | 'CONTRACTOR_WINS'; reason: string }) =>
      client.post<DisputeResponse>(`/disputes/${id}/resolve`, data).then((r) => r.data),
  },
}
