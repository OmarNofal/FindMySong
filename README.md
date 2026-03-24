# FindMySong - Audio Recognition App

A mobile application that identifies songs from microphone input using audio fingerprinting techniques.

## Why I Built This

I built FindMySong to explore how audio recognition systems work internally. Applications like Shazam appear like magic from the outside, and I wanted to understand the underlying mechanisms by implementing a simplified version myself.

This project focuses on learning and experimentation, it has no intention of being
a commercial tool.

## How It Works

At a high level, the idea is to turn audio into a compact representation that can be matched efficiently.

Instead of comparing raw audio, the system extracts distinctive patterns from a recording and converts them into a set of fingerprints. These fingerprints capture the structure of the sound in a way that remains stable even if the recording conditions are noisy.

When a new audio sample is recorded, the same process is applied to generate its fingerprints. These are then compared against a database of previously stored fingerprints from known tracks.

If a strong match is found, the corresponding song is returned. Otherwise, the system can reject the input and return no result.

## Demo and Screenshots
![Home screen](screenshots/Home%20Screen.jpg) ![Listening screen](screenshots/Listening%20Screen.jpg)

![](screenshots/Demo.mp4)

## Features

* Easy one-line command for indexing process 
* Real-time audio recognition from phone
* Generates fingerprints from recorded input
* Matches input against a stored database of pre-indexed tracks
* Returns the most likely song match
* Simple and clean mobile interface
* App can do offline recording and retrieve results automatically when internet is available
* Quick matching and pretty high accuracy. It can reliably detect most of my tests in under 5 seconds. If the input is noisy it can take longer.
* The system includes a rejection mechanism that avoids false matches by returning no result when confidence is low or the track is not present in the database.

## Limitations

This project is not intended to compete with production systems.

* It uses a limited personal dataset (800 tracks)
* It can be sensitive to background noise and recording quality
* Matching accuracy varies depending on input conditions
* The indexing process can be compute heavy and take some time

The goal was to understand how the system works and not worry about optimisations,
and large-scale deployment.

## Tech Stack

* Languages: Python, Kotlin
* Audio processing: `numpy`, `audiofile`, `pyfftw` 
* Android Libraries: `Hilt`, `Jetpack Compose`, `Retrofit`, `Gson`, `WorkManager`
* Databases: PostgreSQL
  
## Future Improvements

* Improve robustness against noise
* Explore more advanced fingerprinting techniques
* Test it on a larger scale database (thousands of tracks)
* Create a user interface for the server to ease testing, and indexing  


## How to Use?

First, clone the repo then follow these steps to run the app

### 1. Setting up the server

1. Install PostgreSQL on your system if you don't have it
2. Navigate to the backend folder, go to `database/config.py` and configure your database parameters
3. Start indexing your library. In the backend folder run this command:\
       ```  
       python -m indexing.index_songs _library_dir_ -m MAXIMUM_TRACK_LENGTH_IN_SECONDS -w NUMBER_OF_WORKERS -pt
       ```\
       > Note: The `-m`, `-w` and `-pt` modifiers are optional. `-pt` just makes it output a pretty table to report results  

4. Run the server\
        `uvicorn api.server:app --reload --host 0.0.0.0`
5. Now the server is running and ready to respond to recognition requests

### 2. Running the mobile app

1. Open the `android` folder on Android Studio and install all the dependencies
2. Navigate to the `network/constants.kt` file and edit the `IP` and `PORT` to match your computer's ip address and the server's port (obviously they should be connected to the same local network)
3. Build and run on a device with mic access
4. Start matching on any playing song and it should work
5. To test offline recognition, turn off Wifi and start the recognition process until it saves the recording
6. Close the app, turn on Wifi and you should see it return the results

## Learn more...

If you find this topic interesting, you can check these sources as they provided me with the necessary details to implement this project.

1. [An Industrial-Strength Audio Search Algorithm 
](https://www.ee.columbia.edu/~dpwe/papers/Wang03-shazam.pdf) (The original Shazam paper)
2. https://www.toptal.com/developers/algorithms/shazam-it-music-processing-fingerprinting-and-recognition
3. https://www.cameronmacleod.com/blog/how-does-shazam-work