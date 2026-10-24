
import { Link } from 'react-router-dom'
import useAuth from '../auth/useAuth'
import AdminHome from './AdminHome'

const featuredSkills = [
  {
    icon: '💻',
    title: 'Technology',
    description: 'Explore Java, web development, and programming.',
  },
  {
    icon: '🎨',
    title: 'Design',
    description: 'Discover UI/UX, graphic design, and creativity.',
  },
  {
    icon: '📸',
    title: 'Creative Arts',
    description: 'Learn photography and other creative skills.',
  },
  {
    icon: '🎤',
    title: 'Communication',
    description: 'Build confidence and improve speaking skills.',
  },
]

const steps = [
  {
    number: '01',
    title: 'Explore Skills',
    description: 'Discover skills you want to learn from the community.',
  },
  {
    number: '02',
    title: 'Connect with People',
    description: 'Find people who can share their knowledge with you.',
  },
  {
    number: '03',
    title: 'Learn and Share',
    description: 'Exchange knowledge and grow together.',
  },
]

function Home() {
  const { user } = useAuth()

  if (user?.role === 'ADMIN') {
    return <AdminHome />
  }

  return (
    <div className="home-page">
      <section className="hero" id="home">
        <div className="hero-content">
          <p className="eyebrow">LEARN · SHARE · GROW</p>

          <h1>
            Share What You Know.
            <br />
            <span>Learn What You Love.</span>
          </h1>

          <p className="hero-description">
            Connect with people, exchange skills, and learn something new.
            Your next skill starts with a simple connection.
          </p>

          <div className="hero-buttons">
            <Link to="/skills" className="primary-btn">
              Explore Skills →
            </Link>

            <Link to="/register" className="secondary-btn">
              Join SkillSwap
            </Link>
          </div>

          <p className="hero-note">
            A community where everyone has something to teach.
          </p>
        </div>

        <div className="hero-visual">
          <div className="visual-card">
            <div className="visual-icon">✦</div>

            <p className="home-card-label">WELCOME TO SKILLSWAP</p>

            <h2>Everyone has a skill to share.</h2>

            <p>Find your people. Learn together.</p>

            <div className="skill-tags">
              <span>Java</span>
              <span>Design</span>
              <span>Photography</span>
              <span>Communication</span>
            </div>
          </div>
        </div>
      </section>

      <section className="home-stats">
        <div>
          <h3>Learn</h3>
          <p>Discover something new</p>
        </div>

        <div>
          <h3>Share</h3>
          <p>Teach what you know</p>
        </div>

        <div>
          <h3>Grow</h3>
          <p>Improve together</p>
        </div>
      </section>

      <section className="how-it-works">
        <div className="home-section-heading">
          <p className="eyebrow">YOUR LEARNING JOURNEY</p>
          <h2>How SkillSwap Works</h2>
          <p>
            Getting started is simple. Discover, connect, and learn.
          </p>
        </div>

        <div className="steps-grid">
          {steps.map((step) => (
            <article className="step-card" key={step.number}>
              <span className="step-number">{step.number}</span>
              <h3>{step.title}</h3>
              <p>{step.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="featured-skills">
        <div className="home-section-heading">
          <p className="eyebrow">FIND YOUR NEXT SKILL</p>
          <h2>Explore What You Can Learn</h2>
          <p>
            From technology to creativity, there is always something new to
            discover.
          </p>
        </div>

        <div className="featured-skills-grid">
          {featuredSkills.map((skill) => (
            <article className="featured-skill-card" key={skill.title}>
              <div className="featured-skill-icon">{skill.icon}</div>
              <h3>{skill.title}</h3>
              <p>{skill.description}</p>

              <Link to="/skills" className="featured-skill-link">
                Explore skills →
              </Link>
            </article>
          ))}
        </div>
      </section>

      <section className="home-cta">
        <p className="eyebrow">YOUR NEXT CHAPTER STARTS HERE</p>
        <h2>Ready to learn something new?</h2>
        <p>
          Join SkillSwap and turn what you know into an opportunity to grow
          with others.
        </p>

        <div className="hero-buttons">
          <Link to="/register" className="primary-btn">
            Get Started →
          </Link>

          <Link to="/skills" className="secondary-btn">
            Browse Skills
          </Link>
        </div>
      </section>
    </div>
  )
}

export default Home
