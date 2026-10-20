
import { useEffect, useState } from 'react'
import {
  BrowserRouter,
  Routes,
  Route,
  NavLink,
  Link,
  useNavigate,
} from 'react-router-dom'

import './App.css'
import Home from './pages/Home'
import ExploreSkills from './pages/ExploreSkills'
import Login from './pages/Login'
import Register from './pages/Register'
import ForgotPassword from './pages/ForgotPassword'
import Terms from './pages/Terms'

function AppContent() {
  const [menuOpen, setMenuOpen] = useState(false)
  const [isLoggedIn, setIsLoggedIn] = useState(
    () => Boolean(localStorage.getItem('skillswap_token'))
  )

  const navigate = useNavigate()
  const closeMenu = () => setMenuOpen(false)

  useEffect(() => {
    const syncAuth = () => {
      setIsLoggedIn(Boolean(localStorage.getItem('skillswap_token')))
    }

    window.addEventListener('skillswap-auth-change', syncAuth)
    window.addEventListener('storage', syncAuth)

    return () => {
      window.removeEventListener('skillswap-auth-change', syncAuth)
      window.removeEventListener('storage', syncAuth)
    }
  }, [])

  const handleLogout = () => {
    localStorage.removeItem('skillswap_token')
    setIsLoggedIn(false)
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

            <NavLink to="/skills" onClick={closeMenu}>
              Explore Skills
            </NavLink>
          </nav>

          <div className="nav-buttons">
            {isLoggedIn ? (
              <button
                type="button"
                className="primary-btn nav-signup"
                onClick={handleLogout}
              >
                Logout
              </button>
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
                  Sign Up
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="main-content">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/skills" element={<ExploreSkills />} />
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
      <AppContent />
    </BrowserRouter>
  )
}

export default App
