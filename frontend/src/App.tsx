import { Routes, Route } from 'react-router-dom'
import { Layout } from '@/components/Layout'
import { LandingPage } from '@/pages/LandingPage'
import { ExplorerPage } from '@/pages/ExplorerPage'
import { AgentProfilePage } from '@/pages/AgentProfilePage'
import { RegisterPage } from '@/pages/RegisterPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<LandingPage />} />
        <Route path="/explore" element={<ExplorerPage />} />
        <Route path="/agent/:address" element={<AgentProfilePage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
    </Routes>
  )
}
