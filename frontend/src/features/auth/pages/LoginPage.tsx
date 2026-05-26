import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { useAppDispatch } from '@/hooks/store'
import { setCredentials } from '../store/authSlice'
import { authApi } from '../api/authApi'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card'
import { Label } from '@/components/ui/Label'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Alert, AlertDescription } from '@/components/ui/Alert'
import { AuthLayout } from '../components/AuthLayout'
import type { UserRole } from '../types/authTypes'
import { Logo } from '@/components/branding/Logo'

const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address').min(1, 'Email is required'),
  password: z.string().min(1, 'Password is required'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export const LoginPage: React.FC = () => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [apiError, setApiError] = React.useState<string | null>(null)
  const [isLoading, setIsLoading] = React.useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  const onSubmit = async (values: LoginFormValues) => {
    setIsLoading(true)
    setApiError(null)
    try {
      const response = await authApi.login({
        email: values.email,
        password: values.password,
      })

      const userEmail = response.email
      const userRole = response.role as UserRole
      const accessToken = response.token
      const refreshToken = response.refreshToken

      const user = {
        id: response.id || 1,
        email: userEmail,
        name: response.name || userEmail.split('@')[0],
        role: userRole,
      }

      dispatch(setCredentials({ user, accessToken, refreshToken }))
      navigate('/', { replace: true })
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Invalid email or password'
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
          <CardTitle className="text-3xl font-extrabold tracking-tight">Sign In</CardTitle>
          <CardDescription className="text-muted-foreground">
            Enter your credentials to access your workspace
          </CardDescription>
        </CardHeader>
        
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-4">
            {apiError && (
              <Alert variant="destructive">
                <AlertDescription>{apiError}</AlertDescription>
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

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password">Password</Label>
                <Link
                  to="/forgot-password"
                  className="text-xs text-primary hover:underline font-medium"
                >
                  Forgot password?
                </Link>
              </div>
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
          </CardContent>

          <CardFooter className="flex flex-col space-y-4">
            <Button type="submit" className="w-full" isLoading={isLoading}>
              Sign In
            </Button>
            <div className="text-sm text-center text-muted-foreground">
              Don't have an account?{' '}
              <Link to="/register" className="text-primary hover:underline font-semibold">
                Sign up
              </Link>
            </div>
          </CardFooter>
        </form>
      </Card>
    </AuthLayout>
  )
}
