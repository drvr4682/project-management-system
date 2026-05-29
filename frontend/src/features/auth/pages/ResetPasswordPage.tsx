import React from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
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

const resetPasswordSchema = z
  .object({
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/,
        'Password must contain uppercase, lowercase, number, and special character'
      ),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>

export const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const navigate = useNavigate()

  const [apiError, setApiError] = React.useState<string | null>(null)
  const [apiSuccess, setApiSuccess] = React.useState<string | null>(null)
  const [isLoading, setIsLoading] = React.useState(false)

  // Additional state for requesting reset link (when token is missing)
  const [requestEmail, setRequestEmail] = React.useState('')
  const [requestError, setRequestError] = React.useState<string | null>(null)
  const [requestSuccess, setRequestSuccess] = React.useState<string | null>(null)
  const [isRequesting, setIsRequesting] = React.useState(false)

  const handleRequestResetLink = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!requestEmail.trim()) {
      setRequestError('Please enter your email address.')
      return
    }
    setIsRequesting(true)
    setRequestError(null)
    setRequestSuccess(null)
    try {
      await authApi.forgotPassword(requestEmail.trim())
      setRequestSuccess('If the account exists, a secure password reset link has been sent to your email.')
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      const msg = axiosError.response?.data?.message || 'Failed to send reset link.'
      setRequestError(msg)
    } finally {
      setIsRequesting(false)
    }
  }

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      password: '',
      confirmPassword: '',
    },
  })

  const onSubmit = async (values: ResetPasswordFormValues) => {
    if (!token) {
      setApiError('Reset token is missing. Please follow the link in your email again.')
      return
    }

    setIsLoading(true)
    setApiError(null)
    setApiSuccess(null)
    try {
      const response = await authApi.resetPassword({
        token,
        newPassword: values.password,
      })
      setApiSuccess(response.message || 'Your password has been reset successfully. You can now log in.')
      setTimeout(() => {
        navigate('/login')
      }, 4000)
    } catch (e: unknown) {
      const axiosError = e as { response?: { data?: { message?: string } } }
      const msg = axiosError.response?.data?.message || 'Failed to reset password. The link may have expired or is invalid.'
      setApiError(msg)
    } finally {
      setIsLoading(false)
    }
  }

  const sidePanel = (
    <AuthSidePanel
      title="New Credentials"
      subtitle="Set up your new secure password below to regain full access to your collaborative workspace."
      ctaLabel="Back to safety?"
      ctaText="Sign In"
      ctaLink="/login"
    />
  )

  return (
    <AuthLayout sidePanel={sidePanel}>
      <AuthCard>
        <AuthHeader
          title="Reset Password"
          subtitle="Enter your new secure password details below."
        />

        {!token ? (
          <form onSubmit={handleRequestResetLink} className="space-y-4">
            <p className="text-xs text-muted-foreground font-semibold leading-relaxed">
              Want to reset or change your password? Enter your email address below and we will send you a secure link to update your credentials.
            </p>

            {requestError && (
              <Alert variant="destructive" className="rounded-xl border border-destructive/20 bg-destructive/5">
                <AlertDescription className="text-xs font-semibold">{requestError}</AlertDescription>
              </Alert>
            )}

            {requestSuccess && (
              <Alert className="rounded-xl border border-emerald-500/20 bg-emerald-500/5">
                <AlertDescription className="text-xs text-emerald-500 font-bold">{requestSuccess}</AlertDescription>
              </Alert>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="requestEmail" className="text-xs font-bold text-foreground">
                Email Address
              </Label>
              <Input
                id="requestEmail"
                type="email"
                placeholder="you@example.com"
                value={requestEmail}
                onChange={(e) => setRequestEmail(e.target.value)}
                className="rounded-xl h-10 border-border/80 text-sm focus-visible:ring-primary/30"
              />
            </div>

            <Button
              type="submit"
              className="w-full h-10 rounded-xl font-bold bg-primary hover:bg-primary/95 text-sm shadow-md shadow-primary/20 transition-all duration-300"
              isLoading={isRequesting}
            >
              Send Reset Link
            </Button>

            <AuthFooter
              backLinkText="Back to Safety"
              backLinkTo="/"
            />
          </form>
        ) : (
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
              <Label htmlFor="password" className="text-xs font-bold text-foreground">
                New Password
              </Label>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                error={!!errors.password}
                className="rounded-xl h-10 border-border/80 text-sm focus-visible:ring-primary/30"
                {...register('password')}
              />
              {errors.password && (
                <p className="text-[10px] text-destructive font-semibold">{errors.password.message}</p>
              )}
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.2 }}
              className="space-y-1.5"
            >
              <Label htmlFor="confirmPassword" className="text-xs font-bold text-foreground">
                Confirm New Password
              </Label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="••••••••"
                error={!!errors.confirmPassword}
                className="rounded-xl h-10 border-border/80 text-sm focus-visible:ring-primary/30"
                {...register('confirmPassword')}
              />
              {errors.confirmPassword && (
                <p className="text-[10px] text-destructive font-semibold">{errors.confirmPassword.message}</p>
              )}
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.25 }}
              className="pt-2"
            >
              <Button
                type="submit"
                className="w-full h-10 rounded-xl font-bold bg-primary hover:bg-primary/95 text-sm shadow-md shadow-primary/20 transition-all duration-300"
                isLoading={isLoading}
              >
                Reset Password
              </Button>
            </motion.div>

            <AuthFooter
              backLinkText="Back to Login"
              backLinkTo="/login"
            />
          </form>
        )}
      </AuthCard>
    </AuthLayout>
  )
}

export default ResetPasswordPage
