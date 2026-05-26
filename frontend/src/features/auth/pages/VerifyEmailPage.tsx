import React from 'react'
import { useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
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

const resendSchema = z.object({
  email: z.string().email('Please enter a valid email address').min(1, 'Email is required'),
})

type ResendFormValues = z.infer<typeof resendSchema>

export const VerifyEmailPage: React.FC = () => {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')

  const [verificationStatus, setVerificationStatus] = React.useState<'idle' | 'verifying' | 'success' | 'failed'>('idle')
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null)
  const [successMessage, setSuccessMessage] = React.useState<string | null>(null)
  const [resendLoading, setResendLoading] = React.useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResendFormValues>({
    resolver: zodResolver(resendSchema),
    defaultValues: { email: '' },
  })

  // Auto-verify token on mount
  React.useEffect(() => {
    if (!token) {
      setVerificationStatus('failed')
      setErrorMessage('Verification token is missing. Please request a new verification link.')
      return
    }

    const verify = async () => {
      setVerificationStatus('verifying')
      try {
        await authApi.verifyEmail(token)
        setVerificationStatus('success')
        setSuccessMessage('Your email has been verified successfully!')
      } catch (e: any) {
        const msg = e.response?.data?.message || 'Verification failed. The token may be expired or invalid.'
        setVerificationStatus('failed')
        setErrorMessage(msg)
      }
    }

    verify()
  }, [token])

  const onResend = async (values: ResendFormValues) => {
    setResendLoading(true)
    setErrorMessage(null)
    setSuccessMessage(null)
    try {
      await authApi.resendVerification(values.email)
      setSuccessMessage('A new verification email has been sent successfully!')
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to resend verification email.'
      setErrorMessage(msg)
    } finally {
      setResendLoading(false)
    }
  }

  const sidePanel = (
    <AuthSidePanel
      title="Email Validation"
      subtitle="Confirm your registration details to unlock your DRVRHub dashboard metrics and collaboration hubs."
      ctaLabel="Back to safety?"
      ctaText="Sign In"
      ctaLink="/login"
    />
  )

  return (
    <AuthLayout sidePanel={sidePanel}>
      <AuthCard>
        <AuthHeader
          title="Email Verification"
          subtitle="Verifying your registration details with the server."
        />

        <div className="space-y-6">
          {verificationStatus === 'verifying' && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="flex flex-col items-center justify-center py-6 space-y-4"
            >
              <div className="relative w-12 h-12">
                <div className="absolute w-full h-full border-4 border-primary/20 rounded-full"></div>
                <div className="absolute w-full h-full border-4 border-t-primary rounded-full animate-spin"></div>
              </div>
              <p className="text-sm font-semibold text-muted-foreground">
                Verifying your token. Please wait...
              </p>
            </motion.div>
          )}

          {verificationStatus === 'success' && (
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="space-y-4 py-4 text-center"
            >
              <div className="mx-auto w-16 h-16 rounded-full bg-emerald-500/10 flex items-center justify-center">
                <svg
                  className="w-8 h-8 text-emerald-500"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <p className="text-emerald-500 font-bold text-sm">{successMessage}</p>
            </motion.div>
          )}

          {verificationStatus === 'failed' && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="space-y-4"
            >
              <div className="mx-auto w-16 h-16 rounded-full bg-destructive/10 flex items-center justify-center mb-2">
                <svg
                  className="w-8 h-8 text-destructive"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </div>

              {errorMessage && (
                <Alert variant="destructive" className="rounded-xl border border-destructive/20 bg-destructive/5 text-left">
                  <AlertDescription className="text-xs font-semibold">{errorMessage}</AlertDescription>
                </Alert>
              )}

              {successMessage && (
                <Alert className="rounded-xl border border-emerald-500/20 bg-emerald-500/5 text-left">
                  <AlertDescription className="text-xs text-emerald-500 font-bold">{successMessage}</AlertDescription>
                </Alert>
              )}

              <div className="border-t border-border/60 pt-4 text-left">
                <h4 className="text-xs font-bold text-foreground mb-2">Resend Verification Email</h4>
                <form onSubmit={handleSubmit(onResend)} className="space-y-3">
                  <div className="space-y-1">
                    <Label htmlFor="email" className="text-xs font-bold text-foreground">Email Address</Label>
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
                  </div>
                  <Button
                    type="submit"
                    className="w-full h-10 rounded-xl font-bold bg-primary hover:bg-primary/95 text-sm shadow-md"
                    isLoading={resendLoading}
                  >
                    Send Link
                  </Button>
                </form>
              </div>
            </motion.div>
          )}
        </div>

        <AuthFooter
          backLinkText="Return to Login"
          backLinkTo="/login"
        />
      </AuthCard>
    </AuthLayout>
  )
}

export default VerifyEmailPage
