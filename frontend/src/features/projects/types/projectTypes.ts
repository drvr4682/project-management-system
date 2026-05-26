export type ProjectStatus = 'ACTIVE' | 'COMPLETED' | 'ARCHIVED'

export interface ProjectDto {
  id: number
  name: string
  description: string
  owner: string
  status: ProjectStatus
  createdAt: number // milliseconds epoch
  updatedAt: number // milliseconds epoch
}

export interface CreateProjectRequest {
  name: string
  description?: string
  status?: ProjectStatus
}

export interface UpdateProjectRequest {
  name: string
  description?: string
  status: ProjectStatus
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}
