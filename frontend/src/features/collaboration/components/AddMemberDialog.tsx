import React, { useState, useEffect } from 'react'
import { X, Search, UserPlus, Sparkles } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Label } from '@/components/ui/Label'
import UserAvatar from './UserAvatar'
import { collaborationApi } from '../api/collaborationApi'
import type { UserSummaryDto, ProjectRole } from '../types/collaborationTypes'
import { toast } from 'sonner'

interface AddMemberDialogProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: (userId: string, role: ProjectRole) => Promise<void>
  isLoading: boolean
}

export const AddMemberDialog: React.FC<AddMemberDialogProps> = ({
  isOpen,
  onClose,
  onConfirm,
  isLoading,
}) => {
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<UserSummaryDto[]>([])
  const [isSearching, setIsSearching] = useState(false)
  const [selectedUser, setSelectedUser] = useState<UserSummaryDto | null>(null)
  const [role, setRole] = useState<ProjectRole>('MEMBER')

  // Search users dynamically on query change
  useEffect(() => {
    if (!searchQuery.trim() || searchQuery.length < 2) {
      setSearchResults([])
      return
    }

    const delayDebounce = setTimeout(async () => {
      setIsSearching(true)
      try {
        const results = await collaborationApi.searchUsers(searchQuery)
        setSearchResults(results)
      } catch (e: any) {
        // Fail silently during typing search
      } finally {
        setIsSearching(false)
      }
    }, 400) // 400ms debounce guard

    return () => clearTimeout(delayDebounce)
  }, [searchQuery])

  if (!isOpen) return null

  const handleSelectUser = (user: UserSummaryDto) => {
    setSelectedUser(user)
    setSearchQuery('')
    setSearchResults([])
  }

  const handleInviteSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedUser) {
      toast.error('Please select a user to invite.')
      return
    }

    try {
      await onConfirm(selectedUser.email, role)
      setSelectedUser(null)
      setSearchQuery('')
      onClose()
    } catch (err: any) {
      // hook handles toast
    }
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
              <Sparkles size={16} />
              <span className="text-xs uppercase tracking-wider font-extrabold">Collaborate</span>
            </div>
            <CardTitle className="text-xl font-extrabold tracking-tight">Invite Workspace Member</CardTitle>
            <CardDescription className="text-muted-foreground text-sm font-medium">
              Invite active users and set their roles to assign sprint deliverables.
            </CardDescription>
          </CardHeader>

          <form onSubmit={handleInviteSubmit}>
            <CardContent className="space-y-4 pt-0">
              {/* User Selection */}
              <div className="space-y-2 relative">
                <Label>Search Registered User</Label>
                {selectedUser ? (
                  <div className="flex items-center justify-between p-3 rounded-lg border border-primary/20 bg-primary/5">
                    <div className="flex items-center space-x-2.5">
                      <UserAvatar nameOrEmail={selectedUser.name} size="sm" />
                      <div>
                        <div className="text-sm font-bold text-foreground">{selectedUser.name}</div>
                        <div className="text-xs font-medium text-muted-foreground">{selectedUser.email}</div>
                      </div>
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setSelectedUser(null)}
                      className="h-8 px-2 text-xs font-bold hover:text-destructive transition-colors"
                    >
                      Change
                    </Button>
                  </div>
                ) : (
                  <div className="relative">
                    <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                    <Input
                      type="text"
                      placeholder="Type colleague's name or email..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="pl-9 h-10"
                      disabled={isLoading}
                      autoComplete="off"
                    />

                    {/* Autocomplete Search Dropdown list */}
                    {(searchResults.length > 0 || isSearching) && (
                      <div className="absolute left-0 right-0 z-50 mt-1 max-h-48 overflow-y-auto rounded-lg border border-border bg-card shadow-xl p-1 space-y-0.5">
                        {isSearching ? (
                          <div className="p-3 text-center text-xs font-semibold text-muted-foreground animate-pulse">
                            Searching database...
                          </div>
                        ) : (
                          searchResults.map((user) => (
                            <button
                              key={user.id}
                              type="button"
                              onClick={() => handleSelectUser(user)}
                              className="flex items-center space-x-3 w-full p-2 text-left rounded-md hover:bg-muted/80 text-foreground transition-colors group"
                            >
                              <UserAvatar nameOrEmail={user.name} size="xs" />
                              <div className="flex-1 min-w-0">
                                <div className="text-xs font-bold truncate group-hover:text-primary transition-colors">
                                  {user.name}
                                </div>
                                <div className="text-[10px] font-medium text-muted-foreground truncate">
                                  {user.email}
                                </div>
                              </div>
                            </button>
                          ))
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Role Selection */}
              <div className="space-y-2">
                <Label htmlFor="role">Workspace Role</Label>
                <select
                  id="role"
                  value={role}
                  onChange={(e) => setRole(e.target.value as ProjectRole)}
                  disabled={isLoading}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                >
                  <option value="MEMBER">Member (Can view & update tasks)</option>
                  <option value="ADMIN">Admin (Full project control & invites)</option>
                  <option value="VIEWER">Viewer (Read-only workspace access)</option>
                </select>
              </div>
            </CardContent>

            <CardFooter className="flex items-center space-x-3 pt-3 border-t border-border/50 bg-muted/20">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                className="flex-1"
                disabled={isLoading}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                className="flex-1 flex items-center justify-center space-x-1.5 font-bold shadow-md"
                disabled={!selectedUser || isLoading}
                isLoading={isLoading}
              >
                <UserPlus size={15} />
                <span>Add Member</span>
              </Button>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  )
}

export default AddMemberDialog
