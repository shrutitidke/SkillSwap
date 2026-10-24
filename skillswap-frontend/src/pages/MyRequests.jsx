import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../auth/useAuth'
import {
  cancelClassSession,
  createSessionReview,
  getSessionReviews,
  getMyClassSessions,
  getReceivedExchangeRequests,
  getSentExchangeRequests,
  proposeClassSession,
  respondToClassSession,
  respondToExchangeRequest,
} from '../services/api'

function formatSessionDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Time unavailable'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function getLocalDateTimeMin() {
  const date = new Date(Date.now() + 60_000)
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return localDate.toISOString().slice(0, 16)
}

function getSafeMeetingUrl(value) {
  try {
    const url = new URL(value.trim())
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null
  } catch {
    return null
  }
}

function SessionReviewPanel({ session, currentUserId }) {
  const [reviews, setReviews] = useState(null)
  const [rating, setRating] = useState('5')
  const [comment, setComment] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const currentId = String(currentUserId)
  const revieweeId =
    String(session.teacherId) === currentId
      ? session.learnerId
      : session.teacherId
  const revieweeName =
    String(session.teacherId) === currentId
      ? session.learnerName
      : session.teacherName

  const fetchReviews = useCallback(
    () => getSessionReviews(session.id),
    [session.id]
  )

  useEffect(() => {
    let active = true

    fetchReviews()
      .then((data) => {
        if (active) setReviews(data)
      })
      .catch((loadError) => {
        if (active) {
          setError(loadError.message || 'Unable to load session reviews.')
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [fetchReviews])

  const ownReview = reviews?.find(
    (review) => String(review.reviewerId) === currentId
  )

  const handleSubmit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const savedReview = await createSessionReview(session.id, {
        rating: Number(rating),
        comment: comment.trim(),
      })
      setReviews((current) => [...(current || []), savedReview])
      setComment('')
    } catch (saveError) {
      setError(saveError.message || 'Unable to submit your review.')
    } finally {
      setSaving(false)
    }
  }

  const handleRetry = async () => {
    setLoading(true)
    setError('')
    try {
      setReviews(await fetchReviews())
    } catch (loadError) {
      setError(loadError.message || 'Unable to load session reviews.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="session-review-panel">
      <h5>Review this session</h5>
      <p>
        <Link to={`/users/${revieweeId}/profile`}>
          View {revieweeName}&apos;s public profile
        </Link>
      </p>
      {error && <p className="request-error" role="alert">{error}</p>}
      {loading ? (
        <p role="status">Loading reviews...</p>
      ) : reviews === null ? (
        <button
          type="button"
          className="secondary-btn"
          onClick={handleRetry}
        >
          Retry loading reviews
        </button>
      ) : ownReview ? (
        <p className="session-review-submitted" role="status">
          You rated {revieweeName} {ownReview.rating} out of 5.
          {ownReview.comment && ` Your comment: “${ownReview.comment}”`}
        </p>
      ) : (
        <form className="session-review-form" onSubmit={handleSubmit}>
          <label>
            Rating
            <select
              value={rating}
              onChange={(event) => setRating(event.target.value)}
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
              rows={3}
            />
          </label>
          <button type="submit" className="primary-btn" disabled={saving}>
            {saving ? 'Submitting...' : 'Submit review'}
          </button>
        </form>
      )}
    </section>
  )
}

function SessionPanel({
  request,
  incoming,
  sessions,
  currentUserId,
  currentTime,
  actionId,
  onPropose,
  onRespond,
  onCancel,
}) {
  const [scheduledAt, setScheduledAt] = useState('')
  const [durationMinutes, setDurationMinutes] = useState(60)
  const [meetingLink, setMeetingLink] = useState('')
  const [description, setDescription] = useState('')
  const teachingSkillName = incoming
    ? request.skillName
    : request.learnerSkillName

  const handleSubmit = async (event) => {
    event.preventDefault()
    const saved = await onPropose(request.id, {
      scheduledAt,
      durationMinutes: Number(durationMinutes),
      meetingLink: meetingLink.trim(),
      description: description.trim(),
    })

    if (saved) {
      setScheduledAt('')
      setDurationMinutes(60)
      setMeetingLink('')
      setDescription('')
    }
  }

  return (
    <section className="request-session-panel" aria-label="Class scheduling">
      <h4>Class sessions</h4>
      <p className="request-session-help">
        Discuss the lesson in private chat before proposing a time.
      </p>
      {sessions.length ? (
        <div className="request-session-list">
          {sessions.map((session) => {
            const isBusy = actionId === `session-${session.id}`
            const canCancel = ['PROPOSED', 'CONFIRMED'].includes(session.status)
            const sessionStartTime = new Date(session.scheduledAt).getTime()
            const sessionEndTime =
              sessionStartTime + session.durationMinutes * 60_000
            const isFutureSession =
              Number.isFinite(sessionStartTime) && sessionStartTime > currentTime
            const canReview =
              String(session.learnerId) !== String(currentUserId) &&
              session.status === 'CONFIRMED' &&
              Number.isFinite(sessionEndTime) &&
              sessionEndTime <= currentTime
            const safeMeetingUrl = session.meetingLink
              ? getSafeMeetingUrl(session.meetingLink)
              : null
            const canOpenMeetingUrl =
              safeMeetingUrl &&
              ['PROPOSED', 'CONFIRMED'].includes(session.status) &&
              Number.isFinite(sessionEndTime) &&
              sessionEndTime > currentTime
            return (
              <article className="request-session" key={session.id}>
                <div className="request-session-heading">
                  <strong>{formatSessionDate(session.scheduledAt)}</strong>
                  <span
                    className={`request-status status-${session.status.toLowerCase()}`}
                  >
                    {session.status}
                  </span>
                </div>
                <p>{session.durationMinutes} minutes</p>
                <p>
                  <strong>Teaching:</strong>{' '}
                  {session.teachingSkillName || session.skillName}
                  {session.proposerName && ` · Proposed by ${session.proposerName}`}
                </p>
                {session.description && <p>{session.description}</p>}
                {session.meetingLink && (
                  <p className="request-session-link">
                    <strong>Meeting link:</strong>{' '}
                    {canOpenMeetingUrl ? (
                      <a
                        href={safeMeetingUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {session.meetingLink}
                      </a>
                    ) : safeMeetingUrl ? (
                      <span>
                        {session.meetingLink}
                        {sessionEndTime <= currentTime && ' (session ended)'}
                      </span>
                    ) : (
                      <span>{session.meetingLink} (not a valid web link)</span>
                    )}
                  </p>
                )}
                <div className="request-session-actions">
                  {session.status === 'PROPOSED' &&
                    String(session.proposerId) !== String(currentUserId) && (
                    <>
                      <button
                        type="button"
                        className="primary-btn"
                        onClick={() => onRespond(session.id, 'CONFIRMED')}
                        disabled={actionId !== null}
                      >
                        {isBusy ? 'Saving...' : 'Confirm'}
                      </button>
                      <button
                        type="button"
                        className="secondary-btn"
                        onClick={() => onRespond(session.id, 'DECLINED')}
                        disabled={actionId !== null}
                      >
                        Decline
                      </button>
                    </>
                  )}
                  {canCancel && isFutureSession && (
                    <button
                      type="button"
                      className="secondary-btn"
                      onClick={() => onCancel(session.id)}
                      disabled={actionId !== null}
                    >
                      {isBusy ? 'Cancelling...' : 'Cancel session'}
                    </button>
                  )}
                </div>
                {canReview && (
                  <SessionReviewPanel
                    session={session}
                    currentUserId={currentUserId}
                  />
                )}
              </article>
            )
          })}
        </div>
      ) : (
        <p className="request-session-empty">No class sessions proposed yet.</p>
      )}

      {teachingSkillName && (
        <form className="request-session-form" onSubmit={handleSubmit}>
          <h5>Propose a time to teach {teachingSkillName}</h5>
          <label>
            Date and time
            <input
              type="datetime-local"
              value={scheduledAt}
              min={getLocalDateTimeMin()}
              onChange={(event) => setScheduledAt(event.target.value)}
              required
            />
          </label>
          <label>
            Duration
            <select
              value={durationMinutes}
              onChange={(event) => setDurationMinutes(event.target.value)}
            >
              {[15, 30, 45, 60, 90, 120, 180, 240].map((minutes) => (
                <option key={minutes} value={minutes}>
                  {minutes} minutes
                </option>
              ))}
            </select>
          </label>
          <label>
            Meeting link (optional)
            <input
              type="url"
              maxLength={1000}
              value={meetingLink}
              onChange={(event) => setMeetingLink(event.target.value)}
              placeholder="https://..."
            />
          </label>
          <label>
            Description (optional)
            <textarea
              maxLength={500}
              rows={2}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </label>
          <button
            type="submit"
            className="primary-btn"
            disabled={actionId !== null}
          >
            {actionId === `proposal-${request.id}`
              ? 'Sending proposal...'
              : 'Propose session'}
          </button>
        </form>
      )}
    </section>
  )
}

function RequestCard({
  request,
  incoming,
  sessions,
  currentUserId,
  currentTime,
  onRespond,
  respondingId,
  sessionActionId,
  onProposeSession,
  onRespondToSession,
  onCancelSession,
}) {
  const otherUser = incoming ? request.learnerName : request.teacherName

  return (
    <article className="request-card">
      <div className="request-card-header">
        <h3>{otherUser}</h3>
        <span className={`request-status status-${request.status?.toLowerCase()}`}>
          {request.status}
        </span>
      </div>
      <p>
        <strong>{incoming ? 'You teach:' : 'They teach:'}</strong>{' '}
        {request.skillName}
      </p>
      {request.learnerSkillName && (
        <p>
          <strong>{incoming ? 'They offer:' : 'You offer:'}</strong>{' '}
          {request.learnerSkillName}
        </p>
      )}
      {request.message && <p className="request-message">{request.message}</p>}
      {request.status === 'ACCEPTED' && (
        <>
          <Link
            to={`/my-requests/${request.id}/chat`}
            className="secondary-btn request-chat-link"
          >
            Open private chat
          </Link>
          <SessionPanel
            request={request}
            incoming={incoming}
            sessions={sessions}
            currentUserId={currentUserId}
            currentTime={currentTime}
            actionId={sessionActionId}
            onPropose={onProposeSession}
            onRespond={onRespondToSession}
            onCancel={onCancelSession}
          />
        </>
      )}
      {incoming && request.status === 'PENDING' && (
        <div className="request-actions">
          <button
            type="button"
            className="primary-btn"
            onClick={() => onRespond(request.id, 'ACCEPTED')}
            disabled={respondingId !== null}
          >
            {respondingId === request.id ? 'Saving...' : 'Accept'}
          </button>
          <button
            type="button"
            className="secondary-btn"
            onClick={() => onRespond(request.id, 'REJECTED')}
            disabled={respondingId !== null}
          >
            Reject
          </button>
        </div>
      )}
    </article>
  )
}

function MyRequests() {
  const { user } = useAuth()
  const [received, setReceived] = useState([])
  const [sent, setSent] = useState([])
  const [sessions, setSessions] = useState([])
  const [currentTime, setCurrentTime] = useState(() => Date.now())
  const [loading, setLoading] = useState(true)
  const [respondingId, setRespondingId] = useState(null)
  const [sessionActionId, setSessionActionId] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadRequests = useCallback(async (showLoading = true) => {
    if (showLoading) setLoading(true)
    setError('')
    try {
      const [incoming, outgoing, mySessions] = await Promise.all([
        getReceivedExchangeRequests(),
        getSentExchangeRequests(),
        getMyClassSessions(),
      ])
      setReceived(incoming)
      setSent(outgoing)
      setSessions(mySessions)
      setCurrentTime(Date.now())
    } catch (loadError) {
      setError(loadError.message || 'Unable to load your requests and sessions.')
    } finally {
      if (showLoading) setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true

    async function loadInitialRequests() {
      try {
        const [incoming, outgoing, mySessions] = await Promise.all([
          getReceivedExchangeRequests(),
          getSentExchangeRequests(),
          getMyClassSessions(),
        ])
        if (active) {
          setReceived(incoming)
          setSent(outgoing)
          setSessions(mySessions)
          setCurrentTime(Date.now())
        }
      } catch (loadError) {
        if (active) {
          setError(
            loadError.message || 'Unable to load your requests and sessions.'
          )
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    loadInitialRequests()
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    const upcomingSessionTimes = sessions
      .filter((session) => ['PROPOSED', 'CONFIRMED'].includes(session.status))
      .flatMap((session) => {
        const start = new Date(session.scheduledAt).getTime()
        const end = start + session.durationMinutes * 60_000
        return session.status === 'CONFIRMED' ? [start, end] : [start]
      })
      .filter((timestamp) => Number.isFinite(timestamp) && timestamp > currentTime)
      .sort((first, second) => first - second)
    const nextSessionTime = upcomingSessionTimes[0]

    if (nextSessionTime === undefined) return undefined

    const delay = Math.min(nextSessionTime - currentTime, 60_000)
    const timer = window.setTimeout(() => setCurrentTime(Date.now()), delay)
    return () => window.clearTimeout(timer)
  }, [sessions, currentTime])

  const handleRespond = async (requestId, status) => {
    if (respondingId !== null) return
    setRespondingId(requestId)
    setError('')
    setNotice('')
    try {
      await respondToExchangeRequest(requestId, status)
      setNotice(
        status === 'ACCEPTED'
          ? 'Exchange request accepted.'
          : 'Exchange request rejected.'
      )
      await loadRequests(false)
    } catch (responseError) {
      setError(responseError.message || 'Unable to update this request.')
    } finally {
      setRespondingId(null)
    }
  }

  const handleProposeSession = async (requestId, session) => {
    setSessionActionId(`proposal-${requestId}`)
    setError('')
    setNotice('')
    try {
      await proposeClassSession(requestId, session)
      setNotice('Class session proposed. The other participant must confirm it.')
      await loadRequests(false)
      return true
    } catch (proposalError) {
      setError(proposalError.message || 'Unable to propose this class session.')
      return false
    } finally {
      setSessionActionId(null)
    }
  }

  const handleRespondToSession = async (sessionId, status) => {
    setSessionActionId(`session-${sessionId}`)
    setError('')
    setNotice('')
    try {
      await respondToClassSession(sessionId, status)
      setNotice(
        status === 'CONFIRMED'
          ? 'Class session confirmed by both participants.'
          : 'Class session declined.'
      )
      await loadRequests(false)
    } catch (responseError) {
      setError(responseError.message || 'Unable to update this class session.')
    } finally {
      setSessionActionId(null)
    }
  }

  const handleCancelSession = async (sessionId) => {
    setSessionActionId(`session-${sessionId}`)
    setError('')
    setNotice('')
    try {
      await cancelClassSession(sessionId)
      setNotice('Class session cancelled.')
      await loadRequests(false)
    } catch (cancelError) {
      setError(cancelError.message || 'Unable to cancel this class session.')
    } finally {
      setSessionActionId(null)
    }
  }

  const renderRequestCard = (request, incoming) => (
    <RequestCard
      key={request.id}
      request={request}
      incoming={incoming}
      sessions={sessions.filter(
        (session) => String(session.requestId) === String(request.id)
      )}
      currentUserId={user?.id}
      currentTime={currentTime}
      onRespond={handleRespond}
      respondingId={respondingId}
      sessionActionId={sessionActionId}
      onProposeSession={handleProposeSession}
      onRespondToSession={handleRespondToSession}
      onCancelSession={handleCancelSession}
    />
  )

  return (
    <section className="requests-page">
      <header className="requests-header">
        <p className="eyebrow">YOUR SKILLSWAP ACTIVITY</p>
        <h1>My Requests</h1>
        <p>Review incoming exchange requests and track requests you sent.</p>
      </header>

      {notice && <p className="request-notice" role="status">{notice}</p>}
      {error && <p className="request-error" role="alert">{error}</p>}

      {loading ? (
        <div className="explore-empty" role="status">
          Loading your requests...
        </div>
      ) : (
        <div className="request-columns">
          <section className="request-section">
            <div className="results-heading">
              <h2>Received</h2>
              <p>{received.length} requests</p>
            </div>
            {received.length ? (
              <div className="request-list">
                {received.map((request) => renderRequestCard(request, true))}
              </div>
            ) : (
              <div className="explore-empty">
                <p>No incoming requests yet.</p>
              </div>
            )}
          </section>

          <section className="request-section">
            <div className="results-heading">
              <h2>Sent</h2>
              <p>{sent.length} requests</p>
            </div>
            {sent.length ? (
              <div className="request-list">
                {sent.map((request) => renderRequestCard(request, false))}
              </div>
            ) : (
              <div className="explore-empty">
                <p>You have not sent any requests yet.</p>
              </div>
            )}
          </section>
        </div>
      )}
    </section>
  )
}

export default MyRequests
