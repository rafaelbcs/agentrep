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
  UNKNOWN: { color: 'text-gray-400', bg: 'bg-gray-100', label: 'Unknown', border: 'border-gray-200' },
  EMERGING: { color: 'text-blue-600', bg: 'bg-blue-50', label: 'Emerging', border: 'border-blue-200' },
  TRUSTED: { color: 'text-green-600', bg: 'bg-green-50', label: 'Trusted', border: 'border-green-200' },
  ELITE: { color: 'text-purple-600', bg: 'bg-purple-50', label: 'Elite', border: 'border-purple-200' },
} as const
