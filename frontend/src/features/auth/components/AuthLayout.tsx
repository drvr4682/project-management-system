import React from 'react'

interface AuthLayoutProps {
  children: React.ReactNode
}

export const AuthLayout: React.FC<AuthLayoutProps> = ({ children }) => {
  return (
    <div className="flex items-center justify-center min-h-screen bg-background p-4 relative overflow-hidden">
      {/* Visual background ambient shapes for premium glassmorphic depth */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full bg-primary/10 blur-[120px] pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] rounded-full bg-primary/15 blur-[120px] pointer-events-none"></div>
      
      <div className="w-full max-w-md relative z-10">
        {children}
      </div>
    </div>
  )
}
