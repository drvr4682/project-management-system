import React from 'react'
import { Provider } from 'react-redux'
import { RouterProvider } from 'react-router-dom'
import { store } from '@/app/store'
import { AuthLoader } from '@/components/AuthLoader'
import { router } from '@/routes'
import { Toaster } from 'sonner'

export const App: React.FC = () => {
  return (
    <Provider store={store}>
      <AuthLoader>
        <RouterProvider router={router} />
        <Toaster position="top-right" richColors />
      </AuthLoader>
    </Provider>
  )
}

export default App
