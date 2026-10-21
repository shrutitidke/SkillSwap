import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getReceivedExchangeRequests,
  getSentExchangeRequests,
  respondToExchangeRequest,
} from '../services/api'

function RequestCard({ request, incoming, onRespond, respondingId }) {
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
        <Link
          to={`/my-requests/${request.id}/chat`}
          className="secondary-btn request-chat-link"
        >
          Open private chat
        </Link>
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
  const [received, setReceived] = useState([])
  const [sent, setSent] = useState([])
  const [loading, setLoading] = useState(true)
  const [respondingId, setRespondingId] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadRequests = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [incoming, outgoing] = await Promise.all([
        getReceivedExchangeRequests(),
        getSentExchangeRequests(),
      ])
      setReceived(incoming)
      setSent(outgoing)
    } catch (loadError) {
      setError(loadError.message || 'Unable to load exchange requests.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true

    async function loadInitialRequests() {
      try {
        const [incoming, outgoing] = await Promise.all([
          getReceivedExchangeRequests(),
          getSentExchangeRequests(),
        ])
        if (active) {
          setReceived(incoming)
          setSent(outgoing)
        }
      } catch (loadError) {
        if (active) {
          setError(loadError.message || 'Unable to load exchange requests.')
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    loadInitialRequests()
    return () => {
      active = false
    }
  }, [])

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
      await loadRequests()
    } catch (responseError) {
      setError(responseError.message || 'Unable to update this request.')
    } finally {
      setRespondingId(null)
    }
  }

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
                {received.map((request) => (
                  <RequestCard
                    key={request.id}
                    request={request}
                    incoming
                    onRespond={handleRespond}
                    respondingId={respondingId}
                  />
                ))}
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
                {sent.map((request) => (
                  <RequestCard
                    key={request.id}
                    request={request}
                    incoming={false}
                    onRespond={handleRespond}
                    respondingId={respondingId}
                  />
                ))}
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
