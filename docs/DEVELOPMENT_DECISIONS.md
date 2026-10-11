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

Reporting only `TRANSPORT_VPN` hides whether the device actually depends on Wi-Fi or cellular service. A future connection model will report the logical path and best observable underlying non-VPN transport separately. This app's DNS-only VPN should be labeled distinctly from an external VPN, but the UI must not claim a full/split-tunnel classification when Android does not provide enough evidence.

## ADR-013 — Local-network checks remain bounded

Local IP, gateway, private-address classification, captive-portal state, and a bounded gateway reachability check are accepted. Broad LAN scanning, device discovery, or unrestricted port scanning are not. Android ICMP limitations mean gateway reachability should use a small, time-bounded method and must be presented as evidence rather than proof that the gateway is healthy.

## ADR-014 — Tests follow protocol risk

Hand-written packet parsing requires focused unit tests and CI execution. The current IPv4 UDP DNS parser covers truncation, fragmentation, invalid UDP lengths, and reply address/port construction. IPv6, TCP fallback, checksum vectors, and fuzz cases must arrive with their own tests. Replacing the parser with a dependency still requires license, maintenance, size, and security review.

## ADR-015 — Deferred transports and expensive features

Bluetooth PAN and USB/RJ-45 Ethernet are recorded future transports but are not near-term priorities. Room history, configurable monitoring, charts, DoH, IPv6 DNS, network selection, WorkManager scheduling, controlled speed tests, root hosts management, and SOCKS5/proxy routing remain staged work with the prerequisites in the roadmap. Recording an item does not authorize background data use, new sensitive permissions, root operations, or a full-tunnel VPN.
