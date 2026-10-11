# Architecture

## Modules

```text
Compose UI
 ├─ External health screen ─ MainViewModel ─ NetworkDiagnostics
 ├─ HiNet screen ────────── MainViewModel ─ FastlyDiagnostics
 └─ DNS screen ──────────────────────────── DnsVpnService

Foreground execution
 └─ NetworkMonitorService ─ NetworkDiagnostics ─ private CSV history
```

The MVP uses one Android application module and keeps protocol engines separate from Compose. This makes the UI replaceable and allows Room, WorkManager, export, or a more complete VPN stack to be added without rewriting screens.

## Responsibilities

- `NetworkDiagnostics`: inspects the active Android network, resolves `www.hinet.net`, probes three independent HTTPS connectivity endpoints, and retrieves the public IP.
- `FastlyDiagnostics`: resolves and performs small HTTP range checks against Fastly-backed service domains.
- `NetworkMonitorService`: runs a foreground monitoring loop, retries the first failure after five seconds, confirms an outage only after consecutive failure, and appends private CSV history.
- `DnsVpnService`: captures only traffic addressed to the VPN-provided DNS IP, forwards UDP DNS to the selected resolver on a protected socket, and constructs the reply packet locally.
- Compose screens: presentation and user consent entry points only; network work stays off the main thread.

## Data and privacy

- Monitoring history stays in the app-private files directory.
- No analytics, account, remote database, or telemetry SDK is included.
- The DNS VPN does not capture general traffic because its route contains only the synthetic DNS address (`10.77.0.2/32`).
- Public-IP lookup necessarily contacts `api64.ipify.org`; connectivity probes contact the providers named in the UI.

## Diagnostic console

`DiagnosticLog` keeps the newest 500 structured lifecycle events in memory and app-private storage. The Console renders them newest-first and exports through the Storage Access Framework. Entries stay private until the user explicitly exports or clears them, and can include public IP and network test outcomes.
