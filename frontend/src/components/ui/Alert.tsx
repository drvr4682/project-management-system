import * as React from 'react'
import { cn } from '@/lib/utils'

export interface AlertProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'destructive' | 'success'
}

export const Alert = React.forwardRef<HTMLDivElement, AlertProps>(
  ({ className, variant = 'default', ...props }, ref) => {
    const baseStyles = 'relative w-full rounded-lg border p-4 [&>svg+div]:translate-y-[-3px] [&>svg]:absolute [&>svg]:left-4 [&>svg]:top-4 [&>svg]:text-foreground [&>svg~*]:pl-7'
    const variants = {
      default: 'bg-background text-foreground border-border',
      destructive: 'border-destructive/50 text-destructive bg-destructive/10 dark:border-destructive [&>svg]:text-destructive',
      success: 'border-emerald-500/30 text-emerald-600 bg-emerald-500/10 dark:border-emerald-500 [&>svg]:text-emerald-500',
    }

    return (
      <div
        ref={ref}
        role="alert"
        className={cn(baseStyles, variants[variant], className)}
        {...props}
      />
    )
  }
)
Alert.displayName = 'Alert'

export const AlertTitle = React.forwardRef<HTMLParagraphElement, React.HTMLAttributes<HTMLHeadingElement>>(
  ({ className, ...props }, ref) => (
    <h5
      ref={ref}
      className={cn('mb-1 font-medium leading-none tracking-tight', className)}
      {...props}
    />
  )
)
AlertTitle.displayName = 'AlertTitle'

export const AlertDescription = React.forwardRef<HTMLParagraphElement, React.HTMLAttributes<HTMLParagraphElement>>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn('text-sm [&_p]:leading-relaxed', className)}
      {...props}
    />
  )
)
AlertDescription.displayName = 'AlertDescription'
