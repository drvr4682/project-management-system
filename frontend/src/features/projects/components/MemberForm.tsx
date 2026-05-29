import React, { useState } from 'react'
import { useAppDispatch } from '@/hooks/store'
import { addProjectMember } from '../store/projectSlice'
import { UserSearchPicker } from '@/components/UserSearchPicker'
import { type UserSearchResponse } from '@/features/users/api/searchApi'
import { Button } from '@/components/ui/Button'
import { Label } from '@/components/ui/Label'
import { X, UserPlus, Info } from 'lucide-react'
import { toast } from 'sonner'

interface MemberFormProps {
  isOpen: boolean
  onClose: () => void
  projectId: number
  excludeUserIds?: string[]
}

export const MemberForm: React.FC<MemberFormProps> = ({
  isOpen,
  onClose,
  projectId,
  excludeUserIds = [],
}) => {
  const dispatch = useAppDispatch()
  
  const [selectedUser, setSelectedUser] = useState<UserSearchResponse | null>(null)
  const [role, setRole] = useState('MEMBER')
  const [submitting, setSubmitting] = useState(false)

  if (!isOpen) return null

  const handleSelectUser = (user: UserSearchResponse) => {
    setSelectedUser(user)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedUser) {
      toast.error('Please search and select a user first')
      return
    }

    setSubmitting(true)
    try {
      await dispatch(
        addProjectMember({
          projectId,
          userId: selectedUser.id,
          role,
        })
      ).unwrap()
      toast.success('Member added to project successfully')
      setSelectedUser(null)
      onClose()
    } catch (err: any) {
      toast.error(err || 'Failed to add project member')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm animate-in fade-in duration-300">
      <div 
        className="w-full max-w-md bg-card border border-border/80 rounded-2xl shadow-xl overflow-hidden animate-in zoom-in-95 duration-200"
        role="dialog"
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-border/60">
          <div className="flex items-center space-x-2 text-foreground font-bold">
            <UserPlus className="w-5 h-5 text-primary" />
            <span>Invite Collaborator</span>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-muted-foreground hover:bg-muted transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div className="space-y-1.5">
            <Label>Search User Profiles</Label>
            <UserSearchPicker
              onSelect={handleSelectUser}
              excludeUserIds={excludeUserIds}
              placeholder="Type first name, surname, or username..."
            />
          </div>

          {selectedUser ? (
            <div className="p-4 rounded-xl bg-primary/5 border border-primary/10 flex items-center space-x-3">
              <div className="w-10 h-10 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center font-bold text-primary text-sm font-outfit">
                {`${selectedUser.firstName?.[0] || ''}${selectedUser.surname?.[0] || ''}`.toUpperCase()}
              </div>
              <div className="flex-1 min-w-0">
                <span className="text-sm font-bold text-foreground block truncate">
                  {selectedUser.firstName} {selectedUser.surname}
                </span>
                <span className="text-xs text-muted-foreground block truncate">
                  @{selectedUser.username}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setSelectedUser(null)}
                className="text-xs text-red-500 font-bold hover:underline"
              >
                Clear
              </button>
            </div>
          ) : (
            <div className="p-4 rounded-xl bg-muted/30 border border-border flex items-start space-x-3 text-xs text-muted-foreground">
              <Info className="w-4 h-4 text-muted-foreground shrink-0 mt-0.5" />
              <span>
                Search is connected directly to user identity services. Members will be invited using secure UUID identity keys.
              </span>
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="memberRole">Workspace Role</Label>
            <select
              id="memberRole"
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="w-full h-11 px-3 rounded-xl border border-border bg-card/50 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
            >
              <option value="MEMBER">MEMBER (View & edit tasks)</option>
              <option value="ADMIN">ADMIN (Full management controls)</option>
              <option value="VIEWER">VIEWER (Read-only access)</option>
            </select>
          </div>

          <div className="flex justify-end space-x-3 pt-4 border-t border-border/60 mt-6">
            <Button type="button" variant="outline" onClick={onClose} className="rounded-xl h-10 font-bold">
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={submitting || !selectedUser}
              className="rounded-xl h-10 font-bold px-6"
            >
              {submitting ? 'Adding...' : 'Add Collaborator'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
