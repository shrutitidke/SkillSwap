
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { loginUser, setAuthToken } from '../services/api'

function Login() {
  const navigate = useNavigate()
  const location = useLocation()

  const [showPassword, setShowPassword] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)

  const [popup, setPopup] = useState({
    show: false,
    message: '',
    type: 'success',
  })

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (loading) return

    try {
      setLoading(true)

      const response = await loginUser({
        email: email.trim(),
        password,
      })

      if (!response?.token) {
        throw new Error('Login response did not contain an authentication token.')
      }

      setAuthToken(response.token)

      setPopup({
        show: true,
        message: 'Login successful! Welcome to SkillSwap.',
        type: 'success',
      })
    } catch (error) {
      setPopup({
        show: true,
        message: error.message || 'Login failed. Please try again.',
        type: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const closePopup = () => {
    const loginSuccessful = popup.type === 'success'

    setPopup({
      show: false,
      message: '',
      type: 'success',
    })

    if (loginSuccessful) {
      navigate(location.state?.from?.pathname || '/skills', {
        replace: true,
      })
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-card login-card">
        <Link to="/" className="logo">
          Skill<span>Swap</span>
        </Link>

        <h1>Welcome back!</h1>

        <p className="auth-description">
          Log in to continue your SkillSwap journey.
        </p>

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="login-email">Email Address</label>

            <input
              id="login-email"
              name="email"
              type="email"
              placeholder="Enter your email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="login-password">Password</label>

            <div className="password-input-wrap">
              <input
                id="login-password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                placeholder="Enter your password"
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />

              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
          </div>

          <div className="auth-options">
            <Link
              to="/forgot-password"
              className="forgot-password-link"
            >
              Forgot password?
            </Link>
          </div>

          <button
            type="submit"
            className="primary-btn auth-submit"
            disabled={loading}
          >
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>

        <p className="auth-switch">
          Don't have an account? <Link to="/register">Sign up</Link>
        </p>
      </div>

      {popup.show && (
        <div
          className="popup-overlay"
          role="presentation"
        >
          <div
            className="popup-box"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="login-popup-message"
          >
            <div className={`popup-icon ${popup.type}`}>
              {popup.type === 'success' ? '✓' : '!'}
            </div>

            <p id="login-popup-message">{popup.message}</p>

            <button
              type="button"
              className="popup-ok-btn"
              onClick={closePopup}
            >
              OK
            </button>
          </div>
        </div>
      )}
    </section>
  )
}

export default Login
