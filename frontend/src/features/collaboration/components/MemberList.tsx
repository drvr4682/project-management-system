import React from 'react'
import { Trash2, Crown } from 'lucide-react'
import UserAvatar from './UserAvatar'
import { Button } from '@/components/ui/Button'
import type { ProjectMemberDto } from '../types/collaborationTypes'

interface MemberListProps {
  members: ProjectMemberDto[]
  ownerEmail: string
  canManage: boolean
  onRemoveClick: (userId: string) => void
  isLoading: boolean
}

export const MemberList: React.FC<MemberListProps> = ({
  members,
  ownerEmail,
  canManage,
  onRemoveClick,
  isLoading,
}) => {
  const getRoleBadgeClass = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-violet-500/10 text-violet-500 border border-violet-500/20'
      case 'VIEWER':
        return 'bg-slate-500/10 text-slate-500 border border-slate-500/20'
      case 'MEMBER':
      default:
        return 'bg-blue-500/10 text-blue-500 border border-blue-500/20'
    }
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 animate-in fade-in duration-300">
      {members.map((member) => {
        const isOwner = member.userId === ownerEmail
        return (
          <div
            key={member.userId}
            className="flex items-center justify-between p-4 bg-card border border-border/80 rounded-xl shadow-sm hover:border-primary/20 transition-all duration-300 group"
          >
            <div className="flex items-center space-x-3.5 min-w-0">
              <UserAvatar nameOrEmail={member.userId} size="md" />
              <div className="min-w-0">
                <div className="flex items-center space-x-2">
                  <span className="text-sm font-bold text-foreground truncate max-w-[150px] sm:max-w-[200px]" title={member.userId}>
                    {member.userId}
                  </span>
                  {isOwner && (
                    <span className="inline-flex items-center text-amber-500" title="Project Owner">
                      <Crown size={14} className="fill-current" />
                    </span>
                  )}
                </div>
                <div className="flex items-center space-x-1.5 mt-1.5">
                  <span
                    className={`text-[9px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded-full ${getRoleBadgeClass(
                      member.role
                    )}`}
                  >
                    {member.role.toLowerCase()}
                  </span>
                </div>
              </div>
            </div>

            {/* Remove member button */}
            {canManage && !isOwner && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onRemoveClick(member.userId)}
                disabled={isLoading}
                className="h-8 w-8 p-0 rounded-lg border-border hover:bg-destructive/10 hover:text-destructive hover:border-destructive/20 transition-colors"
                title="Remove member"
              >
                <Trash2 size={13} />
              </Button>
            )}
          </div>
        )
      })}
    </div>
  )
}

export default MemberList
