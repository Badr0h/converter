# ZenithConvert

Welcome to **ZenithConvert** (formerly Converter)

## Overview
ZenithConvert is a powerful codebase and data formatting conversion tool. It utilizes AI to provide robust and intelligent conversions across countless programming languages and formats.

## Deployment Instructions

### Frontend (Vercel)
The frontend application is built using Angular and should be deployed to **Vercel** (`https://zenithconvert.vercel.app/`).

1. Connect your GitHub repository to your Vercel account.
2. Select the `frontend` folder as the Root Directory.
3. Vercel will automatically detect the Angular framework and execute `npm install` and `npm run build`.
4. Ensure the `vercel.json` contains `"name": "zenithconvert"` and proper rewrites.
5. Set any required environment variables (e.g. `NODE_ENV=production`) in Vercel settings.
6. Deploy your application.

### Backend (Render)
The backend service is built using Spring Boot and should be deployed as a Web Service on **Render**.

1. Connect your GitHub repository to your Render account.
2. Create a new Web Service.
3. Configure it to utilize Java/Maven as the build environment.
4. Build Command: `mvn clean package -DskipTests`
5. Start Command: `java -jar target/zenithconvert-0.0.1-SNAPSHOT.jar`
6. Make sure to define all essential environment variables inside your Render deployment settings:
   - Database credentials
   - JWT Secrets
   - OpenAPI/SMTP configurations
   - Vercel Frontend URL for CORS (`https://zenithconvert.vercel.app`)

---
© ZenithConvert. All rights reserved.
