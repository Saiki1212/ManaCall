# ManaCall

ManaCall is a photo-based calling app for Android 8.0 and up. It uses your phone's existing Contacts app for names, photos, favorites, and numbers.

## Using ManaCall

1. Open ManaCall and allow Contacts access.
2. Use the three large icons at the bottom.
   - **Star** shows people starred in Android Contacts. A family member can add or remove stars in the Contacts app.
   - **Photo** shows people who have a contact photo. This is the starting tab.
   - **People** shows everyone, with photo contacts first. Android's alphabetical order is retained within each group.
3. Tap a photo, then choose the phone, WhatsApp voice, or WhatsApp video button. For a contact with several numbers, select the number first.
4. In Phone, tap its call button. In WhatsApp, tap the phone or video icon inside the chat. A contact without a WhatsApp account may appear as unavailable in WhatsApp.

The language control switches between English and Telugu. Names stay as saved in Contacts. Edits made in Android Contacts appear when you return to ManaCall, and the app also refreshes if Contacts changes while it is open. Only the language choice and permission prompt state are stored locally. There is no separate contact database.

Open [the interactive preview](preview/index.html) from the cloned folder in a browser. Its photos and calls are samples.

## Build

Open this folder in Android Studio or use JDK 17 and the Android SDK command-line tools with Android SDK 36 and Build Tools 35.0.0. Set the SDK path with `ANDROID_HOME` or with `sdk.dir` in an untracked `local.properties`.

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

Gradle writes the signed test APK to `app/build/outputs/apk/debug/app-debug.apk`. Use the same signing key for later APKs if they should install as updates on the same phone.

The project has one app module. The `contacts` package reads and observes Android Contacts, `calling` opens the Phone and WhatsApp apps, and `ui` contains the Compose screens. Localized text lives in `values` and `values-te`.

## On-device test checklist

- Install the APK and grant Contacts access. Try denying it first and verify that the permission screen offers a way to continue.
- Use Android Contacts to create sample contacts with and without photos, star one, and save two numbers for one. Confirm the star, photo, and people tabs each show the expected contacts and order.
- Change a name, photo, or star and delete a sample contact. Return to ManaCall and confirm the changes appear, including when the selected contact gets deleted.
- Tap a contact with two numbers, choose the second number, open Phone, and place a call to a consenting recipient.
- With WhatsApp set up, open both WhatsApp buttons for a saved number that uses WhatsApp. Check that the correct chat opens and that you can start each call inside WhatsApp. On a phone without WhatsApp, confirm that Normal call is available and ManaCall shows a fallback message.
- Switch to Telugu, reopen the app, and check that the language persists. Increase Android's font size and check that the screens can still scroll and the controls are usable.
