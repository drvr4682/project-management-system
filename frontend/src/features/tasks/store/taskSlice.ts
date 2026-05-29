import { createSlice, createAsyncThunk, createSelector, type PayloadAction } from '@reduxjs/toolkit'
import taskApi, { type TaskResponseDTO, type TaskRequestDTO } from '../api/taskApi'

export interface TaskState {
  items: TaskResponseDTO[]
  filters: {
    status?: string
    priority?: string
    assignedTo?: string
    search?: string
  }
  initialized: boolean
  loading: boolean
  error: string | null
}

const initialState: TaskState = {
  items: [],
  filters: {
    status: undefined,
    priority: undefined,
    assignedTo: undefined,
    search: undefined,
  },
  initialized: false,
  loading: false,
  error: null,
}

// ---------------------------------------------------------------------------
// ASYNC THUNKS
// ---------------------------------------------------------------------------

export const fetchTasks = createAsyncThunk(
  'tasks/fetchTasks',
  async (projectId: number, { getState, rejectWithValue }) => {
    try {
      const state = getState() as { tasks: TaskState }
      const filters = state.tasks.filters

      const params = {
        projectId,
        status: filters.status,
        priority: filters.priority,
        assignedTo: filters.assignedTo,
        search: filters.search,
        page: 0,
        size: 100, // Load all tasks in the project context for the Kanban board
      }

      if (!params.status) delete params.status
      if (!params.priority) delete params.priority
      if (!params.assignedTo) delete params.assignedTo
      if (!params.search) delete params.search

      const response = await taskApi.getTasks(params)
      return response.content
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch tasks')
    }
  }
)

export const createTask = createAsyncThunk(
  'tasks/createTask',
  async (payload: TaskRequestDTO, { rejectWithValue, dispatch }) => {
    try {
      const response = await taskApi.createTask(payload)
      dispatch(fetchTasks(payload.projectId))
      return response
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to create task')
    }
  }
)

export const updateTask = createAsyncThunk(
  'tasks/updateTask',
  async ({ taskId, payload }: { taskId: number; payload: TaskRequestDTO }, { rejectWithValue, dispatch }) => {
    try {
      const response = await taskApi.updateTask(taskId, payload)
      dispatch(fetchTasks(payload.projectId))
      return response
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to update task')
    }
  }
)

// Status update with immediate optimistic feedback
export const updateTaskStatus = createAsyncThunk(
  'tasks/updateTaskStatus',
  async (
    {
      taskId,
      status,
      previousStatus,
      projectId,
      taskData,
    }: {
      taskId: number
      status: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED'
      previousStatus: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED'
      projectId: number
      taskData: TaskResponseDTO
    },
    { dispatch, rejectWithValue }
  ) => {
    try {
      // Optimistically transition status in store
      dispatch(setTaskStatusOptimistic({ taskId, status }))

      const payload: TaskRequestDTO = {
        title: taskData.title,
        description: taskData.description,
        status: status,
        priority: taskData.priority,
        dueDate: taskData.dueDate ? new Date(taskData.dueDate).toISOString() : null,
        projectId: projectId,
      }
      
      const response = await taskApi.updateTask(taskId, payload)
      return response
    } catch (error: any) {
      // Rollback on failure
      dispatch(setTaskStatusOptimistic({ taskId, status: previousStatus }))
      return rejectWithValue(error.response?.data?.message || 'Failed to update task status')
    }
  }
)

export const deleteTask = createAsyncThunk(
  'tasks/deleteTask',
  async ({ taskId, projectId }: { taskId: number; projectId: number }, { rejectWithValue, dispatch }) => {
    try {
      await taskApi.deleteTask(taskId)
      dispatch(fetchTasks(projectId))
      return taskId
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to delete task')
    }
  }
)

// Task Assignment Thunks
export const assignTask = createAsyncThunk(
  'tasks/assignTask',
  async ({ taskId, assigneeId, projectId }: { taskId: number; assigneeId: string; projectId: number }, { rejectWithValue, dispatch }) => {
    try {
      const response = await taskApi.assignTask(taskId, assigneeId)
      dispatch(fetchTasks(projectId))
      return response
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to assign task')
    }
  }
)

export const unassignTask = createAsyncThunk(
  'tasks/unassignTask',
  async ({ taskId, projectId }: { taskId: number; projectId: number }, { rejectWithValue, dispatch }) => {
    try {
      const response = await taskApi.removeAssignee(taskId)
      dispatch(fetchTasks(projectId))
      return response
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to unassign task')
    }
  }
)

// ---------------------------------------------------------------------------
// SLICE
// ---------------------------------------------------------------------------

const taskSlice = createSlice({
  name: 'tasks',
  initialState,
  reducers: {
    setTaskFilters: (state, action: PayloadAction<{ status?: string; priority?: string; assignedTo?: string; search?: string }>) => {
      state.filters = { ...state.filters, ...action.payload }
    },
    clearTaskFilters: (state) => {
      state.filters = initialState.filters
    },
    setTaskStatusOptimistic: (state, action: PayloadAction<{ taskId: number; status: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED' }>) => {
      const task = state.items.find(t => t.id === action.payload.taskId)
      if (task) {
        task.status = action.payload.status
      }
    },
    resetTaskState: (state) => {
      state.items = []
      state.initialized = false
      state.loading = false
      state.error = null
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchTasks.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchTasks.fulfilled, (state, action) => {
        state.loading = false
        state.items = action.payload
        state.initialized = true
      })
      .addCase(fetchTasks.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
  },
})

export const { setTaskFilters, clearTaskFilters, setTaskStatusOptimistic, resetTaskState } = taskSlice.actions
export default taskSlice.reducer

// ---------------------------------------------------------------------------
// MEMOIZED DERIVED SELECTORS
// ---------------------------------------------------------------------------

export const selectTasksState = (state: { tasks: TaskState }) => state.tasks
export const selectTasksItems = (state: { tasks: TaskState }) => state.tasks.items
export const selectTasksFilters = (state: { tasks: TaskState }) => state.tasks.filters
export const selectTasksLoading = (state: { tasks: TaskState }) => state.tasks.loading
export const selectTasksError = (state: { tasks: TaskState }) => state.tasks.error

export const selectGroupedTasks = createSelector(
  [selectTasksItems],
  (items) => {
    const groups = {
      TODO: [] as TaskResponseDTO[],
      IN_PROGRESS: [] as TaskResponseDTO[],
      DONE: [] as TaskResponseDTO[],
      BLOCKED: [] as TaskResponseDTO[],
    }
    
    items.forEach((item) => {
      if (groups[item.status]) {
        groups[item.status].push(item)
      }
    })
    
    return groups
  }
)
