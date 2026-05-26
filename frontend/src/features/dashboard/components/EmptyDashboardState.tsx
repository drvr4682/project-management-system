import React from 'react'
import { Link } from 'react-router-dom'
import { Plus, FolderPlus, Sparkles, Users, Layers } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'

export const EmptyDashboardState: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto py-12 px-4 space-y-8">
      {/* Welcome Hero Card */}
      <Card className="border border-border/80 bg-card overflow-hidden shadow-md">
        <CardContent className="p-8 md:p-12 relative flex flex-col md:flex-row items-center justify-between gap-8">
          <div className="space-y-4 max-w-lg text-center md:text-left z-10">
            <div className="inline-flex items-center space-x-2 text-primary font-bold">
              <Sparkles size={16} />
              <span className="text-xs uppercase tracking-wider font-extrabold">Getting Started</span>
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight text-foreground md:text-4xl">
              Welcome to DRVRHub
            </h1>
            <p className="text-sm text-muted-foreground leading-relaxed font-semibold">
              DRVRHub is your collaborative, role-based productivity workspace. Organize deliverables, set milestones, delegate responsibilities, and monitor team performance seamlessly.
            </p>
            <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center md:justify-start">
              <Link to="/projects/create">
                <Button className="w-full sm:w-auto font-bold rounded-xl flex items-center justify-center space-x-2 shadow-sm shadow-primary/20">
                  <Plus size={16} />
                  <span>Create First Workspace</span>
                </Button>
              </Link>
            </div>
          </div>

          <div className="shrink-0 h-44 w-44 rounded-full bg-primary/5 border border-primary/10 flex items-center justify-center relative overflow-hidden md:scale-110">
            <FolderPlus size={64} className="text-primary animate-pulse" />
            <div className="absolute top-8 left-8 h-3 w-3 rounded-full bg-emerald-500 animate-ping" />
          </div>
        </CardContent>
      </Card>

      {/* Feature Value Matrix */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="border border-border/85 bg-card/60 shadow-sm">
          <CardContent className="p-6 space-y-3">
            <div className="h-10 w-10 rounded-xl bg-violet-500/10 text-violet-500 flex items-center justify-center">
              <Layers size={20} />
            </div>
            <h3 className="font-bold text-sm text-foreground">Structured Workspaces</h3>
            <p className="text-xs text-muted-foreground leading-relaxed font-semibold">
              Group related deliverables, boards, and members under unique, isolated project workspaces with fine-grained statuses.
            </p>
          </CardContent>
        </Card>

        <Card className="border border-border/85 bg-card/60 shadow-sm">
          <CardContent className="p-6 space-y-3">
            <div className="h-10 w-10 rounded-xl bg-blue-500/10 text-blue-500 flex items-center justify-center">
              <Sparkles size={20} />
            </div>
            <h3 className="font-bold text-sm text-foreground">Sprint Execution</h3>
            <p className="text-xs text-muted-foreground leading-relaxed font-semibold">
              Build task backlogs, assign action items, and view live status updates across customized priority matrices.
            </p>
          </CardContent>
        </Card>

        <Card className="border border-border/85 bg-card/60 shadow-sm">
          <CardContent className="p-6 space-y-3">
            <div className="h-10 w-10 rounded-xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center">
              <Users size={20} />
            </div>
            <h3 className="font-bold text-sm text-foreground">Team Collaboration</h3>
            <p className="text-xs text-muted-foreground leading-relaxed font-semibold">
              Invite project members, manage their roles, and assign tasks to users with dedicated avatars and summaries.
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default EmptyDashboardState
