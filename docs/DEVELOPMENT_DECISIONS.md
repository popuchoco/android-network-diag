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
