# Changelog – Sketchware KG Development Milestone

This document tracks all modifications, bug fixes, features, and core architectural expansions implemented in this development milestone.

---

## [1.0.0] - Stable Release

### Added
- **Supabase Integration Subsystem (Core Registries):**
  - Registered Supabase components (Auth = 51, DB = 52, Realtime = 53, Storage = 54) inside `ComponentBean.java` and `ComponentsHandler.java`.
  - Registered Supabase project library (type = 8) inside `ProjectLibraryBean.java`.
- **Supabase Setup Wizard (UI/UX):**
  - Created `manage_library_manage_supabase.xml` containing clean Material 3 inputs for Project URL, Anon Key, and Service Role Key.
  - Implemented `ManageSupabaseActivity.java` setting up the full on-screen settings configurator and input validations.
  - persited configurations securely under `supabase.json` per-project using `SupabaseConfig.java`.
  - Registered `ManageSupabaseActivity` inside `AndroidManifest.xml`.
- **Supabase Block Palettes & Generation templates:**
  - Added full logical block schemas for all asynchronous Auth, DB, Realtime, and Storage methods in `ExtraBlocks.java` and `ExtraPaletteBlock.java`.
  - Integrated on-the-fly compilation helper copy (`SupabaseClient.java` and `SupabaseCallback.java`) inside `yq.java` to prevent class resolution failures.
  - Implemented automatic, headless initialization of `SupabaseClient` at activity startup using stored project credentials in `Jx.java`.
  - Configured complete, bracket-safe async response listeners and callback registrations inside `ManageEvent.java`.
  - Mapped compiler types declarations for Supabase variables inside `mq.java` and `Lx.java`.
- **Asynchronous Listener Events:**
  - Registered and mapped comprehensive, user-friendly event specifications (like `onLoginSuccess`, `onDataFetched`, `onUploadProgress`) for all Supabase components in `ManageEvent.java` and `strings.xml`.
- **Technical Summary Documentation:**
  - Created `SUPABASE_INTEGRATION.md` detailing the entire architecture and wrapper mappings.
- **AI Instructions Guide:**
  - Generated `AGENTS.md` to instruct future AI agents on how to maintain, compile, and expand the project.

### Fixed
- **Gemini API payload errors:**
  - Resolved `HTTP 400` errors by modernizing the nested JSON payload structure (using the `parts` -> `text` hierarchy) and strictly alternating roles inside `ContextBuilder.java`.
  - Appended `"thought_signature": ""` to Gemini tool execution payloads, resolving the function calling verification error.
  - Upgraded registry model selections with modern models like `gemini-3.5-flash` in `VoidPortModelCapabilities.java`.
- **Keystore CI build failures:**
  - Fixed GHA workflow to base64-decode the Keystore variables before release compilation.
  - Updated `app/build.gradle` to fetch signing credentials securely from environment variables.
- **Splash screen crash:**
  - Replaced corrupted visual assets with clean PNGs across mipmap density folders, eliminating the adaptive launcher XML recursive reference loop.
  - Translated remaining Portuguese subtitle copy on the splash screen to English.
- **AAPT2 Stable ID Linking Error:**
  - Registered `title_activity_icon_creator` inside `public.xml` to avoid resource linking failures on strict AAPT2 compilers.
- **Duplicate resource strings:**
  - Removed duplicate declarations of `link_discord_invite` inside `strings.xml` and consolidated it to a single clean reference.
- **Drawer elements cleanup:**
  - Removed `SwAssist` completely from the side drawer layout and controller.
  - Updated all app information Drawer links (Docs, Ideas, Discord, Telegram) to redirect to `t.me/copkg`.
  - Added a "Do not show again" checkbox to the startup donation notice.
- **Branding Replacements:**
  - Completed case-sensitive search-and-replace of "Fabio" with "KG" and "Sketchware IA" with "Sketchware KG" globally.
  - Translated documentation to Arabic.
