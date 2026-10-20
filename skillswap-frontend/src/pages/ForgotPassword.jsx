
import { useState } from 'react'
import { Link } from 'react-router-dom'

function ForgotPassword() {
  const [step, setStep] = useState(1)
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')

  const handleSendOtp = (e) => {
    e.preventDefault()
    setError('')
    setStep(2)
  }

  const handleVerifyOtp = (e) => {
    e.preventDefault()
    setError('')

    if (otp.length !== 6) {
      setError('Please enter a 6-digit OTP.')
      return
    }

    setStep(3)
  }

  const handleSavePassword = (e) => {
    e.preventDefault()
    setError('')

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    setError('Password reset requires backend integration.')
  }

  return (
    <section className="auth-page">
      <div className="auth-card forgot-password-card">
        <Link to="/" className="forgot-logo">
          Skill<span>Swap</span>
        </Link>

        <div className="forgot-icon">🔐</div>

        <h1>
          {step === 1 && 'Forgot Password?'}
          {step === 2 && 'Verify Your Email'}
          {step === 3 && 'Create New Password'}
        </h1>

        <p className="auth-description">
          {step === 1 &&
            'No worries! Enter your registered email and we’ll help you reset your password.'}
          {step === 2 &&
            `Enter the 6-digit OTP sent to ${email}.`}
          {step === 3 &&
            'Choose a strong new password for your SkillSwap account.'}
        </p>

        <div className="forgot-steps">
          {[1, 2, 3].map((number) => (
            <div
              key={number}
              className={`forgot-step ${step >= number ? 'active' : ''}`}
            >
              <span>{number}</span>
              <small>
                {number === 1 && 'Email'}
                {number === 2 && 'Verify'}
                {number === 3 && 'Reset'}
              </small>
            </div>
          ))}
        </div>

        {error && <p className="forgot-error">{error}</p>}

        {step === 1 && (
          <form onSubmit={handleSendOtp}>
            <label htmlFor="email">Email Address</label>
            <input
              id="email"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />

            <button type="submit" className="primary-btn auth-submit">
              Send OTP →
            </button>
          </form>
        )}

        {step === 2 && (
          <form onSubmit={handleVerifyOtp}>
            <label htmlFor="otp">6-Digit OTP</label>
            <input
              id="otp"
              type="text"
              inputMode="numeric"
              pattern="[0-9]{6}"
              placeholder="Enter OTP"
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
              required
            />

            <button type="submit" className="primary-btn auth-submit">
              Verify OTP →
            </button>

            <button
              type="button"
              className="forgot-text-button"
              onClick={() => {
                setStep(1)
                setOtp('')
                setError('')
              }}
            >
              Change email address
            </button>
          </form>
        )}

        {step === 3 && (
          <form onSubmit={handleSavePassword}>
            <label htmlFor="newPassword">New Password</label>
            <input
              id="newPassword"
              type="password"
              placeholder="Enter new password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              minLength={8}
              required
            />

            <label htmlFor="confirmPassword">Confirm New Password</label>
            <input
              id="confirmPassword"
              type="password"
              placeholder="Re-enter new password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              minLength={8}
              required
            />

            <button type="submit" className="primary-btn auth-submit">
              Save Password
            </button>
          </form>
        )}

        <Link to="/login" className="forgot-back-link">
          ← Back to Login
        </Link>
      </div>
    </section>
  )
}

export default ForgotPassword
