# AURA Player — Android (Kotlin)

Spotify-style audio player app with **local MP3 playback**, **streaming support**, and
system **notification + lock-screen media controls** (play/pause/next/prev with album art),
built with Jetpack Compose + Media3 (ExoPlayer).

## How to open & run

1. Open this folder (`AuraPlayer/`) directly in **Android Studio** (Hedgehog or newer).
2. Let Gradle sync — Android Studio will auto-generate the wrapper jar if missing.
3. Run on a device or emulator (API 24+). On first launch the app asks for:
   - **Music/Storage permission** → to scan local MP3s
   - **Video permission** → to scan local videos (separate grant, same screen flow)
   - **Notification permission** (Android 13+) → required for the playback notification

## What's included

| Feature | Where |
|---|---|
| Local MP3 scanning (MediaStore) | `data/LocalMusicScanner.kt` |
| Local video scanning (MediaStore) | `data/LocalVideoScanner.kt` |
| Demo streaming catalog (swap for your API) | `data/StreamingCatalog.kt` |
| Combined library + playlists state | `data/MusicRepository.kt` |
| **Spotify-style notification** (auto-built by Media3) | `service/PlaybackService.kt` |
| UI ↔ playback bridge | `service/PlayerController.kt` |
| Bottom mini-player | `ui/components/MiniPlayer.kt` |
| Full "Now Playing" screen (vinyl art) | `ui/player/PlayerScreen.kt` |
| Library tabs (Local / Streaming) | `ui/library/LibraryScreen.kt` |
| **Video library grid + full-screen video player** | `ui/video/` |
| Search | `ui/search/SearchScreen.kt` |
| Playlists (create / add / remove) | `ui/playlist/` |
| Navigation + bottom nav bar | `MainActivity.kt` |

## How the notification works

`PlaybackService` extends `MediaSessionService` and owns an `ExoPlayer` wrapped in a
`MediaSession`. Media3 automatically builds the system notification from that session —
you don't hand-craft a `NotificationCompat` yourself. It gives you, for free:

- Album art, title, artist
- Play / Pause / Skip Next / Skip Previous actions
- Lock-screen + notification-shade controls
- Works even when the app is backgrounded (foreground service)

Every screen (mini player, full player) reads from the **same** `MediaController`,
so the notification and in-app UI always stay in sync.

## Swapping in real streaming

Replace `StreamingCatalog.kt` with real API calls (Retrofit/Ktor) that return `Song`
objects pointing at your stream URLs. Everything else (queue, notification, playlists)
works unchanged since it's driven by the generic `Song` model.

## Notes

- Demo streaming tracks use public royalty-free sample URLs (SoundHelix) just so
  streaming playback is testable immediately — replace with your real source.
- Minimum SDK 24, target/compile SDK 34.
- No analytics, ads, or trackers included.
