# RoamTV Privacy

RoamTV is a local network client. It does not include analytics, advertising,
telemetry, crash reporting, or a RoamTV-operated account service.

## Data stored on the device

- Server profiles, groups, subscriptions, routing rules, DNS settings, and app
  preferences are stored locally in the app's private storage.
- Backup files are created only when the user explicitly exports them. They may
  contain proxy credentials and should be stored securely.
- Logs are kept for troubleshooting and are exported only when the user chooses
  to export them.

## Network connections

RoamTV connects only to endpoints the user configures, plus the following
functional endpoints:

- GitHub is used to check for app updates and to download GeoIP/GeoSite assets
  when the user requests an update.
- The user's subscription providers and proxy servers are contacted directly.

RoamTV does not route this metadata through a RoamTV-operated server.

## Phone pairing

The phone-pairing page is served by the TV on the local network for a short,
one-time session. The payload is encrypted in the phone browser before it is
sent; the pairing key is carried in the QR code fragment and is not sent to the
TV's HTTP server. The session ends when the import completes, when the user
cancels, or after the timeout. Pairing payloads are not written to logs or disk
before the user confirms the import.

## Permissions

- **VPN**: required to create the local tunnel when VPN mode is selected.
- **Notifications**: used for the foreground-service notification.
- **Local network**: required on Android 17+ before the temporary pairing socket
  can be opened.
- **Install unknown apps**: used only when the user chooses to install a
  downloaded update.
- **Query installed apps**: used to build the per-app proxy list. The list stays
  on the device.

## Children

RoamTV does not knowingly collect personal information from anyone, including
children.

## Changes

Material changes to this policy will be published in this repository. Questions
can be raised through the repository's issue tracker.
