import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  deleteAdminUser,
  getAdminUsers,
  sendAdminMessage,
} from '../services/api'

function formatDate(value) {
  if (!value) return 'Unknown'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Unknown'
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date)
}

function AdminUsers() {
  const [users, setUsers] = useState([])
  const [search, setSearch] = useState('')
  const [messageType, setMessageType] = useState('ADMIN_INSTRUCTION')
  const [recipientId, setRecipientId] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)
  const [deletingId, setDeletingId] = useState(null)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const fetchUsers = useCallback(() => getAdminUsers(), [])

  useEffect(() => {
    let active = true
    fetchUsers()
      .then((data) => {
        if (active) setUsers(data)
      })
      .catch((loadError) => {
        if (active) {
          setError(loadError.message || 'Unable to load registered users.')
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [fetchUsers])

  const loadUsers = async () => {
    setLoading(true)
    setError('')
    try {
      setUsers(await fetchUsers())
    } catch (loadError) {
      setError(loadError.message || 'Unable to load registered users.')
    } finally {
      setLoading(false)
    }
  }

  const stats = useMemo(() => {
    const members = users.filter((user) => user.role === 'USER')
    return {
      members: members.length,
      teach: members.reduce(
        (total, user) =>
          total + user.skills.filter((skill) => skill.type === 'TEACH').length,
        0
      ),
      learn: members.reduce(
        (total, user) =>
          total + user.skills.filter((skill) => skill.type === 'LEARN').length,
        0
      ),
    }
  }, [users])

  const filteredUsers = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return users
    return users.filter((user) => {
      const searchableText = [
        user.name,
        user.email,
        ...user.skills.map((skill) => skill.name),
      ]
        .join(' ')
        .toLowerCase()
      return searchableText.includes(query)
    })
  }, [users, search])

  const handleDelete = async (user) => {
    if (deletingId !== null || user.role !== 'USER') return
    const confirmed = window.confirm(
      `Permanently delete ${user.name} (${user.email}) and all of their skills, exchange requests, sessions, chats, reviews, notifications, and reset data? This cannot be undone.`
    )
    if (!confirmed) return

    setDeletingId(user.id)
    setError('')
    setNotice('')
    try {
      await deleteAdminUser(user.id)
      setUsers((current) => current.filter((item) => item.id !== user.id))
      setNotice(`${user.name} and their related records were deleted.`)
    } catch (deleteError) {
      setError(deleteError.message || 'Unable to delete this user.')
    } finally {
      setDeletingId(null)
    }
  }

  const handleSendMessage = async (event) => {
    event.preventDefault()
    if (sending) return
    setSending(true)
    setError('')
    setNotice('')
    try {
      const response = await sendAdminMessage({
        type: messageType,
        userId: recipientId ? Number(recipientId) : null,
        message: message.trim(),
      })
      setNotice(response.message || 'Admin message sent.')
      setMessageType('ADMIN_INSTRUCTION')
      setRecipientId('')
      setMessage('')
    } catch (sendError) {
      setError(sendError.message || 'Unable to send the admin message.')
    } finally {
      setSending(false)
    }
  }

  const handleResetMessage = () => {
    if (sending) return
    setMessageType('ADMIN_INSTRUCTION')
    setRecipientId('')
    setMessage('')
    setError('')
    setNotice('')
  }

  return (
    <section className="admin-page">
      <header className="admin-page-header">
        <div>
          <p className="eyebrow">ADMINISTRATION</p>
          <h1>Member directory</h1>
          <p>Review the community and the skills members want to share.</p>
        </div>
        <button
          type="button"
          className="secondary-btn admin-refresh"
          onClick={loadUsers}
          disabled={loading}
        >
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </header>

      {error && <p className="request-error" role="alert">{error}</p>}
      {notice && <p className="request-notice" role="status">{notice}</p>}

      <div className="admin-stats">
        <article className="admin-stat-card">
          <span className="admin-stat-icon members" aria-hidden="true">◎</span>
          <div><strong>{loading ? '—' : stats.members}</strong><span>Members</span></div>
        </article>
        <article className="admin-stat-card">
          <span className="admin-stat-icon teach" aria-hidden="true">↗</span>
          <div><strong>{loading ? '—' : stats.teach}</strong><span>Skills to teach</span></div>
        </article>
        <article className="admin-stat-card">
          <span className="admin-stat-icon learn" aria-hidden="true">✦</span>
          <div><strong>{loading ? '—' : stats.learn}</strong><span>Skills to learn</span></div>
        </article>
      </div>

      <section
        className="admin-message-composer"
        id="admin-messages"
        aria-labelledby="admin-messages-title"
      >
        <div>
          <p className="eyebrow">COMMUNITY COMMUNICATION</p>
          <h2 id="admin-messages-title">Send a warning or instruction</h2>
          <p>
            Messages appear in each recipient’s notifications. Leave the
            recipient set to everyone to broadcast to all regular users.
          </p>
        </div>
        <form onSubmit={handleSendMessage}>
          <label>
            Message type
            <select
              value={messageType}
              onChange={(event) => setMessageType(event.target.value)}
            >
              <option value="ADMIN_INSTRUCTION">Instruction</option>
              <option value="ADMIN_WARNING">Warning</option>
            </select>
          </label>
          <label>
            Recipient
            <select
              value={recipientId}
              onChange={(event) => setRecipientId(event.target.value)}
            >
              <option value="">All regular users</option>
              {users
                .filter((user) => user.role === 'USER')
                .map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.name} ({user.email})
                  </option>
                ))}
            </select>
          </label>
          <label className="admin-message-field">
            Message (maximum 500 characters)
            <textarea
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              maxLength={500}
              rows={4}
              required
            />
          </label>
          <button
            type="button"
            className="primary-btn"
            onClick={handleSendMessage}
            disabled={sending || loading || !message.trim()}
          >
            {sending ? 'Sending...' : recipientId ? 'Send to user' : 'Send to all users'}
          </button>
          <button
            type="button"
            className="secondary-btn"
            onClick={handleResetMessage}
            disabled={sending}
          >
            Reset message
          </button>
        </form>
      </section>

      <section className="admin-directory" aria-labelledby="admin-directory-title">
        <div className="admin-directory-heading">
          <div>
            <p className="eyebrow">COMMUNITY</p>
            <h2 id="admin-directory-title">Registered accounts</h2>
          </div>
          <label className="admin-search">
            <span className="sr-only">Search members or skills</span>
            <input
              type="search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search name, email, or skill"
            />
          </label>
        </div>

        {loading ? (
          <p className="admin-state" role="status">Loading registered users...</p>
        ) : filteredUsers.length ? (
          <div className="admin-user-list">
            {filteredUsers.map((user) => (
              <article className="admin-user-card" key={user.id}>
                <div className="admin-user-heading">
                  <div>
                    <h3>{user.name}</h3>
                    <p>{user.email}</p>
                  </div>
                  <div className="admin-user-meta">
                    <span className={`admin-role-badge ${user.role.toLowerCase()}`}>
                      {user.role}
                    </span>
                    <span className="admin-joined">Joined {formatDate(user.createdAt)}</span>
                  </div>
                </div>

                <div className="admin-user-skills">
                  {['TEACH', 'LEARN'].map((type) => {
                    const skills = user.skills.filter((skill) => skill.type === type)
                    return (
                      <section className={`admin-skill-group ${type.toLowerCase()}`} key={type}>
                        <h4>{type === 'TEACH' ? 'Can teach' : 'Wants to learn'}</h4>
                        {skills.length ? (
                          <ul>
                            {skills.map((skill) => (
                              <li key={skill.id}>
                                <strong>{skill.name}</strong>
                                {skill.description && <span>{skill.description}</span>}
                              </li>
                            ))}
                          </ul>
                        ) : (
                          <p className="admin-no-skills">No skills added</p>
                        )}
                      </section>
                    )
                  })}
                </div>

                {user.role === 'USER' && (
                  <div className="admin-user-actions">
                    <button
                      type="button"
                      className="admin-delete-button"
                      onClick={() => handleDelete(user)}
                      disabled={deletingId !== null}
                    >
                      {deletingId === user.id ? 'Deleting account...' : 'Delete user'}
                    </button>
                  </div>
                )}
              </article>
            ))}
          </div>
        ) : (
          <p className="admin-state">
            {users.length ? 'No members match that search.' : 'No accounts have been registered yet.'}
          </p>
        )}
      </section>
    </section>
  )
}

export default AdminUsers
