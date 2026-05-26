import { useState, useCallback } from 'react'
import { collaborationApi } from '../api/collaborationApi'
import type { ProjectMemberDto, ProjectRole } from '../types/collaborationTypes'
import { toast } from 'sonner'

export const useCollaboration = () => {
  const [members, setMembers] = useState<ProjectMemberDto[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchMembers = useCallback(async (projectId: number) => {
    setIsLoading(true)
    setError(null)
    try {
      const result = await collaborationApi.getMembers(projectId)
      setMembers(result)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to fetch project members.'
      setError(msg)
      toast.error(msg)
    } finally {
      setIsLoading(false)
    }
  }, [])

  const addMember = async (projectId: number, userId: string, role: ProjectRole) => {
    setIsLoading(true)
    setError(null)
    try {
      await collaborationApi.addMember(projectId, { userId, role })
      toast.success(`User "${userId}" added as project ${role.toLowerCase()}.`)
      await fetchMembers(projectId)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to add member.'
      setError(msg)
      toast.error(msg)
      throw e
    } finally {
      setIsLoading(false)
    }
  }

  const removeMember = async (projectId: number, userId: string) => {
    setIsLoading(true)
    setError(null)
    try {
      await collaborationApi.removeMember(projectId, userId)
      toast.success(`Member "${userId}" removed successfully.`)
      await fetchMembers(projectId)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to remove member.'
      setError(msg)
      toast.error(msg)
      throw e
    } finally {
      setIsLoading(false)
    }
  }

  const assignTask = async (taskId: number, assigneeEmail: string) => {
    setIsLoading(true)
    setError(null)
    try {
      const updatedTask = await collaborationApi.assignTask(taskId, assigneeEmail)
      toast.success(`Task successfully delegated to "${assigneeEmail}".`)
      return updatedTask
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to assign task.'
      setError(msg)
      toast.error(msg)
      throw e
    } finally {
      setIsLoading(false)
    }
  }

  const unassignTask = async (taskId: number) => {
    setIsLoading(true)
    setError(null)
    try {
      const updatedTask = await collaborationApi.unassignTask(taskId)
      toast.success('Task unassigned successfully.')
      return updatedTask
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to remove task assignee.'
      setError(msg)
      toast.error(msg)
      throw e
    } finally {
      setIsLoading(false)
    }
  }

  return {
    members,
    isLoading,
    error,
    fetchMembers,
    addMember,
    removeMember,
    assignTask,
    unassignTask,
  }
}

export default useCollaboration
