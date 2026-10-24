# SkillSwap Frontend

The SkillSwap frontend is a React application built with Vite.

## Run locally

```bash
npm install
npm run dev
```

The development server runs at `http://localhost:5173`. The backend API
defaults to `http://localhost:8080/api`; set `VITE_API_BASE_URL` if the API is
hosted elsewhere.

## Main user features

- Registration, login, JWT-authenticated routes, and logout.
- Home, dashboard, member skill discovery, reciprocal matches, and skill
  exchange requests.
- Class session proposals, confirmations, cancellation before the scheduled
  start, and meeting links available only until the session ends.
- Notifications with an unread count. Opening Notifications marks unread
  messages as read.
- Course ratings: after an accepted exchange reaches 10 days and its latest
  confirmed session has ended, the learner must rate the teacher before
  accessing other app features.

## Admin features

Administrators have a separate home page and member directory. The directory
shows registered members and their TEACH/LEARN skills, supports search, and
allows administrators to delete regular user accounts. Admins can also send
warnings or instructions to an individual member or all regular members.

## Checks

```bash
npm run lint
npm run build
```
