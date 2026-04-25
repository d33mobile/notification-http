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
