# My Aniyomi Extensions

A personal extension repo for [Aniyomi](https://github.com/aniyomiorg/aniyomi).
Every push to `main` builds all extensions in `src/` and publishes them to the `repo` branch.

## Add this repo to Aniyomi

Aniyomi → **More → Settings → Browse → Anime extension repos → Add**, then paste:

```
https://raw.githubusercontent.com/Kthedestroyer/aniyomi-extensions/repo/index.min.json
```

(Change `Kthedestroyer/aniyomi-extensions` if you named the GitHub repo something else.)
Then go to **Browse → Extensions**, pull down to refresh and install your extension.

---

## First-time setup

1. **Create the GitHub repo**: make a new, empty repo called `aniyomi-extensions` (no README/licence),
   then upload/push this folder to its `main` branch.
2. **Add the signing key secrets**: open `signing-key.txt` (sent separately, never commit it) and add each value as a
   secret under **Settings → Secrets and variables → Actions → New repository secret**:
   `SIGNING_KEY`, `ALIAS`, `KEY_STORE_PASSWORD`, `KEY_PASSWORD`.
   Keep using the same key forever, or Aniyomi will refuse to update your extensions.
3. **Run it**: go to the **Actions** tab → *Build & publish repo* → **Run workflow**
   (or just push a commit). When it goes green, a `repo` branch appears with your APKs and `index.min.json`.

## Adding your website

The template lives in `src/en/mysite/`. For each website:

1. Copy `src/en/mysite` to `src/<lang>/<yoursite>` (lowercase, no spaces, e.g. `src/en/animepahe`).
2. Rename the package folders and `package` line to match:
   `src/<lang>/<yoursite>/src/eu/kanade/tachiyomi/animeextension/<lang>/<yoursite>/`
3. Edit `build.gradle`: set `extName` and `extClass` (the class name with a leading dot).
4. In the Kotlin file, set `name`, `baseUrl` and `lang`, then fill in every `// TODO`:
   the URLs for popular/latest/search, and the CSS selectors for anime cards, episode lists and video links.
5. **Bump `extVersionCode`** every time you change an extension, otherwise Aniyomi won't see the update.
6. Delete the `src/en/mysite` template once your own extension works.

Optional: put a launcher icon in `src/<lang>/<yoursite>/res/mipmap-*/ic_launcher.png`; otherwise the default Aniyomi icon is used.

The template uses `ParsedAnimeHttpSource` (plain HTML + CSS selectors). If your site uses a JSON API,
extend `AnimeHttpSource` instead and override the `*Parse(response)` functions.

## Building locally (optional)

Install Android Studio, open this folder, then:

```
./gradlew -p src assembleDebug
```

The debug APK ends up in `src/<lang>/<yoursite>/build/outputs/apk/debug/` and can be installed straight onto your phone for testing.

## Notes

- Built on the official Aniyomi extension build setup with extensions-lib 14, the format that stable Aniyomi reads.
- `./gradlew formatKotlin` auto-formats your code (optional).
