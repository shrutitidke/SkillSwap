import { Navigate, Outlet } from 'react-router-dom'
import useAuth from './useAuth'

function RequireUser() {
  const { user } = useAuth()

  if (user?.role === 'ADMIN') {
    return <Navigate to="/" replace />
  }

  if (user?.role !== 'USER') {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}

export default RequireUser
