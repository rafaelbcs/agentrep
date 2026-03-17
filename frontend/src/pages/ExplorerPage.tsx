import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import { api } from '@/lib/api'
import { AgentCard } from '@/components/AgentCard'
import { CATEGORIES, cn } from '@/lib/utils'

export function ExplorerPage() {
  const [category, setCategory] = useState('all')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['explore', category, search, page],
    queryFn: () =>
      search.length > 1
        ? api.explore.search(search, { page, size: 20 })
        : api.explore.list({ category, page, size: 20 }),
  })

  return (
    <div className="max-w-7xl mx-auto px-4 py-12">
      {/* Header */}
      <div className="mb-10">
        <h1 className="text-3xl font-bold text-white mb-2">Explorer</h1>
        <p className="text-slate-500">Browse verified AI agents by reputation and category</p>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 mb-8">
        {/* Search */}
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-600" />
          <input
            type="text"
            placeholder="Search by name..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0) }}
            className="w-full pl-9 pr-4 py-2.5 bg-ar-surface border border-white/[0.07] rounded-xl text-sm text-slate-200 placeholder-slate-600 focus:outline-none focus:border-blue-500/40 focus:ring-1 focus:ring-blue-500/20 transition-all"
          />
        </div>

        {/* Category pills */}
        <div className="flex gap-2 flex-wrap">
          <button
            onClick={() => { setCategory('all'); setPage(0) }}
            className={cn(
              'px-4 py-2 rounded-xl text-xs font-medium border transition-all',
              category === 'all'
                ? 'bg-blue-500/15 text-blue-400 border-blue-500/30'
                : 'text-slate-500 border-white/[0.07] hover:text-slate-300 hover:border-white/[0.14] bg-ar-surface'
            )}
          >
            All
          </button>
          {CATEGORIES.map((cat) => (
            <button
              key={cat.value}
              onClick={() => { setCategory(cat.value); setPage(0) }}
              className={cn(
                'px-4 py-2 rounded-xl text-xs font-medium border transition-all',
                category === cat.value
                  ? 'bg-blue-500/15 text-blue-400 border-blue-500/30'
                  : 'text-slate-500 border-white/[0.07] hover:text-slate-300 hover:border-white/[0.14] bg-ar-surface'
              )}
            >
              {cat.label}
            </button>
          ))}
        </div>
      </div>

      {/* Results */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="bg-ar-surface border border-white/[0.07] rounded-xl h-64 animate-pulse" />
          ))}
        </div>
      ) : data?.content.length === 0 ? (
        <div className="text-center py-28">
          <div className="w-12 h-12 bg-ar-surface border border-white/[0.07] rounded-2xl flex items-center justify-center mx-auto mb-4">
            <Search className="w-5 h-5 text-slate-600" />
          </div>
          <p className="text-slate-400 font-medium">No agents found</p>
          <p className="text-slate-600 text-sm mt-1">Try a different category or search term</p>
        </div>
      ) : (
        <>
          {data && (
            <p className="text-xs text-slate-600 mb-5">
              {data.totalElements.toLocaleString()} agent{data.totalElements !== 1 ? 's' : ''}
            </p>
          )}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
            {data?.content.map((agent) => (
              <AgentCard key={agent.id} agent={agent} />
            ))}
          </div>

          {data && data.totalPages > 1 && (
            <div className="flex justify-center items-center gap-3 mt-12">
              <button
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
                className="px-4 py-2 bg-ar-surface border border-white/[0.07] rounded-lg text-sm text-slate-400 hover:text-slate-200 hover:border-white/[0.14] disabled:opacity-30 disabled:cursor-not-allowed transition-all"
              >
                ← Previous
              </button>
              <span className="px-4 py-2 text-sm text-slate-600 font-mono">
                {page + 1} / {data.totalPages}
              </span>
              <button
                disabled={page >= data.totalPages - 1}
                onClick={() => setPage(p => p + 1)}
                className="px-4 py-2 bg-ar-surface border border-white/[0.07] rounded-lg text-sm text-slate-400 hover:text-slate-200 hover:border-white/[0.14] disabled:opacity-30 disabled:cursor-not-allowed transition-all"
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
