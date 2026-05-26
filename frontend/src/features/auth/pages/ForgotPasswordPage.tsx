import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Link } from 'react-router-dom'
import { authApi } from '../api/authApi'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card'
import { Label } from '@/components/ui/Label'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Alert, AlertDescription } from '@/components/ui/Alert'
import { AuthLayout } from '../components/AuthLayout'
import { Logo } from '@/components/branding/Logo'

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
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Something went wrong. Please try again.'
      setApiError(msg)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <AuthLayout>
      <Card className="border border-border bg-card/85 backdrop-blur-lg shadow-xl">
        <CardHeader className="space-y-1 text-center flex flex-col items-center">
          <Logo size="lg" showText={false} className="mb-2" />
          <CardTitle className="text-3xl font-extrabold tracking-tight">Forgot Password</CardTitle>
          <CardDescription className="text-muted-foreground">
            Enter your email to receive a password reset link
          </CardDescription>
        </CardHeader>

        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-4">
            {apiError && (
              <Alert variant="destructive">
                <AlertDescription>{apiError}</AlertDescription>
              </Alert>
            )}

            {apiSuccess && (
              <Alert>
                <AlertDescription className="text-emerald-500 font-semibold">{apiSuccess}</AlertDescription>
              </Alert>
            )}

            <div className="space-y-2">
              <Label htmlFor="email">Email Address</Label>
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
          </CardContent>

          <CardFooter className="flex flex-col space-y-4">
            <Button type="submit" className="w-full" isLoading={isLoading}>
              Send Reset Link
            </Button>
            <div className="text-sm text-center text-muted-foreground">
              Remember your password?{' '}
              <Link to="/login" className="text-primary hover:underline font-semibold">
                Sign in
              </Link>
            </div>
          </CardFooter>
        </form>
      </Card>
    </AuthLayout>
  )
}
