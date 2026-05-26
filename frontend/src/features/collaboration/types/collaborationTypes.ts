export type ProjectRole = 'ADMIN' | 'MEMBER' | 'VIEWER'

export interface ProjectMemberDto {
  userId: string // user's email
  role: ProjectRole
}

export interface UserSummaryDto {
  id: number
  name: string
  email: string
}

export interface AddMemberRequest {
  userId: string // target user's email
  role: ProjectRole
}
