# NIV Bible (Android)

Native Android app built with Kotlin and Jetpack Compose. The chapter reader uses the official YouVersion Platform REST API for NIV content (Bible version ID **111**).

## Connect real NIV chapter text

1. Register this Android app at the [YouVersion Platform](https://platform.youversion.com/) and obtain an App Key.
2. Confirm your app has access to the NIV translation and has accepted any required license terms. A Bible ID by itself does not grant access to copyrighted translation content.
3. Add the key to your **user-level Gradle properties**, not to a committed repository file. For example, add the following line to `~/.gradle/gradle.properties` on the machine building the app:

   ```properties
   YVP_APP_KEY=your_app_key_here
   ```

   You can alternatively provide `YVP_APP_KEY` as an environment variable when running Gradle.
4. Sync Gradle and rebuild/reinstall the Android app.

The app sends chapter requests to `https://api.youversion.com/v1/bibles/111/passages/{USFM}.{chapter}?format=text` using the `X-YVP-App-Key` header. It shows separate states for an absent key, an invalid key, missing NIV access, network failure, rate limiting, and provider errors.

**Do not commit app keys to Git.** A key packaged in an Android APK can be extracted; follow YouVersion's app-key restrictions, and review their current platform terms and the NIV publisher's permission requirements before public or commercial distribution.

## Features

- A custom 3D-style Bible cover and a native Compose UI
- Browse all 66 books and move between chapters and books
- Live NIV passage loading from YouVersion Platform when app access is configured
- Local bookmarks, last-read chapter, font size and reader theme preferences
- Reading-plan starter cards and book-name search
