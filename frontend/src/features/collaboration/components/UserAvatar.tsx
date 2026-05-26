import React from 'react'
import { cn } from '@/lib/utils'

interface UserAvatarProps {
  nameOrEmail?: string
  size?: 'xs' | 'sm' | 'md' | 'lg'
  className?: string
}

export const UserAvatar: React.FC<UserAvatarProps> = ({
  nameOrEmail = '?',
  size = 'md',
  className,
}) => {
  // Deterministic color assignment using name string hash
  const avatarColors = [
    'bg-violet-500 text-white',
    'bg-blue-500 text-white',
    'bg-emerald-500 text-white',
    'bg-amber-500 text-white',
    'bg-rose-500 text-white',
    'bg-indigo-500 text-white',
    'bg-cyan-500 text-white',
  ]

  const getHashColor = (str: string) => {
    let sum = 0
    for (let i = 0; i < str.length; i++) {
      sum += str.charCodeAt(i)
    }
    return avatarColors[sum % avatarColors.length]
  }

  // Generate clean initials
  const getInitials = (str: string) => {
    if (!str || str === '?') return '?'
    if (str.includes('@')) {
      const parts = str.split('@')[0]
      return parts.charAt(0).toUpperCase()
    }
    const cleanStr = str.trim()
    const words = cleanStr.split(/\s+/)
    if (words.length >= 2) {
      return (words[0].charAt(0) + words[1].charAt(0)).toUpperCase()
    }
    return cleanStr.slice(0, 2).toUpperCase()
  }

  const sizes = {
    xs: 'h-6 w-6 text-[10px] font-extrabold',
    sm: 'h-8 w-8 text-xs font-extrabold',
    md: 'h-10 w-10 text-sm font-extrabold',
    lg: 'h-14 w-14 text-lg font-extrabold',
  }

  const colorClass = getHashColor(nameOrEmail)
  const initials = getInitials(nameOrEmail)

  return (
    <div
      className={cn(
        'flex items-center justify-center rounded-full shrink-0 uppercase select-none tracking-tight shadow-sm border border-background',
        sizes[size],
        colorClass,
        className
      )}
      title={nameOrEmail}
    >
      {initials}
    </div>
  )
}

export default UserAvatar
