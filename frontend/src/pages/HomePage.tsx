import React from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/store'
import { logout, selectAuth } from '@/features/auth/store/authSlice'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Link } from 'react-router-dom'

import { Logo } from '@/components/branding/Logo'

export const HomePage: React.FC = () => {
  const dispatch = useAppDispatch()
  const { user } = useAppSelector(selectAuth)

  const handleLogout = () => {
    dispatch(logout())
  }

  return (
    <div className="flex flex-col min-h-screen bg-background">
      {/* Premium Header */}
      <header className="border-b border-border bg-card/50 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-6">
            <Logo size="sm" />
            <nav className="hidden md:flex items-center space-x-4">
              <Link
                to="/projects"
                className="text-sm font-bold text-muted-foreground hover:text-foreground transition-colors"
              >
                Projects
              </Link>
            </nav>
          </div>
          
          <div className="flex items-center space-x-3">
            <Link to="/projects" className="md:hidden">
              <Button variant="ghost" size="sm">
                Projects
              </Button>
            </Link>
            <Button variant="outline" size="sm" onClick={handleLogout}>
              Sign Out
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-4 py-8 flex items-center justify-center">
        <div className="w-full max-w-2xl space-y-6">
          <div className="space-y-2 text-center md:text-left">
            <h1 className="text-4xl font-extrabold tracking-tight text-foreground sm:text-5xl">
              Welcome back, <span className="text-primary">{user?.name || 'User'}</span>!
            </h1>
            <p className="text-muted-foreground text-lg">
              This is a secure page protected by active JWT authorization.
            </p>
            <div className="pt-2 flex justify-center md:justify-start">
              <Link to="/projects">
                <Button className="font-semibold shadow-md">
                  Go to Projects Workspace
                </Button>
              </Link>
            </div>
          </div>

          <Card className="border border-border bg-card shadow-md">
            <CardHeader>
              <CardTitle>User Session Details</CardTitle>
              <CardDescription>
                Session profile values loaded from local storage hydration.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="p-4 rounded-lg bg-muted/50 border border-border">
                  <span className="text-xs text-muted-foreground uppercase font-bold block mb-1">
                    User Identifier
                  </span>
                  <span className="text-foreground font-mono text-sm">
                    {user?.id || 'N/A'}
                  </span>
                </div>
                <div className="p-4 rounded-lg bg-muted/50 border border-border">
                  <span className="text-xs text-muted-foreground uppercase font-bold block mb-1">
                    Full Name
                  </span>
                  <span className="text-foreground font-semibold">
                    {user?.name || 'N/A'}
                  </span>
                </div>
                <div className="p-4 rounded-lg bg-muted/50 border border-border">
                  <span className="text-xs text-muted-foreground uppercase font-bold block mb-1">
                    Email Address
                  </span>
                  <span className="text-foreground">
                    {user?.email || 'N/A'}
                  </span>
                </div>
                <div className="p-4 rounded-lg bg-muted/50 border border-border">
                  <span className="text-xs text-muted-foreground uppercase font-bold block mb-1">
                    Assigned Role
                  </span>
                  <div className="flex items-center space-x-1.5 mt-0.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-primary animate-pulse"></span>
                    <span className="text-foreground font-semibold text-sm">
                      {user?.role || 'N/A'}
                    </span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  )
}
