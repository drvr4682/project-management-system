import React from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { authApi } from '../api/authApi'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card'
import { Label } from '@/components/ui/Label'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Alert, AlertDescription } from '@/components/ui/Alert'
import { AuthLayout } from '../components/AuthLayout'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Logo } from '@/components/branding/Logo'

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

  return (
    <AuthLayout>
      <Card className="border border-border bg-card/85 backdrop-blur-lg shadow-xl text-center">
        <CardHeader className="space-y-1 flex flex-col items-center">
          <Logo size="lg" showText={false} className="mb-2" />
          <CardTitle className="text-3xl font-extrabold tracking-tight">Email Verification</CardTitle>
          <CardDescription className="text-muted-foreground">
            Verifying your registration details with the server
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-6">
          {verificationStatus === 'verifying' && (
            <div className="flex flex-col items-center justify-center py-6 space-y-4">
              <div className="relative w-12 h-12">
                <div className="absolute w-full h-full border-4 border-primary/20 rounded-full"></div>
                <div className="absolute w-full h-full border-4 border-t-primary rounded-full animate-spin"></div>
              </div>
              <p className="text-sm font-medium text-muted-foreground">
                Verifying your token. Please wait...
              </p>
            </div>
          )}

          {verificationStatus === 'success' && (
            <div className="space-y-4 py-4">
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
              <p className="text-emerald-500 font-semibold">{successMessage}</p>
            </div>
          )}

          {verificationStatus === 'failed' && (
            <div className="space-y-4">
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
                <Alert variant="destructive">
                  <AlertDescription>{errorMessage}</AlertDescription>
                </Alert>
              )}

              {successMessage && (
                <Alert>
                  <AlertDescription className="text-emerald-500 font-semibold">{successMessage}</AlertDescription>
                </Alert>
              )}

              <div className="border-t border-border pt-4 text-left">
                <h4 className="text-sm font-bold text-foreground mb-2">Resend Verification Email</h4>
                <form onSubmit={handleSubmit(onResend)} className="space-y-3">
                  <div className="space-y-1">
                    <Label htmlFor="email" className="text-xs">Email Address</Label>
                    <Input
                      id="email"
                      type="email"
                      placeholder="you@example.com"
                      error={!!errors.email}
                      {...register('email')}
                    />
                    {errors.email && (
                      <p className="text-xs text-destructive font-semibold">{errors.email.message}</p>
                    )}
                  </div>
                  <Button type="submit" size="sm" className="w-full" isLoading={resendLoading}>
                    Send Link
                  </Button>
                </form>
              </div>
            </div>
          )}
        </CardContent>

        <CardFooter className="justify-center border-t border-border/50 pt-4">
          <Link to="/login" className="text-sm text-primary hover:underline font-semibold">
            Return to Login
          </Link>
        </CardFooter>
      </Card>
    </AuthLayout>
  )
}
