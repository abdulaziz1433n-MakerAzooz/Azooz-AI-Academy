# Azooz AI Academy 🎓

Android app for learning programming languages with an online Arabic-language AI tutor.

## Features
- Kotlin native Android app
- HTML, CSS, JavaScript, Python, Kotlin, Swift, Luau, Java, C++, Dart, SQL and more
- Beginner, intermediate, and advanced levels
- Interactive AI lessons, exercises, and quizzes
- Local storage of selected options and last response

## Build APK
The GitHub Actions workflow **Build Android APK** compiles a debug APK on pushes to main and on manual dispatch. Download its artifact from Actions after a successful build.

## Configure AI backend
1. Deploy `backend/worker.js` to Cloudflare Workers (using `backend/wrangler.toml` if desired).
2. Add `OPENAI_API_KEY` as a **secret** in your Worker configuration, never in Android or the GitHub repo.
3. Optionally set `OPENAI_MODEL` to a compatible model available to your API project.
4. Enter the Worker HTTPS URL ending in `/chat` in the Android app.

**Important:** The sample backend does **not** include authentication or per-user rate limits. Before publishing the Worker URL or distributing the app, add authentication, abuse prevention, rate limits and spend caps to avoid unexpected charges. Do not put an API key in the app. OpenAI API use may incur costs.

The initial version evaluates text with AI; it does not run or compile submitted code. It does not yet preserve full chat history. The debug APK is for testing only.
