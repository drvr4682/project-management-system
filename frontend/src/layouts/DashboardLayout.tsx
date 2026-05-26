import React from 'react'
import { Outlet, Link } from 'react-router-dom'
import { useAppDispatch } from '@/hooks/store'
import { logout } from '@/features/auth/store/authSlice'
import { Logo } from '@/components/branding/Logo'
import { Button } from '@/components/ui/Button'

export const DashboardLayout: React.FC = () => {
  const dispatch = useAppDispatch()

  const handleLogout = () => {
    dispatch(logout())
  }

  return (
    <div className="flex flex-col min-h-screen bg-background">
      {/* Shared Premium Header */}
      <header className="border-b border-border bg-card/50 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-6">
            <Link to="/">
              <Logo size="sm" />
            </Link>
            <nav className="hidden md:flex items-center space-x-4">
              <Link
                to="/dashboard"
                className="text-sm font-bold text-muted-foreground hover:text-foreground transition-colors"
              >
                Dashboard
              </Link>
              <Link
                to="/projects"
                className="text-sm font-bold text-muted-foreground hover:text-foreground transition-colors"
              >
                Projects
              </Link>
              <Link
                to="/my-tasks"
                className="text-sm font-bold text-muted-foreground hover:text-foreground transition-colors"
              >
                My Tasks
              </Link>
            </nav>
          </div>

          <div className="flex items-center space-x-3">
            <div className="md:hidden flex items-center space-x-1">
              <Link to="/dashboard">
                <Button variant="ghost" size="sm" className="px-2">
                  Dashboard
                </Button>
              </Link>
              <Link to="/projects">
                <Button variant="ghost" size="sm" className="px-2">
                  Projects
                </Button>
              </Link>
              <Link to="/my-tasks">
                <Button variant="ghost" size="sm" className="px-2">
                  My Tasks
                </Button>
              </Link>
            </div>
            <Button variant="outline" size="sm" onClick={handleLogout}>
              Sign Out
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}

export default DashboardLayout
