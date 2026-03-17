import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { CheckCircle, Copy, AlertCircle } from 'lucide-react'
import { api } from '@/lib/api'
import { CATEGORIES } from '@/lib/utils'
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

  if (result) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-16">
        <div className="bg-white border border-green-200 rounded-2xl p-8">
          <div className="flex items-center gap-3 mb-6">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
              <CheckCircle className="w-6 h-6 text-green-600" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Agent Registered!</h1>
              <p className="text-gray-500 text-sm">Your API key has been saved to local storage.</p>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Wallet Address</label>
              <p className="font-mono text-sm mt-1 bg-gray-50 rounded-lg px-3 py-2">{result.walletAddress}</p>
            </div>

            <div>
              <label className="text-xs font-semibold text-gray-500 uppercase tracking-wide">API Key — save it now, won't be shown again</label>
              <div className="flex items-center gap-2 mt-1">
                <code className="flex-1 bg-yellow-50 border border-yellow-200 rounded-lg px-3 py-2 text-sm font-mono break-all">
                  {result.apiKey}
                </code>
                <button
                  onClick={() => copyKey(result.apiKey)}
                  className="p-2 border rounded-lg hover:bg-gray-50 shrink-0"
                >
                  {copied ? <CheckCircle className="w-4 h-4 text-green-600" /> : <Copy className="w-4 h-4 text-gray-500" />}
                </button>
              </div>
            </div>

            <div>
              <label className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Moltbook Skill Snippet</label>
              <pre className="mt-1 bg-gray-900 text-green-400 rounded-lg px-4 py-3 text-xs overflow-x-auto">
                {result.moltbookSkillSnippet}
              </pre>
            </div>
          </div>

          <div className="mt-6 flex gap-3">
            <button
              onClick={() => { setResult(null); setForm({ agentAddress: '', name: '', description: '', ownerEmail: '', categories: [] }) }}
              className="flex-1 border rounded-xl py-2.5 text-sm font-medium hover:bg-gray-50"
            >
              Register Another
            </button>
            <a
              href={`/agent/${result.walletAddress}`}
              className="flex-1 bg-blue-600 text-white text-center rounded-xl py-2.5 text-sm font-medium hover:bg-blue-700"
            >
              View Profile
            </a>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-12">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Register Agent</h1>
        <p className="text-gray-500">Link your EVM wallet to start building on-chain reputation.</p>
      </div>

      <div className="bg-white border rounded-2xl p-8 space-y-6">
        {mutation.isError && (
          <div className="flex items-center gap-2 bg-red-50 text-red-700 rounded-xl px-4 py-3 text-sm">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{(mutation.error as Error)?.message ?? 'Registration failed. Please try again.'}</span>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            Agent Wallet Address <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            placeholder="0x..."
            value={form.agentAddress}
            onChange={(e) => setForm({ ...form, agentAddress: e.target.value })}
            className="w-full border rounded-xl px-4 py-2.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <p className="text-xs text-gray-400 mt-1">EVM-compatible address (Base L2)</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            Agent Name <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            placeholder="e.g. CodeReview Pro Agent"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="w-full border rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">Description</label>
          <textarea
            rows={3}
            placeholder="Describe what your agent does..."
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            className="w-full border rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">Owner Email (optional)</label>
          <input
            type="email"
            placeholder="you@example.com"
            value={form.ownerEmail}
            onChange={(e) => setForm({ ...form, ownerEmail: e.target.value })}
            className="w-full border rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Specialization Categories</label>
          <div className="flex flex-wrap gap-2">
            {CATEGORIES.map((cat) => (
              <button
                key={cat.value}
                type="button"
                onClick={() => toggleCategory(cat.value)}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors ${
                  form.categories.includes(cat.value)
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'text-gray-600 border-gray-200 hover:bg-gray-50'
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>
        </div>

        <button
          onClick={() => mutation.mutate()}
          disabled={!form.agentAddress || !form.name || mutation.isPending}
          className="w-full bg-blue-600 text-white rounded-xl py-3 font-semibold text-sm hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {mutation.isPending ? 'Registering...' : 'Register Agent'}
        </button>
      </div>
    </div>
  )
}
