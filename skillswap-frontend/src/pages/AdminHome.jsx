import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAdminUsers } from '../services/api'

function AdminHome() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')

  const loadStats = useCallback(() => getAdminUsers(), [])

  useEffect(() => {
    let active = true
    loadStats()
      .then((users) => {
        if (!active) return
        const members = users.filter((user) => user.role === 'USER')
        setStats({
          members: members.length,
          skills: members.reduce((sum, user) => sum + user.skills.length, 0),
          teach: members.reduce(
            (sum, user) =>
              sum + user.skills.filter((skill) => skill.type === 'TEACH').length,
            0
          ),
          learn: members.reduce(
            (sum, user) =>
              sum + user.skills.filter((skill) => skill.type === 'LEARN').length,
            0
          ),
        })
      })
      .catch((loadError) => {
        if (active) {
          setError(loadError.message || 'Unable to load community overview.')
        }
      })
    return () => {
      active = false
    }
  }, [loadStats])

  return (
    <section className="admin-home-page">
      <header className="admin-home-hero">
        <p className="eyebrow">SKILLSWAP ADMIN</p>
        <h1>Community overview</h1>
        <p>
          Welcome to the admin workspace. Review members and skills, share
          important guidance, and help keep SkillSwap safe and welcoming.
        </p>
        <div className="admin-home-actions">
          <Link to="/admin" className="primary-btn">Open member directory</Link>
          <Link to="/admin#admin-messages" className="secondary-btn">Send a message</Link>
        </div>
      </header>

      {error && <p className="request-error" role="alert">{error}</p>}
      <section className="admin-home-stats" aria-label="Community statistics">
        {[
          ['Registered members', stats?.members],
          ['Skills listed', stats?.skills],
          ['Skills members teach', stats?.teach],
          ['Skills members want to learn', stats?.learn],
        ].map(([label, value]) => (
          <article className="admin-home-stat" key={label}>
            <strong>{value ?? '—'}</strong>
            <span>{label}</span>
          </article>
        ))}
      </section>

      <section className="admin-home-guidance">
        <div className="admin-guidance-heading">
          <p className="eyebrow">ADMIN GUIDE</p>
          <h2>Helpful actions</h2>
          <p>Use the tools below to support members and manage the community.</p>
        </div>
        <div className="admin-guidance-grid">
          <article>
            <span aria-hidden="true">◎</span>
            <h3>Review member skills</h3>
            <p>Search by name, email, or skill and see what each person teaches and wants to learn.</p>
            <Link to="/admin">Go to member directory →</Link>
          </article>
          <article>
            <span aria-hidden="true">✉</span>
            <h3>Share guidance</h3>
            <p>Send an instruction or warning to one member or broadcast it to all regular users.</p>
            <Link to="/admin#admin-messages">Open message tools →</Link>
          </article>
          <article>
            <span aria-hidden="true">✓</span>
            <h3>Use deletion carefully</h3>
            <p>Account deletion also removes the selected user’s related exchanges, sessions, chats, and reviews.</p>
            <Link to="/admin">Manage accounts →</Link>
          </article>
        </div>
      </section>
    </section>
  )
}

export default AdminHome
