import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../auth/useAuth'
import {
  getMyClassSessions,
  getMySkills,
  getReciprocalMatches,
  getReceivedExchangeRequests,
  getSentExchangeRequests,
} from '../services/api'

const EMPTY_DASHBOARD = {
  skills: [],
  matches: [],
  received: [],
  sent: [],
  sessions: [],
  loadedAt: 0,
}

function formatSessionDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date unavailable'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function getExchangeProgress(acceptedAt) {
  if (!acceptedAt) return null

  const acceptedDate = new Date(acceptedAt)
  if (Number.isNaN(acceptedDate.getTime())) return null

  const today = new Date()
  const todayStart = Date.UTC(
    today.getFullYear(),
    today.getMonth(),
    today.getDate()
  )
  const acceptedStart = Date.UTC(
    acceptedDate.getFullYear(),
    acceptedDate.getMonth(),
    acceptedDate.getDate()
  )
  const elapsedDays = Math.max(
    0,
    Math.floor((todayStart - acceptedStart) / 86400000)
  )
  const day = Math.min(elapsedDays + 1, 10)

  return {
    day,
    percent: (day / 10) * 100,
    targetReached: elapsedDays >= 10,
  }
}

function RequestSummary({ request, incoming }) {
  return (
    <article className="dashboard-request">
      <div>
        <h3>{incoming ? request.learnerName : request.teacherName}</h3>
        <p>{request.skillName}</p>
      </div>
      <span className={`request-status status-${request.status?.toLowerCase()}`}>
        {request.status}
      </span>
    </article>
  )
}

function ActiveExchange({ request, incoming }) {
  const progress = getExchangeProgress(request.acceptedAt)
  const partnerName = incoming ? request.learnerName : request.teacherName

  return (
    <article className="dashboard-active-exchange">
      <div className="dashboard-active-heading">
        <div>
          <h3>{partnerName}</h3>
          <p>
            {incoming
              ? `You teach ${request.skillName}${
                  request.learnerSkillName
                    ? ` and learn ${request.learnerSkillName}`
                    : ''
                }`
              : `You learn ${request.skillName}${
                  request.learnerSkillName
                    ? ` and teach ${request.learnerSkillName}`
                    : ''
                }`}
          </p>
        </div>
        <span className="request-status status-accepted">ACCEPTED</span>
      </div>
      {progress ? (
        <div className="dashboard-progress">
          <div className="dashboard-progress-label">
            <span>
              {progress.targetReached
                ? '10-day target reached'
                : `Day ${progress.day} of 10`}
            </span>
            <span>Since request acceptance</span>
          </div>
          <div
            className="dashboard-progress-track"
            role="progressbar"
            aria-label={`Exchange timeline for ${partnerName}`}
            aria-valuemin="0"
            aria-valuemax="10"
            aria-valuenow={progress.day}
          >
            <span style={{ width: `${progress.percent}%` }} />
          </div>
        </div>
      ) : (
        <p className="dashboard-muted">
          Acceptance date is unavailable for this exchange.
        </p>
      )}
    </article>
  )
}

function Dashboard() {
  const { user } = useAuth()
  const [data, setData] = useState(EMPTY_DASHBOARD)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true

    async function loadDashboard() {
      setLoading(true)
      setError('')

      try {
        const [skills, matches, received, sent, sessions] = await Promise.all([
          getMySkills(),
          getReciprocalMatches(),
          getReceivedExchangeRequests(),
          getSentExchangeRequests(),
          getMyClassSessions(),
        ])

        if (active) {
          setData({
            skills,
            matches,
            received,
            sent,
            sessions,
            loadedAt: Date.now(),
          })
        }
      } catch (loadError) {
        if (active) {
          setError(loadError.message || 'Unable to load your dashboard.')
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    loadDashboard()
    return () => {
      active = false
    }
  }, [reloadKey])

  const teachingSkills = data.skills.filter((skill) => skill.type === 'TEACH')
  const learningSkills = data.skills.filter((skill) => skill.type === 'LEARN')
  const pendingReceived = data.received.filter(
    (request) => request.status === 'PENDING'
  ).length
  const pendingSent = data.sent.filter(
    (request) => request.status === 'PENDING'
  ).length
  const activeExchanges = [
    ...data.received.map((request) => ({ request, incoming: true })),
    ...data.sent.map((request) => ({ request, incoming: false })),
  ].filter(({ request }) => request.status === 'ACCEPTED')
  const allUpcomingSessions = data.sessions
    .filter((session) => {
      const scheduledAt = new Date(session.scheduledAt)
      return (
        ['PROPOSED', 'CONFIRMED'].includes(session.status) &&
        !Number.isNaN(scheduledAt.getTime()) &&
        scheduledAt.getTime() >= data.loadedAt
      )
    })
    .sort(
      (first, second) =>
        new Date(first.scheduledAt).getTime() -
        new Date(second.scheduledAt).getTime()
    )
  const upcomingSessions = allUpcomingSessions.slice(0, 3)

  return (
    <section className="dashboard-page">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">YOUR SKILLSWAP OVERVIEW</p>
          <h1>Welcome back, {user?.name || 'learner'}!</h1>
          <p>See what is happening with your skills and exchanges.</p>
        </div>
        <Link to="/skills" className="primary-btn">
          Find a match
        </Link>
      </header>

      {error && (
        <div className="dashboard-error" role="alert">
          <p>{error}</p>
          <button
            type="button"
            className="secondary-btn"
            onClick={() => setReloadKey((current) => current + 1)}
          >
            Try again
          </button>
        </div>
      )}

      {loading ? (
        <div className="explore-empty" role="status">
          Loading your dashboard...
        </div>
      ) : !error ? (
        <>
          <section className="dashboard-stats" aria-label="Your activity summary">
            <article className="dashboard-stat">
              <span className="dashboard-stat-icon" aria-hidden="true">↗</span>
              <p>Skills you teach</p>
              <strong>{teachingSkills.length}</strong>
            </article>
            <article className="dashboard-stat">
              <span className="dashboard-stat-icon" aria-hidden="true">✦</span>
              <p>Skills you want to learn</p>
              <strong>{learningSkills.length}</strong>
            </article>
            <article className="dashboard-stat">
              <span className="dashboard-stat-icon" aria-hidden="true">↓</span>
              <p>Incoming requests to review</p>
              <strong>{pendingReceived}</strong>
            </article>
            <article className="dashboard-stat">
              <span className="dashboard-stat-icon" aria-hidden="true">↑</span>
              <p>Outgoing requests pending</p>
              <strong>{pendingSent}</strong>
            </article>
            <article className="dashboard-stat">
              <span className="dashboard-stat-icon" aria-hidden="true">⇄</span>
              <p>Reciprocal matches</p>
              <strong>{data.matches.length}</strong>
            </article>
            <article className="dashboard-stat">
              <span className="dashboard-stat-icon" aria-hidden="true">✓</span>
              <p>Accepted exchanges</p>
              <strong>{activeExchanges.length}</strong>
            </article>
            <article className="dashboard-stat">
              <span className="dashboard-stat-icon" aria-hidden="true">◷</span>
              <p>Upcoming sessions</p>
              <strong>{allUpcomingSessions.length}</strong>
            </article>
          </section>

          <section className="dashboard-shortcuts" aria-label="Quick links">
            <Link to="/my-skills" className="dashboard-shortcut">
              <span aria-hidden="true">✎</span>
              <div>
                <h2>Manage your skills</h2>
                <p>Update what you can teach and want to learn.</p>
              </div>
              <span className="dashboard-arrow" aria-hidden="true">→</span>
            </Link>
            <Link to="/skills" className="dashboard-shortcut">
              <span aria-hidden="true">⇄</span>
              <div>
                <h2>Get What You Want</h2>
                <p>Explore people who are a reciprocal skill match.</p>
              </div>
              <span className="dashboard-arrow" aria-hidden="true">→</span>
            </Link>
            <Link to="/my-requests" className="dashboard-shortcut">
              <span aria-hidden="true">⇄</span>
              <div>
                <h2>Review requests</h2>
                <p>
                  {pendingReceived + pendingSent
                    ? `${pendingReceived + pendingSent} pending exchange ${
                        pendingReceived + pendingSent === 1 ? 'request' : 'requests'
                      }.`
                    : 'Check the status of your exchanges.'}
                </p>
              </div>
              <span className="dashboard-arrow" aria-hidden="true">→</span>
            </Link>
            <a href="#active-exchanges" className="dashboard-shortcut">
              <span aria-hidden="true">✓</span>
              <div>
                <h2>Active exchanges</h2>
                <p>
                  {activeExchanges.length
                    ? `${activeExchanges.length} accepted ${
                        activeExchanges.length === 1 ? 'exchange' : 'exchanges'
                      } in progress.`
                    : 'See your accepted exchanges and progress.'}
                </p>
              </div>
              <span className="dashboard-arrow" aria-hidden="true">→</span>
            </a>
          </section>

          <div className="dashboard-content-grid">
            <section className="dashboard-panel">
              <div className="dashboard-panel-heading">
                <div>
                  <p className="eyebrow">YOUR PROFILE</p>
                  <h2>My skills</h2>
                </div>
                <Link to="/my-skills">Manage</Link>
              </div>
              <div className="dashboard-skill-groups">
                <div>
                  <h3>I can teach</h3>
                  {teachingSkills.length ? (
                    <ul className="dashboard-skill-list">
                      {teachingSkills.map((skill) => (
                        <li key={skill.id}>{skill.name}</li>
                      ))}
                    </ul>
                  ) : (
                    <p className="dashboard-muted">
                      Add a skill you can share.
                    </p>
                  )}
                </div>
                <div>
                  <h3>I want to learn</h3>
                  {learningSkills.length ? (
                    <ul className="dashboard-skill-list">
                      {learningSkills.map((skill) => (
                        <li key={skill.id}>{skill.name}</li>
                      ))}
                    </ul>
                  ) : (
                    <p className="dashboard-muted">
                      Add a skill you would like to learn.
                    </p>
                  )}
                </div>
              </div>
            </section>

            <section className="dashboard-panel">
              <div className="dashboard-panel-heading">
                <div>
                  <p className="eyebrow">YOUR EXCHANGES</p>
                  <h2>Recent requests</h2>
                </div>
                <Link to="/my-requests">View all</Link>
              </div>
              {data.received.length || data.sent.length ? (
                <div className="dashboard-request-list">
                  {data.received.slice(0, 2).map((request) => (
                    <RequestSummary
                      key={`received-${request.id}`}
                      request={request}
                      incoming
                    />
                  ))}
                  {data.sent.slice(0, 2).map((request) => (
                    <RequestSummary
                      key={`sent-${request.id}`}
                      request={request}
                      incoming={false}
                    />
                  ))}
                </div>
              ) : (
                <div className="dashboard-empty">
                  <p>You do not have any exchange requests yet.</p>
                  <Link to="/skills">Explore reciprocal matches</Link>
                </div>
              )}
            </section>
          </div>

          <section
            className="dashboard-panel dashboard-active-panel"
            id="active-exchanges"
          >
            <div className="dashboard-panel-heading">
              <div>
                <p className="eyebrow">ACCEPTED EXCHANGES</p>
                <h2>Exchange progress</h2>
              </div>
              <Link to="/my-requests">Manage exchanges</Link>
            </div>
            {activeExchanges.length ? (
              <div className="dashboard-active-list">
                {activeExchanges.map(({ request, incoming }) => (
                  <ActiveExchange
                    key={request.id}
                    request={request}
                    incoming={incoming}
                  />
                ))}
              </div>
            ) : (
              <div className="dashboard-empty">
                <p>You do not have any accepted exchanges yet.</p>
                <Link to="/skills">Find a reciprocal match</Link>
              </div>
            )}
          </section>

          <section className="dashboard-panel dashboard-sessions">
            <div className="dashboard-panel-heading">
              <div>
                <p className="eyebrow">KEEP LEARNING</p>
                <h2>Upcoming sessions</h2>
              </div>
              <Link to="/my-requests">View requests</Link>
            </div>
            {upcomingSessions.length ? (
              <div className="dashboard-session-list">
                {upcomingSessions.map((session) => (
                  <article className="dashboard-session" key={session.id}>
                    <div className="dashboard-session-date" aria-hidden="true">
                      ◷
                    </div>
                    <div>
                      <h3>{session.skillName}</h3>
                      <p>
                        {session.teacherName} and {session.learnerName}
                      </p>
                      <time dateTime={session.scheduledAt}>
                        {formatSessionDate(session.scheduledAt)}
                      </time>
                    </div>
                    <span
                      className={`request-status status-${session.status.toLowerCase()}`}
                    >
                      {session.status}
                    </span>
                  </article>
                ))}
              </div>
            ) : (
              <div className="dashboard-empty">
                <p>No upcoming sessions are scheduled.</p>
                <span>Confirmed or proposed future sessions will appear here.</span>
              </div>
            )}
          </section>
        </>
      ) : null}
    </section>
  )
}

export default Dashboard
