import { useState, useCallback } from 'react'
import { projectApi } from '../api/projectApi'
import type { ProjectDto, CreateProjectRequest, UpdateProjectRequest } from '../types/projectTypes'

export const useProjects = () => {
  const [projects, setProjects] = useState<ProjectDto[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  
  // Pagination & Filters State
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)
  const [pageSize] = useState(6) // 6 cards per page looks great in a 3-column grid
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [searchQuery, setSearchQuery] = useState<string>('')

  const fetchProjects = useCallback(async (page = 0, query = '', status = '') => {
    setIsLoading(true)
    setError(null)
    try {
      const response = await projectApi.getAll({
        page,
        size: pageSize,
        search: query || undefined,
        status: status || undefined,
      })
      setProjects(response.content)
      setTotalElements(response.totalElements)
      setTotalPages(response.totalPages)
      setCurrentPage(response.number)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to fetch projects.'
      setError(msg)
    } finally {
      setIsLoading(false)
    }
  }, [pageSize])

  const createProject = async (payload: CreateProjectRequest) => {
    setIsLoading(true)
    setError(null)
    try {
      const newProject = await projectApi.create(payload)
      return newProject
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to create project.'
      setError(msg)
      throw e
    } finally {
      setIsLoading(false)
    }
  }

  const updateProject = async (id: number, payload: UpdateProjectRequest) => {
    setIsLoading(true)
    setError(null)
    try {
      const updated = await projectApi.update(id, payload)
      return updated
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to update project.'
      setError(msg)
      throw e
    } finally {
      setIsLoading(false)
    }
  }

  const deleteProject = async (id: number) => {
    setIsLoading(true)
    setError(null)
    try {
      await projectApi.delete(id)
      // If we are on a page where there is only 1 item and it gets deleted, go to previous page
      const nextPage = projects.length === 1 && currentPage > 0 ? currentPage - 1 : currentPage
      await fetchProjects(nextPage, searchQuery, statusFilter)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to delete project.'
      setError(msg)
      throw e
    } finally {
      setIsLoading(false)
    }
  }

  return {
    projects,
    isLoading,
    error,
    totalElements,
    totalPages,
    currentPage,
    setCurrentPage,
    statusFilter,
    setStatusFilter,
    searchQuery,
    setSearchQuery,
    fetchProjects,
    createProject,
    updateProject,
    deleteProject,
  }
}

export default useProjects
