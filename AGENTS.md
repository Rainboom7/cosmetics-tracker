# AGENTS.md

Guide for AI coding agents (Claude Code, Codex, Cursor, …) working on this repo. Humans: see
[README.md](README.md). `CLAUDE.md` just points here.

## What this is

**Косметичка** is a native Android app (Kotlin, Jetpack Compose, Material 3) for tracking the
cosmetics a person owns and wants. UI language is Russian.

- **Коллекция**: owned items with photos, usage state (Новое / Открыто / Закончилось), remaining %,
  opened date + PAO (period after opening) → expiry date, badges and expiry notifications.
  Status marks (`ShelfStatus`: Есть запас / Заканчивается / Пробую / Не использую / Отдам; "Заканчивается"
  is also derived from remaining ≤ 35%), "Любимое", and `finishedOn` for the "Пустые баночки" counter.
- **Календарь**: usage calendar, one mark per item per day (`UsageCalendar`): month grid, streak,
  most used, "Давно не пользовались" (opened, no marks for 30+ days). Marking a new item opens it.
- **Желаемое**: wishlist with "Хочу сильно", price, link, notes; "Куплено" moves an item to the
  collection; "Купить снова" puts an owned item back on the wishlist.
- **Каталог**: search in [Open Beauty Facts](https://world.openbeautyfacts.org) (open data,
  ODbL; photos CC BY-SA, attribution is required and shown on every catalog photo). Search
  runs offline over an index bundled in the APK and tops up from the API when the index finds
  little; tapping a result opens the product card (description, characteristics, ingredients,
  only the fields the catalog actually has).
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
  catalog/            ProductCatalog interface, OpenBeautyFactsCatalog (OkHttp + kotlinx.serialization,
                      own RateLimiter: OBF allows ~10 searches/min), CompositeCatalog (index first,
                      API top-up, merge without duplicates), CategoryClassifier (OBF tags / names → Category)
  search/             Offline search: SearchText (normalization shared with the indexer),
                      SearchDictionary (ru → en/fr words, Cyrillic brand names), IndexQuery (FTS4
                      expressions), IndexSchema + LocalIndexCatalog (ranking by words matched, then popularity)
core/src/indexer/     JVM tool, not in the app: ObfIndexBuilder turns the OBF CSV export into the
                      SQLite index (`./gradlew -p core buildObfIndex`); JdbcIndexStore for tests
  collection/         CollectionRepository interface, InMemoryCollectionRepository (tests/fakes),
                      CollectionService (all business rules), queries/filters, DuplicateFinder,
                      ExpiryReminders, CollectionBackup (JSON export/import, format 2 with usage marks)
  usage/              UsageLogRepository (+ in-memory), UsageCalendar (month view, streak, forgotten)
  photo/              PhotoQueryPlanner + PhotoSearch (recognition → catalog queries), MatchScorer,
                      FormPrefiller (OCR → form fields)
app/                  Android app, depends on core via includeBuild("core")
  data/db/            Room: ItemEntity, ItemDao, UsageEntity/UsageDao (usage_log, FK cascade to items),
                      AppDatabase (version 2, MIGRATION_1_2), RoomCollectionRepository, RoomUsageLogRepository
  data/photo/         PhotoStorage (filesDir/photos), MlKitPhotoRecognizer
  data/catalog/       BundledIndexStore: copies the asset index to noBackupFilesDir once per app
                      version and queries it with Android SQLite
  work/               ExpiryReminderWorker (WorkManager, daily)
  ui/<screen>/        One package per screen: <Screen>ViewModel + <Screen>Screen.kt
  ui/navigation/      CosmeticsNavHost + Routes (string routes from the design spec)
  ui/components/      Shared composables (ItemImage with placeholder + attribution, labels)
  ui/theme/           Material 3 theme: palette v3 "Облако" (dynamic color off by default),
                      shapes, Spacing, category and status tones, Candy and Sky accents
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
- **Look**: shop-like, warm pink and cloudy (design spec v3 «Облако»). Tab headers use
  `Modifier.cloudSky()` (with `bleed()` inside padded lists); white cards float on `cloudShadow()`
  (`CloudCard`); pill shapes for chips and buttons; one solid `primary` call to action per card or
  screen. Use theme roles, `Spacing`, `Candy`/`Sky` tokens and `ShelfStatus.tone()`; no other
  hard-coded colors in screens.
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
debug APK as an artifact. Before building the APK it downloads the fresh OBF export (~18 MB,
~40k named products) and builds the index into `app/build/generated/obfIndex/obf-index.db`,
which the app bundles as an asset; the job log prints a self-check (hits for sample queries).
Local builds without that step have no index and search online only.

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
  and add a `Migration`, covered in `MigrationTest`; schemas are exported to `app/schemas/`) → `CollectionBackup.BackupItem`
  (with a default, so old backups import) → form `FormFields`/`toItem` → tests in
  `CollectionBackupTest` and `RoomCollectionRepositoryTest`.
- **New catalog source**: implement `ProductCatalog` in `core/catalog`, test it with
  MockWebServer + fixtures, wire it into the `CompositeCatalog` in `AppContainer`. Show its
  attribution if its license needs it.
- **Search misses a common Russian word or brand**: add it to `SearchDictionary` and a case to
  `LocalIndexTest` (the fixture `obf/export_sample.csv` is the export format).
- **Index format change**: bump `IndexSchema.VERSION`, keep `ObfIndexBuilder`, `JdbcIndexStore`
  and `SqliteIndexStore` column order in sync (`IndexSchema.COLUMNS`).
- **New screen**: `ui/<name>/<Name>ViewModel.kt` + `<Name>Screen.kt` (Route + stateless Screen),
  add a route to `Routes` and `CosmeticsNavHost`, strings to `strings.xml`, a test in `ScreensTest`.

## Known gaps / next steps

- Image-embedding similarity (MediaPipe Image Embedder) for duplicate detection by photo.
- Paging 3 instead of the manual "load more" in the catalog.
- Index updates without an app update (e.g. a weekly release asset); today it refreshes with each APK.
- Prices in Russia: no free open API exists. Today the product card has "Найти цену" links
  (`core/price/PriceLinks`) that open the shops' own search pages in a Custom Tab; the app sends
  nothing to the shops. Do not call shops' internal APIs or scrape them. A legal automatic option
  is shop feeds via Admitad (needs the owner's account).
- Contributing user-added products back to Open Beauty Facts (needs explicit consent).
- Instrumented (device) tests; currently everything runs on the JVM.
