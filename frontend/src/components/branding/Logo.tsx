import React from 'react'

interface LogoProps {
  size?: 'sm' | 'md' | 'lg' | number
  showText?: boolean
  className?: string
}

export const Logo: React.FC<LogoProps> = ({
  size = 'md',
  showText = true,
  className = '',
}) => {
  const getDimensions = () => {
    switch (size) {
      case 'sm':
        return { boxSize: 'w-8 h-8', textStyle: 'text-lg', letterStyle: 'text-sm' }
      case 'lg':
        return { boxSize: 'w-12 h-12', textStyle: 'text-2xl', letterStyle: 'text-xl' }
      case 'md':
      default:
        return { boxSize: 'w-10 h-10', textStyle: 'text-xl', letterStyle: 'text-lg' }
    }
  }

  const { boxSize, textStyle, letterStyle } = getDimensions()

  return (
    <div className={`flex items-center space-x-2 select-none ${className}`}>
      {/* Reverted back to the old simple box-style logo featuring 'D' for DRVRHub */}
      <div className={`${boxSize} rounded-lg bg-primary flex items-center justify-center text-primary-foreground font-extrabold ${letterStyle}`}>
        D
      </div>
      {showText && (
        <span className={`font-bold text-foreground tracking-tight ${textStyle}`}>
          DRVRHub
        </span>
      )}
    </div>
  )
}

export default Logo
