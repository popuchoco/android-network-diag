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
