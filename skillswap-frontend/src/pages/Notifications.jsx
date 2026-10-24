import { useCallback, useEffect, useState } from 'react'
import {
  getMyNotifications,
  markNotificationRead,
  NOTIFICATIONS_CHANGE_EVENT,
} from '../services/api'

function formatDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date unavailable'
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function notificationLabel(type) {
  if (type === 'ADMIN_WARNING') return 'Admin warning'
  if (type === 'ADMIN_INSTRUCTION') return 'Admin instruction'
  return type.replaceAll('_', ' ').toLowerCase()
}

async function markUnreadNotificationsAsRead(notifications, onRead) {
  const unread = notifications.filter((notification) => !notification.read)
  const results = await Promise.allSettled(
    unread.map(async (notification) => {
      await markNotificationRead(notification.id)
      onRead(notification.id)
    })
  )
  const failedCount = results.filter(
    (result) => result.status === 'rejected'
  ).length
  window.dispatchEvent(new Event(NOTIFICATIONS_CHANGE_EVENT))
  if (failedCount) {
    throw new Error(
      `Could not mark ${failedCount} notification${failedCount === 1 ? '' : 's'} as read.`
    )
  }
}

function Notifications() {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState(null)
  const [error, setError] = useState('')

  const loadNotifications = useCallback(async (showLoading = true) => {
    if (showLoading) setLoading(true)
    try {
      const data = await getMyNotifications()
      setNotifications(data)
      try {
        await markUnreadNotificationsAsRead(data, (notificationId) => {
          setNotifications((current) =>
            current.map((notification) =>
              notification.id === notificationId
                ? { ...notification, read: true }
                : notification
            )
          )
        })
        setError('')
      } catch (markError) {
        setError(markError.message || 'Unable to mark notifications as read.')
      }
    } catch (loadError) {
      setError(loadError.message || 'Unable to load notifications.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    let inFlight = false

    const refresh = async (showLoading = false) => {
      if (inFlight) return
      inFlight = true
      if (showLoading) setLoading(true)
      try {
        const data = await getMyNotifications()
        if (active) {
          setNotifications(data)
        }
        try {
          await markUnreadNotificationsAsRead(data, (notificationId) => {
            if (active) {
              setNotifications((current) =>
                current.map((notification) =>
                  notification.id === notificationId
                    ? { ...notification, read: true }
                    : notification
                )
              )
            }
          })
          if (active) setError('')
        } catch (markError) {
          if (active) {
            setError(
              markError.message || 'Unable to mark notifications as read.'
            )
          }
        }
      } catch (loadError) {
        if (active) {
          setError(loadError.message || 'Unable to load notifications.')
        }
      } finally {
        inFlight = false
        if (active) setLoading(false)
      }
    }

    const refreshWhenVisible = () => {
      if (document.visibilityState === 'visible') void refresh()
    }

    void refresh(true)
    window.addEventListener('focus', refreshWhenVisible)
    document.addEventListener('visibilitychange', refreshWhenVisible)
    const refreshInterval = window.setInterval(refreshWhenVisible, 30000)

    return () => {
      active = false
      window.clearInterval(refreshInterval)
      window.removeEventListener('focus', refreshWhenVisible)
      document.removeEventListener('visibilitychange', refreshWhenVisible)
    }
  }, [])

  const handleMarkRead = async (notification) => {
    if (busyId !== null) return
    setBusyId(notification.id)
    setError('')
    try {
      await markNotificationRead(notification.id)
      setNotifications((current) =>
        current.map((item) =>
          item.id === notification.id ? { ...item, read: true } : item
        )
      )
    } catch (markError) {
      setError(markError.message || 'Unable to update this notification.')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section className="notifications-page">
      <header className="notifications-header">
        <p className="eyebrow">YOUR UPDATES</p>
        <h1>Notifications</h1>
        <p>Read important updates, instructions, and warnings from SkillSwap.</p>
        <button
          type="button"
          className="secondary-btn"
          onClick={() => loadNotifications()}
          disabled={loading}
        >
          Refresh
        </button>
      </header>
      {error && <p className="request-error" role="alert">{error}</p>}
      {loading ? (
        <p className="notifications-state" role="status">Loading notifications...</p>
      ) : notifications.length ? (
        <div className="notification-list">
          {notifications.map((notification) => (
            <article
              className={`notification-card ${notification.read ? 'read' : 'unread'} ${
                notification.type.startsWith('ADMIN_') ? 'admin-message' : ''
              }`}
              key={notification.id}
            >
              <div className="notification-card-heading">
                <span className="notification-type">
                  {notificationLabel(notification.type)}
                </span>
                <time dateTime={notification.createdAt}>
                  {formatDate(notification.createdAt)}
                </time>
              </div>
              <p>{notification.message}</p>
              {!notification.read && (
                <button
                  type="button"
                  className="secondary-btn notification-read-button"
                  onClick={() => handleMarkRead(notification)}
                  disabled={busyId !== null}
                >
                  {busyId === notification.id ? 'Saving...' : 'Mark as read'}
                </button>
              )}
            </article>
          ))}
        </div>
      ) : (
        <p className="notifications-state">You have no notifications yet.</p>
      )}
    </section>
  )
}

export default Notifications
