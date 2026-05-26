import React from 'react'
import { X, UserCheck, UserMinus, ShieldAlert } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import UserAvatar from './UserAvatar'
import type { ProjectMemberDto } from '../types/collaborationTypes'

interface AssignTaskDialogProps {
  isOpen: boolean
  onClose: () => void
  onAssign: (assigneeEmail: string) => Promise<void>
  onUnassign: () => Promise<void>
  taskTitle: string
  currentAssignee?: string | null
  members: ProjectMemberDto[]
  isLoading: boolean
}

export const AssignTaskDialog: React.FC<AssignTaskDialogProps> = ({
  isOpen,
  onClose,
  onAssign,
  onUnassign,
  taskTitle,
  currentAssignee,
  members,
  isLoading,
}) => {
  if (!isOpen) return null

  const handleSelectMember = async (email: string) => {
    if (email === currentAssignee) {
      onClose()
      return
    }
    await onAssign(email)
    onClose()
  }

  const handleUnassignClick = async () => {
    await onUnassign()
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-md relative z-50 animate-in zoom-in-95 duration-200">
        <Card className="border border-border/80 bg-card shadow-2xl">
          <CardHeader className="relative pb-4">
            <button
              onClick={onClose}
              disabled={isLoading}
              className="absolute right-4 top-4 text-muted-foreground hover:text-foreground hover:bg-muted p-1.5 rounded-lg transition-colors"
            >
              <X size={16} />
            </button>
            <div className="flex items-center space-x-2 text-primary font-bold mb-1">
              <UserCheck size={16} />
              <span className="text-xs uppercase tracking-wider font-extrabold">Task Ownership</span>
            </div>
            <CardTitle className="text-xl font-extrabold tracking-tight">Assign Task</CardTitle>
            <CardDescription className="text-muted-foreground text-sm font-medium">
              Select a project team member to delegate <span className="font-bold text-foreground">"{taskTitle}"</span>.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4 pt-0">
            {currentAssignee && (
              <div className="flex items-center justify-between p-3 rounded-lg border border-primary/25 bg-primary/5">
                <div className="flex items-center space-x-2.5">
                  <UserAvatar nameOrEmail={currentAssignee} size="sm" />
                  <div>
                    <div className="text-xs font-semibold text-muted-foreground">Currently Assigned</div>
                    <div className="text-sm font-bold text-foreground truncate max-w-[200px]">{currentAssignee}</div>
                  </div>
                </div>

                <Button
                  type="button"
                  variant="destructive"
                  size="sm"
                  onClick={handleUnassignClick}
                  disabled={isLoading}
                  className="flex items-center space-x-1 h-8 text-xs font-bold"
                >
                  <UserMinus size={13} />
                  <span>Unassign</span>
                </Button>
              </div>
            )}

            <div className="space-y-2">
              <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block mb-1">
                Select Project Member
              </span>
              <div className="border border-border rounded-lg max-h-48 overflow-y-auto p-1 space-y-0.5 bg-background/50">
                {members.length === 0 ? (
                  <div className="p-4 text-center text-xs text-muted-foreground font-semibold flex flex-col items-center space-y-2">
                    <ShieldAlert size={20} className="text-muted-foreground/60" />
                    <span>No members invited to this project yet. Add members in the team workspace settings.</span>
                  </div>
                ) : (
                  members.map((member) => (
                    <button
                      key={member.userId}
                      type="button"
                      disabled={isLoading}
                      onClick={() => handleSelectMember(member.userId)}
                      className={`flex items-center space-x-3 w-full p-2.5 text-left rounded-md transition-colors group ${
                        member.userId === currentAssignee
                          ? 'bg-muted border border-border cursor-default pointer-events-none'
                          : 'hover:bg-muted/80'
                      }`}
                    >
                      <UserAvatar nameOrEmail={member.userId} size="sm" />
                      <div className="flex-1 min-w-0">
                        <div className="text-xs font-bold text-foreground truncate group-hover:text-primary transition-colors">
                          {member.userId}
                        </div>
                        <div className="text-[10px] uppercase font-extrabold text-muted-foreground tracking-wider mt-0.5">
                          {member.role.toLowerCase()}
                        </div>
                      </div>
                    </button>
                  ))
                )}
              </div>
            </div>
          </CardContent>

          <CardFooter className="pt-2 border-t border-border/50 bg-muted/20 flex justify-end">
            <Button
              variant="outline"
              size="sm"
              onClick={onClose}
              disabled={isLoading}
              className="px-4 font-bold"
            >
              Cancel
            </Button>
          </CardFooter>
        </Card>
      </div>
    </div>
  )
}

export default AssignTaskDialog
