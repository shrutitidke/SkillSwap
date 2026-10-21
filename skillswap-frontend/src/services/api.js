const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const TOKEN_KEY = 'skillswap_token'
export const AUTH_CHANGE_EVENT = 'skillswap-auth-change'

export function setAuthToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT))
}

export function clearAuthToken() {
  localStorage.removeItem(TOKEN_KEY)
  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT))
}

export async function apiRequest(path, options = {}) {
  const { authenticated = true, ...fetchOptions } = options
  const token = authenticated ? localStorage.getItem(TOKEN_KEY) : null
  const headers = new Headers(fetchOptions.headers || {})

  headers.set('Accept', 'application/json')

  if (fetchOptions.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...fetchOptions,
    headers,
  })
  const text = await response.text()
  let data = null

  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = { message: text }
    }
  }

  if (!response.ok) {
    if (authenticated && token && response.status === 401) {
      clearAuthToken()
    }

    throw new Error(
      data?.message ||
        data?.error ||
        `Request failed (${response.status})`
    )
  }

  return data
}

export function registerUser(userData) {
  return apiRequest('/users/register', {
    method: 'POST',
    authenticated: false,
    body: JSON.stringify(userData),
  })
}

export function loginUser(credentials) {
  return apiRequest('/users/login', {
    method: 'POST',
    authenticated: false,
    body: JSON.stringify(credentials),
  })
}

export function getCurrentUser() {
  return apiRequest('/users/me')
}

export function requestPasswordReset(email) {
  return apiRequest('/users/forgot-password', {
    method: 'POST',
    authenticated: false,
    body: JSON.stringify({ email }),
  })
}

export function verifyPasswordResetOtp(email, otp) {
  return apiRequest('/users/verify-reset-otp', {
    method: 'POST',
    authenticated: false,
    body: JSON.stringify({ email, otp }),
  })
}

export function resetPassword(resetToken, newPassword) {
  return apiRequest('/users/reset-password', {
    method: 'POST',
    authenticated: false,
    body: JSON.stringify({ resetToken, newPassword }),
  })
}

export async function getMySkills() {
  const skills = await apiRequest('/skills/my-skills')

  if (!Array.isArray(skills)) {
    throw new Error('The skills API did not return a list of your skills.')
  }

  return skills
}

export function createSkill(skill) {
  return apiRequest('/skills', {
    method: 'POST',
    body: JSON.stringify(skill),
  })
}

export function updateSkill(skillId, skill) {
  return apiRequest(`/skills/${skillId}`, {
    method: 'PUT',
    body: JSON.stringify(skill),
  })
}

export function deleteSkill(skillId) {
  return apiRequest(`/skills/${skillId}`, {
    method: 'DELETE',
  })
}

export async function discoverSkills() {
  const data = await apiRequest('/skills/discover')

  if (!Array.isArray(data)) {
    throw new Error('The skills API did not return a list of skills.')
  }

  return data
}

export async function getReciprocalMatches() {
  const data = await apiRequest('/skills/matches')

  if (!Array.isArray(data)) {
    throw new Error('The matches API did not return a list of matches.')
  }

  return data
}

export async function sendExchangeRequest({
  skillId,
  learnerSkillId,
  message = '',
}) {
  return apiRequest('/skill-requests', {
    method: 'POST',
    body: JSON.stringify({ skillId, learnerSkillId, message }),
  })
}

export async function getReceivedExchangeRequests() {
  const data = await apiRequest('/skill-requests/received')
  if (!Array.isArray(data)) {
    throw new Error('The received requests API did not return a list.')
  }
  return data
}

export async function getSentExchangeRequests() {
  const data = await apiRequest('/skill-requests/sent')
  if (!Array.isArray(data)) {
    throw new Error('The sent requests API did not return a list.')
  }
  return data
}

export function respondToExchangeRequest(requestId, status) {
  return apiRequest(`/skill-requests/${requestId}/respond`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  })
}

export async function getExchangeChatMessages(requestId, afterId) {
  const query = afterId == null ? '' : `?afterId=${encodeURIComponent(afterId)}`
  const data = await apiRequest(
    `/skill-requests/${requestId}/messages${query}`
  )

  if (!Array.isArray(data)) {
    throw new Error('The exchange chat API did not return a message list.')
  }

  return data
}

export function sendExchangeChatMessage(requestId, content) {
  return apiRequest(`/skill-requests/${requestId}/messages`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  })
}

export async function getMyClassSessions() {
  const data = await apiRequest('/class-sessions/my-sessions')
  if (!Array.isArray(data)) {
    throw new Error('The class sessions API did not return a list.')
  }
  return data
}

export function proposeClassSession(requestId, session) {
  return apiRequest(`/class-sessions/requests/${requestId}`, {
    method: 'POST',
    body: JSON.stringify(session),
  })
}

export function respondToClassSession(sessionId, status) {
  return apiRequest(`/class-sessions/${sessionId}/respond`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  })
}

export function cancelClassSession(sessionId) {
  return apiRequest(`/class-sessions/${sessionId}/cancel`, {
    method: 'PUT',
  })
}

export async function getSessionReviews(sessionId) {
  const data = await apiRequest(`/class-sessions/${sessionId}/reviews`)
  if (!Array.isArray(data)) {
    throw new Error('The session reviews API did not return a list.')
  }
  return data
}

export function createSessionReview(sessionId, review) {
  return apiRequest(`/class-sessions/${sessionId}/reviews`, {
    method: 'POST',
    body: JSON.stringify(review),
  })
}

export function getPublicUserProfile(userId) {
  return apiRequest(`/public/users/${userId}/profile`, {
    authenticated: false,
  })
}
