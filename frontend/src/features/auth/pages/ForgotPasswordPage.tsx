import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { motion } from 'framer-motion'
import { authApi } from '../api/authApi'
import { Label } from '@/components/ui/Label'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Alert, AlertDescription } from '@/components/ui/Alert'
import { AuthLayout } from '../components/AuthLayout'
import { AuthSidePanel } from '../components/AuthSidePanel'
import { AuthCard } from '../components/AuthCard'
import { AuthHeader } from '../components/AuthHeader'
import { AuthFooter } from '../components/AuthFooter'

const forgotPasswordSchema = z.object({
  email: z.string().email('Please enter a valid email address').min(1, 'Email is required'),
})

type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>

export const ForgotPasswordPage: React.FC = () => {
  const [apiError, setApiError] = React.useState<string | null>(null)
  const [apiSuccess, setApiSuccess] = React.useState<string | null>(null)
  const [isLoading, setIsLoading] = React.useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  })

  const onSubmit = async (values: ForgotPasswordFormValues) => {
    setIsLoading(true)
    setApiError(null)
    setApiSuccess(null)
    try {
      const response = await authApi.forgotPassword(values.email)
      setApiSuccess(response.message || 'If the account exists, a password reset link has been sent.')
    } catch (e: unknown) {
      const axiosError = e as { response?: { data?: { message?: string } } }
      const msg = axiosError.response?.data?.message || 'Something went wrong. Please try again.'
      setApiError(msg)
    } finally {
      setIsLoading(false)
    }
  }

  const sidePanel = (
    <AuthSidePanel
      title="Password Recovery"
      subtitle="No worries! Enter your registered email address and we will dispatch a secure link to reset your credentials."
      ctaLabel="Remember your credentials?"
      ctaText="Sign In"
      ctaLink="/login"
    />
  )

  return (
    <AuthLayout sidePanel={sidePanel}>
      <AuthCard>
        <AuthHeader
          title="Forgot Password"
          subtitle="Enter your email to receive a secure recovery link."
        />

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {apiError && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
            >
              <Alert variant="destructive" className="rounded-xl border border-destructive/20 bg-destructive/5">
                <AlertDescription className="text-xs font-semibold">{apiError}</AlertDescription>
              </Alert>
            </motion.div>
          )}

          {apiSuccess && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
            >
              <Alert className="rounded-xl border border-emerald-500/20 bg-emerald-500/5">
                <AlertDescription className="text-xs text-emerald-500 font-bold">{apiSuccess}</AlertDescription>
              </Alert>
            </motion.div>
          )}

          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.15 }}
            className="space-y-1.5"
          >
            <Label htmlFor="email" className="text-xs font-bold text-foreground">
              Email Address
            </Label>
            <Input
              id="email"
              type="email"
              placeholder="you@example.com"
              error={!!errors.email}
              className="rounded-xl h-10 border-border/80 text-sm focus-visible:ring-primary/30"
              {...register('email')}
            />
            {errors.email && (
              <p className="text-[10px] text-destructive font-semibold">{errors.email.message}</p>
            )}
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.2 }}
            className="pt-2"
          >
            <Button
              type="submit"
              className="w-full h-10 rounded-xl font-bold bg-primary hover:bg-primary/95 text-sm shadow-md shadow-primary/20 transition-all duration-300"
              isLoading={isLoading}
            >
              Send Reset Link
            </Button>
          </motion.div>
        </form>

        <AuthFooter
          backLinkText="Back to Sign In"
          backLinkTo="/login"
        />
      </AuthCard>
    </AuthLayout>
  )
}

export default ForgotPasswordPage
