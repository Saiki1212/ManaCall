# ManaCall

ManaCall is a photo-based calling app for Android 8.0 and up. It uses your phone's existing Contacts app for names, photos, favorites, and numbers.

## Using ManaCall

1. Install the APK from [GitHub Releases](https://github.com/Saiki1212/ManaCall/releases), open ManaCall, and allow Contacts access.
2. Use the three large icons at the bottom.
   - **Star** shows favorite contacts. Tap the star on a contact screen to add or remove them in ManaCall. Android Contacts stars appear here by default.
   - **Photo** shows people who have a contact photo. This is the starting tab.
   - **People** shows everyone in the phone’s alphabetical contact order.
3. Use the magnifying glass field to search all saved names. Exact and partial names appear first, followed by close spellings. Search works on the phone.
4. Tap a photo, then choose the handset, WhatsApp, or camera icon. For a contact with several numbers, select the number first.
5. The handset starts a normal call after you allow Phone permission. Android may still ask which SIM to use. The WhatsApp and camera icons start a call when WhatsApp exposes a matching voice or video action through Android Contacts. This route also needs Phone permission. If the call action is unavailable, ManaCall opens the contact’s WhatsApp chat so you can call from there.

The language control switches between English and Telugu. Names stay as saved in Contacts. Edits made in Android Contacts appear when you return to ManaCall, and the app also refreshes if Contacts changes while it is open. ManaCall stores your language, permission prompt state, and favorite choices locally. It reads contact details from Android Contacts.

Open [the interactive preview](preview/index.html) from the cloned folder in a browser. Its photos and calls are samples.

## Build

Open this folder in Android Studio or use JDK 17 and the Android SDK command-line tools with Android SDK 36 and Build Tools 35.0.0. Set the SDK path with `ANDROID_HOME` or with `sdk.dir` in an untracked `local.properties`.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Gradle writes the signed test APK to `app/build/outputs/apk/debug/app-debug.apk`. Use the same signing key for later APKs if they should install as updates on the same phone.

The project has one app module. The `contacts` package reads and observes Android Contacts, `calling` opens the Phone and WhatsApp apps, and `ui` contains the Compose screens. Localized text lives in `values` and `values-te`.

## On-device test checklist

- Install the APK and grant Contacts access. Try denying it first and verify that the permission screen offers a way to continue.
- Use Android Contacts to create sample contacts with and without photos, star one, and save two numbers for one. Confirm the star, photo, and people tabs each show the expected contacts and order.
- Add and remove a favorite using the star on the contact screen. Reopen ManaCall and check that the changes persist.
- Change a name, photo, or Android star and delete a sample contact. Return to ManaCall and confirm the changes appear, including when the selected contact gets deleted.
- Search for an exact name, part of a name, and a close spelling such as `Anjail` for `Anjali`. Confirm that search includes contacts without photos and that clearing search restores the tab.
- Tap a contact with two numbers and select the second. Tap the handset and allow Phone permission when Android asks. Verify that this starts the call to the chosen number and that the next call takes one tap on the contact screen.
- With WhatsApp set up, open both WhatsApp buttons for a saved number that uses WhatsApp. On a device where Android Contacts offers direct WhatsApp calls, check that each ManaCall icon starts the corresponding call to the selected number. If a direct action is unavailable, check that ManaCall opens the correct chat. On a phone without WhatsApp, confirm that Normal call is available and ManaCall shows a fallback message.
- Switch to Telugu, reopen the app, and check that the language persists. Increase Android's font size and check that the screens can still scroll and the controls are usable.
