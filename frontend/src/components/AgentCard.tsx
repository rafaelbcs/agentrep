import { Link } from 'react-router-dom'
import { ExternalLink, CheckCircle } from 'lucide-react'
import { cn, truncateAddress, formatSuccessRate, TIER_CONFIG } from '@/lib/utils'
import type { Agent } from '@/types'

interface AgentCardProps {
  agent: Agent
}

export function AgentCard({ agent }: AgentCardProps) {
  const tier = TIER_CONFIG[agent.tier]

  return (
    <div className="bg-white border rounded-xl p-6 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-full bg-gradient-to-br from-blue-400 to-purple-600 flex items-center justify-center text-white font-bold text-lg shrink-0">
            {(agent.name || '?')[0].toUpperCase()}
          </div>
          <div className="min-w-0">
            <h3 className="font-semibold text-gray-900 truncate">{agent.name || 'Unnamed Agent'}</h3>
            <p className="text-xs text-gray-400 font-mono">{truncateAddress(agent.walletAddress)}</p>
          </div>
        </div>
        <span className={cn('text-xs font-semibold px-2 py-1 rounded-full shrink-0', tier.bg, tier.color)}>
          {tier.label}
        </span>
      </div>

      <div className="flex items-end gap-1 mb-1">
        <span className="text-4xl font-bold text-gray-900">{Math.round(agent.score)}</span>
        <span className="text-gray-400 mb-1 text-sm">/100</span>
      </div>

      <div className="w-full bg-gray-100 rounded-full h-1.5 mb-4">
        <div
          className="h-1.5 rounded-full bg-gradient-to-r from-blue-500 to-purple-600"
          style={{ width: `${Math.min(agent.score, 100)}%` }}
        />
      </div>

      <div className="grid grid-cols-2 gap-3 mb-4 text-sm">
        <div>
          <p className="text-gray-400 text-xs">Success Rate</p>
          <p className="font-semibold">{formatSuccessRate(agent.successRate)}</p>
        </div>
        <div>
          <p className="text-gray-400 text-xs">Outcomes</p>
          <p className="font-semibold">{agent.totalOutcomes.toLocaleString()}</p>
        </div>
      </div>

      {agent.onChainSynced && (
        <div className="flex items-center gap-1 text-xs text-green-600 mb-3">
          <CheckCircle className="w-3 h-3" />
          <span>On-chain verified</span>
        </div>
      )}

      <div className="flex gap-2 mt-2">
        <a
          href={`https://basescan.org/address/${agent.walletAddress}`}
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700 border rounded-lg px-3 py-1.5 transition-colors"
        >
          <ExternalLink className="w-3 h-3" />
          On-chain
        </a>
        <Link
          to={`/agent/${agent.walletAddress}`}
          className="flex-1 text-center text-xs font-medium bg-blue-600 text-white rounded-lg px-3 py-1.5 hover:bg-blue-700 transition-colors"
        >
          View Profile
        </Link>
      </div>
    </div>
  )
}
