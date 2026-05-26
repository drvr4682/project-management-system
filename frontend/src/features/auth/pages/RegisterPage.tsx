import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/authApi'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card'
import { Label } from '@/components/ui/Label'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Alert, AlertDescription } from '@/components/ui/Alert'
import { AuthLayout } from '../components/AuthLayout'
import type { UserRole } from '../types/authTypes'
import { Logo } from '@/components/branding/Logo'

const registerSchema = z
  .object({
    name: z.string().min(1, 'Name is required'),
    email: z.string().email('Please enter a valid email address').min(1, 'Email is required'),
    role: z.enum(['USER', 'ADMIN']),
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

type RegisterFormValues = z.infer<typeof registerSchema>

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate()
  const [apiError, setApiError] = React.useState<string | null>(null)
  const [apiSuccess, setApiSuccess] = React.useState<string | null>(null)
  const [isLoading, setIsLoading] = React.useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      name: '',
      email: '',
      role: 'USER',
      password: '',
      confirmPassword: '',
    },
  })

  const onSubmit = async (values: RegisterFormValues) => {
    setIsLoading(true)
    setApiError(null)
    setApiSuccess(null)
    try {
      await authApi.register({
        name: values.name,
        email: values.email,
        role: values.role as UserRole,
        password: values.password,
      })

      setApiSuccess('Registration successful! Please check your email to verify your account.')
      // Optionally redirect to login after a delay
      setTimeout(() => {
        navigate('/login')
      }, 5000)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Registration failed. Please try again.'
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
          <CardTitle className="text-3xl font-extrabold tracking-tight">Create Account</CardTitle>
          <CardDescription className="text-muted-foreground">
            Get started with your DRVRHub workspace
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
              <Label htmlFor="name">Full Name</Label>
              <Input
                id="name"
                type="text"
                placeholder="John Doe"
                error={!!errors.name}
                {...register('name')}
              />
              {errors.name && (
                <p className="text-xs text-destructive font-semibold">{errors.name.message}</p>
              )}
            </div>

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
              <Label htmlFor="role">Account Role</Label>
              <select
                id="role"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                {...register('role')}
              >
                <option value="USER">Standard User</option>
                <option value="ADMIN">Administrator</option>
              </select>
              {errors.role && (
                <p className="text-xs text-destructive font-semibold">{errors.role.message}</p>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
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
                <Label htmlFor="confirmPassword">Confirm Password</Label>
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
            </div>
          </CardContent>

          <CardFooter className="flex flex-col space-y-4">
            <Button type="submit" className="w-full" isLoading={isLoading}>
              Sign Up
            </Button>
            <div className="text-sm text-center text-muted-foreground">
              Already have an account?{' '}
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
