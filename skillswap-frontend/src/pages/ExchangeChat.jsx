import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import useAuth from '../auth/useAuth'
import {
  getExchangeChatMessages,
  getReceivedExchangeRequests,
  getSentExchangeRequests,
  sendExchangeChatMessage,
} from '../services/api'

const POLL_INTERVAL_MS = 5000
const MAX_MESSAGE_LENGTH = 2000

function formatMessageDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Time unavailable'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function mergeMessages(current, incoming) {
  const unique = new Map(current.map((message) => [message.id, message]))
  incoming.forEach((message) => unique.set(message.id, message))
  return [...unique.values()].sort((first, second) => first.id - second.id)
}

function ExchangeChat() {
  const { requestId } = useParams()
  const { user } = useAuth()
  const [exchange, setExchange] = useState(null)
  const [messages, setMessages] = useState([])
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const lastMessageId = useRef(0)
  const messageListRef = useRef(null)

  useEffect(() => {
    let active = true
    let pollingInterval
    let pollingInProgress = false

    async function loadConversation() {
      setLoading(true)
      setError('')
      lastMessageId.current = 0
      setMessages([])

      try {
        const [initialMessages, received, sent] = await Promise.all([
          getExchangeChatMessages(requestId),
          getReceivedExchangeRequests(),
          getSentExchangeRequests(),
        ])
        const currentExchange = [...received, ...sent].find(
          (request) => String(request.id) === requestId
        )

        if (!currentExchange || currentExchange.status !== 'ACCEPTED') {
          throw new Error('This accepted exchange chat could not be found.')
        }

        if (active) {
          setExchange({
            ...currentExchange,
            incoming: received.some(
              (request) => String(request.id) === requestId
            ),
          })
          setMessages(initialMessages)
          lastMessageId.current = initialMessages.reduce(
            (latest, message) => Math.max(latest, message.id),
            0
          )
          pollingInterval = window.setInterval(async () => {
            if (pollingInProgress) return
            pollingInProgress = true
            try {
              const newerMessages = await getExchangeChatMessages(
                requestId,
                lastMessageId.current
              )
              if (!active || newerMessages.length === 0) return

              lastMessageId.current = newerMessages.reduce(
                (latest, message) => Math.max(latest, message.id),
                lastMessageId.current
              )
              setMessages((current) =>
                mergeMessages(current, newerMessages)
              )
              setError('')
            } catch (pollError) {
              if (active) {
                setError(
                  pollError.message ||
                    'Unable to refresh chat messages. Retrying shortly.'
                )
              }
            } finally {
              pollingInProgress = false
            }
          }, POLL_INTERVAL_MS)
        }
      } catch (loadError) {
        if (active) {
          setError(loadError.message || 'Unable to load this exchange chat.')
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    loadConversation()
    return () => {
      active = false
      window.clearInterval(pollingInterval)
    }
  }, [requestId, reloadKey])

  useEffect(() => {
    messageListRef.current?.scrollTo({
      top: messageListRef.current.scrollHeight,
      behavior: 'smooth',
    })
  }, [messages])

  const handleSend = async (event) => {
    event.preventDefault()
    const trimmedContent = content.trim()
    if (!trimmedContent || sending) return

    setSending(true)
    setError('')
    try {
      const sentMessage = await sendExchangeChatMessage(
        requestId,
        trimmedContent
      )
      lastMessageId.current = Math.max(
        lastMessageId.current,
        sentMessage.id
      )
      setMessages((current) => mergeMessages(current, [sentMessage]))
      setContent('')
    } catch (sendError) {
      setError(sendError.message || 'Unable to send this message.')
    } finally {
      setSending(false)
    }
  }

  const partnerName = exchange
    ? exchange.incoming
      ? exchange.learnerName
      : exchange.teacherName
    : 'Exchange participant'

  return (
    <section className="chat-page">
      <Link to="/my-requests" className="chat-back-link">
        ← Back to My Requests
      </Link>

      <header className="chat-header">
        <div>
          <p className="eyebrow">PRIVATE EXCHANGE CHAT</p>
          <h1>{partnerName}</h1>
          {exchange && (
            <p>
              You teach {exchange.incoming ? exchange.skillName : exchange.learnerSkillName || 'your skill'}
              {' · '}
              You learn {exchange.incoming ? exchange.learnerSkillName || 'their skill' : exchange.skillName}
            </p>
          )}
        </div>
        <button
          type="button"
          className="secondary-btn"
          onClick={() => setReloadKey((current) => current + 1)}
          disabled={loading}
        >
          Refresh chat
        </button>
      </header>

      {error && <p className="chat-error" role="alert">{error}</p>}

      {loading ? (
        <div className="explore-empty" role="status">
          Loading private messages...
        </div>
      ) : exchange ? (
        <section className="chat-panel" aria-label={`Private chat with ${partnerName}`}>
          <div className="chat-refresh-note">
            New messages refresh automatically every 5 seconds.
          </div>
          <div className="chat-messages" ref={messageListRef} aria-live="polite">
            {messages.length ? (
              messages.map((message) => {
                const ownMessage = message.senderId === user?.id
                return (
                  <article
                    className={`chat-message-row ${ownMessage ? 'chat-message-own' : ''}`}
                    key={message.id}
                  >
                    <div className="chat-message">
                      <div className="chat-message-meta">
                        <strong>{ownMessage ? 'You' : message.senderName}</strong>
                        <time dateTime={message.sentAt}>
                          {formatMessageDate(message.sentAt)}
                        </time>
                      </div>
                      <p>{message.content}</p>
                    </div>
                  </article>
                )
              })
            ) : (
              <div className="chat-empty">
                <h2>Start the conversation</h2>
                <p>Discuss what you want to learn and how to plan your exchange.</p>
              </div>
            )}
          </div>

          <form className="chat-compose" onSubmit={handleSend}>
            <label htmlFor="exchange-chat-message">Message</label>
            <textarea
              id="exchange-chat-message"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              maxLength={MAX_MESSAGE_LENGTH}
              rows={3}
              placeholder="Discuss skills, timing, or meeting details..."
              required
            />
            <div className="chat-compose-footer">
              <span>{content.length}/{MAX_MESSAGE_LENGTH}</span>
              <button
                type="submit"
                className="primary-btn"
                disabled={sending || !content.trim()}
              >
                {sending ? 'Sending...' : 'Send message'}
              </button>
            </div>
          </form>
        </section>
      ) : (
        <div className="explore-empty">
          <p>This chat is unavailable or you are not a participant in the exchange.</p>
        </div>
      )}
    </section>
  )
}

export default ExchangeChat
