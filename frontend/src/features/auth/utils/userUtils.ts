import type { UserProfile } from '../types/authTypes'

export function formatDisplayName(user: Partial<UserProfile> | null | undefined): string {
  if (!user) return 'User'
  return user.userName || user.email || 'User'
}
