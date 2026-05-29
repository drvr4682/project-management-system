export function isTokenExpired(token: string | null): boolean {
  if (!token) return true
  try {
    const parts = token.split('.')
    if (parts.length < 2) return true

    // Decode base64 URL payload safely in browsers using window.atob
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      window
        .atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )

    const payload = JSON.parse(jsonPayload)
    if (!payload.exp) return true

    // Return true if within 10 seconds of expiration (safety buffer to avoid network race conditions)
    const currentTime = Math.floor(Date.now() / 1000)
    return payload.exp - currentTime < 10
  } catch {
    return true
  }
}
