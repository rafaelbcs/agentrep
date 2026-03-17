import { Link } from 'react-router-dom'
import { ExternalLink, CheckCircle } from 'lucide-react'
import { cn, truncateAddress, formatSuccessRate, TIER_CONFIG } from '@/lib/utils'
import type { Agent } from '@/types'

interface AgentCardProps {
  agent: Agent
}

export function AgentCard({ agent }: AgentCardProps) {
  const tier = TIER_CONFIG[agent.tier]
  const initials = (agent.name || '?').slice(0, 2).toUpperCase()

  return (
    <div className={cn(
      'bg-ar-surface border border-white/[0.07] rounded-xl p-5',
      'hover:border-white/[0.14] hover:bg-ar-elevated transition-all duration-200',
      agent.tier === 'ELITE' && 'hover:glow-amber',
    )}>
      {/* Header */}
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500/30 to-indigo-600/30 border border-white/[0.1] flex items-center justify-center text-white font-bold text-sm shrink-0">
            {initials}
          </div>
          <div className="min-w-0">
            <h3 className="font-semibold text-slate-100 truncate text-sm">{agent.name || 'Unnamed Agent'}</h3>
            <p className="text-xs text-slate-500 font-mono mt-0.5">{truncateAddress(agent.walletAddress)}</p>
          </div>
        </div>
        <span className={cn(
          'text-xs font-semibold px-2 py-0.5 rounded-full border shrink-0',
          tier.bg, tier.color, tier.border
        )}>
          {tier.label}
        </span>
      </div>

      {/* Score */}
      <div className="flex items-end gap-1.5 mb-2">
        <span className="text-4xl font-bold text-white tabular-nums">{Math.round(agent.score)}</span>
        <span className="text-slate-600 mb-1 text-sm">/100</span>
      </div>

      {/* Score bar */}
      <div className="w-full bg-white/[0.06] rounded-full h-1 mb-4">
        <div
          className="h-1 rounded-full score-bar transition-all duration-500"
          style={{ width: `${Math.min(agent.score, 100)}%` }}
        />
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-3 mb-4 text-sm">
        <div>
          <p className="text-slate-600 text-xs mb-0.5">Success Rate</p>
          <p className="font-semibold text-slate-200">{formatSuccessRate(agent.successRate)}</p>
        </div>
        <div>
          <p className="text-slate-600 text-xs mb-0.5">Outcomes</p>
          <p className="font-semibold text-slate-200">{agent.totalOutcomes.toLocaleString()}</p>
        </div>
      </div>

      {/* On-chain badge */}
      {agent.onChainSynced && (
        <div className="flex items-center gap-1 text-xs text-emerald-400/80 mb-3">
          <CheckCircle className="w-3 h-3" />
          <span>On-chain verified</span>
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-2">
        <a
          href={`https://basescan.org/address/${agent.walletAddress}`}
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-1 text-xs text-slate-500 hover:text-slate-300 border border-white/[0.07] rounded-lg px-3 py-1.5 transition-colors hover:border-white/[0.14]"
        >
          <ExternalLink className="w-3 h-3" />
          BaseScan
        </a>
        <Link
          to={`/agent/${agent.walletAddress}`}
          className="flex-1 text-center text-xs font-medium bg-blue-600/80 text-white rounded-lg px-3 py-1.5 hover:bg-blue-600 transition-colors border border-blue-500/30"
        >
          View Profile
        </Link>
      </div>
    </div>
  )
}
