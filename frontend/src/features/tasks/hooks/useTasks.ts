import { useState, useCallback, useMemo } from 'react'
import { taskApi } from '../api/taskApi'
import type { TaskDto, CreateTaskRequest, UpdateTaskRequest } from '../types/taskTypes'

export const useTasks = () => {
  const [tasks, setTasks] = useState<TaskDto[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Frontend Filter & Sort States
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [priorityFilter, setPriorityFilter] = useState<string>('')
  const [searchQuery, setSearchQuery] = useState<string>('')
  const [sortBy, setSortBy] = useState<'dueDate' | 'priority' | 'createdAt'>('createdAt')
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc')

  const fetchTasks = useCallback(async (projectId: number) => {
    setIsLoading(true)
    setError(null)
    try {
      // Load all tasks (max page size 100 for fast local filtering/sorting)
      const response = await taskApi.getAll({
        projectId,
        size: 100,
      })
      setTasks(response.content)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to fetch tasks.'
      setError(msg)
    } finally {
      setIsLoading(false)
    }
  }, [])

  const createTask = async (payload: CreateTaskRequest) => {
    setIsLoading(true)
    setError(null)
    try {
      const newTask = await taskApi.create(payload)
      return newTask
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to create task.'
      setError(msg)
      throw e
    } finally {
      setIsLoading(false)
    }
  }

  const updateTask = async (id: number, payload: UpdateTaskRequest) => {
    setIsLoading(true)
    setError(null)
    try {
      const updated = await taskApi.update(id, payload)
      return updated
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to update task.'
      setError(msg)
      throw e
    } finally {
      setIsLoading(false)
    }
  }

  const deleteTask = async (id: number, projectId: number) => {
    setIsLoading(true)
    setError(null)
    try {
      await taskApi.delete(id)
      await fetchTasks(projectId)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to delete task.'
      setError(msg)
      throw e
    } finally {
      setIsLoading(false)
    }
  }

  // Frontend filter/sort mapping
  const filteredAndSortedTasks = useMemo(() => {
    let result = [...tasks]

    // 1. Filter by search query (title / description)
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase()
      result = result.filter(
        (t) =>
          t.title.toLowerCase().includes(query) ||
          t.description.toLowerCase().includes(query)
      )
    }

    // 2. Filter by status
    if (statusFilter) {
      result = result.filter((t) => t.status === statusFilter)
    }

    // 3. Filter by priority
    if (priorityFilter) {
      result = result.filter((t) => t.priority === priorityFilter)
    }

    // 4. Sort
    result.sort((a, b) => {
      let comparison = 0

      if (sortBy === 'dueDate') {
        const valA = a.dueDate || 0
        const valB = b.dueDate || 0
        comparison = valA - valB
      } else if (sortBy === 'priority') {
        const priorityWeight = { CRITICAL: 4, HIGH: 3, MEDIUM: 2, LOW: 1 }
        comparison = priorityWeight[a.priority] - priorityWeight[b.priority]
      } else {
        // createdAt
        comparison = a.createdAt - b.createdAt
      }

      return sortDirection === 'asc' ? comparison : -comparison
    })

    return result
  }, [tasks, searchQuery, statusFilter, priorityFilter, sortBy, sortDirection])

  return {
    tasks: filteredAndSortedTasks,
    rawTasksCount: tasks.length,
    isLoading,
    error,
    statusFilter,
    setStatusFilter,
    priorityFilter,
    setPriorityFilter,
    searchQuery,
    setSearchQuery,
    sortBy,
    setSortBy,
    sortDirection,
    setSortDirection,
    fetchTasks,
    createTask,
    updateTask,
    deleteTask,
  }
}

export default useTasks
