# Sketchware KG – AI Developer Instructions (`AGENTS.md`)

Welcome, Agent! This file provides critical instructions, guidelines, and structural patterns to help you maintain, fix, and expand Sketchware KG successfully.

---

## 1. Core Architecture & Component Registries

Sketchware KG uses highly centralized registries to map IDE components and Java compiler structures. When adding or modifying components:
1. **Component Mapping (`ComponentBean.java`):**
   - Direct components (such as Intent, Vibrator, Supabase, Firebase) are identified by static integers (e.g., `COMPONENT_TYPE_SUPABASE_AUTH = 51`).
   - Map their friendly names, class builds, descriptions, and icon bindings inside `getComponentTypeName`, `getComponentTypeByTypeName`, `getIconResource`, and `buildClassInfo`.
2. **Library Registries (`ProjectLibraryBean.java`):**
   - Active project-specific libraries (like Material 3, Firebase, Supabase) are managed under `PROJECT_LIB_TYPE_*` constant integers.
   - Bind descriptions, name strings, and fallback drawables inside `getLibraryResName`, `getLibraryResDesc`, and `getLibraryIcon`.
3. **UI Options Toggle (`ManageLibraryActivity.java`):**
   - Any main library settings (like the newly added **Supabase Manager**) must be registered within the `ManageLibraryActivity` categories list (such as `basicCategory`).
   - Handle their intent results, save configurations to disk, and keep their state variables synchronized.

---

## 2. On-the-Fly Code Generation & Same-Package Compilation

For high-privilege components (such as **Supabase**):
1. **Dynamic Java Files Injection (`yq.java`):**
   - To prevent classpath failures, dynamic dexing issues, or jar bloat during the user app's compilation, Sketchware KG generates helper files directly inside the user's package workspace at compile-time.
   - Scan for the component usage in `yq.java` and write helper classes like `SupabaseClient.java` and `SupabaseCallback.java` directly into `javaDir` with `package <packageName>;`.
2. **Same-Package Reference Mappings (`mq.java` & `Lx.java`):**
   - Since generated helpers reside inside the user's same package namespace, **NEVER** prefix instantiations with external namespaces (e.g. use `new SupabaseClient.SupabaseAuth();` instead of `new pro.sketchware.supabase.SupabaseClient.SupabaseAuth();`).
   - Declare variables in `mq.java` to map to static inner helper wrappers (e.g., `"SupabaseClient.SupabaseAuth"`).

---

## 3. Gemini API Constraints & MCP Integrations

1. **Alternate Roles Strictly (`ContextBuilder.java`):**
   - The Gemini API requires strict role alternation (User, Model, User, Model).
   - If consecutive messages from the same role are encountered, they **MUST** be merged into a single message object's `parts` array.
2. **Modern Payload Formatting:**
   - Use the `parts` -> `text` JSON layout strictly inside `ContextBuilder.java` rather than obsolete root `content` keys.
3. **Thought Signature Field:**
   - Gemini function calls require a `thought_signature: ""` field inside the `functionCall` object to avoid HTTP 400 validation failures.

---

## 4. Build Environment & AAPT2 Stable IDs

1. **Keystore Signing in CI/CD:**
   - The GitHub Actions workflow decodes a release keystore from a Base64 secret variable to `app/sketchware-kg.keystore`.
   - `build.gradle` must prioritize environment parameters (`KEYSTORE_PASSWORD`, etc.) to securely sign the output APK.
2. **Stable Resource Linking (`public.xml` & `public-stable-ids.txt`):**
   - Sketchware uses a strict `public.xml` structure to map stable IDs.
   - If you define or reference a resource (such as `title_activity_icon_creator`) in the Android manifest or public layouts, you **MUST** declare it inside `app/src/main/res/values/public.xml` to prevent AAPT2 compilation failures during resource linking.
