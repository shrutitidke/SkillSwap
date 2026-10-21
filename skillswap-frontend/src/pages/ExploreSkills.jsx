import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getReciprocalMatches,
  getReceivedExchangeRequests,
  getSentExchangeRequests,
  sendExchangeRequest,
} from '../services/api'

function ExploreSkills() {
  const [matches, setMatches] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [requestsLoading, setRequestsLoading] = useState(true)
  const [error, setError] = useState('')
  const [requestError, setRequestError] = useState('')
  const [sendingKey, setSendingKey] = useState('')
  const [sentRequests, setSentRequests] = useState({})
  const [existingRequests, setExistingRequests] = useState([])
  const [requestNotice, setRequestNotice] = useState('')

  useEffect(() => {
    let cancelled = false

    async function loadMatches() {
      try {
        setLoading(true)
        setError('')

        const [data, received, sent] = await Promise.all([
          getReciprocalMatches(),
          getReceivedExchangeRequests(),
          getSentExchangeRequests(),
        ])

        if (!cancelled) {
          setMatches(data)
          setExistingRequests([...received, ...sent])
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || 'Unable to load reciprocal matches.')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
          setRequestsLoading(false)
        }
      }
    }

    loadMatches()

    return () => {
      cancelled = true
    }
  }, [])

  const searchText = search.trim().toLowerCase()
  const filteredMatches = matches.filter((match) =>
    [
      match.userName,
      match.skillTheyTeach,
      match.skillTheyTeachDescription,
      match.skillYouTeach,
      match.skillYouTeachDescription,
    ].some((value) => value?.toLowerCase().includes(searchText))
  )

  const handleSendRequest = async (match) => {
    const requestKey = `${match.userId}-${match.skillTheyTeachId}-${match.skillYouTeachId}`
    if (sendingKey) return

    setSendingKey(requestKey)
    setRequestNotice('')
    setRequestError('')
    try {
      await sendExchangeRequest({
        skillId: match.skillTheyTeachId,
        learnerSkillId: match.skillYouTeachId,
      })
      setSentRequests((current) => ({ ...current, [requestKey]: 'PENDING' }))
      setExistingRequests((current) => [
        ...current,
        {
          status: 'PENDING',
          teacherSkillId: match.skillTheyTeachId,
          learnerSkillId: match.skillYouTeachId,
        },
      ])
      setRequestNotice(`Request sent to ${match.userName}.`)
    } catch (requestError) {
      setRequestError(
        requestError.message || 'Unable to send the exchange request.'
      )
    } finally {
      setSendingKey('')
    }
  }

  return (
    <section className="explore-page">
      <div className="explore-header">
        <p className="eyebrow">RECIPROCAL SKILL EXCHANGE</p>

        <h1>
          Get What <span>You Want</span>
        </h1>

        <p>
          Find people whose skills they want to learn match the skills you can
          teach, and who can teach something you want to learn.
        </p>
      </div>

      <div className="search-bar">
        <input
          type="search"
          placeholder="Search by member, skill, or description..."
          aria-label="Search reciprocal matches"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>

      <div className="skills-results">
        {requestNotice && (
          <p className="request-notice" role="status">
            {requestNotice}
          </p>
        )}
        {requestError && (
          <p className="request-error" role="alert">
            {requestError}
          </p>
        )}
        <div className="results-heading">
          <h2>Your Reciprocal Matches</h2>
          <p>
            {loading
              ? 'Finding reciprocal matches...'
              : `${filteredMatches.length} matches found`}
          </p>
        </div>

        {loading ? (
          <div className="explore-empty">
            <p>Finding members who can exchange skills with you...</p>
          </div>
        ) : error ? (
          <div className="explore-empty">
            <h2>Unable to load matches</h2>
            <p>{error}</p>

            {!localStorage.getItem('skillswap_token') && (
              <Link to="/login" className="primary-btn">
                Log in
              </Link>
            )}

            <button
              type="button"
              className="secondary-btn"
              onClick={() => window.location.reload()}
            >
              Try Again
            </button>
          </div>
        ) : filteredMatches.length > 0 ? (
          <div className="skills-grid">
            {filteredMatches.map((match) => {
              const requestKey =
                `${match.userId}-${match.skillTheyTeachId}-` +
                match.skillYouTeachId
              const matchingRequest = existingRequests.find(
                (request) =>
                  (request.status === 'PENDING' ||
                    (request.status === 'ACCEPTED' &&
                      !request.skillsChangedSinceAcceptance)) &&
                  ((request.teacherSkillId === match.skillTheyTeachId &&
                    request.learnerSkillId === match.skillYouTeachId) ||
                    (request.teacherSkillId === match.skillYouTeachId &&
                      request.learnerSkillId === match.skillTheyTeachId))
              )
              const requestStatus =
                sentRequests[requestKey] || matchingRequest?.status

              return (
              <article
                className="skill-card reciprocal-match-card"
                key={`${match.userId}-${match.skillTheyTeachId}-${match.skillYouTeachId}`}
              >
                <div className="skill-card-top">
                  <div className="skill-icon" aria-hidden="true">
                    ⇄
                  </div>
                  <span className="skill-level">Reciprocal match</span>
                </div>

                <p className="skill-category">SKILLSWAP MEMBER</p>
                <h3>{match.userName}</h3>

                <div className="match-skill">
                  <strong>They can teach you: {match.skillTheyTeach}</strong>
                  <p className="skill-description">
                    {match.skillTheyTeachDescription ||
                      'No description provided.'}
                  </p>
                </div>

                <div className="match-skill">
                  <strong>You can teach them: {match.skillYouTeach}</strong>
                  <p className="skill-description">
                    {match.skillYouTeachDescription ||
                      'No description provided.'}
                  </p>
                </div>

                <p className="match-explanation">{match.explanation}</p>
                {requestsLoading ? (
                  <p className="match-request-status" role="status">
                    Checking exchange status...
                  </p>
                ) : requestStatus === 'ACCEPTED' ? (
                  <p className="match-request-status" role="status">
                    Exchange accepted
                  </p>
                ) : requestStatus === 'PENDING' ? (
                  <p className="match-request-status" role="status">
                    Exchange request pending
                  </p>
                ) : (
                  <button
                    type="button"
                    className="primary-btn match-request-btn"
                    onClick={() => handleSendRequest(match)}
                    disabled={sendingKey !== ''}
                  >
                    {sendingKey === requestKey
                      ? 'Sending...'
                      : 'Send exchange request'}
                  </button>
                )}
              </article>
              )
            })}
          </div>
        ) : (
          <div className="explore-empty">
            <div className="empty-icon" aria-hidden="true">
              ⇄
            </div>

            <h2>
              {matches.length === 0
                ? 'No reciprocal matches yet'
                : 'No matches found'}
            </h2>

            <p>
              {matches.length === 0
                ? 'Add at least one TEACH skill and one LEARN skill to your profile. Matches appear when another member can teach what you want to learn and wants to learn something you can teach.'
                : 'Try another name or skill, or search a description.'}
            </p>

            {search && (
              <button
                type="button"
                className="secondary-btn"
                onClick={() => setSearch('')}
              >
                Clear Search
              </button>
            )}
          </div>
        )}
      </div>
    </section>
  )
}

export default ExploreSkills
