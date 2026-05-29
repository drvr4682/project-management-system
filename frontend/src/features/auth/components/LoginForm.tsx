import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAppDispatch } from '@/hooks/store'
import { setCredentials } from '../store/authSlice'
import { authApi } from '../api/authApi'
import { Label } from '@/components/ui/Label'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Alert, AlertDescription } from '@/components/ui/Alert'
import { AuthHeader } from './AuthHeader'
import { AuthFooter } from './AuthFooter'
import type { UserRole } from '../types/authTypes'
import { cn } from '@/lib/utils'

const loginSchema = z.object({
  email: z.string().min(1, 'Email or Username is required'),
  password: z.string().min(1, 'Password is required'),
})

type LoginFormValues = z.infer<typeof loginSchema>

interface LoginFormProps {
  isRegister: boolean
}

export const LoginForm: React.FC<LoginFormProps> = ({ isRegister }) => {
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
        emailOrUsername: values.email,
        password: values.password,
      })

      const accessToken = response.token
      const refreshToken = response.refreshToken

      const user = {
        id: response.id,
        email: response.email,
        userName: response.userName,
        role: response.role as UserRole,
      }

      dispatch(setCredentials({ user, accessToken, refreshToken }))
      navigate('/', { replace: true })
    } catch (e: unknown) {
      const axiosError = e as { response?: { data?: { message?: string } } }
      const msg = axiosError.response?.data?.message || 'Invalid email or password'
      setApiError(msg)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: isRegister ? 0 : 1 }}
      transition={{
        duration: 0.9,
        ease: [0.25, 1, 0.5, 1],
      }}
      className={cn(
        'w-full md:w-1/2 h-full flex flex-col justify-center p-6 md:pl-12 md:pr-8 md:py-6 bg-card relative z-10',
        isRegister ? 'hidden md:flex pointer-events-none select-none' : 'flex'
      )}
    >
      <AuthHeader
        title="Sign In"
        subtitle="Welcome back! Please enter your workspace credentials."
      />

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-2">
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

        <div className="space-y-1.5">
          <Label htmlFor="email" className="text-xs font-bold text-foreground">
            Email or Username
          </Label>
          <Input
            id="email"
            type="text"
            placeholder="you@example.com or username"
            error={!!errors.email}
            className="rounded-xl h-10 border-border/80 text-sm focus-visible:ring-primary/30"
            {...register('email')}
          />
          {errors.email && (
            <p className="text-[10px] text-destructive font-semibold">{errors.email.message}</p>
          )}
        </div>

        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <Label htmlFor="password" className="text-xs font-bold text-foreground">
              Password
            </Label>
            <Link
              to="/forgot-password"
              className="text-[11px] text-primary hover:underline font-bold transition-all"
            >
              Forgot password?
            </Link>
          </div>
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

        <div className="pt-0.5">
          <Button
            type="submit"
            className="w-full h-10 rounded-xl font-bold bg-primary hover:bg-primary/95 text-sm shadow-md shadow-primary/20 transition-all duration-300"
            isLoading={isLoading}
          >
            Sign In
          </Button>
        </div>
      </form>

      {/* Mobile view only switcher helper link */}
      <div className="md:hidden">
        <AuthFooter
          text="Don't have an account?"
          linkText="Sign up"
          linkTo="/register"
        />
      </div>
    </motion.div>
  )
}

export default LoginForm
