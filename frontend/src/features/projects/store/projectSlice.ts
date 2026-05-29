import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit'
import projectApi, { type ProjectResponseDTO, type ProjectMemberResponseDTO } from '../api/projectApi'

export interface ProjectState {
  items: ProjectResponseDTO[]
  activeProject: ProjectResponseDTO | null
  members: ProjectMemberResponseDTO[]
  filters: {
    status?: string
    search?: string
  }
  pagination: {
    page: number
    size: number
    totalPages: number
    totalElements: number
  }
  initialized: boolean
  loading: boolean
  error: string | null
}

const initialState: ProjectState = {
  items: [],
  activeProject: null,
  members: [],
  filters: {
    status: undefined,
    search: undefined,
  },
  pagination: {
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
  },
  initialized: false,
  loading: false,
  error: null,
}

// ---------------------------------------------------------------------------
// ASYNC THUNKS
// ---------------------------------------------------------------------------

export const fetchProjects = createAsyncThunk(
  'projects/fetchProjects',
  async (params: { page?: number; size?: number; status?: string; search?: string } | undefined, { getState, rejectWithValue }) => {
    try {
      const state = getState() as { projects: ProjectState }
      const filters = state.projects.filters
      const pagination = state.projects.pagination

      const mergedParams = {
        page: params?.page ?? pagination.page,
        size: params?.size ?? pagination.size,
        status: params?.status !== undefined ? params.status : filters.status,
        search: params?.search !== undefined ? params.search : filters.search,
      }

      // Convert empty strings to undefined to avoid empty param passing
      if (!mergedParams.status) delete mergedParams.status
      if (!mergedParams.search) delete mergedParams.search

      const response = await projectApi.getProjects(mergedParams)
      return response
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch projects')
    }
  }
)

export const fetchProjectById = createAsyncThunk(
  'projects/fetchProjectById',
  async (projectId: number, { rejectWithValue }) => {
    try {
      const response = await projectApi.getProjectById(projectId)
      return response
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch project details')
    }
  }
)

export const createProject = createAsyncThunk(
  'projects/createProject',
  async (payload: { name: string; description: string; status?: string }, { rejectWithValue, dispatch }) => {
    try {
      const response = await projectApi.createProject(payload)
      dispatch(fetchProjects({ page: 0 }))
      return response
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to create project')
    }
  }
)

export const updateProject = createAsyncThunk(
  'projects/updateProject',
  async ({ id, payload }: { id: number; payload: { name: string; description: string; status?: string } }, { rejectWithValue }) => {
    try {
      const response = await projectApi.updateProject(id, payload)
      return response
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to update project')
    }
  }
)

export const deleteProject = createAsyncThunk(
  'projects/deleteProject',
  async (id: number, { rejectWithValue, dispatch }) => {
    try {
      await projectApi.deleteProject(id)
      dispatch(fetchProjects({ page: 0 }))
      return id
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to delete project')
    }
  }
)

// Member thunks
export const fetchProjectMembers = createAsyncThunk(
  'projects/fetchProjectMembers',
  async (projectId: number, { rejectWithValue }) => {
    try {
      const response = await projectApi.getMembers(projectId)
      return response
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch project members')
    }
  }
)

export const addProjectMember = createAsyncThunk(
  'projects/addProjectMember',
  async ({ projectId, userId, role }: { projectId: number; userId: string; role: string }, { rejectWithValue, dispatch }) => {
    try {
      await projectApi.addMember(projectId, { userId, role })
      dispatch(fetchProjectMembers(projectId))
      return { userId, role }
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to add project member')
    }
  }
)

export const removeProjectMember = createAsyncThunk(
  'projects/removeProjectMember',
  async ({ projectId, userId }: { projectId: number; userId: string }, { rejectWithValue, dispatch }) => {
    try {
      await projectApi.removeMember(projectId, userId)
      dispatch(fetchProjectMembers(projectId))
      return userId
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to remove project member')
    }
  }
)

// ---------------------------------------------------------------------------
// SLICE
// ---------------------------------------------------------------------------

const projectSlice = createSlice({
  name: 'projects',
  initialState,
  reducers: {
    setProjectFilters: (state, action: PayloadAction<{ status?: string; search?: string }>) => {
      state.filters = { ...state.filters, ...action.payload }
      state.pagination.page = 0 // Reset page on filter change
    },
    clearActiveProject: (state) => {
      state.activeProject = null
      state.members = []
    },
    clearProjectsState: () => initialState,
  },
  extraReducers: (builder) => {
    builder
      // Fetch Projects
      .addCase(fetchProjects.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchProjects.fulfilled, (state, action) => {
        state.loading = false
        state.items = action.payload.content
        state.pagination.totalPages = action.payload.page.totalPages
        state.pagination.totalElements = action.payload.page.totalElements
        state.pagination.page = action.payload.page.number
        state.pagination.size = action.payload.page.size
        state.initialized = true
      })
      .addCase(fetchProjects.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })

      // Fetch Project By Id
      .addCase(fetchProjectById.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchProjectById.fulfilled, (state, action) => {
        state.loading = false
        state.activeProject = action.payload
      })
      .addCase(fetchProjectById.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })

      // Update Project
      .addCase(updateProject.fulfilled, (state, action) => {
        state.activeProject = action.payload
        const index = state.items.findIndex(p => p.id === action.payload.id)
        if (index !== -1) {
          state.items[index] = action.payload
        }
      })

      // Fetch Members
      .addCase(fetchProjectMembers.pending, (state) => {
        state.error = null
      })
      .addCase(fetchProjectMembers.fulfilled, (state, action) => {
        state.members = action.payload
      })
      .addCase(fetchProjectMembers.rejected, (state, action) => {
        state.error = action.payload as string
      })
  },
})

export const { setProjectFilters, clearActiveProject, clearProjectsState } = projectSlice.actions
export default projectSlice.reducer
export const selectProjects = (state: { projects: ProjectState }) => state.projects
export const selectActiveProject = (state: { projects: ProjectState }) => state.projects.activeProject
export const selectProjectMembers = (state: { projects: ProjectState }) => state.projects.members
export const selectProjectLoading = (state: { projects: ProjectState }) => state.projects.loading
export const selectProjectError = (state: { projects: ProjectState }) => state.projects.error
export const selectProjectFilters = (state: { projects: ProjectState }) => state.projects.filters
export const selectProjectPagination = (state: { projects: ProjectState }) => state.projects.pagination
export const selectProjectInitialized = (state: { projects: ProjectState }) => state.projects.initialized
