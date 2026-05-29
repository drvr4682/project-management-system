import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { useNavigate, Navigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAppDispatch, useAppSelector } from '@/hooks/store'
import { selectProfile, setProfileData } from '../store/profileSlice'
import { profileApi } from '../api/profileApi'
import { Label } from '@/components/ui/Label'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Alert, AlertDescription } from '@/components/ui/Alert'
import { AuthLayout } from '../components/AuthLayout'
import { AuthSidePanel } from '../components/AuthSidePanel'
import { AuthCard } from '../components/AuthCard'
import { AuthHeader } from '../components/AuthHeader'

const completeProfileSchema = z.object({
  firstName: z.string().min(1, 'First name is required').max(100, 'First name cannot exceed 100 characters'),
  surname: z.string().max(100, 'Surname cannot exceed 100 characters').optional(),
})

type CompleteProfileFormValues = z.infer<typeof completeProfileSchema>

export const CompleteProfilePage: React.FC = () => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const { profileCompleted } = useAppSelector(selectProfile)
  const [apiError, setApiError] = React.useState<string | null>(null)
  const [isLoading, setIsLoading] = React.useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CompleteProfileFormValues>({
    resolver: zodResolver(completeProfileSchema),
    defaultValues: {
      firstName: '',
      surname: '',
    },
  })

  // Prevent accessing onboarding page if profile is already completed
  if (profileCompleted === true) {
    return <Navigate to="/" replace />
  }

  const onSubmit = async (values: CompleteProfileFormValues) => {
    setIsLoading(true)
    setApiError(null)
    try {
      const profile = await profileApi.completeProfile({
        firstName: values.firstName,
        surname: values.surname || '',
      })
      // Update Redux status
      dispatch(setProfileData(profile))
      navigate('/')
    } catch (e: unknown) {
      const axiosError = e as { response?: { data?: { message?: string } } }
      const msg = axiosError.response?.data?.message || 'Failed to save profile. Please try again.'
      setApiError(msg)
    } finally {
      setIsLoading(false)
    }
  }

  const sidePanel = (
    <AuthSidePanel
      title="Complete Your Profile"
      subtitle="Just a final step! Introduce yourself so your teammates can identify and collaborate with you on DRVRHub workspace."
      ctaLabel="Need assistance?"
      ctaText="Support Desk"
      ctaLink="/support"
    />
  )

  return (
    <AuthLayout sidePanel={sidePanel}>
      <AuthCard>
        <AuthHeader
          title="Onboarding Setup"
          subtitle="Please fill out your basic professional identity."
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

          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.15 }}
            className="space-y-1.5"
          >
            <Label htmlFor="firstName" className="text-xs font-bold text-foreground">
              First Name <span className="text-destructive">*</span>
            </Label>
            <Input
              id="firstName"
              type="text"
              placeholder="e.g. John"
              error={!!errors.firstName}
              className="rounded-xl h-10 border-border/80 text-sm focus-visible:ring-primary/30"
              {...register('firstName')}
            />
            {errors.firstName && (
              <p className="text-[10px] text-destructive font-semibold">{errors.firstName.message}</p>
            )}
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.2 }}
            className="space-y-1.5"
          >
            <Label htmlFor="surname" className="text-xs font-bold text-foreground">
              Surname (Optional)
            </Label>
            <Input
              id="surname"
              type="text"
              placeholder="e.g. Doe"
              error={!!errors.surname}
              className="rounded-xl h-10 border-border/80 text-sm focus-visible:ring-primary/30"
              {...register('surname')}
            />
            {errors.surname && (
              <p className="text-[10px] text-destructive font-semibold">{errors.surname.message}</p>
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
              Get Started
            </Button>
          </motion.div>
        </form>
      </AuthCard>
    </AuthLayout>
  )
}

export default CompleteProfilePage
