# RoadSafe Zimbabwe
### AI-Powered Road Incident Detection and Credible Public Information System

**RoadSafe Zimbabwe** is an Android-based road safety solution designed to improve how road incidents are reported, analysed and communicated. It uses mobile camera technology, AI-assisted incident analysis, GPS location services and cloud integration to support faster and more organised access to road incident information.

## The Problem

Road accidents and other road-related emergencies can go unreported or take time to reach the relevant people. Delayed, incomplete or unverified information makes it difficult for road users and relevant stakeholders to respond effectively.

## Our Solution

RoadSafe Zimbabwe provides a digital platform where users can capture road incidents, attach photographic evidence, record GPS coordinates and submit incident reports. AI-assisted analysis helps identify incident categories, while a centralised system supports the management and verification of reports.

The application is designed with offline functionality so that reports can be captured and stored locally when internet connectivity is unavailable.

## Key Features

- **Incident Reporting:** Capture road incidents using the device camera.
- **AI-Assisted Analysis:** Analyse captured images to support incident classification.
- **GPS Location:** Attach geographical coordinates to reported incidents.
- **Offline-First Reporting:** Save incident reports locally when offline.
- **Cloud Synchronisation:** Upload stored reports when connectivity becomes available.
- **Incident Management:** View and manage saved incident reports.
- **Map Visualisation:** Display incident locations on an interactive map.
- **Verification Workflow:** Support the review and verification of submitted reports.

## Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Kotlin |
| Android UI | Jetpack Compose |
| Camera | CameraX |
| Local Database | Room |
| Background Tasks | WorkManager |
| AI Image Analysis | TensorFlow Lite / LiteRT |
| Maps | Google Maps SDK / Maps Compose |
| Backend | Supabase |
| Database and Cloud Storage | Supabase PostgreSQL and Storage |
| Networking | Ktor |

## Application Architecture

1. A user captures an image of a road incident.
2. The application obtains the available GPS coordinates.
3. AI-assisted analysis processes the captured image.
4. The report is stored locally using Room.
5. When internet connectivity is available, background synchronisation sends the report to Supabase.
6. Incident information can be viewed and managed through the application.

## Installation and Setup

### Requirements
- Android Studio
- Android SDK
- JDK 17
- An Android device or emulator running Android 8.0 (API 26) or later
- Internet access for cloud features

### Steps

1. Clone the repository:

   ```bash
   git clone https://github.com/mggtm/RoadSafeZimbabwe.git
   ```

2. Open the project in Android Studio.

3. Configure your local credentials in the root `local.properties` file:

   ```properties
   SUPABASE_URL=YOUR_SUPABASE_URL
   SUPABASE_PUBLISHABLE_KEY=YOUR_SUPABASE_PUBLISHABLE_KEY
   MAPS_API_KEY=YOUR_RESTRICTED_GOOGLE_MAPS_API_KEY
   ```

4. Sync the project with Gradle.

5. Connect an Android device or start an emulator.

6. Build and run the application.

**Note:** Never commit API keys, private credentials, signing keys or your `local.properties` file to GitHub.

## Project Status

RoadSafe Zimbabwe is a working prototype developed for the **ZMC Media Hackathon 2026**, under the Mobility and Road Safety track.

The prototype demonstrates incident capture, offline storage, GPS-based incident reporting, AI-assisted analysis, map visualisation and cloud integration.

Some capabilities may require further testing and refinement before production deployment.

## Future Improvements

- Improve AI incident classification accuracy.
- Expand real-time incident notifications.
- Introduce stronger report verification and duplicate detection.
- Improve integration with relevant road safety stakeholders.
- Expand analytics and incident trend reporting.

## Project Repository

**GitHub:** https://github.com/mggtm/RoadSafeZimbabwe

**Developed for:** ZMC Media Hackathon 2026  
**Project:** RoadSafe Zimbabwe  
**Track:** Mobility and Road Safety

---

*RoadSafe Zimbabwe — Making road incident information more accessible, organised and actionable.*
