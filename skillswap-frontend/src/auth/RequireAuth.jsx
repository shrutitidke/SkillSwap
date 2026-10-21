import { Navigate, Outlet, useLocation } from 'react-router-dom'
import useAuth from './useAuth'

function RequireAuth() {
  const { token, user, loading, profileError, refreshProfile } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <p className="auth-route-status" role="status">
        Loading your account...
      </p>
    )
  }

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (profileError) {
    return (
      <div className="auth-route-status" role="alert">
        <p>{profileError}</p>
        <button
          type="button"
          className="secondary-btn"
          onClick={refreshProfile}
        >
          Try again
        </button>
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}

export default RequireAuth
