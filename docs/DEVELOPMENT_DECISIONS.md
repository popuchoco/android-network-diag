# Development Decisions

## ADR-001 — Native Kotlin and Jetpack Compose

Chosen for direct access to `ConnectivityManager`, foreground services, and `VpnService`, with a small single-module MVP. Flutter-based references were treated as UX/feature research only.

## ADR-002 — Dark mode only

The product defaults to and ships only a dark theme. The palette uses a near-black green base, elevated charcoal surfaces, and restrained green status/CTA accents. There is no automatic light-mode branch.

## ADR-003 — Verify the internet, not only the network link

`NET_CAPABILITY_VALIDATED` is useful but not sufficient evidence for troubleshooting. The app also performs real HTTPS probes against independent providers and reports DNS separately.

## ADR-004 — No fake hosts modification

Desktop hosts editing requires privileges unavailable to a standard Android app. The MVP exposes Fastly diagnostics and explains the limit. Root support, if added, must be opt-in, backed up, reversible, and isolated from the normal build.

## ADR-005 — DNS-only local VPN

A local VPN gives reversible per-device DNS selection without root. The route is deliberately limited to one synthetic DNS IP rather than `0.0.0.0/0`, reducing scope and privacy risk. The system Private DNS route remains available for users who already use another VPN.

## ADR-006 — Conservative open-source reuse

Apache-2.0 and MIT projects informed the design. GPL projects were studied but no source was copied into this Apache-2.0 repository. PCAPdroid is not embedded; its documented external API is a possible future integration.

## ADR-007 — Low-volume checks first

The MVP uses small requests and range reads. High-volume scheduled speed tests need metered-network detection, a persistent cooldown, explicit consent, and provider backoff before implementation.

## ADR-008 — Reference projects as capability research

`AndroidProxy` is the relevant Android reference. Its cellular `Network` binding, service-owned status model, Happy Eyeballs behavior, and step-by-step self-test are suitable future features after they are rewritten behind this app's own interfaces and tests. Embedding its full SOCKS5 server is not part of the diagnostics MVP and would add NDK, background-service, authentication, and LAN exposure risks.

`NetRedirector` is a Windows/WinDivert product. Its rule vocabulary, proxy health checks, failover concepts, and monitor UX are useful product references, but its packet interception, per-process routing, SoftEther, and driver implementation cannot be integrated into a regular Android app. Equivalent Android traffic routing would require a separate full-tunnel `VpnService` architecture and would conflict with the existing DNS-only VPN slot.

Both projects were MIT at the revisions reviewed on 2026-10-11. Any copied code still requires retained copyright/license notices and a focused security review.

## ADR-009 — Bounded local diagnostic console

Operational events use a small structured logger rather than capturing unrestricted Logcat. This avoids requiring privileged log access and reduces accidental collection of unrelated app data. The newest 500 entries persist privately; export uses Android's document picker so the destination and timing remain user-controlled.

## ADR-010 — Roadmap is capability-led, not protocol-led

Future work is ordered by the user question it can answer. Connection explainability comes before adding more protocols: airplane mode, underlying transport, local address/gateway, VPN context, and Wi-Fi/SIM combinations are higher priority than DoH, traceroute, charts, or proxy serving. The maintained scope and entry criteria live in [ROADMAP.md](ROADMAP.md).

## ADR-011 — Telephony detail must use minimum permissions

SIM and cellular diagnostics are accepted future features, but they must begin with `ConnectivityManager`, `NetworkCapabilities`, device feature detection, and non-sensitive SIM readiness APIs. Phone-state permissions are not assumed. If a useful state requires a runtime phone permission, the implementation must document why, handle denial without breaking Wi-Fi diagnostics, account for dual-SIM devices, and avoid exposing identifiers.

## ADR-012 — VPN diagnostics separate logical and physical transport

Reporting only `TRANSPORT_VPN` hides whether the device actually depends on Wi-Fi or cellular service. The connection model now reports the logical path and best observable underlying non-VPN transport separately. This app's DNS-only VPN is labeled distinctly from an external VPN, but the UI does not claim a full/split-tunnel classification when Android does not provide enough evidence.

An enabled airplane-mode setting is context, not proof of disconnection: Android permits Wi-Fi to be switched back on while airplane mode remains enabled. External probes are skipped only when there is no active network. Android's VPN transport also covers products marketed as game accelerators, per-app route optimizers, and relays. The app therefore uses the neutral `VPN／加速器／跳板` label, reports the interface, and lists known installed clients (currently Tailscale and WireGuard) only as candidates. It does not claim which candidate owns the active tunnel because Android clears another app's VPN owner UID for privacy. App-private HTTP/SOCKS relays that do not register a system VPN or proxy are outside what another ordinary app can reliably observe.

## ADR-013 — Local-network checks remain bounded

Local IP, gateway, private-address classification, captive-portal state, and a bounded gateway reachability check are accepted. Broad LAN scanning, device discovery, or unrestricted port scanning are not. Android ICMP limitations mean gateway reachability should use a small, time-bounded method and must be presented as evidence rather than proof that the gateway is healthy.

## ADR-014 — Tests follow protocol risk

Hand-written packet parsing requires focused unit tests and CI execution. The current IPv4 UDP DNS parser covers truncation, fragmentation, invalid UDP lengths, and reply address/port construction. IPv6, TCP fallback, checksum vectors, and fuzz cases must arrive with their own tests. Replacing the parser with a dependency still requires license, maintenance, size, and security review.

## ADR-015 — Deferred transports and expensive features

Bluetooth PAN and USB/RJ-45 Ethernet are recorded future transports but are not near-term priorities. Room history, configurable monitoring, charts, DoH, IPv6 DNS, network selection, WorkManager scheduling, controlled speed tests, root hosts management, and SOCKS5/proxy routing remain staged work with the prerequisites in the roadmap. Recording an item does not authorize background data use, new sensitive permissions, root operations, or a full-tunnel VPN.

## ADR-016 — DNS service failures must reach the visible UI

Starting a foreground service is asynchronous: a successful `startForegroundService` call does not prove that `VpnService.Builder.establish()` or the forwarding loop succeeded. The DNS service therefore owns failure detection and cleanup, publishes a transient user-facing error through `ServiceState`, and writes technical detail to the diagnostic console. The DNS screen consumes only the matching error value so an older UI callback cannot erase a newer failure. Normal stop closes the current descriptor first; a retired forwarding worker must not clear or stop a newer VPN session.

## ADR-017 — Save stable identifiers across Android activity recreation

Compose `rememberSaveable` state must contain Bundle-compatible values. The DNS screen stores only the selected provider ID and resolves the current provider from the maintained catalog; it does not attempt to save the custom `DnsProvider` object. This is required because opening Android's VPN consent activity can trigger state saving before the result returns. Consent results are also authoritative: cancellation records a diagnostic event and never starts the VPN service.

## ADR-018 — DNS probes require a currently resolvable target

A DNS probe must not interpret NXDOMAIN for the chosen test name as failure of the device's resolver. On 2026-10-14, both Cloudflare and Google public resolvers returned NXDOMAIN for `www.hinet.net`, while the registrable `hinet.net` resolved normally. The diagnostic target was therefore changed to `hinet.net`. Endpoint validity must be rechecked when field evidence shows DNS failure alongside successful resolution and HTTPS access to unrelated hosts.

## ADR-019 — DNS VPN conflict is explicit

Android permits only one active `VpnService`. The in-app DNS switch therefore cannot coexist with Tailscale, WireGuard, a consumer VPN, or a game accelerator/relay implemented with `VpnService`. The DNS screen detects an active external tunnel before requesting consent, explains why the operation cannot be applied, and keeps system Private DNS as the alternative. Private DNS may still be overridden by the tunnel provider's own DNS policy, so the app does not promise that changing it supersedes MagicDNS or another tunnel DNS configuration.

## ADR-020 — DNS provider cards apply immediately

Choosing a DNS provider is the primary action, so provider cards are actionable controls rather than radio buttons followed by a separate Apply button. A tap selects and applies the provider. Android VPN consent appears only when required; once this app's DNS VPN is active, tapping another provider replaces the upstream without a manual stop. The destructive/reversing action remains a separate Stop button so status and intent are unambiguous.

## ADR-021 — The product is a change verifier, not a tool grid

The product's durable differentiation is evidence-backed before/after diagnosis. New protocol engines are accepted when they strengthen a user flow that captures a baseline, observes one explicit change, repeats comparable probes, and explains whether the result improved. Raw observations and derived inferences must remain separate, with confidence and evidence visible. The home experience should prioritize current state and the next useful action instead of accumulating independent utility buttons. The complete product thesis, staged epics, non-goals, and success metrics live in [PRODUCT_STRATEGY.md](PRODUCT_STRATEGY.md).
