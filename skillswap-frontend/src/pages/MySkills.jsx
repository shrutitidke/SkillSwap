import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  createSkill,
  deleteSkill,
  getMySkills,
  updateSkill,
} from '../services/api'

const EMPTY_FORM = {
  name: '',
  type: 'TEACH',
  description: '',
}

function MySkills() {
  const [skills, setSkills] = useState([])
  const [form, setForm] = useState(EMPTY_FORM)
  const [editingId, setEditingId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [deletingId, setDeletingId] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const teachCount = skills.filter((skill) => skill.type === 'TEACH').length
  const learnCount = skills.filter((skill) => skill.type === 'LEARN').length

  const loadSkills = async () => {
    setLoading(true)
    setError('')

    try {
      const savedSkills = await getMySkills()
      setSkills(savedSkills)
    } catch (loadError) {
      setError(loadError.message || 'Unable to load your skills.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true

    async function loadInitialSkills() {
      try {
        const savedSkills = await getMySkills()
        if (active) {
          setSkills(savedSkills)
        }
      } catch (loadError) {
        if (active) {
          setError(loadError.message || 'Unable to load your skills.')
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    loadInitialSkills()
    return () => {
      active = false
    }
  }, [])

  const handleFieldChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const clearForm = () => {
    setForm(EMPTY_FORM)
    setEditingId(null)
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (saving) return

    const skill = {
      name: form.name.trim().replace(/\s+/g, ' '),
      type: form.type,
      description: form.description.trim(),
    }

    if (!skill.name) {
      setError('Enter a skill name.')
      return
    }

    setSaving(true)
    setError('')
    setNotice('')

    try {
      if (editingId === null) {
        await createSkill(skill)
        setNotice('Skill added.')
      } else {
        await updateSkill(editingId, skill)
        setNotice('Skill updated.')
      }

      clearForm()
      await loadSkills()
    } catch (saveError) {
      setError(saveError.message || 'Unable to save this skill.')
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = (skill) => {
    setEditingId(skill.id)
    setForm({
      name: skill.name || '',
      type: skill.type || 'TEACH',
      description: skill.description || '',
    })
    setError('')
    setNotice('')
  }

  const handleDelete = async (skill) => {
    if (deletingId !== null) return
    if (!window.confirm(`Delete "${skill.name}" from your skills?`)) return

    setDeletingId(skill.id)
    setError('')
    setNotice('')

    try {
      await deleteSkill(skill.id)
      setSkills((current) => current.filter((item) => item.id !== skill.id))
      if (editingId === skill.id) {
        clearForm()
      }
      setNotice('Skill deleted.')
    } catch (deleteError) {
      setError(deleteError.message || 'Unable to delete this skill.')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <section className="my-skills-page">
      <header className="my-skills-header">
        <div>
          <p className="eyebrow">YOUR SKILL PROFILE</p>
          <h1>My <span>Skills</span></h1>
          <p>Share what you know and discover what you can learn next.</p>
        </div>
        <Link to="/skills" className="my-skills-discover-link">
          Find a skill match <span aria-hidden="true">→</span>
        </Link>
      </header>

      <div className="my-skills-summary" aria-label="Skill totals">
        <article className="my-skills-summary-card teach">
          <span className="my-skills-summary-icon" aria-hidden="true">↗</span>
          <div>
            <strong>{loading ? '—' : teachCount}</strong>
            <span>{teachCount === 1 ? 'Skill to teach' : 'Skills to teach'}</span>
          </div>
        </article>
        <article className="my-skills-summary-card learn">
          <span className="my-skills-summary-icon" aria-hidden="true">✦</span>
          <div>
            <strong>{loading ? '—' : learnCount}</strong>
            <span>{learnCount === 1 ? 'Skill to learn' : 'Skills to learn'}</span>
          </div>
        </article>
      </div>

      {error && (
        <p className="my-skills-message error" role="alert">
          {error}
        </p>
      )}
      {notice && (
        <p className="my-skills-message success" role="status">
          {notice}
        </p>
      )}

      <div className="my-skills-workspace">
        <section className="my-skills-form-card" aria-labelledby="skill-form-title">
          <div className="my-skills-form-heading">
            <span className="my-skills-form-icon" aria-hidden="true">
              {editingId === null ? '+' : '✎'}
            </span>
            <div>
              <p className="eyebrow">{editingId === null ? 'GROW YOUR PROFILE' : 'UPDATE YOUR PROFILE'}</p>
              <h2 id="skill-form-title">
                {editingId === null ? 'Add a skill' : 'Edit skill'}
              </h2>
            </div>
          </div>

          <form className="skill-editor" onSubmit={handleSubmit}>
            <div className="skill-editor-field">
              <label htmlFor="skill-name">Skill name</label>
              <input
                id="skill-name"
                name="name"
                type="text"
                value={form.name}
                onChange={handleFieldChange}
                placeholder="For example, JavaScript"
                maxLength={100}
                required
              />
            </div>

            <div className="skill-editor-field">
              <label htmlFor="skill-type">I want to</label>
              <select
                id="skill-type"
                name="type"
                value={form.type}
                onChange={handleFieldChange}
                required
              >
                <option value="TEACH">Teach this skill</option>
                <option value="LEARN">Learn this skill</option>
              </select>
            </div>

            <div className="skill-editor-field">
              <label htmlFor="skill-description">Description (optional)</label>
              <textarea
                id="skill-description"
                name="description"
                value={form.description}
                onChange={handleFieldChange}
                placeholder="Add a little detail about your experience or goals."
                maxLength={1000}
                rows={3}
              />
            </div>

            <div className="skill-editor-actions">
              <button
                type="submit"
                className="primary-btn"
                disabled={saving}
              >
                {saving
                  ? 'Saving...'
                  : editingId === null
                    ? 'Add skill'
                    : 'Save changes'}
              </button>
              {editingId !== null && (
                <button
                  type="button"
                  className="secondary-btn"
                  onClick={clearForm}
                  disabled={saving}
                >
                  Cancel edit
                </button>
              )}
            </div>
          </form>
        </section>

        <section className="my-skills-list" aria-labelledby="my-skills-list-title">
          <div className="my-skills-list-heading">
            <div>
              <p className="eyebrow">YOUR COLLECTION</p>
              <h2 id="my-skills-list-title">Saved skills</h2>
            </div>
            {!loading && (
              <span className="my-skills-total">
                {skills.length} {skills.length === 1 ? 'skill' : 'skills'}
              </span>
            )}
          </div>

          {loading ? (
            <p className="my-skills-state" role="status">Loading your skills...</p>
          ) : error && skills.length === 0 ? (
            <div className="my-skills-state">
              <p>We couldn’t load your skills.</p>
              <button
                type="button"
                className="secondary-btn"
                onClick={loadSkills}
              >
                Try again
              </button>
            </div>
          ) : skills.length === 0 ? (
            <div className="my-skills-state">
              <h3>No skills added yet</h3>
              <p>Add a TEACH or LEARN skill above to get started.</p>
            </div>
          ) : (
            <div className="my-skills-grid">
              {skills.map((skill) => (
                <article
                  className={`my-skill-card ${skill.type === 'TEACH' ? 'teach' : 'learn'}`}
                  key={skill.id}
                >
                  <div className="skill-card-top">
                    <div className="my-skill-card-icon" aria-hidden="true">
                      {skill.type === 'TEACH' ? '↗' : '✦'}
                    </div>
                    <span className="my-skill-type">
                      {skill.type === 'TEACH' ? 'TEACH' : 'LEARN'}
                    </span>
                  </div>

                  <h3>{skill.name}</h3>
                  <p className="my-skill-description">
                    {skill.description || 'No description added.'}
                  </p>

                  <div className="my-skill-actions">
                    <button
                      type="button"
                      className="secondary-btn"
                      onClick={() => handleEdit(skill)}
                      disabled={deletingId !== null || saving}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="skill-delete-button"
                      onClick={() => handleDelete(skill)}
                      disabled={deletingId !== null || saving}
                    >
                      {deletingId === skill.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </section>
  )
}

export default MySkills
