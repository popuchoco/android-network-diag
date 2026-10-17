# Architecture

## Modules

```text
Compose UI
 ├─ External health screen ─ MainViewModel ─ NetworkDiagnostics
│  └─ Change Lab ─ SnapshotFactory/Comparator ─ Room ─ Evidence Capsule
 ├─ HiNet screen ────────── MainViewModel ─ FastlyDiagnostics
 └─ DNS screen ──────────────────────────── DnsVpnService

Foreground execution
 └─ NetworkMonitorService ─ NetworkDiagnostics ─ private CSV history
```

The MVP uses one Android application module and keeps protocol engines separate from Compose. This makes the UI replaceable and allows Room, WorkManager, export, or a more complete VPN stack to be added without rewriting screens.

## Responsibilities

- `NetworkDiagnostics`: uses `ConnectionExplainer`, short-circuits external work only when airplane mode has left no active network, resolves `hinet.net`, probes Google and Cloudflare HTTPS 204 endpoints, and retrieves the public IP with a fallback provider. Airplane mode with Wi-Fi or a VPN still runs the checks. The former `www.hinet.net` target was removed after authoritative public resolvers returned NXDOMAIN; a deliberately nonexistent name is not evidence of a broken DNS path. The former Microsoft Windows connectivity endpoint was removed because it was not an Android requirement and produced unrelated CDN certificate failures on some VPN paths.
- `ConnectionExplainer`: separates an Android network tunnel from the best observable physical transport and reads local addresses, default gateway, validation/captive-portal capabilities, and permission-free cellular availability. A tunnel may be marketed as a VPN, game accelerator, per-app route optimizer, or relay. The app reports the interface and visible installed Tailscale/WireGuard candidates, but Android deliberately withholds another VPN app's owner identity, so candidates are never presented as certain attribution.
- `FastlyDiagnostics`: resolves and performs small HTTP range checks against Fastly-backed service domains.
- `NetworkMonitorService`: runs a foreground monitoring loop, retries the first failure after five seconds, confirms an outage only after consecutive failure, and appends private CSV history.
- `DnsVpnService`: captures only traffic addressed to the VPN-provided DNS IP, forwards UDP DNS to the selected resolver on a protected socket, and constructs the reply packet locally. Establishment and forwarding failures are published through `ServiceState`, allowing the visible DNS screen to show an actionable error even though the failure occurs asynchronously inside the service.
- The DNS provider cards are direct actions rather than a two-step selection form. The first tap may require Android VPN consent; later taps replace the upstream in place, while stopping DNS remains a separate secondary action.
- Compose screens: presentation and user consent entry points only; network work stays off the main thread.
- `NetworkStabilizer`: samples the active network, transports, validation, and captive-portal capabilities until the observable signature repeats or a four-second bound expires. Change Lab records the actual wait duration and then runs comparable probes; it never waits indefinitely for a network to become healthy.
- `DiagnosticSnapshot`: versioned raw observations from the health and Fastly engines, including an optional local annotation and stabilization duration. `SnapshotComparator` derives before/after changes without mutating observations. Room retains the newest 100 snapshots and restores only records explicitly labeled as baselines.
- `SnapshotCodec`: maps Room entities and creates versioned JSON and readable-text Evidence Capsules. Address fields are redacted by default and included only after the user opts in from the export preview.

## Data and privacy

- Monitoring history stays in the app-private files directory.
- No analytics, account, remote database, or telemetry SDK is included.
- The DNS VPN does not capture general traffic because its route contains only the synthetic DNS address (`10.77.0.2/32`).
- Public-IP lookup contacts `api64.ipify.org`, then `ifconfig.me/ip` only on failure; connectivity probes contact the providers named in the UI.
- Change Lab snapshots remain in the app-private Room database. Evidence Capsules use the Android document picker and are never uploaded automatically.

## Diagnostic console

`DiagnosticLog` keeps the newest 500 structured lifecycle events in memory and app-private storage. The Console renders them newest-first and exports through the Storage Access Framework. Entries stay private until the user explicitly exports or clears them, and can include public IP and network test outcomes.
