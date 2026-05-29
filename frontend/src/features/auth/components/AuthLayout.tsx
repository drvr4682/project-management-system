import React from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Link } from 'react-router-dom'
import { Logo } from '@/components/branding/Logo'

interface AuthLayoutProps {
  isRegister?: boolean
  isSlider?: boolean
  children: React.ReactNode
  sidePanel?: React.ReactNode
}

export const AuthLayout: React.FC<AuthLayoutProps> = ({
  isRegister = false,
  isSlider = false,
  children,
  sidePanel,
}) => {
  // 3D Tilt Effect State
  const [rotateX, setRotateX] = React.useState(0)
  const [rotateY, setRotateY] = React.useState(0)

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    const card = e.currentTarget
    const box = card.getBoundingClientRect()
    const x = e.clientX - box.left - box.width / 2
    const y = e.clientY - box.top - box.height / 2
    
    // Subtle tilt: max 3.5 degrees
    setRotateX(-y / 65)
    setRotateY(x / 65)
  }

  const handleMouseLeave = () => {
    setRotateX(0)
    setRotateY(0)
  }

  if (!isSlider) {
    // Compact static layout for forgot-password, reset-password, verify-email
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#F9FAFB] p-4 md:p-6 relative overflow-hidden font-sans">
        <div className="absolute top-[-15%] left-[-15%] w-[55%] h-[55%] rounded-full bg-primary/10 blur-[130px] pointer-events-none" />
        <div className="absolute bottom-[-15%] right-[-15%] w-[55%] h-[55%] rounded-full bg-primary/15 blur-[130px] pointer-events-none" />

        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.55, ease: 'easeOut' }}
          className="w-full max-w-3xl min-h-[500px] md:h-[580px] bg-card rounded-[28px] overflow-hidden border border-border/80 shadow-2xl grid grid-cols-1 md:grid-cols-12 relative z-10"
        >
          {/* Static Side Panel */}
          <div className="hidden md:block md:col-span-5 h-full">
            {sidePanel ? (
              sidePanel
            ) : (
              <div className="relative h-full w-full bg-gradient-to-br from-violet-600 via-violet-700 to-indigo-900 text-white p-6 md:p-8 flex flex-col justify-between overflow-hidden">
                <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full blur-3xl -mr-16 -mt-16 pointer-events-none" />
                <div className="absolute bottom-0 left-0 w-80 h-80 bg-violet-500/20 rounded-full blur-3xl -ml-20 -mb-20 pointer-events-none" />
                <div className="z-10">
                  <Link to="/" className="inline-block">
                    <Logo size="md" light />
                  </Link>
                </div>
                <div className="my-auto py-12 space-y-4 z-10">
                  <h2 className="text-2xl font-extrabold tracking-tight leading-tight">Password & Verification</h2>
                  <p className="text-violet-200/90 text-xs font-semibold leading-relaxed">
                    Regain account access, confirm credentials, and keep your collaborative workspace secure.
                  </p>
                </div>
                <div className="z-10 space-y-4 border-t border-white/10 pt-6">
                  <Link to="/login" className="inline-block">
                    <motion.button
                      whileHover={{ scale: 1.03, backgroundColor: 'rgba(255,255,255,1)', color: '#7C3AED' }}
                      whileTap={{ scale: 0.98 }}
                      className="px-5 py-2 rounded-xl border border-white/30 text-white font-bold text-xs bg-white/5 backdrop-blur-sm transition-all duration-300 shadow-sm"
                    >
                      Back to Sign In
                    </motion.button>
                  </Link>
                </div>
              </div>
            )}
          </div>

          {/* Form Content */}
          <div className="col-span-12 md:col-span-7 flex flex-col justify-center">
            {children}
          </div>
        </motion.div>
      </div>
    )
  }

  // Unified Sliding Mode for Login & Register (Double Slider with 3D Hover Tilt)
  return (
    <div className="flex items-center justify-center min-h-screen bg-[#F9FAFB] p-4 md:p-6 relative overflow-hidden font-sans perspective-[1200px]">
      <div className="absolute top-[-15%] left-[-15%] w-[55%] h-[55%] rounded-full bg-primary/10 blur-[130px] pointer-events-none" />
      <div className="absolute bottom-[-15%] right-[-15%] w-[55%] h-[55%] rounded-full bg-primary/15 blur-[130px] pointer-events-none" />

      <motion.div
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        animate={{ rotateX, rotateY }}
        transition={{ type: 'spring', stiffness: 120, damping: 25 }}
        style={{ transformStyle: 'preserve-3d' }}
        className="w-full max-w-3xl min-h-[500px] md:h-[580px] bg-card rounded-[28px] overflow-hidden border border-border/80 shadow-2xl flex flex-col md:flex-row relative z-10 transition-shadow duration-300 hover:shadow-primary/5 hover:shadow-3xl"
      >
        {children}

        {/* Absolute Sliding Overlay Panel (hidden on mobile) */}
        <motion.div
          animate={{
            x: isRegister ? '100%' : '0%',
            borderTopLeftRadius: isRegister ? '160px' : '0px',
            borderBottomLeftRadius: isRegister ? '160px' : '0px',
            borderTopRightRadius: isRegister ? '0px' : '160px',
            borderBottomRightRadius: isRegister ? '0px' : '160px',
          }}
          transition={{
            duration: 0.9,
            ease: [0.25, 1, 0.5, 1], // Cinematic cubic-bezier glide
          }}
          className="hidden md:block absolute top-0 bottom-0 left-0 w-1/2 h-full z-20 overflow-hidden bg-gradient-to-br from-violet-600 via-violet-700 to-indigo-900 text-white"
          style={{ transform: 'translateZ(10px)' }} // 3D Layer Elevation
        >
          <motion.div
            animate={{
              paddingLeft: isRegister ? '96px' : '48px',
              paddingRight: isRegister ? '48px' : '96px',
            }}
            transition={{
              duration: 0.9,
              ease: [0.25, 1, 0.5, 1],
            }}
            className="relative h-full w-full flex flex-col justify-between overflow-hidden py-10 text-white"
          >
            {/* Shimmer Ambient shapes */}
            <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full blur-3xl -mr-16 -mt-16 pointer-events-none" />
            <div className="absolute bottom-0 left-0 w-80 h-80 bg-violet-500/20 rounded-full blur-3xl -ml-20 -mb-20 pointer-events-none" />

            {/* Logo */}
            <div className="z-10">
              <Link to="/" className="inline-block">
                <Logo size="md" light />
              </Link>
            </div>

            {/* Sliding text based on registration status */}
            <div className="my-auto py-8 space-y-4 z-10 min-h-[160px] flex flex-col justify-center">
              <AnimatePresence mode="wait">
                {!isRegister ? (
                  <motion.div
                    key="login-msg"
                    initial={{ opacity: 0, x: -25 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: 25 }}
                    transition={{ duration: 0.3 }}
                    className="space-y-3.5"
                  >
                    <h2 className="text-2xl font-extrabold tracking-tight leading-tight">Hello, Welcome!</h2>
                    <p className="text-violet-200/95 text-xs md:text-sm font-semibold leading-relaxed">
                      Join your team space, track deliverables, and manage milestones on DRVRHub.
                    </p>
                  </motion.div>
                ) : (
                  <motion.div
                    key="register-msg"
                    initial={{ opacity: 0, x: 25 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -25 }}
                    transition={{ duration: 0.3 }}
                    className="space-y-3.5"
                  >
                    <h2 className="text-2xl font-extrabold tracking-tight leading-tight">Welcome Back!</h2>
                    <p className="text-violet-200/95 text-xs md:text-sm font-semibold leading-relaxed">
                      To keep connected with your teammates and maintain project velocity, please sign in.
                    </p>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* CTA Switcher */}
            <div className="z-10 space-y-4 border-t border-white/10 pt-4">
              <AnimatePresence mode="wait">
                {!isRegister ? (
                  <motion.div
                    key="login-cta"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    transition={{ duration: 0.25 }}
                    className="space-y-2.5"
                  >
                    <div className="text-[10px] text-violet-200 font-bold uppercase tracking-wider">Don't have an account?</div>
                    <Link to="/register">
                      <motion.button
                        whileHover={{ scale: 1.03, backgroundColor: 'rgba(255,255,255,1)', color: '#7C3AED' }}
                        whileTap={{ scale: 0.98 }}
                        className="px-5 py-2 rounded-xl border border-white/30 text-white font-bold text-xs bg-white/5 backdrop-blur-sm transition-all duration-300 shadow-sm"
                      >
                        Register
                      </motion.button>
                    </Link>
                  </motion.div>
                ) : (
                  <motion.div
                    key="register-cta"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    transition={{ duration: 0.25 }}
                    className="space-y-2.5"
                  >
                    <div className="text-[10px] text-violet-200 font-bold uppercase tracking-wider">Already have an account?</div>
                    <Link to="/login">
                      <motion.button
                        whileHover={{ scale: 1.03, backgroundColor: 'rgba(255,255,255,1)', color: '#7C3AED' }}
                        whileTap={{ scale: 0.98 }}
                        className="px-5 py-2 rounded-xl border border-white/30 text-white font-bold text-xs bg-white/5 backdrop-blur-sm transition-all duration-300 shadow-sm"
                      >
                        Sign In
                      </motion.button>
                    </Link>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </motion.div>
        </motion.div>
      </motion.div>
    </div>
  )
}

export default AuthLayout
