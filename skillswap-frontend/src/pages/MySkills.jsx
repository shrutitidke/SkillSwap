import { useEffect, useState } from 'react'
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
        <p className="eyebrow">YOUR SKILL PROFILE</p>
        <h1>My <span>Skills</span></h1>
        <p>Add what you can teach and what you would like to learn.</p>
      </header>

      <section className="my-skills-form-card" aria-labelledby="skill-form-title">
        <h2 id="skill-form-title">
          {editingId === null ? 'Add a skill' : 'Edit skill'}
        </h2>

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
          <h2 id="my-skills-list-title">Your saved skills</h2>
          {!loading && <span>{skills.length}</span>}
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
          <div className="skills-grid">
            {skills.map((skill) => (
              <article className="skill-card my-skill-card" key={skill.id}>
                <div className="skill-card-top">
                  <div className="skill-icon" aria-hidden="true">✦</div>
                  <span className="skill-level">
                    {skill.type === 'TEACH' ? 'I can teach' : 'I want to learn'}
                  </span>
                </div>

                <p className="skill-category">{skill.type}</p>
                <h3>{skill.name}</h3>
                <p className="skill-description">
                  {skill.description || 'No description added.'}
                </p>

                <div className="skill-editor-actions">
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
    </section>
  )
}

export default MySkills
