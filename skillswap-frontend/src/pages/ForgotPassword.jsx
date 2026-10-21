import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  requestPasswordReset,
  resetPassword,
  verifyPasswordResetOtp,
} from '../services/api'

const PASSWORD_PATTERN = '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$'

function ForgotPassword() {
  const [step, setStep] = useState(1)
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [cooldown, setCooldown] = useState(0)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    if (cooldown === 0) {
      return undefined
    }

    const timer = window.setTimeout(() => {
      setCooldown((remaining) => Math.max(remaining - 1, 0))
    }, 1000)

    return () => window.clearTimeout(timer)
  }, [cooldown])

  const handleSendOtp = async (event, isResend = false) => {
    event.preventDefault()
    if (loading) return

    setError('')
    setNotice('')
    setLoading(true)

    try {
      await requestPasswordReset(email.trim())
      if (!isResend) {
        setStep(2)
      }
      setOtp('')
      setCooldown(60)
      setNotice(
        'If this email is registered, a reset code will be sent. The response is the same for security. Codes expire in five minutes; requests have a 60-second cooldown and a three-per-hour limit.'
      )
    } catch (requestError) {
      setError(requestError.message || 'Unable to request a reset code.')
    } finally {
      setLoading(false)
    }
  }

  const handleVerifyOtp = async (event) => {
    event.preventDefault()
    if (loading) return

    setError('')
    setNotice('')
    setLoading(true)

    try {
      const response = await verifyPasswordResetOtp(email.trim(), otp)
      if (!response?.resetToken) {
        throw new Error('The reset verification response was incomplete.')
      }

      setResetToken(response.resetToken)
      setStep(3)
    } catch (verificationError) {
      setError(
        verificationError.message ||
          'The code is invalid, expired, or has too many attempts.'
      )
    } finally {
      setLoading(false)
    }
  }

  const handleSavePassword = async (event) => {
    event.preventDefault()
    if (loading) return

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    setError('')
    setNotice('')
    setLoading(true)

    try {
      const response = await resetPassword(resetToken, newPassword)
      setNotice(response?.message || 'Password reset successfully.')
      setResetToken('')
      setNewPassword('')
      setConfirmPassword('')
      setStep(4)
    } catch (resetError) {
      setError(resetError.message || 'Unable to reset the password.')
    } finally {
      setLoading(false)
    }
  }

  const handleChangeEmail = () => {
    setStep(1)
    setOtp('')
    setResetToken('')
    setError('')
    setNotice('')
    setCooldown(0)
  }

  return (
    <section className="auth-page">
      <div className="auth-card forgot-password-card">
        <Link to="/" className="forgot-logo">
          Skill<span>Swap</span>
        </Link>

        <div className="forgot-icon" aria-hidden="true">🔐</div>

        <h1>
          {step === 1 && 'Forgot Password?'}
          {step === 2 && 'Verify Your Email'}
          {step === 3 && 'Create New Password'}
          {step === 4 && 'Password Updated'}
        </h1>

        <p className="auth-description">
          {step === 1 &&
            'Enter the email address linked to your SkillSwap account.'}
          {step === 2 &&
            `Enter the 6-digit code sent to ${email.trim()}.`}
          {step === 3 &&
            'Choose a new password that meets the requirements below.'}
          {step === 4 &&
            'Your password has been changed. You can now sign in with it.'}
        </p>

        {step !== 4 && (
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
        )}

        {error && (
          <p className="forgot-error" role="alert">
            {error}
          </p>
        )}
        {notice && (
          <p className="forgot-notice" role="status">
            {notice}
          </p>
        )}

        {step === 1 && (
          <form onSubmit={handleSendOtp}>
            <label htmlFor="reset-email">Email Address</label>
            <input
              id="reset-email"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              maxLength={254}
              required
            />

            <button
              type="submit"
              className="primary-btn auth-submit"
              disabled={loading}
            >
              {loading ? 'Sending...' : 'Send reset code'}
            </button>
          </form>
        )}

        {step === 2 && (
          <form onSubmit={handleVerifyOtp}>
            <label htmlFor="reset-otp">6-Digit Code</label>
            <input
              id="reset-otp"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              pattern="[0-9]{6}"
              placeholder="Enter your code"
              value={otp}
              onChange={(event) =>
                setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))
              }
              required
            />

            <button
              type="submit"
              className="primary-btn auth-submit"
              disabled={loading}
            >
              {loading ? 'Checking...' : 'Verify code'}
            </button>

            <button
              type="button"
              className="forgot-text-button"
              onClick={(event) => handleSendOtp(event, true)}
              disabled={loading || cooldown > 0}
            >
              {cooldown > 0
                ? `Resend code in ${cooldown}s`
                : 'Resend code'}
            </button>

            <button
              type="button"
              className="forgot-text-button"
              onClick={handleChangeEmail}
              disabled={loading}
            >
              Change email address
            </button>
          </form>
        )}

        {step === 3 && (
          <form onSubmit={handleSavePassword}>
            <label htmlFor="new-password">New Password</label>
            <input
              id="new-password"
              type="password"
              autoComplete="new-password"
              placeholder="Create a new password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              minLength={8}
              maxLength={100}
              pattern={PASSWORD_PATTERN}
              title="Use uppercase and lowercase letters, a number, and one of @$!%*?&."
              required
            />

            <label htmlFor="confirm-password">Confirm New Password</label>
            <input
              id="confirm-password"
              type="password"
              autoComplete="new-password"
              placeholder="Enter the password again"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              minLength={8}
              maxLength={100}
              required
            />

            <button
              type="submit"
              className="primary-btn auth-submit"
              disabled={loading}
            >
              {loading ? 'Saving...' : 'Save new password'}
            </button>
            <button
              type="button"
              className="forgot-text-button"
              onClick={handleChangeEmail}
              disabled={loading}
            >
              Start over
            </button>
          </form>
        )}

        {step === 4 ? (
          <Link to="/login" className="primary-btn forgot-login-button">
            Return to Login
          </Link>
        ) : (
          <Link to="/login" className="forgot-back-link">
            ← Back to Login
          </Link>
        )}
      </div>
    </section>
  )
}

export default ForgotPassword
