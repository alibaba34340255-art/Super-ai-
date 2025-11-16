# Super AI

This project consists of an Android application and a Python backend.

## Running the Application

To run the application, you need to start both the backend server and the Android app.

### 1. Start the Backend Server

The backend is a Python Flask application located in the `api` directory.

**Installation:**

First, install the required Python packages:

```bash
pip install -r api/requirements.txt
```

**Running the server:**

Once the dependencies are installed, you can start the server:

```bash
python3 api/app.py
```

The server will start on `0.0.0.0:5000`. Keep this terminal window open.

### 2. Run the Android App

The Android app is located in the `android-app` directory.

**Build and Run:**

1.  Open the `android-app` project in Android Studio.
2.  Let Gradle sync and build the project.
3.  Run the app on an Android emulator or a physical device.

The app will automatically connect to the backend server at `http://10.0.2.2:5000` (the default address for the host machine from the Android emulator).
