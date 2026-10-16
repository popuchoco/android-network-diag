# Product Roadmap

This roadmap records accepted future directions from peer review and project research. It is a planning document, not a promise that every item will ship. Priority can change after device testing, Android platform changes, or privacy review.

The differentiating product direction is maintained in [PRODUCT_STRATEGY.md](PRODUCT_STRATEGY.md): evolve from a collection of network tools into a local-first change verifier that explains what changed and whether a corrective action worked.

## Completed foundations

- Layered external HTTPS and DNS checks.
- Low-volume Fastly service probes.
- Foreground outage monitoring with consecutive-failure confirmation.
- DNS-only local VPN and system Private DNS handoff.
- Persistent in-app diagnostic console and explicit SAF export.
- UI state sourced from the running monitor and DNS services.
- Defensive IPv4 UDP DNS packet parsing with unit coverage for truncation, fragmentation, invalid lengths, and reply addressing.
- Unit tests included in GitHub CI.
- Airplane-mode short-circuit for manual and continuous checks.
- Logical VPN versus observable underlying transport, local IP/default gateway, validation/captive-portal state, and permission-free cellular availability.
- Airplane mode with an active Wi-Fi/VPN path continues diagnostics; external VPNs expose their interface and conservatively list visible Tailscale/WireGuard candidates.
- DNS-only VPN startup is blocked with an actionable explanation while another VPN, game accelerator, or relay owns Android's single VPN slot.
- DNS provider cards apply on tap; switching providers no longer requires a separate Apply action or manual stop.
- Notification-permission denial guidance.
- Public-IP fallback with strict timeouts.
- Explicit consecutive-failure counter in continuous monitoring.
- DNS selection state stored as a Bundle-safe provider ID, with VPN consent cancellation handled without starting the service.
- Android 17 field validation on 2026-10-14 confirmed that the DNS screen remained stable and the Google DNS-only VPN started successfully; see [DEVICE_TEST_RESULTS.md](DEVICE_TEST_RESULTS.md). Other matrix cases remain open.

## Near term — high priority

### Change verification foundation

- Define a versioned `DiagnosticSnapshot` containing network identity, transport, capabilities, VPN/tunnel context, DNS observations, public address family, routes, probes, and resolved service addresses.
- Move durable evidence to Room and keep raw observations separate from derived explanations.
- Build a Change Lab MVP: capture a baseline, apply or guide one explicit change, wait for network stabilization, rerun the same probes, and show only meaningful differences.
- Verify DNS-only VPN activation with an actual resolver-path check; success means more than `VpnService.Builder.establish()` returning a descriptor.
- Add a previewable, privacy-redacted Evidence Capsule v1 in text and versioned JSON.

### Route-conflict explanations

- Compare observable private routes, default routes, gateway, DNS, and validation state before and after a VPN/game accelerator/relay change.
- Detect evidence patterns for external internet working while LAN/NAS access disappears, a tunnel taking the only VPN slot, and private-prefix route overlap.
- Express conclusions as observation, inference, recommendation, and confidence; never present a hidden third-party policy as known fact.

### Explain the active connection

- Validate the new airplane/VPN/Wi-Fi/cellular explanations across physical devices and vendor Android builds.
- Improve multi-network selection when multiple non-VPN networks are simultaneously validated.
- Expand Wi-Fi-only explanations if permission-free APIs can distinguish additional causes reliably.

### Cellular and SIM diagnostics

- Report whether telephony hardware exists and whether a SIM is ready.
- Report whether a cellular network with `INTERNET` and `VALIDATED` capabilities is available.
- Where platform APIs and permissions permit, explain whether mobile data is disabled or disallowed.
- Design dual-SIM output around subscriptions/slots instead of assuming one SIM.
- Minimize sensitive permissions. `READ_PHONE_STATE` or newer equivalents must only be added if capability-only APIs cannot provide the required explanation, with an explicit rationale and denial path.

### Reliability and user guidance

- Add a lifecycle reconciliation mechanism only if device tests demonstrate that process/service teardown can leave `ServiceState` stale; do not rely on deprecated running-service enumeration as the default design.
- Run the maintained physical-device matrix for DNS start/stop, permission denial, an occupied VPN slot, rapid restart, network loss, process termination, airplane mode, external VPNs, Wi-Fi-only devices, cellular-only devices, captive portals, and dual-SIM devices. A checklist is not a completed result; record device model, Android version, network context, exported console log, and observed outcome for each run.

## Mid term

### History and monitoring

- Move session history from append-only CSV to Room before adding filtering and charts.
- Preserve CSV/text export as a user-controlled interoperability format.
- Add configurable monitoring interval, failure threshold, pause/resume, event annotations, metered-network policy, and optional reboot resumption.
- Add a simple outage timeline, success rate, latency distribution, and jitter visualization after the Room schema stabilizes.
- Consider WorkManager for explicitly opted-in periodic lightweight checks; it does not replace the user-visible foreground monitor for continuous observation.
- Prefer an event-driven timeline using `NetworkCallback`; trigger bounded probes on meaningful state transitions instead of permanently polling when the network is stable.

### DNS evolution

- Add IPv6 DNS packet support.
- Add TCP fallback for truncated/oversized DNS replies.
- Evaluate DNS-over-HTTPS only with clear resolver disclosure and without silently expanding the VPN route.
- Show the currently observed resolver/result comparison where Android APIs make this reliable.
- Add a Resolver Truth Lab that compares system resolution with explicit resolvers on a selected `Network` before offering a configuration change.
- Continue fuzz/edge-case testing of the hand-written packet layer; adopt a library only after license, size, maintenance, and packet-safety review.

### Route and interface diagnostics

- Add a small gateway reachability check using a bounded TCP strategy because unprivileged ICMP is unreliable on Android.
- Show resolved service IPs and whether an address belongs to a known Fastly range.
- Validate Range responses by status and returned byte count.
- Allow advanced users to select a specific available `Network` for a diagnostic run, then bind probes to that network.
- Add Path A/B to compare the same low-volume probes over Wi-Fi and cellular without changing the device-wide default route; require explicit consent and a data estimate before activating cellular work.
- Explore limited route/hop information only where Android permissions and reliability justify it.

## Later / high cost

- Add IPv6-first/only, NAT64/DNS64, 464XLAT, CGNAT-indicator, and HTTP/3/QUIC fallback evidence to snapshots and explanations after device fixtures exist.
- Add versioned service-diagnostic packs only when endpoints, expected responses, traffic limits, and maintenance ownership are documented.

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
