import React, { useEffect, useState } from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/store'
import { selectProfile, setProfileData } from '@/features/auth/store/profileSlice'
import { selectAuth } from '@/features/auth/store/authSlice'
import profileApi, { type SocialLinkResponse } from '@/features/auth/api/profileApi'
import authApi from '@/features/auth/api/authApi'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Label } from '@/components/ui/Label'
import { Card, CardHeader, CardContent } from '@/components/ui/Card'
import {
  User,
  Lock,
  Save,
  Globe,
  Briefcase,
  Smile,
  Trash2,
  Plus,
  Link2,
  X,
  ShieldAlert,
} from 'lucide-react'
import { toast } from 'sonner'

// Premium custom SVG icons for platforms missing in installed lucide-react version
const GithubIcon: React.FC<React.SVGProps<SVGSVGElement>> = (props) => (
  <svg
    viewBox="0 0 24 24"
    width="24"
    height="24"
    stroke="currentColor"
    strokeWidth="2"
    fill="none"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={props.className}
  >
    <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4" />
    <path d="M9 18c-4.51 2-5-2-7-2" />
  </svg>
)

const LinkedinIcon: React.FC<React.SVGProps<SVGSVGElement>> = (props) => (
  <svg
    viewBox="0 0 24 24"
    width="24"
    height="24"
    stroke="currentColor"
    strokeWidth="2"
    fill="none"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={props.className}
  >
    <path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z" />
    <rect x="2" y="9" width="4" height="12" />
    <circle cx="4" cy="4" r="2" />
  </svg>
)

const TwitterIcon: React.FC<React.SVGProps<SVGSVGElement>> = (props) => (
  <svg
    viewBox="0 0 24 24"
    width="24"
    height="24"
    stroke="currentColor"
    strokeWidth="2"
    fill="none"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={props.className}
  >
    <path d="M22 4s-.7 2.1-2 3.4c1.6 10-9.4 17.3-18 11.6 2.2.1 4.4-.6 6-2C3 15.5.5 9.6 3 5c2.2 2.6 5.6 4.1 9 4-.9-4.2 4-6.6 7-3.8 1.1 0 3-1.2 3-1.2z" />
  </svg>
)

export const ProfilePage: React.FC = () => {
  const dispatch = useAppDispatch()
  
  const { user } = useAppSelector(selectAuth)
  const { profileData } = useAppSelector(selectProfile)

  // Profile fields state
  const [firstName, setFirstName] = useState('')
  const [surname, setSurname] = useState('')
  const [designation, setDesignation] = useState('')
  const [bio, setBio] = useState('')
  const [timezone, setTimezone] = useState('GMT+5:30')
  const [statusMessage, setStatusMessage] = useState('')
  const [updatingProfile, setUpdatingProfile] = useState(false)

  // Social link adding state
  const [newPlatform, setNewPlatform] = useState('GitHub')
  const [newUrl, setNewUrl] = useState('')
  const [addingLink, setAddingLink] = useState(false)

  // Change password modal states
  const [passModalOpen, setPassModalOpen] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [updatingPassword, setUpdatingPassword] = useState(false)

  const reloadProfileData = async () => {
    try {
      const profile = await profileApi.getMyProfile()
      dispatch(setProfileData(profile))
    } catch (err) {
      console.error('Failed to reload profile data', err)
    }
  }

  // Hydrate fields on load
  useEffect(() => {
    if (profileData) {
      setFirstName(profileData.firstName || '')
      setSurname(profileData.surname || '')
      setDesignation(profileData.designation || '')
      setBio(profileData.bio || '')
      setTimezone(profileData.timezone || 'GMT+5:30')
      setStatusMessage(profileData.statusMessage || '')
    } else {
      reloadProfileData()
    }
  }, [profileData])

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!firstName.trim()) {
      toast.error('First Name is required')
      return
    }

    setUpdatingProfile(true)
    try {
      const updated = await profileApi.updateProfile({
        firstName,
        surname,
        designation,
        bio,
        timezone,
        statusMessage,
      })
      dispatch(setProfileData(updated))
      toast.success('Profile details updated successfully')
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to update profile')
    } finally {
      setUpdatingProfile(false)
    }
  }

  const handleAddSocialLink = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newUrl.trim()) {
      toast.error('URL is required')
      return
    }

    setAddingLink(true)
    try {
      await profileApi.addSocialLink({
        platform: newPlatform,
        url: newUrl.trim(),
      })
      toast.success(`${newPlatform} link added successfully`)
      setNewUrl('')
      reloadProfileData()
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to add social link')
    } finally {
      setAddingLink(false)
    }
  }

  const handleDeleteSocialLink = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this social link?')) {
      try {
        await profileApi.deleteSocialLink(id)
        toast.success('Social link deleted')
        reloadProfileData()
      } catch (err: any) {
        toast.error(err.response?.data?.message || 'Failed to delete social link')
      }
    }
  }

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!currentPassword || !newPassword || !confirmPassword) {
      toast.error('Please fill in all password fields')
      return
    }

    if (newPassword !== confirmPassword) {
      toast.error('New passwords do not match')
      return
    }

    // Regex check: min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special character
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/
    if (!passwordRegex.test(newPassword)) {
      toast.error('Password must contain at least 8 characters, an uppercase letter, a lowercase letter, a number, and a special character (@$!%*?&)')
      return
    }

    setUpdatingPassword(true)
    try {
      await authApi.changePassword({
        currentPassword,
        newPassword,
      })
      toast.success('Your credentials have been changed successfully')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setPassModalOpen(false)
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to change password. Make sure current password is correct.')
    } finally {
      setUpdatingPassword(false)
    }
  }

  const getPlatformIcon = (platformName: string) => {
    const name = platformName.toLowerCase()
    if (name.includes('github')) return <GithubIcon className="w-4 h-4" />
    if (name.includes('linkedin')) return <LinkedinIcon className="w-4 h-4" />
    if (name.includes('twitter')) return <TwitterIcon className="w-4 h-4" />
    return <Link2 className="w-4 h-4" />
  }

  const initials = `${firstName?.[0] || ''}${surname?.[0] || ''}`.toUpperCase() || (user?.userName?.[0] || 'U').toUpperCase()
  const currentSocials = profileData?.socialLinks || []

  return (
    <div className="max-w-5xl mx-auto px-4 md:px-8 py-8 space-y-8 animate-in fade-in duration-300">
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight text-foreground font-outfit">
          Account Settings
        </h1>
        <p className="text-muted-foreground text-sm font-semibold mt-1">
          Manage your professional identity, timezone preferences, social connections, and change credentials.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        
        {/* Left Column: Profile Card & Active Social List */}
        <div className="lg:col-span-4 space-y-6">
          <Card className="border border-border bg-card shadow-sm text-center p-6">
            <CardContent className="space-y-4 pt-4">
              <div className="w-20 h-20 rounded-2xl bg-primary/10 border-2 border-primary/20 flex items-center justify-center font-extrabold text-primary text-3xl font-outfit mx-auto shadow-md shadow-primary/5">
                {initials}
              </div>
              <div>
                <h3 className="font-extrabold text-lg text-foreground">
                  {firstName} {surname}
                </h3>
                <p className="text-xs text-muted-foreground font-semibold mt-0.5">
                  @{user?.userName || 'username'}
                </p>
                {designation && (
                  <span className="inline-block mt-2 text-[10px] bg-primary/10 border border-primary/20 px-2.5 py-0.5 rounded-full text-primary font-bold">
                    {designation}
                  </span>
                )}
              </div>
              
              <div className="border-t border-border/60 pt-4 text-left text-xs space-y-3 text-muted-foreground">
                <div>
                  <span className="font-bold text-foreground block mb-0.5">Email Address</span>
                  <span className="truncate block">{user?.email}</span>
                </div>
                <div>
                  <span className="font-bold text-foreground block mb-0.5">User Identity ID</span>
                  <span className="font-mono text-[9px] break-all block">{user?.id}</span>
                </div>
              </div>

              {/* Sidebar Active Social list badges */}
              {currentSocials.length > 0 && (
                <div className="border-t border-border/60 pt-4 text-left space-y-2">
                  <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                    Connected Networks
                  </span>
                  <div className="flex flex-wrap gap-1.5 mt-2">
                    {currentSocials.map((link: SocialLinkResponse) => (
                      <a
                        key={link.id}
                        href={link.url.startsWith('http') ? link.url : `https://${link.url}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center space-x-1 px-2.5 py-1 rounded-lg bg-muted/60 border border-border text-[10px] font-bold text-foreground hover:text-primary transition-colors"
                      >
                        {getPlatformIcon(link.platform)}
                        <span>{link.platform}</span>
                      </a>
                    ))}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Right Column: Update Panels */}
        <div className="lg:col-span-8 space-y-6">
          
          {/* Panel 1: Profile Details Form */}
          <Card className="border border-border bg-card shadow-sm">
            <CardHeader className="border-b border-border/40 pb-4">
              <div className="flex items-center space-x-2.5 text-foreground font-bold">
                <User className="w-5 h-5 text-primary" />
                <span>Professional Profile details</span>
              </div>
            </CardHeader>
            <CardContent className="pt-6">
              <form onSubmit={handleUpdateProfile} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <Label htmlFor="profFirstName">First Name <span className="text-red-500">*</span></Label>
                    <Input
                      id="profFirstName"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      required
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="profSurname">Surname</Label>
                    <Input
                      id="profSurname"
                      value={surname}
                      onChange={(e) => setSurname(e.target.value)}
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="profDesignation">Designation</Label>
                  <div className="relative">
                    <Input
                      id="profDesignation"
                      placeholder="e.g. Lead Software Architect"
                      value={designation}
                      onChange={(e) => setDesignation(e.target.value)}
                      className="pl-10"
                    />
                    <Briefcase className="w-4 h-4 text-muted-foreground absolute left-3 top-3.5" />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="profStatus">Status Message</Label>
                  <div className="relative">
                    <Input
                      id="profStatus"
                      placeholder="e.g. Out of office, coding..."
                      value={statusMessage}
                      onChange={(e) => setStatusMessage(e.target.value)}
                      className="pl-10"
                    />
                    <Smile className="w-4 h-4 text-muted-foreground absolute left-3 top-3.5" />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="profBio">Bio Description</Label>
                  <textarea
                    id="profBio"
                    placeholder="Tell us about yourself..."
                    rows={3}
                    value={bio}
                    onChange={(e) => setBio(e.target.value)}
                    className="w-full rounded-xl border border-border bg-card/50 px-3 py-2 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200 resize-none"
                  />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="profTimezone">Timezone Preference</Label>
                  <div className="relative">
                    <select
                      id="profTimezone"
                      value={timezone}
                      onChange={(e) => setTimezone(e.target.value)}
                      className="w-full h-11 pl-10 pr-4 rounded-xl border border-border bg-card/50 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
                    >
                      <option value="GMT-05:00">GMT-05:00 (EST)</option>
                      <option value="GMT+00:00">GMT+00:00 (UTC)</option>
                      <option value="GMT+01:00">GMT+01:00 (CET)</option>
                      <option value="GMT+05:30">GMT+05:30 (IST)</option>
                      <option value="GMT+08:00">GMT+08:00 (SGT)</option>
                    </select>
                    <Globe className="w-4 h-4 text-muted-foreground absolute left-3.5 top-3.5" />
                  </div>
                </div>

                <div className="flex justify-end pt-4 border-t border-border/40 mt-6">
                  <Button type="submit" disabled={updatingProfile} className="rounded-xl h-10 font-bold space-x-2 px-6">
                    {updatingProfile ? (
                      <span>Saving...</span>
                    ) : (
                      <>
                        <Save className="w-4 h-4" />
                        <span>Save Profile</span>
                      </>
                    )}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          {/* Panel 2: Social Links Management */}
          <Card className="border border-border bg-card shadow-sm">
            <CardHeader className="border-b border-border/40 pb-4">
              <div className="flex items-center space-x-2.5 text-foreground font-bold">
                <Globe className="w-5 h-5 text-primary" />
                <span>Connected Social Networks</span>
              </div>
            </CardHeader>
            <CardContent className="pt-6 space-y-6">
              
              {/* Existing Social Links list list */}
              {currentSocials.length === 0 ? (
                <div className="text-center py-6 border border-dashed border-border/60 rounded-xl text-xs text-muted-foreground">
                  No social connections added. Use the form below to connect your networks.
                </div>
              ) : (
                <div className="space-y-2">
                  {currentSocials.map((link: SocialLinkResponse) => (
                    <div
                      key={link.id}
                      className="flex items-center justify-between p-3.5 rounded-xl border border-border bg-muted/30"
                    >
                      <div className="flex items-center space-x-3 min-w-0">
                        <div className="w-8 h-8 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center font-bold text-primary">
                          {getPlatformIcon(link.platform)}
                        </div>
                        <div className="min-w-0">
                          <span className="text-xs font-bold text-foreground block uppercase tracking-wide">
                            {link.platform}
                          </span>
                          <a
                            href={link.url.startsWith('http') ? link.url : `https://${link.url}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-xs text-primary hover:underline block truncate"
                          >
                            {link.url}
                          </a>
                        </div>
                      </div>
                      <button
                        onClick={() => handleDeleteSocialLink(link.id.toString())}
                        className="p-1.5 rounded-lg text-muted-foreground hover:text-red-500 hover:bg-red-500/10 transition-colors shrink-0"
                        title="Delete Link"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              {/* Add Social Link Form Form */}
              <form onSubmit={handleAddSocialLink} className="pt-4 border-t border-border/60 space-y-4">
                <span className="text-xs text-muted-foreground uppercase font-extrabold tracking-wider block">
                  Add New Connection
                </span>
                
                <div className="grid grid-cols-1 sm:grid-cols-12 gap-4 items-end">
                  <div className="sm:col-span-4 space-y-1.5">
                    <Label htmlFor="platformSelect">Platform</Label>
                    <select
                      id="platformSelect"
                      value={newPlatform}
                      onChange={(e) => setNewPlatform(e.target.value)}
                      className="w-full h-11 px-3 rounded-xl border border-border bg-card text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
                    >
                      <option value="GitHub">GitHub</option>
                      <option value="LinkedIn">LinkedIn</option>
                      <option value="Twitter">Twitter</option>
                      <option value="Website">Website</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>

                  <div className="sm:col-span-6 space-y-1.5">
                    <Label htmlFor="platformUrl">Profile URL</Label>
                    <Input
                      id="platformUrl"
                      placeholder="e.g. github.com/username"
                      value={newUrl}
                      onChange={(e) => setNewUrl(e.target.value)}
                      required
                    />
                  </div>

                  <div className="sm:col-span-2">
                    <Button type="submit" disabled={addingLink} className="w-full h-11 rounded-xl font-bold space-x-1.5">
                      <Plus className="w-4.5 h-4.5" />
                      <span>Add</span>
                    </Button>
                  </div>
                </div>
              </form>

            </CardContent>
          </Card>

          {/* Panel 3: Security Summary with Change Password modal trigger */}
          <Card className="border border-border bg-card shadow-sm overflow-hidden">
            <CardHeader className="border-b border-border/40 pb-4 bg-muted/15">
              <div className="flex items-center space-x-2.5 text-foreground font-bold">
                <Lock className="w-5 h-5 text-primary" />
                <span>Security Settings</span>
              </div>
            </CardHeader>
            <CardContent className="pt-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <div className="space-y-1">
                <span className="text-sm font-bold text-foreground block">Authentication Credentials</span>
                <p className="text-xs text-muted-foreground">
                  Keep your account secure by rotating your password regularly.
                </p>
              </div>

              <Button
                onClick={() => setPassModalOpen(true)}
                className="rounded-xl h-10 font-bold space-x-1.5 shadow-sm shrink-0"
              >
                <Lock className="w-4 h-4" />
                <span>Change Password</span>
              </Button>
            </CardContent>
          </Card>
        </div>

      </div>

      {/* CHANGE PASSWORD FLOATING CARD MODAL */}
      {passModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm animate-in fade-in duration-300">
          <div 
            className="w-full max-w-md bg-card border border-border/85 rounded-2xl shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200"
            role="dialog"
          >
            <div className="flex items-center justify-between px-6 py-4 border-b border-border/60 bg-muted/15">
              <div className="flex items-center space-x-2.5 text-foreground font-bold">
                <Lock className="w-5 h-5 text-primary" />
                <span>Change Password</span>
              </div>
              <button 
                onClick={() => setPassModalOpen(false)}
                className="p-1 rounded-lg text-muted-foreground hover:bg-muted transition-colors focus:outline-none"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleChangePassword} className="p-6 space-y-4">
              <div className="space-y-1.5">
                <Label htmlFor="oldPass">Current Password</Label>
                <Input
                  id="oldPass"
                  type="password"
                  placeholder="Enter current password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  required
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="newPass">New Password</Label>
                <Input
                  id="newPass"
                  type="password"
                  placeholder="Enter new password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="confPass">Confirm New Password</Label>
                <Input
                  id="confPass"
                  type="password"
                  placeholder="Confirm new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
              </div>

              <div className="p-3.5 bg-muted/50 rounded-xl border border-border text-[10px] leading-relaxed text-muted-foreground flex items-start space-x-2.5">
                <ShieldAlert className="w-4 h-4 text-primary shrink-0 mt-0.5" />
                <span>
                  New password must satisfy policies: minimum 8 characters, at least 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special symbol (`@$!%*?&`).
                </span>
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-border/60 mt-6">
                <Button 
                  type="button" 
                  variant="outline" 
                  onClick={() => setPassModalOpen(false)} 
                  className="rounded-xl h-10 font-bold"
                >
                  Cancel
                </Button>
                <Button 
                  type="submit" 
                  disabled={updatingPassword} 
                  className="rounded-xl h-10 font-bold px-6"
                >
                  {updatingPassword ? 'Updating...' : 'Update Password'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  )
}

export default ProfilePage
