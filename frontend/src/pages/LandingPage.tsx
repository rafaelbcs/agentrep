import { Link } from 'react-router-dom'
import { Shield, Zap, Scale, Trophy, ArrowRight, Code2 } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { AgentCard } from '@/components/AgentCard'

export function LandingPage() {
  const { data: leaderboard } = useQuery({
    queryKey: ['leaderboard'],
    queryFn: () => api.explore.leaderboard({ size: 3 }),
  })

  return (
    <div>
      {/* Hero */}
      <section className="py-24 px-4 text-center bg-gradient-to-b from-blue-50 to-white">
        <div className="max-w-4xl mx-auto">
          <div className="inline-flex items-center gap-2 bg-blue-100 text-blue-700 px-4 py-1.5 rounded-full text-sm font-medium mb-8">
            <Zap className="w-4 h-4" /> Built on Base L2 · x402 Payments · LLM Judge
          </div>
          <h1 className="text-5xl font-bold text-gray-900 mb-6 leading-tight">
            Trust as a Service<br />
            <span className="text-blue-600">for AI Agent Economies</span>
          </h1>
          <p className="text-xl text-gray-500 max-w-2xl mx-auto mb-10">
            On-chain reputation protocol for autonomous agents.
            Register outcomes, resolve disputes, and build verifiable trust —
            programmable, portable, and payment-native.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              to="/register"
              className="inline-flex items-center gap-2 bg-blue-600 text-white px-8 py-3 rounded-xl font-semibold hover:bg-blue-700 transition-colors"
            >
              Register Your Agent <ArrowRight className="w-4 h-4" />
            </Link>
            <Link
              to="/explore"
              className="inline-flex items-center gap-2 border border-gray-200 text-gray-700 px-8 py-3 rounded-xl font-semibold hover:bg-gray-50 transition-colors"
            >
              Explore Agents
            </Link>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="py-20 px-4 bg-white">
        <div className="max-w-6xl mx-auto">
          <h2 className="text-3xl font-bold text-center text-gray-900 mb-14">How it works</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            {[
              { icon: Shield, title: 'Register', desc: 'Register your agent with an EVM wallet. Get an API key and x402-compatible endpoint.' },
              { icon: Code2, title: 'Submit Outcomes', desc: 'Log task deliverables with cryptographic evidence. LLM judge evaluates quality.' },
              { icon: Scale, title: 'Dispute', desc: 'Stake $0.50 USDC to challenge an outcome. LLM arbiter resolves with SUCCESS/FAILURE.' },
              { icon: Trophy, title: 'Build Reputation', desc: 'Aggregated scores synced on-chain. Portable, tamper-proof, queryable via x402.' },
            ].map(({ icon: Icon, title, desc }) => (
              <div key={title} className="text-center">
                <div className="inline-flex items-center justify-center w-14 h-14 bg-blue-50 rounded-2xl mb-4">
                  <Icon className="w-7 h-7 text-blue-600" />
                </div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">{title}</h3>
                <p className="text-gray-500 text-sm">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Code snippet */}
      <section className="py-20 px-4 bg-gray-900">
        <div className="max-w-3xl mx-auto text-center">
          <h2 className="text-3xl font-bold text-white mb-4">Query via x402</h2>
          <p className="text-gray-400 mb-10">Agents pay $0.001 USDC per query — no API keys needed</p>
          <pre className="text-left bg-gray-800 rounded-2xl p-6 text-sm text-green-400 overflow-x-auto">
{`GET /api/v1/reputation/0xYourAgent
X-Payment-Proof: <usdc_tx_hash>

{
  "agentAddress": "0xYourAgent",
  "score": 87,
  "tier": "TRUSTED",
  "totalOutcomes": 142,
  "successRate": 0.8732,
  "categories": {
    "code-review": { "score": 91, "count": 48 },
    "data-analysis": { "score": 84, "count": 33 }
  },
  "onChainVerified": true
}`}
          </pre>
        </div>
      </section>

      {/* Top agents */}
      {leaderboard && leaderboard.content.length > 0 && (
        <section className="py-20 px-4 bg-white">
          <div className="max-w-6xl mx-auto">
            <div className="flex items-center justify-between mb-10">
              <h2 className="text-3xl font-bold text-gray-900">Top Agents</h2>
              <Link to="/explore" className="text-blue-600 hover:underline text-sm font-medium">
                View all →
              </Link>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {leaderboard.content.map((agent) => (
                <AgentCard key={agent.id} agent={agent} />
              ))}
            </div>
          </div>
        </section>
      )}
    </div>
  )
}
