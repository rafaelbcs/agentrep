import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { CheckCircle, Copy, AlertCircle, Shield, ArrowRight } from 'lucide-react'
import { api } from '@/lib/api'
import { CATEGORIES, cn } from '@/lib/utils'
import type { AgentRegisterResponse } from '@/types'

export function RegisterPage() {
  const [form, setForm] = useState({
    agentAddress: '',
    name: '',
    description: '',
    ownerEmail: '',
    categories: [] as string[],
  })
  const [result, setResult] = useState<AgentRegisterResponse | null>(null)
  const [copied, setCopied] = useState(false)

  const mutation = useMutation({
    mutationFn: () => api.agents.register(form),
    onSuccess: (data) => {
      setResult(data)
      localStorage.setItem('agentrep_api_key', data.apiKey)
    },
  })

  const toggleCategory = (value: string) => {
    setForm((f) => ({
      ...f,
      categories: f.categories.includes(value)
        ? f.categories.filter((c) => c !== value)
        : [...f.categories, value],
    }))
  }

  const copyKey = (text: string) => {
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  /* ── Success screen ── */
  if (result) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-16">
        <div className="bg-ar-surface border border-emerald-500/20 rounded-2xl p-8 glow-emerald">
          <div className="flex items-center gap-4 mb-8">
            <div className="w-12 h-12 bg-emerald-500/15 border border-emerald-500/25 rounded-xl flex items-center justify-center shrink-0">
              <CheckCircle className="w-6 h-6 text-emerald-400" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-white">Agent Registered</h1>
              <p className="text-slate-500 text-sm mt-0.5">API key saved to local storage.</p>
            </div>
          </div>

          <div className="space-y-5">
            <div>
              <label className="text-xs font-semibold text-slate-600 uppercase tracking-widest">
                Wallet Address
              </label>
              <p className="font-mono text-sm mt-1.5 bg-ar-elevated border border-white/[0.07] rounded-xl px-4 py-3 text-slate-300">
                {result.walletAddress}
              </p>
            </div>

            <div>
              <label className="text-xs font-semibold text-slate-600 uppercase tracking-widest">
                API Key — save it now
              </label>
              <div className="flex items-center gap-2 mt-1.5">
                <code className="flex-1 bg-amber-500/5 border border-amber-500/20 rounded-xl px-4 py-3 text-sm font-mono break-all text-amber-300">
                  {result.apiKey}
                </code>
                <button
                  onClick={() => copyKey(result.apiKey)}
                  className="p-3 border border-white/[0.07] rounded-xl hover:bg-white/[0.05] transition-colors shrink-0"
                >
                  {copied
                    ? <CheckCircle className="w-4 h-4 text-emerald-400" />
                    : <Copy className="w-4 h-4 text-slate-500" />}
                </button>
              </div>
              <p className="text-xs text-slate-600 mt-1.5">This key will not be shown again.</p>
            </div>

            <div>
              <label className="text-xs font-semibold text-slate-600 uppercase tracking-widest">
                Moltbook Skill Snippet
              </label>
              <pre className="mt-1.5 bg-ar-elevated border border-white/[0.07] rounded-xl px-4 py-4 text-xs font-mono overflow-x-auto text-slate-300 leading-relaxed">
                {result.moltbookSkillSnippet}
              </pre>
            </div>
          </div>

          <div className="mt-8 flex gap-3">
            <button
              onClick={() => {
                setResult(null)
                setForm({ agentAddress: '', name: '', description: '', ownerEmail: '', categories: [] })
              }}
              className="flex-1 border border-white/[0.07] text-slate-400 rounded-xl py-2.5 text-sm font-medium hover:bg-white/[0.05] hover:text-slate-200 transition-all"
            >
              Register Another
            </button>
            <a
              href={`/agent/${result.walletAddress}`}
              className="flex-1 bg-blue-600 text-white text-center rounded-xl py-2.5 text-sm font-semibold hover:bg-blue-500 transition-all flex items-center justify-center gap-2"
            >
              View Profile <ArrowRight className="w-3.5 h-3.5" />
            </a>
          </div>
        </div>
      </div>
    )
  }

  /* ── Registration form ── */
  return (
    <div className="max-w-2xl mx-auto px-4 py-12">
      <div className="mb-10">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-10 h-10 bg-blue-500/10 border border-blue-500/20 rounded-xl flex items-center justify-center">
            <Shield className="w-5 h-5 text-blue-400" />
          </div>
          <h1 className="text-3xl font-bold text-white">Register Agent</h1>
        </div>
        <p className="text-slate-500 text-sm ml-13">
          Link your EVM wallet to start building verifiable on-chain reputation.
        </p>
      </div>

      <div className="bg-ar-surface border border-white/[0.07] rounded-2xl p-8 space-y-6">
        {mutation.isError && (
          <div className="flex items-start gap-3 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl px-4 py-3 text-sm">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{(() => {
              const err = mutation.error as any
              const data = err?.response?.data
              if (data?.fields) {
                return Object.values(data.fields).join(' · ')
              }
              return data?.error ?? data?.message ?? 'Registration failed. Please try again.'
            })()}</span>
          </div>
        )}

        {/* Wallet address */}
        <div>
          <label className="block text-xs font-semibold text-slate-500 uppercase tracking-widest mb-2">
            Agent Wallet Address <span className="text-red-400 normal-case tracking-normal">*</span>
          </label>
          <input
            type="text"
            placeholder="0x..."
            value={form.agentAddress}
            onChange={(e) => setForm({ ...form, agentAddress: e.target.value })}
            className="w-full bg-ar-elevated border border-white/[0.07] rounded-xl px-4 py-3 text-sm font-mono text-slate-200 placeholder-slate-700 focus:outline-none focus:border-blue-500/40 focus:ring-1 focus:ring-blue-500/20 transition-all"
          />
          <p className="text-xs text-slate-700 mt-1.5">EVM-compatible address (Base L2)</p>
        </div>

        {/* Name */}
        <div>
          <label className="block text-xs font-semibold text-slate-500 uppercase tracking-widest mb-2">
            Agent Name <span className="text-red-400 normal-case tracking-normal">*</span>
          </label>
          <input
            type="text"
            placeholder="e.g. CodeReview Pro Agent"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="w-full bg-ar-elevated border border-white/[0.07] rounded-xl px-4 py-3 text-sm text-slate-200 placeholder-slate-700 focus:outline-none focus:border-blue-500/40 focus:ring-1 focus:ring-blue-500/20 transition-all"
          />
        </div>

        {/* Description */}
        <div>
          <label className="block text-xs font-semibold text-slate-500 uppercase tracking-widest mb-2">
            Description
          </label>
          <textarea
            rows={3}
            placeholder="Describe what your agent does..."
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            className="w-full bg-ar-elevated border border-white/[0.07] rounded-xl px-4 py-3 text-sm text-slate-200 placeholder-slate-700 focus:outline-none focus:border-blue-500/40 focus:ring-1 focus:ring-blue-500/20 transition-all resize-none"
          />
        </div>

        {/* Email */}
        <div>
          <label className="block text-xs font-semibold text-slate-500 uppercase tracking-widest mb-2">
            Owner Email <span className="text-slate-700 normal-case tracking-normal font-normal">optional</span>
          </label>
          <input
            type="email"
            placeholder="you@example.com"
            value={form.ownerEmail}
            onChange={(e) => setForm({ ...form, ownerEmail: e.target.value })}
            className="w-full bg-ar-elevated border border-white/[0.07] rounded-xl px-4 py-3 text-sm text-slate-200 placeholder-slate-700 focus:outline-none focus:border-blue-500/40 focus:ring-1 focus:ring-blue-500/20 transition-all"
          />
        </div>

        {/* Categories */}
        <div>
          <label className="block text-xs font-semibold text-slate-500 uppercase tracking-widest mb-3">
            Specialization Categories
          </label>
          <div className="flex flex-wrap gap-2">
            {CATEGORIES.map((cat) => (
              <button
                key={cat.value}
                type="button"
                onClick={() => toggleCategory(cat.value)}
                className={cn(
                  'px-3 py-1.5 rounded-lg text-xs font-medium border transition-all',
                  form.categories.includes(cat.value)
                    ? 'bg-blue-500/15 text-blue-400 border-blue-500/30'
                    : 'text-slate-500 border-white/[0.07] hover:text-slate-300 hover:border-white/[0.14] bg-ar-elevated'
                )}
              >
                {cat.label}
              </button>
            ))}
          </div>
        </div>

        {/* Submit */}
        <button
          onClick={() => mutation.mutate()}
          disabled={!form.agentAddress || !form.name || mutation.isPending}
          className="w-full bg-blue-600 text-white rounded-xl py-3 font-semibold text-sm hover:bg-blue-500 disabled:opacity-40 disabled:cursor-not-allowed transition-all border border-blue-500/30 shadow-lg shadow-blue-600/10 flex items-center justify-center gap-2"
        >
          {mutation.isPending ? (
            <>
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              Registering...
            </>
          ) : (
            <>Register Agent <ArrowRight className="w-4 h-4" /></>
          )}
        </button>
      </div>
    </div>
  )
}
