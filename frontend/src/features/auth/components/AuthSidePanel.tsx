import React from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Logo } from '@/components/branding/Logo'

interface AuthSidePanelProps {
  title: string
  subtitle: string
  ctaLabel?: string
  ctaText?: string
  ctaLink?: string
}

export const AuthSidePanel: React.FC<AuthSidePanelProps> = ({
  title,
  subtitle,
  ctaLabel = 'Already have an account?',
  ctaText = 'Sign In',
  ctaLink = '/login',
}) => {
  return (
    <div className="relative h-full w-full bg-gradient-to-br from-violet-600 via-violet-700 to-indigo-900 text-white p-8 md:p-12 flex flex-col justify-between overflow-hidden">
      {/* Decorative premium visual elements */}
      <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full blur-3xl -mr-16 -mt-16 pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-80 h-80 bg-violet-500/20 rounded-full blur-3xl -ml-20 -mb-20 pointer-events-none" />

      {/* Top Section: Branding */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: 'easeOut' }}
        className="z-10"
      >
        <Link to="/" className="inline-block">
          <Logo size="md" light />
        </Link>
      </motion.div>

      {/* Middle Section: Welcome & Marketing Pitch */}
      <div className="my-auto py-12 space-y-4 z-10">
        <motion.h2
          initial={{ opacity: 0, x: -30 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, delay: 0.1, ease: 'easeOut' }}
          className="text-3xl md:text-4xl font-extrabold tracking-tight leading-tight"
        >
          {title}
        </motion.h2>
        <motion.p
          initial={{ opacity: 0, x: -30 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, delay: 0.2, ease: 'easeOut' }}
          className="text-violet-200/90 text-sm md:text-base font-semibold leading-relaxed"
        >
          {subtitle}
        </motion.p>
      </div>

      {/* Bottom Section: CTA Switcher */}
      {ctaLink && ctaText && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.3, ease: 'easeOut' }}
          className="z-10 space-y-4 border-t border-white/10 pt-6"
        >
          <div className="text-xs text-violet-200 font-bold uppercase tracking-wider">
            {ctaLabel}
          </div>
          <Link to={ctaLink} className="inline-block">
            <motion.button
              whileHover={{ scale: 1.03, backgroundColor: 'rgba(255,255,255,1)', color: '#7C3AED' }}
              whileTap={{ scale: 0.98 }}
              className="px-6 py-2.5 rounded-xl border border-white/30 text-white font-bold text-sm bg-white/5 backdrop-blur-sm transition-all duration-300 shadow-sm shadow-black/10"
            >
              {ctaText}
            </motion.button>
          </Link>
        </motion.div>
      )}
    </div>
  )
}

export default AuthSidePanel
