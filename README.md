# AVIATOR X

Android research app for multi-provider crash-game round recording, historical data analysis and SHA-256 commitment verification.

## Working features
- Select a local data profile: Spribe Aviator, SmartSoft JetX, or Generic Crash Game.
- Each provider profile has its own isolated round history on the device.
- Import CSV history from Android's document picker into the currently selected profile.
- Parse `multiplier`, `crash`, `result`, or `coefficient` columns, or plain numeric CSV data.
- Add rounds manually; data persists locally between app launches.
- Historical average, median, below-2x frequency, outcome distribution and latest low-round streak.
- View latest saved outcomes, export the selected provider's history as CSV, or clear it.
- Calculate SHA-256 locally and compare a revealed seed with a published commitment hash.
- No gambling account login, passwords, OTPs, cookies, session tokens, or cloud data collection.

## Provider support status
The provider selector currently provides **separate local data profiles** for Spribe Aviator, SmartSoft JetX, and generic crash games. It does not connect to provider servers, read live rounds, authenticate game accounts, or predict future outcomes. Import each provider's own exported CSV into its matching profile. Additional provider adapters can be added when a provider supplies a documented, permitted data interface.

## Build
GitHub Actions builds a debug APK on every push to `main` and on manual workflow dispatch. The workflow installs Java 17, Android SDK platform 35 / build tools 35.0.0, and Gradle 8.9. It runs unit tests before assembling the APK:

```bash
gradle --no-daemon --stacktrace :app:testDebugUnitTest :app:assembleDebug
```

After a successful run, download the `AVIATOR-X-debug-APK` artifact from the Actions run.

## CSV format
A CSV with a header:

```csv
multiplier
1.24
2.15
1.00
5.60
```

Or plain numeric values separated by commas, semicolons, or new lines.

## Important limitations
A SHA-256 commitment is a one-way hash, not encrypted server-seed data. It can be checked after a seed is revealed; it cannot disclose a hidden future seed. Historical crash multipliers do not establish a reliable next-round prediction. The app intentionally does not generate fake signals, guaranteed-win claims, or fabricated confidence percentages.

All analytics are descriptive statistics of the data entered by the user, not betting advice.
