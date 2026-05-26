import React from 'react'
import { Users, UserPlus } from 'lucide-react'
import { Button } from '@/components/ui/Button'

interface EmptyMembersStateProps {
  onAddClick?: () => void
  canManage?: boolean
}

export const EmptyMembersState: React.FC<EmptyMembersStateProps> = ({
  onAddClick,
  canManage = false,
}) => {
  return (
    <div className="flex flex-col items-center justify-center p-8 text-center bg-card/65 backdrop-blur-md rounded-xl border border-border/80 shadow-sm py-16">
      <div className="mx-auto w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center text-primary mb-5">
        <Users size={32} />
      </div>
      <h3 className="text-2xl font-extrabold tracking-tight text-foreground mb-2">No External Members Yet</h3>
      <p className="text-muted-foreground max-w-sm mb-6 font-medium text-sm leading-relaxed">
        This workspace is currently private to you. Add members to delegate deliverables and build together.
      </p>
      {canManage && onAddClick && (
        <Button onClick={onAddClick} className="flex items-center space-x-2 shadow-sm font-semibold">
          <UserPlus size={16} />
          <span>Invite Project Member</span>
        </Button>
      )}
    </div>
  )
}

export default EmptyMembersState
