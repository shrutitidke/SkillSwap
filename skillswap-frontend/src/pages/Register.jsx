import { useState } from 'react'
import { Link } from 'react-router-dom'
import { registerUser } from '../services/api'

function Register() {
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [agreeToTerms, setAgreeToTerms] = useState(false)
  const [loading, setLoading] = useState(false)

  const [popup, setPopup] = useState({
    show: false,
    message: '',
    type: 'success',
  })

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (password !== confirmPassword) {
      setPopup({
        show: true,
        message: 'Passwords do not match.',
        type: 'error',
      })
      return
    }

    if (!agreeToTerms) {
      setPopup({
        show: true,
        message: 'Please accept the Terms and Conditions.',
        type: 'error',
      })
      return
    }

    setLoading(true)

    try {
      const form = event.currentTarget
      const formData = new FormData(form)

      const userData = {
        name: formData.get('name').trim(),
        email: formData.get('email').trim(),
        password,
      }

      const response = await registerUser(userData)

      const successMessage =
        typeof response === 'string'
          ? (() => {
              try {
                return JSON.parse(response).message || response
              } catch {
                return response
              }
            })()
          : response?.message ||
            response?.data?.message ||
            'User registered successfully'

      setPopup({
        show: true,
        message: successMessage,
        type: 'success',
      })

      setPassword('')
      setConfirmPassword('')
      setAgreeToTerms(false)
      setShowPassword(false)
      setShowConfirmPassword(false)
      form.reset()
    } catch (err) {
      setPopup({
        show: true,
        message:
          err.response?.data?.message ||
          err.message ||
          'Registration failed. Please try again.',
        type: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-card register-card">
        <Link to="/" className="logo">
          Skill<span>Swap</span>
        </Link>

        <h1>Create your account</h1>

        <p className="auth-description">
          Join SkillSwap and start sharing your skills.
        </p>

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Full Name</label>
            <input
              id="name"
              name="name"
              type="text"
              placeholder="Enter your full name"
              autoComplete="name"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email Address</label>
            <input
              id="email"
              name="email"
              type="email"
              placeholder="Enter your email"
              autoComplete="email"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>

            <div className="password-input-wrap">
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                placeholder="Create a password"
                autoComplete="new-password"
                minLength={8}
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

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm Password</label>

            <div className="password-input-wrap">
              <input
                id="confirmPassword"
                name="confirmPassword"
                type={showConfirmPassword ? 'text' : 'password'}
                placeholder="Re-enter your password"
                autoComplete="new-password"
                minLength={8}
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                required
              />

              <button
                type="button"
                className="password-toggle"
                onClick={() =>
                  setShowConfirmPassword(!showConfirmPassword)
                }
                aria-label={
                  showConfirmPassword
                    ? 'Hide confirm password'
                    : 'Show confirm password'
                }
              >
                {showConfirmPassword ? 'Hide' : 'Show'}
              </button>
            </div>
          </div>

          <label className="terms-label">
            <input
              type="checkbox"
              checked={agreeToTerms}
              onChange={(event) => setAgreeToTerms(event.target.checked)}
            />
            <span>
              I agree to the <Link to="/terms">Terms and Conditions</Link>.
            </span>
          </label>

          <button
            type="submit"
            className="primary-btn auth-submit"
            disabled={loading}
          >
            {loading ? 'Creating Account...' : 'Create Account'}
          </button>
        </form>

        <p className="auth-switch">
          Already have an account? <Link to="/login">Login</Link>
        </p>
      </div>

      {popup.show && (
        <div className="popup-overlay">
          <div
            className="popup-box"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="popup-title"
            aria-describedby="popup-message"
          >
            <div
              className={`popup-icon ${popup.type}`}
              aria-hidden="true"
            >
              {popup.type === 'success' ? '✓' : '!'}
            </div>

            <h2 id="popup-title">
              {popup.type === 'success' ? 'Success!' : 'Oops!'}
            </h2>

            <p id="popup-message">{popup.message}</p>

            <button
              type="button"
              className="popup-ok-btn"
              onClick={() =>
                setPopup({ show: false, message: '', type: 'success' })
              }
              autoFocus
            >
              OK
            </button>
          </div>
        </div>
      )}
    </section>
  )
}

export default Register
