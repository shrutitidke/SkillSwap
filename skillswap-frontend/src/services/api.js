
const API_BASE_URL = 'http://localhost:8080/api'

export async function registerUser(userData) {
  const response = await fetch(`${API_BASE_URL}/users/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(userData),
  })

  const data = await response.text()

  if (!response.ok) {
    let message = data || `Registration failed (${response.status})`

    try {
      message = JSON.parse(data).message || message
    } catch {
      // Keep the original response if it isn't JSON.
    }

    throw new Error(message)
  }

  return data
}

export async function loginUser(credentials) {
  const response = await fetch(`${API_BASE_URL}/users/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(credentials),
  })

  const text = await response.text()

  let data

  try {
    data = text ? JSON.parse(text) : {}
  } catch {
    data = { message: text }
  }

  if (!response.ok) {
    throw new Error(
      data.message ||
      data.error ||
      `Login failed (${response.status})`
    )
  }

  return data
}


export async function discoverSkills() {
  const token = localStorage.getItem('skillswap_token')

  if (!token) {
    throw new Error('Your login session was not found. Please log in again.')
  }

  const response = await fetch(`${API_BASE_URL}/skills/discover`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json',
    },
  })

  const text = await response.text()

  let data

  try {
    data = text ? JSON.parse(text) : []
  } catch {
    data = null
  }

  if (!response.ok) {
    throw new Error(
      data?.message ||
      data?.error ||
      `Unable to fetch skills. HTTP ${response.status}`
    )
  }

  if (!Array.isArray(data)) {
    throw new Error('The skills API did not return a list of skills.')
  }

  return data
}


