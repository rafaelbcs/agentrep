import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ExternalLink, CheckCircle, ArrowLeft, Shield } from 'lucide-react'
import { api } from '@/lib/api'
import { TIER_CONFIG, truncateAddress, formatSuccessRate, CATEGORIES, cn } from '@/lib/utils'
import { RadarChart, Radar, PolarGrid, PolarAngleAxis, ResponsiveContainer } from 'recharts'

export function AgentProfilePage() {
  const { address } = useParams<{ address: string }>()

  const { data: rep, isLoading, isError } = useQuery({
    queryKey: ['reputation', address],
    queryFn: () => api.reputation.get(address!),
    enabled: !!address,
  })

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-20">
        <div className="space-y-4 animate-pulse">
          <div className="h-32 bg-ar-surface border border-white/[0.07] rounded-2xl" />
          <div className="grid grid-cols-4 gap-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-20 bg-ar-surface border border-white/[0.07] rounded-xl" />
            ))}
          </div>
        </div>
      </div>
    )
  }

  if (isError || !rep) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-20 text-center">
        <div className="w-16 h-16 bg-ar-surface border border-white/[0.07] rounded-2xl flex items-center justify-center mx-auto mb-4">
          <Shield className="w-8 h-8 text-slate-600" />
        </div>
        <p className="text-slate-400 mb-2 font-medium">Agent not found</p>
        <p className="text-slate-600 text-sm mb-6">This address is not yet registered on AgentRep</p>
        <Link to="/explore" className="text-blue-400 hover:text-blue-300 text-sm transition-colors">
          ← Back to Explorer
        </Link>
      </div>
    )
  }

  const tier = TIER_CONFIG[rep.tier]

  const radarData = CATEGORIES.map((cat) => ({
    category: cat.label,
    score: rep.categories?.[cat.value]?.score ?? 0,
  }))

  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <Link
        to="/explore"
        className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-300 mb-8 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" /> Back to Explorer
      </Link>

      {/* Header card */}
      <div className={cn(
        'bg-ar-surface border border-white/[0.07] rounded-2xl p-8 mb-5 transition-all',
        tier.glow && tier.glow,
      )}>
        <div className="flex flex-col sm:flex-row sm:items-center gap-6">
          {/* Avatar */}
          <div className="w-18 h-18 shrink-0">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-blue-500/30 to-indigo-600/30 border border-white/[0.1] flex items-center justify-center text-white font-bold text-2xl">
              {rep.agentAddress.slice(2, 4).toUpperCase()}
            </div>
          </div>

          {/* Info */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-3 flex-wrap mb-1.5">
              <h1 className="text-xl font-bold text-white">
                {truncateAddress(rep.agentAddress, 6)}
              </h1>
              <span className={cn(
                'text-xs font-semibold px-2.5 py-1 rounded-full border',
                tier.bg, tier.color, tier.border
              )}>
                {tier.label}
              </span>
              {rep.onChainVerified && (
                <span className="inline-flex items-center gap-1 text-xs text-emerald-400/80">
                  <CheckCircle className="w-3 h-3" /> Verified on-chain
                </span>
              )}
            </div>
            <p className="text-slate-500 font-mono text-sm">{rep.agentAddress}</p>
          </div>

          {/* BaseScan link */}
          <a
            href={rep.chainTxUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-300 border border-white/[0.07] hover:border-white/[0.14] rounded-xl px-4 py-2.5 transition-all shrink-0"
          >
            <ExternalLink className="w-3.5 h-3.5" /> BaseScan
          </a>
        </div>

        {/* Score bar */}
        <div className="mt-8">
          <div className="flex items-end gap-2 mb-3">
            <span className="text-6xl font-bold text-white tabular-nums leading-none">{rep.score}</span>
            <span className="text-slate-600 mb-1.5 text-sm">/100</span>
            <span className="ml-auto text-sm text-slate-500">
              {formatSuccessRate(rep.successRate)} success · {rep.totalOutcomes} outcomes
            </span>
          </div>
          <div className="w-full bg-white/[0.06] rounded-full h-2">
            <div
              className="h-2 rounded-full score-bar transition-all duration-700"
              style={{ width: `${Math.min(rep.score, 100)}%` }}
            />
          </div>
        </div>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-5">
        {[
          { label: 'Score', value: rep.score },
          { label: 'Total Outcomes', value: rep.totalOutcomes.toLocaleString() },
          { label: 'Success Rate', value: formatSuccessRate(rep.successRate) },
          { label: 'Tier', value: <span className={tier.color}>{tier.label}</span> },
        ].map(({ label, value }) => (
          <div key={label} className="bg-ar-surface border border-white/[0.07] rounded-xl p-4 text-center">
            <p className="text-xs text-slate-600 mb-1.5 uppercase tracking-wider">{label}</p>
            <p className="text-xl font-bold text-white">{value}</p>
          </div>
        ))}
      </div>

      {/* Category scores + radar */}
      {Object.keys(rep.categories || {}).length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {/* Category bars */}
          <div className="bg-ar-surface border border-white/[0.07] rounded-2xl p-6">
            <h2 className="text-sm font-semibold text-slate-300 mb-5 uppercase tracking-wider">Category Scores</h2>
            <div className="space-y-4">
              {Object.entries(rep.categories).map(([cat, data]) => {
                const catLabel = CATEGORIES.find((c) => c.value === cat)?.label ?? cat
                return (
                  <div key={cat}>
                    <div className="flex justify-between text-sm mb-1.5">
                      <span className="text-slate-400 text-xs">{catLabel}</span>
                      <span className="text-xs font-semibold text-slate-300 tabular-nums">
                        {data.score}<span className="text-slate-600 font-normal">/100</span>
                        <span className="text-slate-600 font-normal ml-1.5">({data.count})</span>
                      </span>
                    </div>
                    <div className="w-full bg-white/[0.05] rounded-full h-1">
                      <div
                        className="h-1 rounded-full score-bar"
                        style={{ width: `${data.score}%` }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>
          </div>

          {/* Radar */}
          <div className="bg-ar-surface border border-white/[0.07] rounded-2xl p-6">
            <h2 className="text-sm font-semibold text-slate-300 mb-5 uppercase tracking-wider">Radar</h2>
            <ResponsiveContainer width="100%" height={220}>
              <RadarChart data={radarData}>
                <PolarGrid stroke="rgba(255,255,255,0.07)" />
                <PolarAngleAxis
                  dataKey="category"
                  tick={{ fontSize: 10, fill: '#64748b' }}
                />
                <Radar
                  name="Score"
                  dataKey="score"
                  stroke="#3b82f6"
                  fill="#3b82f6"
                  fillOpacity={0.15}
                  strokeWidth={1.5}
                />
              </RadarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  )
}
