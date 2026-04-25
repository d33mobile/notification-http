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

## Screenshots

![screenshot showing the list of Apps in NotificationLog](./app/src/main/play/en-US/listing/phoneScreenshots/391911807240794485.png)

![screenshot showing the notifications of one App in NotificationLog](./app/src/main/play/en-US/listing/phoneScreenshots/2565105775754909137.png)

## Download

- <https://f-droid.org/packages/de.jl.notificationlog/>
