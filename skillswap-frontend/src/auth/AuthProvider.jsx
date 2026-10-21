import { useCallback, useEffect, useState } from 'react'
import AuthContext from './AuthContext'
import {
  AUTH_CHANGE_EVENT,
  clearAuthToken,
  getCurrentUser,
} from '../services/api'

const TOKEN_KEY = 'skillswap_token'

function AuthProvider({ children }) {
  const [token, setToken] = useState(
    () => localStorage.getItem(TOKEN_KEY)
  )
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(Boolean(token))
  const [profileError, setProfileError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    const syncToken = () => {
      const currentToken = localStorage.getItem(TOKEN_KEY)
      setToken(currentToken)

      if (currentToken) {
        setLoading(true)
        setProfileError('')
      } else {
        setUser(null)
        setProfileError('')
        setLoading(false)
      }
    }

    window.addEventListener(AUTH_CHANGE_EVENT, syncToken)
    window.addEventListener('storage', syncToken)

    return () => {
      window.removeEventListener(AUTH_CHANGE_EVENT, syncToken)
      window.removeEventListener('storage', syncToken)
    }
  }, [])

  useEffect(() => {
    let cancelled = false

    if (!token) {
      return undefined
    }

    getCurrentUser()
      .then((profile) => {
        if (!cancelled && localStorage.getItem(TOKEN_KEY) === token) {
          setUser(profile)
          setProfileError('')
        }
      })
      .catch((error) => {
        if (!cancelled && localStorage.getItem(TOKEN_KEY) === token) {
          setUser(null)
          setProfileError(
            error.message || 'Unable to load your account profile.'
          )
        }
      })
      .finally(() => {
        if (!cancelled && localStorage.getItem(TOKEN_KEY) === token) {
          setLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [token, refreshKey])

  const logout = useCallback(() => {
    clearAuthToken()
  }, [])

  const refreshProfile = useCallback(() => {
    setLoading(true)
    setProfileError('')
    setRefreshKey((value) => value + 1)
  }, [])

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        profileError,
        isAuthenticated: Boolean(token && user),
        logout,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export default AuthProvider
