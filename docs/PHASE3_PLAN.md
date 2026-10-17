# Phase 3 Implementation Plan

Phase 3 answers a harder question than basic reachability: **which resolver or network path produced the observed result, and why did two paths behave differently?** It must preserve the product's local-first, low-volume, evidence-backed design.

## Delivery order

### P3.0 — Shared network-bound probe foundation

User question: “Was this test actually sent over the path named in the result?”

- Represent an available Android `Network` with a transient handle, transport labels, validation, metered state, VPN context, and address families. Never persist Android network handles.
- Add bounded DNS and HTTPS executors that use `Network.getAllByName`, `Network.openConnection`, or sockets bound to the selected `Network`.
- Record timeout, byte budget, selected path, timestamps, and degradation reason in a reusable result envelope.
- When an external VPN prevents a truthful Wi-Fi/cellular distinction, say so and disable comparison instead of silently testing the default tunnel.

Acceptance: unit-tested classification/result mapping; instrumentation evidence on Wi-Fi, cellular, and external VPN; no new sensitive permission.

### P3.1 — Resolver Truth Lab MVP

User question: “Is the system resolver behaving differently from a resolver I explicitly chose?”

- Let the user enter or select a safe test hostname; reject IP literals and malformed names.
- Compare system resolution on the selected network with explicit Cloudflare, Google, and Quad9 UDP queries.
- Report answer set, response class, address family, latency, timeout, NXDOMAIN, and truncation separately.
- Present observations first, then an inference with confidence. Do not claim DNS hijacking from a single mismatch.
- Run only after an explicit tap; store locally; export through the existing Evidence Capsule privacy preview.

Acceptance: deterministic DNS response parser fixtures, timeout/NXDOMAIN/truncated coverage, selected-network device validation, and a traffic disclosure in the UI.

### P3.2 — Address-family and translation evidence

User question: “Does this path support IPv4, IPv6, or an Android translation mechanism?”

- Extend snapshots with local/global IPv4 and IPv6 availability, DNS A/AAAA results, and observable NAT64/DNS64/464XLAT indicators.
- Treat CGNAT as an indicator only by comparing private/shared local address ranges with the observed public address; do not claim carrier topology as fact.
- Add fixtures before shipping IPv6-only or NAT64 conclusions.

Acceptance: RFC range tests, IPv4-only/dual-stack/IPv6-only fixture coverage, and conservative user-facing wording.

### P3.3 — Path A/B preview, then controlled release

User question: “Does the same small test work better over Wi-Fi or cellular?”

- Require simultaneously available validated Wi-Fi and cellular networks.
- Show the estimated request count and byte ceiling before consent; never change the device-wide default route.
- Run identical bounded DNS/HTTPS probes through network-bound APIs and show side-by-side success, latency, address family, and resolver differences.
- Disable when Android cannot provide two independently testable paths, when airplane mode excludes cellular, or when a full-tunnel VPN obscures truthful attribution.
- Respect metered status and add a persistent cooldown before any broader service pack is considered.

Acceptance: explicit consent, byte accounting, cancellation, device tests on at least two Android versions, and no background execution.

## Cross-cutting rules

- No `READ_PHONE_STATE`, location, root, accessibility, or installed-app enumeration is required for Phase 3 MVP.
- Raw observations, inference, recommendation, and confidence remain separate fields.
- Test endpoints and resolver ownership must be visible before execution.
- Results remain app-private until the system document picker is used.
- External VPN, DNS-only VPN, captive portal, metered network, and airplane-mode degradation paths are first-class test cases.

## First implementation slice

The next code slice is P3.0: introduce the transient available-network model and network-bound DNS executor without exposing a premature UI. Resolver Truth Lab UI begins only after those results can prove which `Network` carried each probe.
