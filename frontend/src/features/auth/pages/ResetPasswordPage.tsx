import React from 'react'
import { useSearchParams, Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { authApi } from '../api/authApi'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card'
import { Label } from '@/components/ui/Label'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Alert, AlertDescription } from '@/components/ui/Alert'
import { AuthLayout } from '../components/AuthLayout'
import { Logo } from '@/components/branding/Logo'

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
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to reset password. The link may have expired or is invalid.'
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
          <CardTitle className="text-3xl font-extrabold tracking-tight">Reset Password</CardTitle>
          <CardDescription className="text-muted-foreground">
            Enter your new password below
          </CardDescription>
        </CardHeader>

        {!token ? (
          <CardContent className="space-y-4">
            <Alert variant="destructive">
              <AlertDescription>
                Reset token is missing in the URL query parameters. Please make sure to copy/paste the entire link from your email.
              </AlertDescription>
            </Alert>
          </CardContent>
        ) : (
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
                <Label htmlFor="password">New Password</Label>
                <Input
                  id="password"
                  type="password"
                  placeholder="••••••••"
                  error={!!errors.password}
                  {...register('password')}
                />
                {errors.password && (
                  <p className="text-xs text-destructive font-semibold">{errors.password.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirmPassword">Confirm New Password</Label>
                <Input
                  id="confirmPassword"
                  type="password"
                  placeholder="••••••••"
                  error={!!errors.confirmPassword}
                  {...register('confirmPassword')}
                />
                {errors.confirmPassword && (
                  <p className="text-xs text-destructive font-semibold">{errors.confirmPassword.message}</p>
                )}
              </div>
            </CardContent>

            <CardFooter className="flex flex-col space-y-4">
              <Button type="submit" className="w-full" isLoading={isLoading}>
                Reset Password
              </Button>
            </CardFooter>
          </form>
        )}

        <div className="pb-6 text-sm text-center text-muted-foreground border-t border-border/50 pt-4">
          <Link to="/login" className="text-primary hover:underline font-semibold">
            Back to Sign In
          </Link>
        </div>
      </Card>
    </AuthLayout>
  )
}
