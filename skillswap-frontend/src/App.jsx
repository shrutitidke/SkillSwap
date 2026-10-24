
import { useEffect, useState } from 'react'
import {
  BrowserRouter,
  Routes,
  Route,
  NavLink,
  Link,
  useNavigate,
  useLocation,
} from 'react-router-dom'

import './App.css'
import RequireAuth from './auth/RequireAuth'
import RequireAdmin from './auth/RequireAdmin'
import RequireUser from './auth/RequireUser'
import AuthProvider from './auth/AuthProvider'
import useAuth from './auth/useAuth'
import Home from './pages/Home'
import ExploreSkills from './pages/ExploreSkills'
import Login from './pages/Login'
import Register from './pages/Register'
import ForgotPassword from './pages/ForgotPassword'
import MySkills from './pages/MySkills'
import MyRequests from './pages/MyRequests'
import Dashboard from './pages/Dashboard'
import ExchangeChat from './pages/ExchangeChat'
import PublicProfile from './pages/PublicProfile'
import Terms from './pages/Terms'
import AdminUsers from './pages/AdminUsers'
import Notifications from './pages/Notifications'
import RequiredCourseReview from './pages/RequiredCourseReview'
import {
  getRequiredCourseReviews,
  getMyUnreadNotifications,
  NOTIFICATIONS_CHANGE_EVENT,
} from './services/api'

function AppContent() {
  const [menuOpen, setMenuOpen] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { user, token, loading, logout } = useAuth()
  const [courseReviewStatus, setCourseReviewStatus] = useState({
    userId: null,
    checked: false,
    pending: false,
    error: '',
  })
  const [courseReviewRetry, setCourseReviewRetry] = useState(0)
  const [notificationCount, setNotificationCount] = useState({
    userId: null,
    count: 0,
    error: '',
  })
  const closeMenu = () => setMenuOpen(false)

  useEffect(() => {
    if (!user || user.role !== 'USER' || !token) {
      return undefined
    }

    let active = true
    let inFlight = false

    const refreshCount = async () => {
      if (inFlight) return
      inFlight = true
      try {
        const unreadNotifications = await getMyUnreadNotifications()
        if (active) {
          setNotificationCount({
            userId: user.id,
            count: unreadNotifications.length,
            error: '',
          })
        }
      } catch (error) {
        if (active) {
          setNotificationCount({
            userId: user.id,
            count: 0,
            error: error.message || 'Unable to load notification count.',
          })
        }
      } finally {
        inFlight = false
      }
    }

    const refreshWhenVisible = () => {
      if (document.visibilityState === 'visible') void refreshCount()
    }

    void refreshCount()
    window.addEventListener('focus', refreshWhenVisible)
    window.addEventListener(NOTIFICATIONS_CHANGE_EVENT, refreshCount)
    document.addEventListener('visibilitychange', refreshWhenVisible)
    const refreshInterval = window.setInterval(refreshWhenVisible, 30000)

    return () => {
      active = false
      window.clearInterval(refreshInterval)
      window.removeEventListener('focus', refreshWhenVisible)
      window.removeEventListener(NOTIFICATIONS_CHANGE_EVENT, refreshCount)
      document.removeEventListener('visibilitychange', refreshWhenVisible)
    }
  }, [user, token])

  const unreadNotificationCount =
    notificationCount.userId === user?.id ? notificationCount.count : 0
  const notificationCountError =
    notificationCount.userId === user?.id ? notificationCount.error : ''

  useEffect(() => {
    if (user?.role !== 'USER' || !token) return undefined

    let active = true
    let inFlight = false

    const checkRequiredReviews = async () => {
      if (inFlight) return
      inFlight = true
      try {
        const requiredReviews = await getRequiredCourseReviews()
        if (!active) return
        setCourseReviewStatus({
          userId: user.id,
          checked: true,
          pending: requiredReviews.length > 0,
          error: '',
        })
        if (
          requiredReviews.length > 0 &&
          location.pathname !== '/required-course-review'
        ) {
          navigate('/required-course-review', { replace: true })
        }
      } catch (error) {
        if (active) {
          setCourseReviewStatus({
            userId: user.id,
            checked: true,
            pending: false,
            error: error.message || 'Unable to check required course ratings.',
          })
        }
      } finally {
        inFlight = false
      }
    }

    void checkRequiredReviews()
    const reviewCheckInterval = window.setInterval(
      checkRequiredReviews,
      15000
    )

    return () => {
      active = false
      window.clearInterval(reviewCheckInterval)
    }
  }, [
    user,
    token,
    location.pathname,
    navigate,
    courseReviewRetry,
  ])

  const courseReviewCheckPending =
    user?.role === 'USER'
    && location.pathname !== '/required-course-review'
    && (courseReviewStatus.userId !== user.id || !courseReviewStatus.checked)
  const courseReviewCheckError =
    courseReviewStatus.userId === user?.id
      ? courseReviewStatus.error
      : ''

  const handleLogout = () => {
    logout()
    closeMenu()
    navigate('/login')
  }

  return (
    <div className="app-shell">
      <header className="navbar">
        <Link to="/" className="logo" onClick={closeMenu}>
          Skill<span>Swap</span>
        </Link>

        <button
          className={`menu-toggle ${menuOpen ? 'open' : ''}`}
          type="button"
          aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen(!menuOpen)}
        >
          <span />
          <span />
          <span />
        </button>

        <div className={`nav-content ${menuOpen ? 'menu-open' : ''}`}>
          <nav className="nav-links">
            <NavLink to="/" end onClick={closeMenu}>
              Home
            </NavLink>

            {user?.role === 'ADMIN' ? (
              <NavLink to="/admin" onClick={closeMenu}>
                Admin Center
              </NavLink>
            ) : user ? (
              <>
                <NavLink to="/dashboard" onClick={closeMenu}>
                  Dashboard
                </NavLink>
                <NavLink to="/skills" onClick={closeMenu}>
                  Get What You Want
                </NavLink>
                <NavLink to="/my-skills" onClick={closeMenu}>
                  My Skills
                </NavLink>
                <NavLink to="/my-requests" onClick={closeMenu}>
                  My Requests
                </NavLink>
                <NavLink
                  to="/notifications"
                  onClick={closeMenu}
                  className="notification-nav-link"
                >
                  <span>Notifications</span>
                  {unreadNotificationCount > 0 && (
                    <span className="notification-count-badge">
                      {unreadNotificationCount}
                    </span>
                  )}
                </NavLink>
              </>
            ) : null}
          </nav>

          <div className="nav-buttons">
            {user ? (
              <>
                <span className="nav-user">Hi, {user.name}</span>
                <button
                  type="button"
                  className="primary-btn nav-signup"
                  onClick={handleLogout}
                >
                  Logout
                </button>
              </>
            ) : token && loading ? (
              <span className="nav-user" role="status">
                Loading profile...
              </span>
            ) : (
              <>
                <Link
                  to="/login"
                  className="login-btn"
                  onClick={closeMenu}
                >
                  Login
                </Link>

                <Link
                  to="/register"
                  className="primary-btn nav-signup"
                  onClick={closeMenu}
                >
                  Register
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {notificationCountError && user?.role === 'USER' && (
        <p className="notification-count-error" role="alert">
          {notificationCountError}
        </p>
      )}

      <main className="main-content">
        {courseReviewCheckPending ? (
          <p className="auth-route-status" role="status">
            Checking for course ratings you need to submit...
          </p>
        ) : courseReviewCheckError && location.pathname !== '/required-course-review' ? (
          <div className="auth-route-status" role="alert">
            <p>{courseReviewCheckError}</p>
            <button
              type="button"
              className="secondary-btn"
              onClick={() => setCourseReviewRetry((current) => current + 1)}
            >
              Try again
            </button>
          </div>
        ) : (
          <Routes>
            <Route path="/" element={<Home />} />
            <Route element={<RequireAuth />}>
              <Route element={<RequireUser />}>
                <Route
                  path="/required-course-review"
                  element={<RequiredCourseReview />}
                />
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/skills" element={<ExploreSkills />} />
                <Route path="/my-skills" element={<MySkills />} />
                <Route path="/my-requests" element={<MyRequests />} />
                <Route path="/notifications" element={<Notifications />} />
                <Route
                  path="/my-requests/:requestId/chat"
                  element={<ExchangeChat />}
                />
              </Route>
              <Route element={<RequireAdmin />}>
                <Route path="/admin" element={<AdminUsers />} />
              </Route>
            </Route>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/terms" element={<Terms />} />
            <Route path="/users/:userId/profile" element={<PublicProfile />} />
          </Routes>
        )}
      </main>

      <footer className="footer">
        <p>
          © {new Date().getFullYear()} SkillSwap. Learn together. Grow together.
        </p>
      </footer>
    </div>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
