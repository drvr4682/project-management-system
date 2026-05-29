import React, { useState, useEffect, useRef } from 'react'
import searchApi, { type UserSearchResponse } from '@/features/users/api/searchApi'
import { Search, Loader2 } from 'lucide-react'

interface UserSearchPickerProps {
  onSelect: (user: UserSearchResponse) => void
  excludeUserIds?: string[]
  placeholder?: string
}

export const UserSearchPicker: React.FC<UserSearchPickerProps> = ({
  onSelect,
  excludeUserIds = [],
  placeholder = 'Search users by name or username...',
}) => {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<UserSearchResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [isOpen, setIsOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Local short-term cache in useRef to avoid spamming the database
  const searchCache = useRef<Record<string, UserSearchResponse[]>>({})
  
  // AbortController reference to cancel stale request races
  const abortControllerRef = useRef<AbortController | null>(null)
  
  // Ref to close dropdown on click outside
  const pickerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (pickerRef.current && !pickerRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  useEffect(() => {
    // 1. Reset if query is empty
    if (!query.trim()) {
      setResults([])
      setLoading(false)
      return
    }

    const trimmedQuery = query.trim().toLowerCase()

    // 2. Check local query cache first
    if (searchCache.current[trimmedQuery]) {
      setResults(searchCache.current[trimmedQuery])
      setIsOpen(true)
      return
    }

    // 3. Debounce lookups (300ms delay)
    setLoading(true)
    setError(null)

    const timer = setTimeout(async () => {
      // Cancel previous outstanding request
      if (abortControllerRef.current) {
        abortControllerRef.current.abort()
      }

      // Create new AbortController
      const controller = new AbortController()
      abortControllerRef.current = controller

      try {
        const response = await searchApi.searchProfiles(trimmedQuery, { size: 10 }, controller.signal)
        
        // Cache the response results
        searchCache.current[trimmedQuery] = response.content
        
        // Only set state if this controller wasn't aborted
        if (!controller.signal.aborted) {
          setResults(response.content)
          setIsOpen(true)
          setLoading(false)
        }
      } catch (err: any) {
        if (err.name !== 'CanceledError' && !controller.signal.aborted) {
          setError('Failed to search users')
          setLoading(false)
        }
      }
    }, 300)

    return () => {
      clearTimeout(timer)
    }
  }, [query])

  // Filter out users who are already project members
  const filteredResults = results.filter((user) => !excludeUserIds.includes(user.id))

  const handleSelectUser = (user: UserSearchResponse) => {
    onSelect(user)
    setQuery('')
    setIsOpen(false)
  }

  // Outfit font initials builder
  const getInitials = (user: UserSearchResponse) => {
    return `${user.firstName?.[0] || ''}${user.surname?.[0] || ''}`.toUpperCase() || user.username?.[0]?.toUpperCase() || 'U'
  }

  return (
    <div ref={pickerRef} className="relative w-full">
      <div className="relative">
        <input
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value)
            setIsOpen(true)
          }}
          onFocus={() => setIsOpen(true)}
          placeholder={placeholder}
          className="w-full h-11 pl-11 pr-4 rounded-xl border border-border bg-card/50 backdrop-blur-sm text-sm focus:outline-none focus:border-primary transition-all duration-200"
        />
        <div className="absolute left-4 top-3 text-muted-foreground">
          {loading ? <Loader2 className="w-5 h-5 animate-spin text-primary" /> : <Search className="w-5 h-5" />}
        </div>
      </div>

      {isOpen && query.trim() && (
        <div className="absolute top-full left-0 right-0 mt-2 z-50 rounded-xl border border-border bg-card shadow-lg max-h-64 overflow-y-auto animate-in fade-in duration-200">
          {error && <div className="p-4 text-xs text-red-500">{error}</div>}
          
          {!loading && filteredResults.length === 0 && (
            <div className="p-4 text-center text-sm text-muted-foreground">
              No matching profiles found
            </div>
          )}

          {filteredResults.map((user) => (
            <button
              key={user.id}
              type="button"
              onClick={() => handleSelectUser(user)}
              className="w-full flex items-center space-x-3 px-4 py-3 hover:bg-muted/50 transition-colors border-b border-border/40 last:border-0 text-left"
            >
              <div className="w-9 h-9 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center font-bold text-primary text-xs font-outfit">
                {getInitials(user)}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-bold text-foreground truncate">
                    {user.firstName} {user.surname}
                  </span>
                  {user.designation && (
                    <span className="text-[10px] bg-muted px-2 py-0.5 rounded-full text-muted-foreground font-semibold">
                      {user.designation}
                    </span>
                  )}
                </div>
                <span className="text-xs text-muted-foreground block truncate">
                  @{user.username}
                </span>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

export default UserSearchPicker
