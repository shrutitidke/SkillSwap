
import { Link } from 'react-router-dom'

const termsSections = [
  {
    number: '01',
    title: 'Account Registration',
    description:
      'Provide accurate and complete information when creating your SkillSwap account. You are responsible for keeping your password secure and protecting access to your account.',
  },
  {
    number: '02',
    title: 'Skill Sharing and Learning',
    description:
      'Use SkillSwap to share genuine knowledge, learn new skills, and connect respectfully with other members. Do not share misleading, harmful, or inappropriate content.',
  },
  {
    number: '03',
    title: 'User Responsibilities',
    description:
      'Treat other members with respect. Do not harass users, misuse their personal information, or use the platform for unlawful activities.',
  },
  {
    number: '04',
    title: 'Account Security',
    description:
      'Keep your login credentials confidential. If you suspect unauthorized access to your account, take appropriate steps to secure it and contact the platform administrator.',
  },
  {
    number: '05',
    title: 'Platform Usage',
    description:
      'Use SkillSwap for its intended purpose of exchanging skills and knowledge. Do not attempt to disrupt the platform or access accounts and information without permission.',
  },
  {
    number: '06',
    title: 'Changes to These Terms',
    description:
      'These terms may be updated as SkillSwap evolves. Updated terms will be published on this page, and users should review them periodically.',
  },
]

function Terms() {
  return (
    <section className="terms-page">
      <div className="terms-container">
        <Link to="/" className="logo terms-logo">
          Skill<span>Swap</span>
        </Link>

        <header className="terms-hero">
          <div className="terms-hero-icon">✦</div>

          <p className="terms-eyebrow">THE GUIDELINES</p>

          <h1>
            Terms & <span>Conditions</span>
          </h1>

          <p className="terms-hero-description">
            A few simple guidelines to help us build a respectful,
            safe, and supportive learning community.
          </p>

          <div className="terms-welcome">
            <span className="terms-welcome-icon">♡</span>
            <div>
              <h3>Welcome to SkillSwap!</h3>
              <p>
                By creating an account or using the platform, you agree
                to follow these guidelines.
              </p>
            </div>
          </div>
        </header>

        <div className="terms-content">
          <div className="terms-content-heading">
            <div>
              <p className="terms-eyebrow">PLEASE READ CAREFULLY</p>
              <h2>Our community guidelines</h2>
            </div>

            <span className="terms-badge">6 sections</span>
          </div>

          <div className="terms-sections">
            {termsSections.map((section) => (
              <article className="terms-section" key={section.number}>
                <div className="terms-section-number">
                  {section.number}
                </div>

                <div className="terms-section-text">
                  <h3>{section.title}</h3>
                  <p>{section.description}</p>
                </div>
              </article>
            ))}
          </div>

          <div className="terms-footer-note">
            <span>✦</span>
            <p>
              Learn something new. Share what you know. Help everyone grow.
            </p>
          </div>
        </div>

        <div className="terms-bottom">
          <p>Have a SkillSwap account already?</p>

          <Link to="/register" className="primary-btn terms-back-btn">
            Back to Registration →
          </Link>

          <Link to="/login" className="terms-login-link">
            Already registered? Log in
          </Link>
        </div>
      </div>
    </section>
  )
}

export default Terms
