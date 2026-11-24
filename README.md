# AVIATOR X

Android research app for historical crash-game data analysis and SHA-256 commitment checking.

## Current features
- Import CSV history from Android document picker.
- Parse multiplier / crash / result / coefficient columns, or plain numeric CSV values.
- Historical average, median and observed frequency summaries.
- Local SHA-256 calculator and commitment comparison.
- No gambling account login, passwords, OTPs, cookies or session tokens are collected.

## Important limitations
A SHA-256 commitment is a one-way hash, not an encrypted server seed. It can be checked after a seed is revealed, but it does not disclose a hidden future seed. Historical crash multipliers do not establish a reliable next-round prediction. The app deliberately does not fabricate predictions or confidence percentages.

## Build APK
GitHub Actions builds a debug APK on every push to main and on manual workflow dispatch. Open the Actions tab and download the `AVIATOR-X-debug-APK` artifact from a successful run.

## CSV format
Either a header row with a column named `multiplier`, `crash`, `result`, or `coefficient`, or a simple list of multiplier values such as:

```csv
multiplier
1.24
2.15
1.00
5.60
```

All analysis is descriptive, not a betting signal.
