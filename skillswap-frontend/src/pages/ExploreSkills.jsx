
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { discoverSkills } from '../services/api'

function ExploreSkills() {
  const [skills, setSkills] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    async function loadSkills() {
      try {
        setLoading(true)
        setError('')

        const data = await discoverSkills()

        if (!cancelled) {
          setSkills(Array.isArray(data) ? data : [])
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || 'Unable to load skills.')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadSkills()

    return () => {
      cancelled = true
    }
  }, [])

  const filteredSkills = skills.filter((skill) => {
    const searchText = search.trim().toLowerCase()

    return (
      skill.name?.toLowerCase().includes(searchText) ||
      skill.description?.toLowerCase().includes(searchText) ||
      skill.userName?.toLowerCase().includes(searchText)
    )
  })

  return (
    <section className="explore-page">
      <div className="explore-header">
        <p className="eyebrow">EXPLORE THE COMMUNITY</p>

        <h1>
          Discover <span>Skills</span>
        </h1>

        <p>
          Find people who are ready to teach and learn something new.
        </p>
      </div>

      <form
        className="search-bar"
        onSubmit={(event) => event.preventDefault()}
      >
        <input
          type="search"
          placeholder="Search by skill or teacher name..."
          aria-label="Search skills or teachers"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />

        <button className="primary-btn" type="submit">
          Search
        </button>
      </form>

      <div className="skills-results">
        <div className="results-heading">
          <h2>Skills Available to Learn</h2>
          <p>
            {loading
              ? 'Loading skills...'
              : `${filteredSkills.length} skills found`}
          </p>
        </div>

        {loading ? (
          <div className="explore-empty">
            <p>Loading skills from SkillSwap...</p>
          </div>
        ) : error ? (
          <div className="explore-empty">
            <h2>Unable to load skills</h2>
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
        ) : filteredSkills.length > 0 ? (
          <div className="skills-grid">
            {filteredSkills.map((skill) => (
              <article className="skill-card" key={skill.id}>
                <div className="skill-card-top">
                  <div className="skill-icon">📚</div>

                  <span className="skill-level">
                    Ready to teach
                  </span>
                </div>

                <p className="skill-category">
                  {skill.type}
                </p>

                <h3>{skill.name}</h3>

                <p className="skill-description">
                  {skill.description || 'No description provided.'}
                </p>

                <p className="skill-teacher">
                  <strong>Teacher:</strong>{' '}
                  {skill.userName || 'SkillSwap member'}
                </p>
              </article>
            ))}
          </div>
        ) : (
          <div className="explore-empty">
            <div className="empty-icon">⌕</div>

            <h2>
              {skills.length === 0
                ? 'No skills available yet'
                : 'No skills found'}
            </h2>

            <p>
              {skills.length === 0
                ? 'When members add skills they can teach, they will appear here.'
                : 'Try another skill name or teacher name.'}
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
