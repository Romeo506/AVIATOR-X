# AVIATOR X

Android research app for local crash-game round recording, historical data analysis and SHA-256 commitment verification.

## Working features
- Import CSV history from the Android document picker.
- Parse `multiplier`, `crash`, `result`, or `coefficient` columns, or plain numeric CSV data.
- Save imported rounds and manually entered multipliers locally on the device.
- Historical average, median, below-2x frequency, distribution bands and latest low-round streak.
- View the latest saved outcomes.
- Export locally saved rounds as CSV.
- Clear local history.
- Calculate SHA-256 locally and compare a revealed seed with a published commitment hash.
- No account login, passwords, OTPs, cookies, session tokens, or cloud data collection.

## Build
GitHub Actions builds a debug APK on every push to `main` and on manual workflow dispatch. The workflow installs Java 17, Android SDK platform 35 / build tools 35.0.0, and Gradle 8.9. It prints diagnostic versions and runs:

```bash
gradle --no-daemon --stacktrace :app:assembleDebug
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
A SHA-256 commitment is a one-way hash, not encrypted server-seed data. It can be checked after a seed is revealed; it cannot disclose a hidden future seed. Historical crash multipliers do not establish a reliable next-round prediction. The app intentionally does not generate fake signals, guaranteed win claims, or fabricated confidence percentages.

All analytics are descriptive statistics of the data entered by the user, not betting advice. Use the app to understand records and fairness proofs, not to decide wagers.
