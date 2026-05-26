import React from 'react'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { AlertTriangle } from 'lucide-react'

interface DeleteProjectDialogProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: () => Promise<void>
  projectName: string
  isLoading: boolean
}

export const DeleteProjectDialog: React.FC<DeleteProjectDialogProps> = ({
  isOpen,
  onClose,
  onConfirm,
  projectName,
  isLoading,
}) => {
  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-md relative z-50 animate-in zoom-in-95 duration-200">
        <Card className="border border-border/80 bg-card shadow-2xl">
          <CardHeader className="space-y-2 flex flex-col items-center text-center">
            <div className="w-12 h-12 rounded-full bg-destructive/10 flex items-center justify-center text-destructive mb-1">
              <AlertTriangle size={24} />
            </div>
            <CardTitle className="text-xl font-extrabold tracking-tight">Delete Project</CardTitle>
            <CardDescription className="text-muted-foreground text-sm">
              Are you sure you want to delete <span className="font-bold text-foreground">"{projectName}"</span>?
            </CardDescription>
          </CardHeader>

          <CardContent className="text-center text-sm text-muted-foreground leading-relaxed">
            This action is permanent and cannot be undone. All project information and future tasks associated with this workspace will be deleted forever.
          </CardContent>

          <CardFooter className="flex items-center space-x-3 pt-4">
            <Button
              variant="outline"
              onClick={onClose}
              className="flex-1"
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={onConfirm}
              className="flex-1"
              isLoading={isLoading}
            >
              Delete Project
            </Button>
          </CardFooter>
        </Card>
      </div>
    </div>
  )
}

export default DeleteProjectDialog
