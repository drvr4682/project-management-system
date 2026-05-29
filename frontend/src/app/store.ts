import { configureStore } from '@reduxjs/toolkit'
import authReducer from '@/features/auth/store/authSlice'
import profileReducer from '@/features/auth/store/profileSlice'
import projectsReducer from '@/features/projects/store/projectSlice'
import tasksReducer from '@/features/tasks/store/taskSlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    profile: profileReducer,
    projects: projectsReducer,
    tasks: tasksReducer,
  },
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
