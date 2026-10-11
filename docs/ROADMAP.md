# Product Roadmap

This roadmap records accepted future directions from peer review and project research. It is a planning document, not a promise that every item will ship. Priority can change after device testing, Android platform changes, or privacy review.

## Completed foundations

- Layered external HTTPS and DNS checks.
- Low-volume Fastly service probes.
- Foreground outage monitoring with consecutive-failure confirmation.
- DNS-only local VPN and system Private DNS handoff.
- Persistent in-app diagnostic console and explicit SAF export.
- UI state sourced from the running monitor and DNS services.
- Defensive IPv4 UDP DNS packet parsing with unit coverage for truncation, fragmentation, invalid lengths, and reply addressing.
- Unit tests included in GitHub CI.

## Near term — high priority

### Explain the active connection

- Detect airplane mode before running external probes. Show a direct explanation and avoid needless timeouts. Continuous monitoring should pause probes, update its notification, and write one explanatory console event.
- Display the physical transport underneath a VPN, for example `VPN (underlying: Wi-Fi)` or `VPN (underlying: cellular)`.
- Distinguish this app's DNS-only VPN from an external full-tunnel or split-tunnel VPN where Android exposes enough information.
- Show local interface address, default gateway, private/public address classification, and validation/captive-portal capabilities from `LinkProperties` and `NetworkCapabilities`.
- Explain common Wi-Fi-only states: no SIM with working Wi-Fi, no SIM and no network, or mobile data unavailable while Wi-Fi remains usable.

### Cellular and SIM diagnostics

- Report whether telephony hardware exists and whether a SIM is ready.
- Report whether a cellular network with `INTERNET` and `VALIDATED` capabilities is available.
- Where platform APIs and permissions permit, explain whether mobile data is disabled or disallowed.
- Design dual-SIM output around subscriptions/slots instead of assuming one SIM.
- Minimize sensitive permissions. `READ_PHONE_STATE` or newer equivalents must only be added if capability-only APIs cannot provide the required explanation, with an explicit rationale and denial path.

### Reliability and user guidance

- Handle notification-permission denial explicitly and explain the effect on foreground monitoring.
- Add one or more public-IP fallback providers with strict timeouts, documented privacy impact, and deterministic result selection.
- Replace the monitor's `firstFailure` Boolean with an explicit consecutive-failure state/count before monitor behavior becomes configurable.
- Add a lifecycle reconciliation mechanism only if device tests demonstrate that process/service teardown can leave `ServiceState` stale; do not rely on deprecated running-service enumeration as the default design.

## Mid term

### History and monitoring

- Move session history from append-only CSV to Room before adding filtering and charts.
- Preserve CSV/text export as a user-controlled interoperability format.
- Add configurable monitoring interval, failure threshold, pause/resume, event annotations, metered-network policy, and optional reboot resumption.
- Add a simple outage timeline, success rate, latency distribution, and jitter visualization after the Room schema stabilizes.
- Consider WorkManager for explicitly opted-in periodic lightweight checks; it does not replace the user-visible foreground monitor for continuous observation.

### DNS evolution

- Add IPv6 DNS packet support.
- Add TCP fallback for truncated/oversized DNS replies.
- Evaluate DNS-over-HTTPS only with clear resolver disclosure and without silently expanding the VPN route.
- Show the currently observed resolver/result comparison where Android APIs make this reliable.
- Continue fuzz/edge-case testing of the hand-written packet layer; adopt a library only after license, size, maintenance, and packet-safety review.

### Route and interface diagnostics

- Add a small gateway reachability check using a bounded TCP strategy because unprivileged ICMP is unreliable on Android.
- Show resolved service IPs and whether an address belongs to a known Fastly range.
- Validate Range responses by status and returned byte count.
- Allow advanced users to select a specific available `Network` for a diagnostic run, then bind probes to that network.
- Explore limited route/hop information only where Android permissions and reliability justify it.

## Later / high cost

- Controlled speed tests require metered-network detection, explicit consent, persistent cooldown, size estimates, and provider backoff.
- A root-only hosts module must be a separate opt-in capability with backup, validation, atomic apply, and rollback. It must not alter the normal non-root promise.
- A full SOCKS5 server or per-app proxy router requires a separate threat model, authentication, LAN exposure controls, NDK maintenance, and interaction rules for Android's single VPN slot.
- Bluetooth PAN and USB/RJ-45 Ethernet should initially receive transport identification and basic reachability only. Deeper quality diagnostics remain deferred until real usage justifies them.

## Explicit non-goals for the current phase

- Copying Windows WinDivert, process interception, or SoftEther internals into Android.
- Claiming that a regular Android app can edit system hosts.
- Running high-volume or scheduled tests without consent.
- Requesting phone permissions solely to make the dashboard appear more detailed.
- Treating Bluetooth tethering or USB Ethernet as near-term priorities.

## Entry criteria for roadmap work

Every roadmap feature should define:

1. The user-visible diagnostic question it answers.
2. Android API and device-version behavior.
3. Permissions and denial/degradation behavior.
4. Data destinations and privacy disclosure.
5. Unit or instrumentation coverage and a CI path.
6. Interaction with external VPNs and the DNS-only VPN slot.

