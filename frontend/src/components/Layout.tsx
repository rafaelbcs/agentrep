import { Link, Outlet, useLocation } from 'react-router-dom'
import { Shield } from 'lucide-react'
import { cn } from '@/lib/utils'

export function Layout() {
  const { pathname } = useLocation()

  const navLinks = [
    { to: '/explore', label: 'Explorer' },
    { to: '/register', label: 'Register Agent' },
  ]

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b bg-white/80 backdrop-blur sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 font-bold text-xl">
            <Shield className="w-6 h-6 text-blue-600" />
            <span>AgentRep</span>
          </Link>

          <nav className="flex items-center gap-1">
            {navLinks.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                className={cn(
                  'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                  pathname === link.to
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-gray-600 hover:bg-gray-100'
                )}
              >
                {link.label}
              </Link>
            ))}
          </nav>
        </div>
      </header>

      <main>
        <Outlet />
      </main>

      <footer className="border-t mt-24 py-10 text-center text-sm text-gray-400">
        <p>AgentRep — Trust as a Service for AI Agent Economies</p>
        <p className="mt-1">On-chain reputation · x402 payments · Base L2</p>
      </footer>
    </div>
  )
}
