import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { authApi } from '../api/authApi'
import { Label } from '@/components/ui/Label'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Alert, AlertDescription } from '@/components/ui/Alert'
import { AuthHeader } from './AuthHeader'
import { AuthFooter } from './AuthFooter'
import { cn } from '@/lib/utils'

const registerSchema = z
  .object({
    userName: z
      .string()
      .min(3, 'Username must be at least 3 characters')
      .max(30, 'Username must not exceed 30 characters')
      .regex(/^[a-zA-Z0-9_]{3,30}$/, 'Username can only contain alphanumeric characters and underscores'),
    email: z.string().email('Please enter a valid email address').min(1, 'Email is required'),
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

interface RegisterFormProps {
  isRegister: boolean
}

export const RegisterForm: React.FC<RegisterFormProps> = ({ isRegister }) => {
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
      userName: '',
      email: '',
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
        userName: values.userName,
        email: values.email,
        password: values.password,
      })

      setApiSuccess('Registration successful! Please check your email to verify your account.')
      setTimeout(() => {
        navigate('/login')
      }, 5000)
    } catch (e: unknown) {
      const axiosError = e as { response?: { data?: { message?: string } } }
      const msg = axiosError.response?.data?.message || 'Registration failed. Please try again.'
      setApiError(msg)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: !isRegister ? 0 : 1 }}
      transition={{
        duration: 0.9,
        ease: [0.25, 1, 0.5, 1],
      }}
      className={cn(
        'w-full md:w-1/2 h-full flex flex-col justify-center p-6 md:pr-12 md:pl-8 md:py-6 bg-card relative z-10',
        !isRegister ? 'hidden md:flex pointer-events-none select-none' : 'flex'
      )}
    >
      <AuthHeader
        title="Create Account"
        subtitle="Get started with your DRVRHub collaborative workspace."
      />

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-1.5">
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

        <div className="space-y-1">
          <Label htmlFor="userName" className="text-xs font-bold text-foreground">
            Username
          </Label>
          <Input
            id="userName"
            type="text"
            placeholder="johndoe_99"
            error={!!errors.userName}
            className="rounded-xl h-10 border-border/80 text-sm focus-visible:ring-primary/30"
            {...register('userName')}
          />
          {errors.userName && (
            <p className="text-[10px] text-destructive font-semibold">{errors.userName.message}</p>
          )}
        </div>

        <div className="space-y-1">
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
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
          <div className="space-y-1">
            <Label htmlFor="password" className="text-xs font-bold text-foreground">
              Password
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
          </div>

          <div className="space-y-1">
            <Label htmlFor="confirmPassword" className="text-xs font-bold text-foreground">
              Confirm Password
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
          </div>
        </div>

        <div className="pt-0.5">
          <Button
            type="submit"
            className="w-full h-10 rounded-xl font-bold bg-primary hover:bg-primary/95 text-sm shadow-md shadow-primary/20 transition-all duration-300"
            isLoading={isLoading}
          >
            Sign Up
          </Button>
        </div>
      </form>

      {/* Mobile view only switcher helper link */}
      <div className="md:hidden">
        <AuthFooter
          text="Already have an account?"
          linkText="Sign in"
          linkTo="/login"
        />
      </div>
    </motion.div>
  )
}

export default RegisterForm
