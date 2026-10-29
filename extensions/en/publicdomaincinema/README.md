# Public Domain Cinema (Tadami)

This module is the first playable source in the Universal Media Extensions project.

## What it does
- Searches the Internet Archive Advanced Search API.
- Lists video items whose metadata declares a Creative Commons Public Domain Mark or CC0 license.
- Shows item title, year, description, and thumbnail.
- Lists MP4/M4V/WebM files as episodes and sends direct, verified item-file URLs to Tadami's player.

## Compatibility
The source declares extensions-lib API version 1.5 and uses the official template's `AnimeHttpSource` interface. The template documents API 1.5 as accepted by Tadami apps with lib 1.5 or later; an installed app's actual bundled library version must still be checked.

## Rights filter
Search is restricted by `licenseurl:http*publicdomain*` and the parser only accepts metadata license URLs containing:
- `creativecommons.org/publicdomain/mark/1.0`
- `creativecommons.org/publicdomain/zero/1.0`

This is a conservative metadata filter, not a legal guarantee. Verify the terms on an item's page before redistribution or reuse.

## Why this is not an adapter for every third-party listing
The user-provided directory includes many separate movie and anime websites. Tadami doesn't convert arbitrary site URLs into playable sources: every provider requires its own parser, episode/stream handling, and compatibility tests. This project does not mass-extract streams from sites whose distribution rights and player behavior haven't been verified. Additional adapters should be added only for authorized sources.
