# Universal Media Extension — implementation plan

## Current repository status
This repository was created from the Tadami extension source template, but the Template setup workflow was skipped. The repository still contains the template marker and the upstream extension folders. Do not publish this repository as a personal extension store yet: `gradle.properties` still uses the upstream `tadami.repoId`, so generated source IDs are not configured for this repository.

## What Tadami extensions can do
Tadami extensions are Android/Kotlin source adapters. A source generally implements search, details, episodes, and stream extraction for a particular site or a compatible family of sites. A directory of website URLs is not itself a universal streaming API.

## Planned architecture
1. **Directory/catalogue layer:** keep a curated list of source websites with name, category (anime, movies, TV, web series), homepage, and a note about lawful availability. This layer can open sites but does not guarantee in-app playback.
2. **Metadata layer:** use a documented metadata API such as AniList for anime titles, covers, descriptions, and genres. Metadata APIs do not provide video streams.
3. **Tadami source adapters:** implement and test each authorized source separately against the exact extension API supported by the installed Tadami app.
4. **Playback:** use Tadami's stream/extractor interfaces only when the source provides a compatible, authorized stream. Some websites require their own player or are not compatible.
5. **Build and release:** after source IDs and repository configuration are correct, build signed release APKs and publish through a separate `<owner>/tadami-extensions` repository following the official Tadami template guide.

## Compatibility gate
The official template documentation currently describes extension API versions and notes that an app accepts extensions only up to its own bundled API version. “Tadami v0.56.1+” alone is not enough to promise compatibility; confirm the installed app's extension API/lib version before choosing `apiVersion` in an extension's `build.gradle.kts`.

## Safety and stability
- Do not scrape or bypass paywalls, access controls, DRM, or other protections.
- Do not treat every URL in a third-party directory as safe, licensed, stable, or compatible.
- Keep existing files until the repository setup is understood; do not run a cleanup workflow blindly.
