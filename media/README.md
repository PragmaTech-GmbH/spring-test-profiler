# Media Recording Tooling

Reusable tooling to (re-)generate the marketing assets for the Spring Test Profiler report:

- `docs/marketing/announcements/assets/report-walkthrough.gif` - a ~20 second animated walkthrough of the HTML report that scrolls through the summary and cache sections and zooms into the Context Cache Timeline
- `docs/marketing/announcements/assets/context-timeline-screenshot.png` - a high-resolution screenshot of the Context Cache Timeline section (used for LinkedIn posts)
- `docs/marketing/announcements/assets/results-json-screenshot.png` - a styled screenshot of the generated `results.json` file (used in newsletter content)

## Prerequisites

- Node.js and npx (any recent version)
- ffmpeg (`brew install ffmpeg`) - used for the webm to GIF conversion
- Java 21 for the demo project (see `demo/.sdkmanrc`)

Playwright is installed on demand by the wrapper script (`npm install --no-save playwright`), the same way `video/record.sh` does it.

## Usage

From the repository root:

```bash
./media/record-report-gif.sh
```

This builds the profiler from the current working tree, runs the `demo/spring-boot-4.0-maven` test suite to generate a fresh report, records the walkthrough with Playwright, converts it to an optimized GIF, and refreshes all three assets listed above.

### Flags

| Flag | Effect |
|---|---|
| `--skip-build` | Reuse the existing demo report instead of rebuilding (fast iteration on timings/captions) |
| `--screenshots-only` | Only refresh the two PNG screenshots, skip the GIF recording |
| `--gif-only` | Only re-record and convert the GIF, skip the screenshots |

## Re-running on a New Version

Nothing to configure: the script always builds the profiler from the current working tree and reruns the demo, so after a release or feature change just run it again and commit the refreshed assets under `docs/marketing/announcements/assets/`.

## Tuning

- Storyboard steps, captions, and timings: constants and the `recordGif()` function in `record-report-gif.mjs`
- GIF size/quality: the ffmpeg flags in `record-report-gif.sh` (current settings: 8 fps, 800 px wide, 64 colors, no dithering, ~1.8 MB; raise `max_colors` or fps if quality matters more than size)
- Viewports: `GIF_VIEWPORT` (1280x800 recording, downscaled to 800 px GIF) and `SCREENSHOT_VIEWPORT` (1600x900 at 2x device scale) in `record-report-gif.mjs`

## Embedding Committed Assets

Once pushed to `main`, assets are embeddable via GitHub raw URLs:

```text
https://raw.githubusercontent.com/PragmaTech-GmbH/spring-test-profiler/main/docs/marketing/announcements/assets/report-walkthrough.gif
```

Intermediate files (webm recording, palette, unoptimized GIF) live in `media/output/`, which is gitignored.
