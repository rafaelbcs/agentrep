import { Link, Outlet, useLocation } from 'react-router-dom'
import { Shield } from 'lucide-react'
import { cn } from '@/lib/utils'

export function Layout() {
  const { pathname } = useLocation()

  const navLinks = [
    { to: '/explore', label: 'Explorer' },
    { to: 'https://docs.agentrep.com.br', label: 'Docs', external: true },
    { to: '/register', label: 'Register Agent' },
  ]

  return (
    <div className="min-h-screen bg-ar-bg flex flex-col">
      {/* Navbar */}
      <header className="sticky top-0 z-50 border-b border-white/[0.06] bg-ar-bg/80 backdrop-blur-xl">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2.5 group">
            <div className="w-8 h-8 rounded-lg bg-blue-500/15 border border-blue-500/25 flex items-center justify-center transition-all group-hover:bg-blue-500/25">
              <Shield className="w-4 h-4 text-blue-400" />
            </div>
            <span className="font-bold text-lg tracking-tight text-white">
              Agent<span className="text-blue-400">Rep</span>
            </span>
          </Link>

          <nav className="flex items-center gap-1">
            {navLinks.map((link) =>
              link.external ? (
                <a
                  key={link.to}
                  href={link.to}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-4 py-2 rounded-lg text-sm font-medium transition-all text-slate-400 hover:text-slate-200 hover:bg-white/[0.05]"
                >
                  {link.label}
                </a>
              ) : (
                <Link
                  key={link.to}
                  to={link.to}
                  className={cn(
                    'px-4 py-2 rounded-lg text-sm font-medium transition-all',
                    pathname === link.to
                      ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-white/[0.05]'
                  )}
                >
                  {link.label}
                </Link>
              )
            )}
          </nav>
        </div>
      </header>

      {/* Content */}
      <main className="flex-1">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="border-t border-white/[0.06] mt-24 py-10">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Shield className="w-4 h-4 text-blue-400/60" />
            <span className="text-sm font-medium text-slate-500">AgentRep</span>
          </div>
          <p className="text-xs text-slate-600 text-center">
            Trust as a Service for AI Agent Economies · On-chain reputation · Base L2
            {' · '}
            <a
              href="https://docs.agentrep.com.br"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-slate-400 transition-colors"
            >
              Docs
            </a>
          </p>
          <div className="flex items-center gap-1">
            <span className="inline-block w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse-slow" />
            <span className="text-xs text-slate-600">Base Mainnet</span>
          </div>
        </div>
      </footer>
    </div>
  )
}
