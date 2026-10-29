# Universal Media Extension — implementation plan

## Verified repository status
- Source repository: `vaibhavaseri90-hue/universal-media-extensions`.
- Default branch: `main`.
- `gradle.properties` already sets `tadami.repoId=vaibhavaseri90-hue/universal-media-extensions`. Keep it unchanged after any extension is released because it contributes to source IDs.
- Existing source folders are present under `extensions/en/` and `extensions/fr/`; they were preserved.
- The separate distribution repository `vaibhavaseri90-hue/tadami-extensions` does not currently exist.
- The release workflow exists, but publishing requires that distribution repository plus GitHub Actions secrets for a signing keystore/password/alias and a write token.
- The template marker was removed manually to prevent the template setup workflow from replacing the existing `extensions/` directory with only a sample extension. The cleanup workflow was intentionally not run.

## What Tadami extensions can do
The current source template is built for anime/video site adapters: search, details, episodes, and stream resolution. A directory of website URLs is not itself a compatible extension store or a universal playback API.

MangaDex is a manga-reading source. It will not show up in this repository just because the repository is added in Tadami; a manga extension must target an app/API that supports manga sources. Do not build a fake entry that promises compatibility it does not have.

## Remaining store setup
1. Create a public repository named `vaibhavaseri90-hue/tadami-extensions` with an initial commit. This connector can edit existing repositories but does not expose a create-repository action, so repository creation could not be performed from here.
2. Configure these Actions secrets in the source repository: `EXTENSIONS_KEYSTORE_BASE64`, `EXTENSIONS_KEY_STORE_PASSWORD`, `EXTENSIONS_KEY_ALIAS`, and `EXTENSIONS_REPO_TOKEN`. Do not share the keystore password or token in chat. The token needs Contents read/write access to the distribution repository.
3. Confirm the installed Tadami app's bundled extensions-lib/API version before releasing sources, and run a dry-run build first.
4. Release only extensions that compile against the app and whose source usage is authorized. The workflow will publish signed APKs, icons, index metadata, and `repo.json` to the distribution repository.
5. Add the distribution repository in Tadami and verify its signing-key fingerprint in the app.

## Planned architecture
1. **Directory/catalogue layer:** curated source websites with name, category, homepage, and notes about lawful availability. Opening a website does not guarantee in-app playback.
2. **Metadata layer:** use a documented metadata API such as AniList for titles, covers, descriptions, and genres. Metadata APIs do not provide video streams.
3. **Tadami source adapters:** implement and test each authorized source separately against the exact API supported by the installed app.
4. **Playback:** use supported stream/extractor interfaces only when the source provides a compatible, authorized stream. Some websites require their own player or are not compatible.

## Compatibility gate
An extension's API declaration must not exceed the API/lib version bundled with the installed app. An app version number alone is not enough to promise compatibility; check the actual extension API/lib version before choosing `apiVersion` in an extension's `build.gradle.kts`.

## Safety and stability
- Do not scrape or bypass paywalls, access controls, DRM, or other protections.
- Do not treat every URL in a third-party directory as safe, licensed, stable, or compatible.
- Do not change source IDs after release, and do not publish unsigned or unverified APKs.
- Preserve existing source folders; do not run the template cleanup script again.
