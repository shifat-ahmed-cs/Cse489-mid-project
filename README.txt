========================================================================
CSE 489: Mobile Application Development - Mid Project / Lab Exam
Project Name: Smart Geo-Tagged Landmarks
Student Key: 22301582
========================================================================

PROJECT OVERVIEW:
-----------------
Smart Geo-Tagged Landmarks is an Android mobile application designed to browse,
manage, visit, filter, and visualize geo-tagged landmarks across Bangladesh.
The application connects to the faculty-provided REST API endpoint
(https://labs.anontech.info/cse489/exm3/api.php) using student key "22301582".
It incorporates full offline capability, Room database local caching, WorkManager
background polling and synchronization, OpenStreetMap map visualization with
score-based gradient markers, and a 4-tab bottom navigation interface.


FEATURES IMPLEMENTED:
---------------------
1. Map View (OpenStreetMap / osmdroid):
   - Interactive map centered on Bangladesh (Lat: 23.6850, Lon: 90.3563).
   - Dynamic marker rendering for active landmarks with custom score-based colors
     (gradient from Red for lowest score to Green for highest score).
   - Marker tap displays an Info Overlay Card containing Title, Image, Score, Visit Count,
     Avg Distance, and an interactive "Visit Landmark" button.

2. Landmarks List:
   - RecyclerView displaying all active landmarks with images (loaded asynchronously via Glide),
     title, server-computed score, visit count, and average distance.
   - Real-time Search Bar to filter landmarks by title string.
   - Minimum Score Slider filter allowing dynamic threshold filtering.
   - Sorting options: Score (Highest First), Score (Lowest First), Title (A-Z).
   - Action buttons on each landmark item: "Visit" and "Delete" (soft-delete).

3. Visit Feature & Asynchronous Job Status:
   - Captures real-time device GPS coordinates via FusedLocationProviderClient.
   - Submits async visit request (`action=visit_landmark`) receiving a background `job_id`.
   - WorkManager background worker polls `action=get_job_status` until status turns "done".
   - Updates local Room database and UI with calculated visit distance.

4. Activity Screen (Visit History):
   - Comprehensive history log of recent visits stored locally in Room DB.
   - Displays Landmark Name, Timestamp (formatted), Status Chip (DONE, PENDING, QUEUED_OFFLINE, FAILED),
     and exact calculated distance.

5. Add Landmark & Soft Delete / Restore Handling:
   - Form to create new landmarks with Title, Latitude, Longitude, and optional Image file.
   - Auto-fetch GPS button automatically populates current device coordinates.
   - Uses `multipart/form-data` upload (`@Multipart`) as required by server `$_FILES`.
   - Soft-deleted landmarks disappear immediately from active views.
   - Dedicated "Soft-Deleted Landmarks" view allowing instant restoration (`action=restore_landmark`).

6. Mandatory Offline Support & WorkManager Queue:
   - Room local database serves as the single source of truth.
   - Fully accessible offline: displays cached landmarks and offline visit history.
   - When offline, visit requests are queued locally in `pending_visits` table.
   - `VisitSyncWorker` enqueued with NetworkType.CONNECTED constraint automatically drains
     queued offline visits and polls pending jobs as soon as network connectivity is restored.


API USAGE:
----------
Base URL: https://labs.anontech.info/cse489/exm3/api.php
Authentication: `key=22301582` on every request.

1. Get Landmarks:
   GET api.php?action=get_landmarks&key=22301582

2. Visit Landmark:
   POST api.php?action=visit_landmark&key=22301582
   Body (JSON): { "landmark_id": Int, "user_lat": Double, "user_lon": Double }

3. Get Job Status:
   GET api.php?action=get_job_status&key=22301582&job_id=INT

4. Create Landmark:
   POST api.php?action=create_landmark&key=22301582
   Body (multipart/form-data): title, lat, lon, image (file)

5. Soft Delete Landmark:
   POST api.php?action=delete_landmark&key=22301582
   Body (form-urlencoded): id=INT

6. Restore Landmark:
   POST api.php?action=restore_landmark&key=22301582
   Body (form-urlencoded): id=INT


OFFLINE STRATEGY:
-----------------
The app implements a Repository Pattern with Room DB as the Single Source of Truth:
- Fetching: On startup/refresh, data fetched from API updates Room entities (`LandmarkEntity`).
  UI observes Room LiveData, so offline mode seamlessly renders cached data.
- Writing/Visiting Offline: When network is disconnected, visit requests are saved into
  `PendingVisitEntity` and logged in `VisitHistoryEntity` with status `QUEUED_OFFLINE`.
- Synchronization: WorkManager (`VisitSyncWorker`) is enqueued with a network constraint.
  When internet connection is re-established, the worker drains the pending queue, sends requests to the server,
  polls job status until complete, and updates the Room DB to update UI reactively.


ARCHITECTURE USED:
------------------
- Pattern: MVVM (Model-View-ViewModel) + Repository Pattern.
- UI: Single Activity (`MainActivity`) with BottomNavigationView switching between 4 Fragments.
- Database: AndroidX Room Database (`AppDatabase`, `LandmarkDao`).
- Networking: Retrofit 2 + OkHttp 3 + Gson.
- Background Work: AndroidX WorkManager (`VisitSyncWorker`).
- Map Engine: OpenStreetMap Android SDK (`osmdroid-android`).
- Image Loading: Glide 4.


CHALLENGES FACED:
-----------------
1. Async Visit Flow & WorkManager Polling:
   The server processes visits asynchronously and returns pending job IDs rather than immediate distances.
   Implementing a robust background polling loop inside `VisitSyncWorker` with retry/backoff handled both
   polling job completion and offline queue draining without blocking the UI thread.

2. Multipart File Upload:
   Handling optional image file uploads required using `MultipartBody.Part` and `RequestBody` with
   `multipart/form-data` encoding to avoid empty `$_FILES` on the PHP backend.

3. Dynamic Map Marker Color Gradient:
   Mapping arbitrary positive and negative server activity scores to a smooth color gradient required
   calculating min/max score bounds dynamically across active landmarks and interpolating RGB values.
