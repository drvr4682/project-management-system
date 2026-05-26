import React from 'react'
import { motion } from 'framer-motion'
import { cn } from '@/lib/utils'

interface AuthCardProps {
  children: React.ReactNode
  className?: string
}

export const AuthCard: React.FC<AuthCardProps> = ({ children, className }) => {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.45, ease: 'easeOut' }}
      className={cn(
        'w-full bg-card p-6 md:p-12 flex flex-col justify-center h-full',
        className
      )}
    >
      {children}
    </motion.div>
  )
}

export default AuthCard
