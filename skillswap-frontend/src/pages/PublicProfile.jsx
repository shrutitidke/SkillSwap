import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getPublicUserProfile } from '../services/api'

function formatReviewDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date unavailable'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
  }).format(date)
}

function PublicProfile() {
  const { userId } = useParams()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    async function loadProfile() {
      try {
        const data = await getPublicUserProfile(userId)
        if (active) setProfile(data)
      } catch (loadError) {
        if (active) {
          setError(loadError.message || 'Unable to load this profile.')
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    loadProfile()
    return () => {
      active = false
    }
  }, [userId])

  if (loading) {
    return <section className="public-profile-page" role="status">Loading profile...</section>
  }

  if (error || !profile) {
    return (
      <section className="public-profile-page">
        <p className="request-error" role="alert">
          {error || 'Public profile not found.'}
        </p>
        <Link to="/skills">Explore reciprocal matches</Link>
      </section>
    )
  }

  return (
    <section className="public-profile-page">
      <header className="public-profile-header">
        <p className="eyebrow">SKILLSWAP COMMUNITY</p>
        <h1>{profile.name}</h1>
        <p className="public-profile-rating">
          {profile.reviewCount
            ? `${profile.averageRating.toFixed(1)} out of 5 · ${profile.reviewCount} ${
                profile.reviewCount === 1 ? 'review' : 'reviews'
              }`
            : 'No reviews yet'}
        </p>
      </header>

      <section className="public-profile-reviews" aria-labelledby="profile-reviews-title">
        <h2 id="profile-reviews-title">Reviews</h2>
        {profile.reviews.length ? (
          <div className="public-profile-review-list">
            {profile.reviews.map((review) => (
              <article className="public-profile-review" key={review.id}>
                <div className="public-profile-review-heading">
                  <strong>{review.reviewerName}</strong>
                  <time dateTime={review.createdAt}>
                    {formatReviewDate(review.createdAt)}
                  </time>
                </div>
                <p
                  className="public-profile-stars"
                  aria-label={`${review.rating} out of 5 stars`}
                >
                  {'★'.repeat(review.rating)}
                  {'☆'.repeat(5 - review.rating)}
                </p>
                {review.comment && <p>{review.comment}</p>}
              </article>
            ))}
          </div>
        ) : (
          <p className="public-profile-empty">This member has not received reviews yet.</p>
        )}
      </section>
    </section>
  )
}

export default PublicProfile
