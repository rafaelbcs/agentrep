import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ExternalLink, CheckCircle, ArrowLeft } from 'lucide-react'
import { api } from '@/lib/api'
import { TIER_CONFIG, truncateAddress, formatSuccessRate, CATEGORIES } from '@/lib/utils'
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
      <div className="max-w-4xl mx-auto px-4 py-20 text-center text-gray-400">
        Loading reputation data...
      </div>
    )
  }

  if (isError || !rep) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-20 text-center">
        <p className="text-gray-500 mb-4">Agent not found or not yet registered.</p>
        <Link to="/explore" className="text-blue-600 hover:underline">← Back to Explorer</Link>
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
      <Link to="/explore" className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-8">
        <ArrowLeft className="w-4 h-4" /> Back to Explorer
      </Link>

      {/* Header */}
      <div className="bg-white border rounded-2xl p-8 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center gap-6">
          <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-blue-400 to-purple-600 flex items-center justify-center text-white font-bold text-3xl shrink-0">
            {rep.agentAddress.slice(2, 4).toUpperCase()}
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-3 flex-wrap mb-1">
              <h1 className="text-2xl font-bold text-gray-900">
                {truncateAddress(rep.agentAddress, 6)}
              </h1>
              <span className={`text-xs font-semibold px-3 py-1 rounded-full ${tier.bg} ${tier.color}`}>
                {tier.label}
              </span>
              {rep.onChainVerified && (
                <span className="inline-flex items-center gap-1 text-xs text-green-600">
                  <CheckCircle className="w-3 h-3" /> On-chain verified
                </span>
              )}
            </div>
            <p className="text-gray-400 font-mono text-sm">{rep.agentAddress}</p>
          </div>
          <a
            href={rep.chainTxUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 text-sm text-gray-500 border rounded-lg px-4 py-2 hover:bg-gray-50 shrink-0"
          >
            <ExternalLink className="w-4 h-4" /> BaseScan
          </a>
        </div>

        {/* Score bar */}
        <div className="mt-8">
          <div className="flex items-end gap-2 mb-2">
            <span className="text-5xl font-bold text-gray-900">{rep.score}</span>
            <span className="text-gray-400 mb-1">/100</span>
            <span className="ml-auto text-sm text-gray-500">
              {formatSuccessRate(rep.successRate)} success · {rep.totalOutcomes} outcomes
            </span>
          </div>
          <div className="w-full bg-gray-100 rounded-full h-3">
            <div
              className="h-3 rounded-full bg-gradient-to-r from-blue-500 to-purple-600 transition-all"
              style={{ width: `${Math.min(rep.score, 100)}%` }}
            />
          </div>
        </div>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Score', value: rep.score },
          { label: 'Total Outcomes', value: rep.totalOutcomes.toLocaleString() },
          { label: 'Success Rate', value: formatSuccessRate(rep.successRate) },
          { label: 'Tier', value: tier.label },
        ].map(({ label, value }) => (
          <div key={label} className="bg-white border rounded-xl p-4 text-center">
            <p className="text-xs text-gray-400 mb-1">{label}</p>
            <p className="text-xl font-bold text-gray-900">{value}</p>
          </div>
        ))}
      </div>

      {/* Category scores */}
      {Object.keys(rep.categories || {}).length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white border rounded-2xl p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Category Scores</h2>
            <div className="space-y-3">
              {Object.entries(rep.categories).map(([cat, data]) => {
                const catLabel = CATEGORIES.find((c) => c.value === cat)?.label ?? cat
                return (
                  <div key={cat}>
                    <div className="flex justify-between text-sm mb-1">
                      <span className="text-gray-700">{catLabel}</span>
                      <span className="font-semibold">{data.score}/100 <span className="text-gray-400 font-normal">({data.count})</span></span>
                    </div>
                    <div className="w-full bg-gray-100 rounded-full h-1.5">
                      <div
                        className="h-1.5 rounded-full bg-blue-500"
                        style={{ width: `${data.score}%` }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>
          </div>

          <div className="bg-white border rounded-2xl p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Radar</h2>
            <ResponsiveContainer width="100%" height={220}>
              <RadarChart data={radarData}>
                <PolarGrid />
                <PolarAngleAxis dataKey="category" tick={{ fontSize: 11 }} />
                <Radar name="Score" dataKey="score" stroke="#3b82f6" fill="#3b82f6" fillOpacity={0.2} />
              </RadarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  )
}
