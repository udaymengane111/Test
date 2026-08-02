# Pathshala — Primary School Attendance

Attendance management web app for Indian primary schools (Classes I–V).

## Features

- **Dashboard** — today's class-wise marking status at a glance
- **Mark attendance** — roll call with Present / Absent / Late / Half Day / On Leave
- **Students** — add, edit, search; parent mobile for follow-up
- **Classes & school** — standards I–V, sections, class teachers, school profile
- **Reports** — daily register, monthly summary, students below 75% attendance

Demo data is seeded for **Shri Saraswati Vidyalaya** (Navi Mumbai) and stored in the browser via `localStorage`.

## Run locally

```bash
npm install
npm run dev
```

Build for production:

```bash
npm run build
npm run preview
```

## Stack

Vite · React · TypeScript · React Router · date-fns
