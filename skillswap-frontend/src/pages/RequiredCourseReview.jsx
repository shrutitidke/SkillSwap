import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  createSessionReview,
  getRequiredCourseReviews,
} from '../services/api'

function formatSessionDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date unavailable'
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function RequiredCourseReview() {
  const navigate = useNavigate()
  const [requirements, setRequirements] = useState([])
  const [rating, setRating] = useState('5')
  const [comment, setComment] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const loadRequirements = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await getRequiredCourseReviews()
      setRequirements(data)
      if (data.length === 0) navigate('/', { replace: true })
    } catch (loadError) {
      setError(loadError.message || 'Unable to load required course reviews.')
    } finally {
      setLoading(false)
    }
  }, [navigate])

  useEffect(() => {
    let active = true
    getRequiredCourseReviews()
      .then((data) => {
        if (!active) return
        setRequirements(data)
        if (data.length === 0) navigate('/', { replace: true })
      })
      .catch((loadError) => {
        if (active) {
          setError(
            loadError.message || 'Unable to load required course reviews.'
          )
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [navigate])

  const current = requirements[0]

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!current || saving) return

    setSaving(true)
    setError('')
    try {
      await createSessionReview(current.sessionId, {
        rating: Number(rating),
        comment: comment.trim(),
      })
      setRating('5')
      setComment('')
      const remaining = await getRequiredCourseReviews()
      setRequirements(remaining)
      if (remaining.length === 0) navigate('/', { replace: true })
    } catch (submitError) {
      setError(submitError.message || 'Unable to submit your course rating.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="required-review-page">
      <div className="required-review-card">
        <p className="eyebrow">COURSE COMPLETED</p>
        <h1>Rate your teacher</h1>
        <p className="required-review-intro">
          Your 10-day exchange course is complete. Submit a rating to continue
          using SkillSwap.
        </p>

        {error && <p className="request-error" role="alert">{error}</p>}
        {loading ? (
          <p className="notifications-state" role="status">
            Loading your completed course...
          </p>
        ) : current ? (
          <>
            <div className="required-review-course">
              <p><strong>Teacher:</strong> {current.teacherName}</p>
              <p><strong>Skill:</strong> {current.skillName}</p>
              <p><strong>Last session:</strong> {formatSessionDate(current.scheduledAt)}</p>
            </div>
            <form className="required-review-form" onSubmit={handleSubmit}>
              <label>
                Rating
                <select
                  value={rating}
                  onChange={(event) => setRating(event.target.value)}
                  required
                >
                  {[5, 4, 3, 2, 1].map((value) => (
                    <option value={value} key={value}>
                      {value} {value === 1 ? 'star' : 'stars'}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Comment (optional)
                <textarea
                  value={comment}
                  onChange={(event) => setComment(event.target.value)}
                  maxLength={1000}
                  rows={4}
                />
              </label>
              <button
                type="submit"
                className="primary-btn"
                disabled={saving}
              >
                {saving ? 'Submitting rating...' : 'Submit rating'}
              </button>
            </form>
            {requirements.length > 1 && (
              <p className="required-review-remaining">
                {requirements.length} completed courses need a rating.
                Submitting this one will show the next course.
              </p>
            )}
          </>
        ) : !error ? (
          <p className="notifications-state">
            No course ratings are currently required.
          </p>
        ) : (
          <button
            type="button"
            className="secondary-btn"
            onClick={loadRequirements}
          >
            Try again
          </button>
        )}
      </div>
    </section>
  )
}

export default RequiredCourseReview
