> **This is an unofficial fork** of [jonas-l/notificationlog-android](https://codeberg.org/jonas-l/notificationlog-android) (GPL-3.0-or-later). All upstream functionality is intact; this fork only adds an opt-in HTTP webhook delivery path. Please file issues unrelated to the webhook against the upstream project.

NotificationLog is an Android App which logs the notifications (locally) and allows to view/ export all of them or all of one App.

## Fork: HTTP webhook delivery

This branch (`webhook-http`) adds an opt-in HTTP forwarding path on top of
upstream's local logging. Each notification you log can also be POSTed to a
`ntfy.sh`-compatible endpoint of your choice (request body = notification
text; `Title`, `Tags`, optional `Authorization: Bearer …` headers).

Per-app filtering reuses the existing whitelist/blacklist — the webhook
honors the same filter as the local log, so there is one source of truth.

**Offline-resilient by design.** Pending deliveries are written to Room in
the same transaction as the notification itself, then handed to WorkManager
with a `NetworkType.CONNECTED` constraint and exponential backoff. The
queue survives process kill, app update and device reboot — no notification
is dropped just because the network was down when it arrived.

Configure under Settings → "Forward notifications via HTTP".

### Install via F-Droid

This fork ships its own F-Droid repo (auto-built on every commit to
`webhook-http`, hosted on GitHub Pages):

| | |
|---|---|
| URL         | `https://d33mobile.github.io/notification-http/fdroid/repo` |
| Fingerprint | `E2978C4365F48A686E19A41CE94D226643686468FC94E5D4D2EBCADD49C3309B` |

Open the F-Droid app on your phone → *Settings* → *Repositories* → **+** → paste both.
Updates ship automatically with every push to `webhook-http`. Landing page:
<https://d33mobile.github.io/notification-http/>

This fork uses the application id `pl.d33.notificationlog.ntfy` (different from
upstream's `de.jl.notificationlog`) so both apps can be installed side by side.

### Tests

* `./gradlew :app:test` — 9 Robolectric + MockWebServer cases that exercise
  the full delivery contract (success, 4xx drop, 5xx retry, FIFO, recovery
  after restart, runtime disable, blank URL, bearer header presence/absence)
  on a real Room DB. No emulator required.
* `./gradlew :app:connectedAndroidTest` — `WebhookE2ETest` runs the same
  flow against real Room + WorkManager + HttpURLConnection on a connected
  device or emulator.
* `scripts/smoke-test.sh` — installs the debug APK, posts notifications
  online + offline, and asserts ordered delivery once the network returns.
  Use this when you have a device/emulator handy. Requires `jq`.

## Screenshots

![screenshot showing the list of Apps in NotificationLog](./app/src/main/play/en-US/listing/phoneScreenshots/391911807240794485.png)

![screenshot showing the notifications of one App in NotificationLog](./app/src/main/play/en-US/listing/phoneScreenshots/2565105775754909137.png)

## Download

- <https://f-droid.org/packages/de.jl.notificationlog/>
