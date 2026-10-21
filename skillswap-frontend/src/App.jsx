
import { useState } from 'react'
import {
  BrowserRouter,
  Routes,
  Route,
  NavLink,
  Link,
  useNavigate,
} from 'react-router-dom'

import './App.css'
import RequireAuth from './auth/RequireAuth'
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
import Terms from './pages/Terms'

function AppContent() {
  const [menuOpen, setMenuOpen] = useState(false)
  const navigate = useNavigate()
  const { user, token, loading, logout } = useAuth()
  const closeMenu = () => setMenuOpen(false)

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

            {user && (
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
              </>
            )}
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

      <main className="main-content">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route element={<RequireAuth />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/skills" element={<ExploreSkills />} />
            <Route path="/my-skills" element={<MySkills />} />
            <Route path="/my-requests" element={<MyRequests />} />
            <Route
              path="/my-requests/:requestId/chat"
              element={<ExchangeChat />}
            />
          </Route>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/terms" element={<Terms />} />
        </Routes>
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
