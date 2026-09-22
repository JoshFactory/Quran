# Quran with Tafseel — Flipbook App

An Android app that presents your "Quran with Tafseel" PDFs as a single
continuous flipbook, opening at Surah 1 (Al-Fatiha) and ending at Surah 114
(An-Nas), with a real 3D page-turn animation and a "Jump to Surah" list.

## ⚠️ Before you build: 4 surahs are missing

Your source zip is missing **Surah 45 (Al-Jathiya), 53 (An-Najm), 54
(Al-Qamar), and 55 (Ar-Rahman)**. The app will build and work fine without
them — the flipbook will just skip straight from Surah 44 to 56 — but if you
want them included:

1. Add the 4 missing `Surah_0NN_....pdf` files into your original zip (same
   naming style as the rest, e.g. `Surah_045_Al_Jathiya_Complete.pdf`).
2. Run: `pip install pypdf` then `python3 tools/build_manifest.py your_updated.zip`
3. Commit and push — the Action below rebuilds the APK automatically.

The same command is how you fix or replace *any* surah later — it always
regenerates `app/src/main/assets/` from scratch from whatever zip you point
it at, so there's nothing to hand-edit.

## Getting an installable APK (no Android Studio needed)

This repo has a GitHub Actions workflow (`.github/workflows/build-apk.yml`)
that compiles a real, installable `.apk` on GitHub's servers every time you
push. You never need to install anything locally.

1. Create a new **empty** repository on GitHub (github.com/new) — don't add
   a README/gitignore there, this project already has them.
2. From this folder, push it:
   ```bash
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git branch -M main
   git push -u origin main
   ```
3. Open your repo on GitHub → the **Actions** tab → you'll see "Build APK"
   running (takes ~2-3 minutes).
4. Once it's green, get the APK either way:
   - **Releases** (right sidebar of the repo, or `/releases`) → download
     `app-debug.apk` from the latest build.
   - Or **Actions** → the completed run → **Artifacts** section at the
     bottom → download `QuranWithTafseel-debug-apk.zip` (unzip to get the
     `.apk`).
5. Transfer the `.apk` to your Android phone (email it to yourself, Google
   Drive, USB, etc.), open it, and tap install. Android will ask you to
   allow installing from this source the first time — that's expected for
   any app installed outside the Play Store.

Every future `git push` to `main` re-runs the build and publishes a new
Release automatically — handy once you add the missing surahs.

## What's actually in the app

- **1,668 pages** across 219 bundled PDFs (110 of 114 surahs currently),
  read in true Quran order: surah number ascending, and each surah's parts
  in ascending order, so multi-part surahs (like Al-Baqarah's 19 parts)
  read seamlessly as one continuous section.
- **Real 3D page-turn animation** — pages rotate around their spine edge
  with proper perspective, not just a flat slide. This uses Android's own
  `View.rotationY` + `cameraDistance` (see `BookFlipPageTransformer.kt`) —
  deliberately *not* a third-party OpenGL "page curl" library. Those
  libraries (e.g. `eschao/android-PageFlip`) need a lot of hand-written GL
  rendering and touch-handling code and currently have known broken
  dependency links on JitPack — not something worth risking on code that
  can't be test-built here. If you'd rather have a true paper-curl effect
  later, that library is a drop-in enough replacement for
  `BookFlipPageTransformer` that a future pass could swap it in.
- **Jump to Surah** — tap the floating button (bottom right) for a list of
  all available surahs; tapping one jumps straight to its first page.
- **Remembers your place** — reopening the app returns to the last page
  you had open.
- Pages are rendered on-demand from the PDFs (Android's built-in
  `PdfRenderer`, no extra rendering library needed) and cached, so startup
  is fast even with 1,668 pages — nothing is pre-rendered up front.

## Project layout

```
app/src/main/
  assets/manifest.json       — generated reading order (don't hand-edit; see tools/)
  assets/pdfs/                — the 219 source PDFs, renamed to sort correctly
  java/.../data/               — manifest loading + PDF page rendering
  java/.../ui/                 — the flip animation, page & surah-list adapters
  java/.../MainActivity.kt     — wires it all together
tools/build_manifest.py      — regenerates assets/ from a source zip
.github/workflows/build-apk.yml — auto-builds the APK on every push
```

## If you'd rather build locally in Android Studio

That works too — open this folder in Android Studio (Giraffe or newer),
let it sync, then **Build → Build Bundle(s) / APK(s) → Build APK(s)**. You
don't need to touch the GitHub Actions workflow for this path.

## App identity

- Package name: `com.joshfactory.qurantafseel`
- App name shown on the device: **Quran with Tafseel**

Change these in `app/build.gradle` (`applicationId`) and
`app/src/main/res/values/strings.xml` (`app_name`) if you'd like something
different.
