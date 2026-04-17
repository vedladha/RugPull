# Ad Integration Setup

## Index

- [Ad Integration Setup](#ad-integration-setup)
  - [Index](#index)
  - [Adding the Video Asset](#adding-the-video-asset)
  - [Updating the Backend Catalog](#updating-the-backend-catalog)
    - [On success](#on-success)

## Adding the Video Asset

The first step to adding a new advertisement is placing the raw video file into the project so the frontend can serve it. Ensure your video is in `.mp4` format for the best browser compatibility.

Navigate to your React frontend's `public` directory. If you do not already have an `ads` folder inside `public`, create one. Move your video file into this directory.

```bash
cd frontend/public/
cp /path/to/your/new-ad-video.mp4 ./ads/
```

Note the exact filename (e.g., `new-ad-video.mp4`), as you will need to reference this precise path when configuring the backend.

## Updating the Backend Catalog

Once the video is accessible to the frontend, you must register the advertisement in the server's authoritative ad catalog. This prevents spoofing and ensures the backend knows exactly how long the video is and how much to reward the user.

Open the `AdController.java` file located in your Spring Boot backend (typically under `src/main/java/.../controller/AdController.java`).

Locate the `adCatalog` list inside the controller. Create a new `Ad` object entry to this list containing your new ad's details.

```java
  private final List<Ad> adCatalog = List.of(
      new Ad(
          "Synapse Data Analytics",
          8,
          5.0f,
          "ads/synapse_ad.mp4"
      ),
      new Ad("My new Ad",
        "A new advertisement",
        10, 
        5.0f,
        "ads/my_ad_path.mp4"
      )
  );
```

**Important:** The `duration` field must exactly match or slightly underestimate the real length of the `.mp4` file. If you set the duration longer than the actual video, users will finish the video early and the server will reject their reward claim. The system gives a leeway of around 2 seconds so if an ad time lands in the .5 seconds just round **down** not up.

### On success

Upon successful integration, the ad will play unskippably from start to finish. Once the video's HTML5 `onEnded` event fires, the frontend will automatically contact the server. The server will validate the session time constraints, and the specified `$RPC` reward amount will be successfully deposited into the user's Hedera wallet.