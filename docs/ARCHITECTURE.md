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

- `NetworkDiagnostics`: uses `ConnectionExplainer`, short-circuits external work in airplane mode, resolves `www.hinet.net`, probes Google and Cloudflare HTTPS 204 endpoints, and retrieves the public IP with a fallback provider. The former Microsoft Windows connectivity endpoint was removed because it was not an Android requirement and produced unrelated CDN certificate failures on some VPN paths.
- `ConnectionExplainer`: separates the logical VPN path from the best observable physical transport and reads local addresses, default gateway, validation/captive-portal capabilities, and permission-free cellular availability.
- `FastlyDiagnostics`: resolves and performs small HTTP range checks against Fastly-backed service domains.
- `NetworkMonitorService`: runs a foreground monitoring loop, retries the first failure after five seconds, confirms an outage only after consecutive failure, and appends private CSV history.
- `DnsVpnService`: captures only traffic addressed to the VPN-provided DNS IP, forwards UDP DNS to the selected resolver on a protected socket, and constructs the reply packet locally. Establishment and forwarding failures are published through `ServiceState`, allowing the visible DNS screen to show an actionable error even though the failure occurs asynchronously inside the service.
- Compose screens: presentation and user consent entry points only; network work stays off the main thread.

## Data and privacy

- Monitoring history stays in the app-private files directory.
- No analytics, account, remote database, or telemetry SDK is included.
- The DNS VPN does not capture general traffic because its route contains only the synthetic DNS address (`10.77.0.2/32`).
- Public-IP lookup contacts `api64.ipify.org`, then `ifconfig.me/ip` only on failure; connectivity probes contact the providers named in the UI.

## Diagnostic console

`DiagnosticLog` keeps the newest 500 structured lifecycle events in memory and app-private storage. The Console renders them newest-first and exports through the Storage Access Framework. Entries stay private until the user explicitly exports or clears them, and can include public IP and network test outcomes.
