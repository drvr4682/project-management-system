import React from 'react'
import { motion } from 'framer-motion'
import { Logo } from '@/components/branding/Logo'

interface AuthHeaderProps {
  title: string
  subtitle: string
  showLogoMobile?: boolean
}

export const AuthHeader: React.FC<AuthHeaderProps> = ({
  title,
  subtitle,
  showLogoMobile = true,
}) => {
  return (
    <div className="space-y-1 text-center md:text-left flex flex-col items-center md:items-start mb-2.5">
      {/* Mobile-only Logo: Visible on small screens, hidden on md and up */}
      {showLogoMobile && (
        <motion.div
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.4 }}
          className="md:hidden mb-3 shrink-0"
        >
          <Logo size="lg" />
        </motion.div>
      )}

      <motion.h1
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.1 }}
        className="text-xl md:text-2xl font-extrabold tracking-tight text-foreground"
      >
        {title}
      </motion.h1>

      <motion.p
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.2 }}
        className="text-xs text-muted-foreground font-semibold leading-relaxed"
      >
        {subtitle}
      </motion.p>
    </div>
  )
}

export default AuthHeader
