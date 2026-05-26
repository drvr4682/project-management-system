import React from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'

interface AuthFooterProps {
  text?: string
  linkText?: string
  linkTo?: string
  backLinkText?: string
  backLinkTo?: string
}

export const AuthFooter: React.FC<AuthFooterProps> = ({
  text,
  linkText,
  linkTo,
  backLinkText,
  backLinkTo,
}) => {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.4, delay: 0.3 }}
      className="text-xs md:text-sm text-center text-muted-foreground mt-6 space-y-3"
    >
      {text && linkText && linkTo && (
        <div className="font-medium">
          {text}{' '}
          <Link to={linkTo} className="text-primary hover:underline font-bold transition-all duration-200">
            {linkText}
          </Link>
        </div>
      )}

      {backLinkText && backLinkTo && (
        <div className="pt-3 border-t border-border/50">
          <Link to={backLinkTo} className="text-muted-foreground hover:text-foreground font-semibold hover:underline transition-colors flex items-center justify-center space-x-1">
            <span>{backLinkText}</span>
          </Link>
        </div>
      )}
    </motion.div>
  )
}

export default AuthFooter
