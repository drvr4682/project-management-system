import React from 'react'

interface LogoProps {
  size?: 'sm' | 'md' | 'lg' | number
  showText?: boolean
  className?: string
  light?: boolean
}

export const Logo: React.FC<LogoProps> = ({
  size = 'md',
  showText = true,
  className = '',
  light = false,
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
      {/* Box logo with dynamic colors based on light background/side panel context */}
      <div className={`${boxSize} rounded-lg flex items-center justify-center font-extrabold ${letterStyle} ${
        light ? 'bg-white text-violet-600 shadow-sm' : 'bg-primary text-primary-foreground'
      }`}>
        D
      </div>
      {showText && (
        <span className={`font-bold tracking-tight ${textStyle} ${
          light ? 'text-white' : 'text-foreground'
        }`}>
          DRVRHub
        </span>
      )}
    </div>
  )
}

export default Logo;
