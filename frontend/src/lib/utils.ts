import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function truncateAddress(address: string, chars = 4): string {
  if (!address) return ''
  return `${address.slice(0, chars + 2)}...${address.slice(-chars)}`
}

export function formatScore(score: number): string {
  return score.toFixed(0)
}

export function formatSuccessRate(rate: number): string {
  return `${(rate * 100).toFixed(1)}%`
}

export const CATEGORIES = [
  { value: 'code-review', label: 'Code Review' },
  { value: 'data-analysis', label: 'Data Analysis' },
  { value: 'research', label: 'Research' },
  { value: 'content', label: 'Content' },
  { value: 'infra', label: 'Infrastructure' },
  { value: 'finance', label: 'Finance' },
  { value: 'trading', label: 'Trading' },
  { value: 'legal', label: 'Legal' },
  { value: 'ops', label: 'Operations' },
]

export const TIER_CONFIG = {
  UNKNOWN:  { color: 'text-slate-400',   bg: 'bg-slate-800/50',    label: 'Unknown',  border: 'border-slate-700/50', glow: '' },
  EMERGING: { color: 'text-blue-400',    bg: 'bg-blue-500/10',     label: 'Emerging', border: 'border-blue-500/25',  glow: 'glow-blue' },
  TRUSTED:  { color: 'text-emerald-400', bg: 'bg-emerald-500/10',  label: 'Trusted',  border: 'border-emerald-500/25', glow: 'glow-emerald' },
  ELITE:    { color: 'text-amber-400',   bg: 'bg-amber-500/10',    label: 'Elite',    border: 'border-amber-500/25', glow: 'glow-amber' },
} as const
