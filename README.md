# StreamHive Backend (Cloudflare R2 + Render)

## Steps to Deploy

1. Create a Cloudflare R2 bucket and get:
   - Account ID
   - Access Key ID
   - Secret Access Key
   - Bucket name
   - (Optional) Public base URL

2. Copy `.env.example` to `.env` and fill in your details.

3. Push this folder to GitHub.

4. Deploy on Render.com:
   - New Web Service
   - Build: npm install
   - Start: npm start
   - Add your environment variables.

5. Use the Render URL as your base URL in the Android app.
