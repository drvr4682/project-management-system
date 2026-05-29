import React, { useState, useEffect, useRef } from 'react'
import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/hooks/store'
import { logout, selectAuth } from '@/features/auth/store/authSlice'
import { selectProfile, clearProfileState } from '@/features/auth/store/profileSlice'
import { Button } from '@/components/ui/Button'
import { formatDisplayName } from '@/features/auth/utils/userUtils'
import {
  LayoutDashboard,
  Briefcase,
  ListTodo,
  LogOut,
  User,
  ChevronLeft,
  ChevronRight,
  Menu,
  X,
} from 'lucide-react'
import { toast } from 'sonner'
import axiosInstance from '@/api/axiosInstance'

export const AppShell: React.FC = () => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAppSelector(selectAuth)
  const { profileData } = useAppSelector(selectProfile)
  
  // Responsive sidebar expand/collapse state
  const [isCollapsed, setIsCollapsed] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  
  // Floating profile popover trigger state
  const [profileMenuOpen, setProfileMenuOpen] = useState(false)
  
  const popoverRef = useRef<HTMLDivElement>(null)

  // Real-time System Health Monitor State & Active background pings
  const [systemStatus, setSystemStatus] = useState<'ONLINE' | 'OFFLINE' | 'CHECKING'>('CHECKING')

  useEffect(() => {
    const checkSystemHealth = async () => {
      try {
        // Fast ping to ApiGateway health endpoint
        await axiosInstance.get('/health', { timeout: 3500 })
        setSystemStatus('ONLINE')
      } catch (err) {
        setSystemStatus('OFFLINE')
      }
    }

    checkSystemHealth()
    const interval = setInterval(checkSystemHealth, 15000) // ping every 15 seconds
    return () => clearInterval(interval)
  }, [])

  // Close floating popover when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (popoverRef.current && !popoverRef.current.contains(event.target as Node)) {
        setProfileMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleLogout = () => {
    dispatch(logout())
    dispatch(clearProfileState())
    toast.success('Successfully logged out')
    navigate('/login')
  }

  const displayName = profileData
    ? `${profileData.firstName} ${profileData.surname || ''}`.trim()
    : formatDisplayName(user)

  const userEmail = user?.email || 'user@projecthub.com'

  // Profile initial letter
  const initialLetter = (profileData?.firstName?.[0] || user?.userName?.[0] || 'U').toUpperCase()

  const navItems = [
    { name: 'Dashboard', path: '/', icon: LayoutDashboard },
    { name: 'Projects', path: '/projects', icon: Briefcase }, 
    { name: 'All Tasks', path: '/tasks', icon: ListTodo }, 
  ]

  const activeProject = useAppSelector((state: any) => state.projects?.activeProject)

  return (
    <div className="min-h-screen bg-background flex text-foreground">
      
      {/* Desktop Collapsible Sidebar (Clean Light/White Theme) */}
      <aside
        className={`hidden md:flex flex-col bg-white text-slate-700 border-r border-border relative transition-all duration-300 ${
          isCollapsed ? 'w-20' : 'w-64'
        }`}
      >
        {/* Toggle Collapse Button centered on vertical border */}
        <button
          onClick={() => setIsCollapsed(!isCollapsed)}
          className="absolute right-[-14px] top-1/2 -translate-y-1/2 w-7 h-7 rounded-full bg-white border border-gray-200 flex items-center justify-center cursor-pointer shadow-md z-40 text-gray-800 hover:text-primary transition-all duration-200 focus:outline-none"
          title={isCollapsed ? 'Expand Sidebar' : 'Collapse Sidebar'}
        >
          {isCollapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </button>

        {/* Sidebar Brand Header */}
        <div className={`h-16 flex items-center border-b border-border/80 ${isCollapsed ? 'justify-center px-0' : 'px-6'}`}>
          <div className="flex items-center space-x-3.5">
            <div className="w-10 h-10 rounded-xl bg-primary flex items-center justify-center font-extrabold text-white text-sm tracking-wide shadow-md shadow-primary/10 shrink-0">
              PM
            </div>
            {!isCollapsed && (
              <span className="font-extrabold text-lg text-slate-900 font-outfit tracking-tight">
                ProjectHub
              </span>
            )}
          </div>
        </div>

        {/* Sidebar Nav Items */}
        <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon
            
            // Standard Active Highlight checking
            const isActive = location.pathname === item.path || 
              (item.path !== '/' && location.pathname.startsWith(item.path))
            
            return (
              <NavLink
                key={item.name}
                to={item.path}
                className={({ isActive: linkActive }) =>
                  `flex items-center rounded-xl text-sm font-semibold transition-all duration-200 px-4 py-3 ${
                    isCollapsed ? 'justify-center' : 'space-x-3.5'
                  } ${
                    (isActive || linkActive) && !isCollapsed
                      ? 'bg-primary text-white shadow-md shadow-primary/15'
                      : (isActive || linkActive) && isCollapsed
                        ? 'bg-primary text-white shadow-sm'
                        : 'text-slate-500 hover:bg-slate-50 hover:text-slate-900'
                  }`
                }
                title={isCollapsed ? item.name : undefined}
              >
                <Icon className="w-5.5 h-5.5 shrink-0" />
                {!isCollapsed && <span>{item.name}</span>}
              </NavLink>
            )
          })}

          {/* Collapsible Active Workspace Context section */}
          {activeProject && !isCollapsed && (
            <div className="pt-4 border-t border-border/80 mt-4 animate-in fade-in">
              <span className="text-[10px] text-slate-400 uppercase font-extrabold tracking-widest px-4 block mb-2">
                Active Workspace
              </span>
              <NavLink
                to={`/projects/${activeProject.id}`}
                className={({ isActive }) =>
                  `flex items-center space-x-3 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 ${
                    isActive
                      ? 'bg-slate-50 text-slate-900 border-l-2 border-primary pl-3.5'
                      : 'text-slate-500 hover:bg-slate-50 hover:text-slate-900'
                  }`
                }
              >
                <Briefcase className="w-5 h-5 text-primary shrink-0" />
                <span className="truncate">{activeProject.name}</span>
              </NavLink>
            </div>
          )}
        </nav>

        {/* Bottom User Area with Floating Popover (Light Theme Frame) */}
        <div ref={popoverRef} className="p-4 border-t border-border bg-slate-50/50 relative">
          
          {/* Floating Profile & Logout Option Popover */}
          {profileMenuOpen && (
            <div
              className={`absolute bg-white text-gray-900 border border-gray-200 rounded-2xl shadow-xl p-2 z-50 flex flex-col space-y-1 animate-in slide-in-from-bottom-3 duration-200 ${
                isCollapsed ? 'bottom-20 left-4 w-48' : 'bottom-20 left-4 right-4'
              }`}
            >
              <button
                onClick={() => {
                  setProfileMenuOpen(false)
                  navigate('/profile') // Navigate to Profile settings page
                }}
                className="w-full flex items-center space-x-3 px-4 py-3 hover:bg-gray-50 rounded-xl transition-all font-bold text-sm text-slate-700 text-left"
              >
                <User className="w-5 h-5 text-slate-500 shrink-0" />
                <span>Profile</span>
              </button>
              <div className="border-t border-gray-100 my-1" />
              <button
                onClick={() => {
                  setProfileMenuOpen(false)
                  handleLogout()
                }}
                className="w-full flex items-center space-x-3 px-4 py-3 hover:bg-red-50 rounded-xl transition-all font-bold text-sm text-red-500 text-left"
              >
                <LogOut className="w-5 h-5 text-red-500 shrink-0" />
                <span>Log out</span>
              </button>
            </div>
          )}

          {/* User metadata block trigger */}
          <div
            onClick={() => setProfileMenuOpen(!profileMenuOpen)}
            className={`flex items-center rounded-xl cursor-pointer hover:bg-slate-100/50 p-2 transition-colors ${
              isCollapsed ? 'justify-center' : 'space-x-3.5'
            }`}
          >
            {/* Round avatar */}
            <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center font-extrabold text-white text-sm font-outfit shadow-md shrink-0">
              {initialLetter}
            </div>
            {!isCollapsed && (
              <div className="flex-1 min-w-0">
                <p className="text-sm font-bold text-slate-900 truncate leading-snug">{displayName}</p>
                <p className="text-xs text-slate-500 truncate leading-normal font-semibold">
                  {userEmail}
                </p>
              </div>
            )}
          </div>
        </div>
      </aside>

      {/* Mobile Drawer Overlay */}
      {mobileMenuOpen && (
        <div
          className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm md:hidden"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      {/* Mobile Sidebar Slide-out */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-white text-slate-700 border-r border-border flex flex-col transform md:hidden transition-transform duration-300 ease-in-out ${
          mobileMenuOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="h-16 flex items-center justify-between px-6 border-b border-border/80">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-primary flex items-center justify-center font-extrabold text-white text-sm tracking-wide">
              PM
            </div>
            <span className="font-extrabold text-lg text-slate-900 font-outfit tracking-tight">
              ProjectHub
            </span>
          </div>
          <button
            onClick={() => setMobileMenuOpen(false)}
            className="p-1 rounded-lg text-slate-400 hover:bg-slate-50"
          >
            <X className="w-6 h-6" />
          </button>
        </div>
        <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon
            
            const isActive = location.pathname === item.path || 
              (item.path !== '/' && location.pathname.startsWith(item.path))

            return (
              <NavLink
                key={item.name}
                to={item.path}
                onClick={() => setMobileMenuOpen(false)}
                className={({ isActive: linkActive }) =>
                  `flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-semibold transition-all duration-200 ${
                    isActive || linkActive
                      ? 'bg-primary text-white font-bold'
                      : 'text-slate-500 hover:bg-slate-50 hover:text-slate-900'
                  }`
                }
              >
                <Icon className="w-5.5 h-5.5 shrink-0" />
                <span>{item.name}</span>
              </NavLink>
            )
          })}
        </nav>

        <div className="p-4 border-t border-border bg-slate-50/50">
          <div className="flex items-center space-x-3 mb-3 p-2">
            <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center font-extrabold text-white text-sm font-outfit">
              {initialLetter}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-bold text-slate-900 truncate leading-snug">{displayName}</p>
              <p className="text-xs text-slate-500 truncate leading-normal">
                {userEmail}
              </p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2 mt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setMobileMenuOpen(false)
                navigate('/profile')
              }}
              className="rounded-xl h-9 font-bold text-slate-500 border-border hover:bg-slate-50 hover:text-slate-900"
            >
              Profile
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setMobileMenuOpen(false)
                handleLogout()
              }}
              className="rounded-xl h-9 font-bold text-red-500 hover:bg-red-500/10 border-red-500/10"
            >
              Sign Out
            </Button>
          </div>
        </div>
      </aside>

      {/* Main Wrapper */}
      <div className="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
        {/* Top Navbar */}
        <header className="h-16 border-b border-border bg-card/30 backdrop-blur-md flex items-center justify-between px-4 md:px-8 z-10">
          <div className="flex items-center space-x-4">
            <button
              onClick={() => setMobileMenuOpen(true)}
              className="p-2 -ml-2 rounded-lg text-muted-foreground hover:bg-muted md:hidden"
            >
              <Menu className="w-6 h-6" />
            </button>

            {/* Dynamic Breadcrumbs */}
            <div className="flex items-center space-x-2 text-sm font-semibold">
              {location.pathname === '/' ? (
                <span className="text-foreground font-outfit">Dashboard</span>
              ) : location.pathname === '/projects' ? (
                <div className="flex items-center space-x-2">
                  <span className="text-muted-foreground hover:text-foreground cursor-pointer" onClick={() => navigate('/')}>Workspace</span>
                  <span className="text-muted-foreground/50">/</span>
                  <span className="text-foreground font-outfit">Projects</span>
                </div>
              ) : location.pathname.startsWith('/projects/') && activeProject ? (
                <div className="flex items-center space-x-2">
                  <span className="text-muted-foreground hover:text-foreground cursor-pointer" onClick={() => navigate('/')}>Workspace</span>
                  <span className="text-muted-foreground/50">/</span>
                  <span className="text-muted-foreground hover:text-foreground cursor-pointer" onClick={() => navigate('/projects')}>Projects</span>
                  <span className="text-muted-foreground/50">/</span>
                  <span className="text-foreground truncate max-w-[120px] sm:max-w-[200px]">{activeProject.name}</span>
                </div>
              ) : location.pathname === '/tasks' ? (
                <div className="flex items-center space-x-2">
                  <span className="text-muted-foreground hover:text-foreground cursor-pointer" onClick={() => navigate('/')}>Workspace</span>
                  <span className="text-muted-foreground/50">/</span>
                  <span className="text-foreground font-outfit">All Tasks</span>
                </div>
              ) : location.pathname === '/profile' ? (
                <div className="flex items-center space-x-2">
                  <span className="text-muted-foreground hover:text-foreground cursor-pointer" onClick={() => navigate('/')}>Workspace</span>
                  <span className="text-muted-foreground/50">/</span>
                  <span className="text-foreground font-outfit">Profile Settings</span>
                </div>
              ) : (
                <span className="text-foreground">Dashboard</span>
              )}
            </div>
          </div>

          {/* Dynamic Active Backend Health Monitor Status Badge */}
          <div className="flex items-center space-x-4">
            {systemStatus === 'ONLINE' && (
              <div className="hidden sm:flex items-center space-x-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20" title="Backend services are fully responsive.">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                <span className="text-emerald-500 text-xs font-bold font-outfit uppercase tracking-wider">System Online</span>
              </div>
            )}
            {systemStatus === 'OFFLINE' && (
              <div className="hidden sm:flex items-center space-x-1.5 px-3 py-1 rounded-full bg-red-500/10 border border-red-500/20" title="Backend services are currently offline or stopped.">
                <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse"></span>
                <span className="text-red-500 text-xs font-bold font-outfit uppercase tracking-wider">System Offline</span>
              </div>
            )}
            {systemStatus === 'CHECKING' && (
              <div className="hidden sm:flex items-center space-x-1.5 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/20" title="Pinging backend gateway...">
                <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse"></span>
                <span className="text-amber-500 text-xs font-bold font-outfit uppercase tracking-wider">Checking...</span>
              </div>
            )}
            
            <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center font-extrabold text-white text-[11px] font-outfit shadow-sm">
              {initialLetter}
            </div>
          </div>
        </header>

        {/* Content Outlet scroll area */}
        <main className="flex-1 overflow-y-auto bg-background/50">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AppShell
