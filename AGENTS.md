# AGENTS.md

Guide for AI coding agents (Claude Code, Codex, Cursor, …) working on this repo. Humans: see
[README.md](README.md). `CLAUDE.md` just points here.

## What this is

**Косметичка** is a native Android app (Kotlin, Jetpack Compose, Material 3) for tracking the
cosmetics a person owns and wants. UI language is Russian.

- **Коллекция**: owned items with photos, usage state (Новое / Открыто / Закончилось), remaining %,
  opened date + PAO (period after opening) → expiry date, badges and expiry notifications.
- **Желаемое**: wishlist with "Хочу сильно", price, link, notes; "Куплено" moves an item to the
  collection; "Купить снова" puts an owned item back on the wishlist.
- **Каталог**: search in [Open Beauty Facts](https://world.openbeautyfacts.org) (open data,
  ODbL; photos CC BY-SA, attribution is required and shown on every catalog photo).
- **Добавить**: live barcode scanning (CameraX + ML Kit), "Фото упаковки" photo search
  (ML Kit OCR → catalog search → "Это оно?" candidates), manual form. A photo is mandatory
  for manual items. Duplicates are hinted ("Похожее уже есть у вас"), never blocked.

Product decisions come from two design inputs kept outside the repo; the essentials are in
this file. Data source research: OBF is the only usable free cosmetics DB; coverage of Russian
products is weak, so "not found" is a normal path that leads to the manual form, not an error.

## Layout

```
core/                 Pure Kotlin/JVM build (no Android SDK). Domain + catalog client.
  model/              CosmeticItem, CatalogProduct, Category, Barcodes (GTIN check digit)
  catalog/            ProductCatalog interface, OpenBeautyFactsCatalog (OkHttp + kotlinx.serialization),
                      CategoryClassifier (OBF tags / names → Category)
  collection/         CollectionRepository interface, InMemoryCollectionRepository (tests/fakes),
                      CollectionService (all business rules), queries/filters, DuplicateFinder,
                      ExpiryReminders, CollectionBackup (JSON export/import)
  photo/              PhotoQueryPlanner + PhotoSearch (recognition → catalog queries), MatchScorer,
                      FormPrefiller (OCR → form fields)
app/                  Android app, depends on core via includeBuild("core")
  data/db/            Room: ItemEntity, ItemDao, AppDatabase, RoomCollectionRepository
  data/photo/         PhotoStorage (filesDir/photos), MlKitPhotoRecognizer
  work/               ExpiryReminderWorker (WorkManager, daily)
  ui/<screen>/        One package per screen: <Screen>ViewModel + <Screen>Screen.kt
  ui/navigation/      CosmeticsNavHost + Routes (string routes from the design spec)
  ui/components/      Shared composables (ItemImage with placeholder + attribution, labels)
  ui/theme/           Material 3 theme, fallback palette (seed #8C4A60), category tones
  AppContainer.kt     Manual DI: one instance per app; ViewModels get deps from it
```

## Architecture rules

- **Room is the source of truth.** The catalog is only called while searching/adding. Never
  store `CatalogProduct` directly: convert with `CatalogProduct.toItem()` via `CollectionService`.
- **Business rules live in `core`**, in `CollectionService` and friends, and are unit-tested on
  the JVM. ViewModels orchestrate; they don't decide (e.g. dedupe on add, "Куплено" reset,
  PAO countdown start are all in `CollectionService`).
- **One table for both lists**: `CosmeticItem.status` is `OWNED` or `WISHLIST`.
- **Screens are split** into `XxxRoute` (gets the ViewModel via `appViewModel { container -> … }`,
  collects flows, handles launchers) and a stateless `XxxScreen(state, callbacks)`. UI tests
  target the stateless one.
- **Images**: own photo → catalog photo → category placeholder (`ItemImage`). Own photos are
  copied into `filesDir/photos` (`PhotoStorage`) and stored as `file://` URIs. Catalog photos
  shown with `attribution = true`.
- **Scanner → form hand-off** goes through `AddFlowStore` (in-memory), not navigation args.
- **No API keys, no backend.** ML Kit runs on device with bundled models. Text recognition is
  Latin-only (ML Kit has no Cyrillic model).
- OBF requires a descriptive `User-Agent` (set in `AppContainer`). Keep it.
- Strings are in `app/src/main/res/values/strings.xml` (Russian). No hard-coded UI text.

## Build and test

JDK 17+. The Android part needs the Android SDK (`ANDROID_HOME` or `local.properties`).

```bash
./gradlew -p core test                          # domain + catalog client, JVM only, fast
./gradlew :app:testDebugUnitTest                # Robolectric: Room, ViewModels, Compose screens
./gradlew :app:assembleDebug                    # APK in app/build/outputs/apk/debug/
./gradlew :app:lintDebug                        # lint (non-fatal)
```

CI (`.github/workflows/ci.yml`) runs all of the above on every push and PR and uploads the
debug APK as an artifact.

Sandboxes without Google Maven / Android SDK access can still run `./gradlew -p core test`
(only Maven Central and the Gradle Plugin Portal are needed).

## Tests: what goes where

| What | Where | Runner |
|---|---|---|
| Domain rules, catalog JSON parsing, photo search planning | `core/src/test` | JUnit 4, MockWebServer for HTTP |
| Room round-trips, DAO queries | `app/src/test/.../data` | Robolectric |
| ViewModels | `app/src/test/.../ui/*ViewModelTest.kt` | JUnit 4 + `MainDispatcherRule`; Robolectric when `android.net.Uri` is involved |
| Composable screens | `app/src/test/.../ui/ScreensTest.kt` | Robolectric + `createComposeRule` |

Test doubles live in `app/src/test/.../testing/TestDoubles.kt` (`FakeCatalog`, `FakeRecognizer`,
`Samples`) and `core`'s `InMemoryCollectionRepository`. Catalog JSON fixtures are in
`core/src/test/resources/obf/`; when OBF changes its format, capture a real response there first.

Robolectric uses a plain `android.app.Application` (see `app/src/test/resources/robolectric.properties`)
so `CosmeticsApp` (WorkManager, Coil setup) doesn't start in tests.

Rules: every behavior change gets a test in the cheapest layer that can hold it (prefer `core`).
Never skip or `@Ignore` a failing test to get green.

## Common tasks

- **New field on an item**: add to `CosmeticItem` → `ItemEntity` (+ bump `AppDatabase.version`
  and add a `Migration`; schemas are exported to `app/schemas/`) → `CollectionBackup.BackupItem`
  (with a default, so old backups import) → form `FormFields`/`toItem` → tests in
  `CollectionBackupTest` and `RoomCollectionRepositoryTest`.
- **New catalog source**: implement `ProductCatalog` in `core/catalog`, test it with
  MockWebServer + fixtures, wire it in `AppContainer`. Show its attribution if its license needs it.
- **New screen**: `ui/<name>/<Name>ViewModel.kt` + `<Name>Screen.kt` (Route + stateless Screen),
  add a route to `Routes` and `CosmeticsNavHost`, strings to `strings.xml`, a test in `ScreensTest`.

## Known gaps / next steps

- Image-embedding similarity (MediaPipe Image Embedder) for duplicate detection by photo.
- Paging 3 instead of the manual "load more" in the catalog.
- Contributing user-added products back to Open Beauty Facts (needs explicit consent).
- Instrumented (device) tests; currently everything runs on the JVM.
