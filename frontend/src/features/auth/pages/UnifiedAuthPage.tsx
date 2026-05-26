import React from 'react'
import { useLocation } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { LoginForm } from '../components/LoginForm'
import { RegisterForm } from '../components/RegisterForm'

export const UnifiedAuthPage: React.FC = () => {
  const location = useLocation()
  const isRegister = location.pathname === '/register'

  return (
    <AuthLayout isRegister={isRegister} isSlider={true}>
      {/* Left side (desktop): RegisterForm */}
      <RegisterForm isRegister={isRegister} />

      {/* Right side (desktop): LoginForm */}
      <LoginForm isRegister={isRegister} />
    </AuthLayout>
  )
}

export default UnifiedAuthPage
