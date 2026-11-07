# Cask & Label – Android

Native Android companion of the Cask & Label iOS app ("Whisky journal met vrienden"), in Kotlin with Jetpack
Compose, on the **same Supabase backend** (project `dccuodnhizifzwnnvchb`, Frankfurt). Decision 2026-11-03 in
the iOS repo (`wwpilot/cask-and-label`, DECISIONS.md): Dirk builds the Android app in its own repo; spec in
Dropbox `Woofpilot/other-business-ideas/Cask-and-Label/spec-voor-dirk.md`, section 12.

Package / application id: `be.webweaver.apps.caskandlabel`. Min SDK 26, target 37. Dutch is the default language.

## Setup

1. `local.properties` (git-ignored) needs the **publishable key** (starts with `sb_publishable_`), from Erik
   or the Supabase dashboard → Project Settings → API Keys. Never the secret or service_role key.

   ```
   sdk.dir=/home/disc/Android/Sdk
   supabase.publishableKey=sb_publishable_…
   ```
   Without it the app builds but shows a "not configured" screen.
2. Build: `./gradlew assembleDebug` (APK: `app/build/outputs/apk/debug/app-debug.apk`); tests:
   `./gradlew testDebugUnitTest`. Gradle needs a full JDK; the system Java 21 on this machine is a JRE only, so
   use Android Studio's JDK or the one Gradle downloaded:
   `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 ./gradlew assembleDebug`.

## What is in it (0.1)

Everything is online only (no local database): row-level security on the server decides what you see; the
app filters only for display.

- **Account**: create an account / sign in with e-mail + password (≥ 8 chars), session kept on the device;
  first sign-in asks your display name + 18+ confirmation and creates the `profiles` row; rename; sign out.
- **Journal**: one row per bottle (label photo, flavour profile, cask types, group labels, average score,
  number of tastings) with your own tastings and those shared with your groups; search, group chips
  (Alles / Privé / each group), sort by date, score or name; pull to refresh.
- **Bottle**: facts, label photo, average, every visible tasting (taster, date, group or Privé, notes, tags,
  price); delete your own tasting; "Nog een tasting".
- **New tasting**: search bottles you may see (approved, your own, tasted in your groups) or add a new bottle
  (name, brand, distillery, kind, country, region, age, ABV, volume, peated, cask types, features, flavour
  profile suggested with the iOS rule) with an optional label photo (camera or gallery → JPEG ≤ 2000 px,
  uploaded to `bottle-photos/<user>/<bottle>/front.jpg`); the bottle is `pending` and you become its holder
  ("Fles van"). Then score 1–10 in half stars, nose/palate/finish, flavour tags, own price, date and time,
  **Delen met: Privé / <groep>**.
- **Groups**: list with member count, create, join with a 6-character code (typed or QR scan), group screen
  with members, the group's tastings and leave (your tastings become private); admins invite: code + QR
  (`caskandlabel://join/<code>`) + share text, and revoke a code.
- **Links**: `caskandlabel://join/<code>` opens the join dialog; `caskandlabel://login-callback` completes a
  mail link.
- **No connection**: a banner, and saving is disabled.
- **Parity logic** in `core/` with unit tests: `Score` (half steps 2–20, "7,5"), `BottleTitle` (age in the
  title), `Vocabulary` (same lists and flavour-profile rule as `CaskCore`), `InviteCode`, `Journal` (rows per
  bottle, group scope, search, sort).

## Not (yet) in it

- On-device label reading (OCR), barcode, back-label and box photos, photo quality check, online lookup
  (Wikidata / Open Food Facts) — iOS prototype features.
- Full Drankdozijn filter screen and Insights (patterns, top, value for money, suggestions).
- Editing a bottle or a tasting after saving; choosing other holders; deleting bottles or photos.
- Export/import (BackupArchive JSON), account deletion, English translation (`values-en`).
- Removing members or renaming a group as admin.
- Release signing / Play Console.

The server contract follows the migrations in the iOS repo up to 0.31 (7 migrations). iOS 0.32–0.36
(`docs/spec/api-contract.md`, `parity-checklist.md`, migration 8) were not yet committed when this was built.
