# Android Network Diag

Dark-mode Android network diagnostics app for Taiwan users, with three focused areas:

- **External network health** — verifies DNS and real HTTPS reachability instead of trusting the Wi-Fi icon alone.
- **Chunghwa Telecom / HiNet** — checks the current Fastly-backed paths used by GitHub, X images, and Twitch static assets.
- **DNS switching** — switches among Cloudflare, Google, and Quad9 with a local DNS-only `VpnService`, or opens Android Private DNS settings with the correct hostname copied.

## Current MVP

- Manual layered connectivity check using Microsoft, Google, Cloudflare, DNS, public IP, and Android network validation.
- Foreground monitor with a 60-second interval, 5-second first-failure retry, consecutive-failure outage confirmation, and private CSV history.
- Lightweight Fastly service checks without a high-volume speed test.
- DNS-only local VPN forwarder; packets are forwarded to the selected resolver and are not sent to a remote VPN provider.
- In-app diagnostic console with a bounded, persistent event log and user-selected plain-text export.
- Dark mode only, Traditional Chinese UI, Android 8.0+ (`minSdk 26`).

## Important platform limits

Android apps without root cannot safely edit `/system/etc/hosts`. The HiNet section therefore diagnoses and recommends routes; it does not claim to apply desktop-style hosts overrides. The DNS-only mode occupies Android's single VPN slot and cannot run beside another non-root VPN.

## Build

Requirements: JDK 17 and Android SDK 35.

```powershell
./gradlew.bat assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

The **Console** tab records diagnostic lifecycle events, keeps the newest 500 entries in app-private storage, and exports a `.log` file through Android's system document picker. Export only happens after an explicit user action.

## Documents

- [Architecture](docs/ARCHITECTURE.md)
- [System design](docs/SYSTEM_DESIGN.md)
- [Development decisions](docs/DEVELOPMENT_DECISIONS.md)
- [Product roadmap](docs/ROADMAP.md)
- [Reference projects and licenses](THIRD_PARTY_REFERENCES.md)

## License

Apache License 2.0. See [LICENSE](LICENSE).
