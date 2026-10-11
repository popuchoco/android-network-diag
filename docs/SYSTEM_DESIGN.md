# System Design (SD)

## Goals

1. Distinguish link presence from real external reachability.
2. Produce evidence useful for HiNet troubleshooting without requiring root.
3. Offer reversible DNS selection that is honest about Android limitations.
4. Keep monitoring low-volume and understandable.

## External health decision

A check is considered online when at least one independent HTTPS probe succeeds. DNS is reported separately so a DNS-only failure remains visible. During continuous monitoring, the first all-probe failure is tentative; a second check five seconds later must also fail before an outage row is written.

## HiNet / Fastly decision

The desktop `fastly-hosts-adjust` project can scan Fastly ranges and pin service-specific IPs in `hosts`. A regular Android app cannot reproduce that final privileged step. The MVP therefore measures the currently selected paths and service reachability. A future root-only module may add explicit, reversible hosts management, isolated behind a capability check.

## DNS design

The app offers two routes:

- DNS-only VPN: immediately reversible and works without root. Only the synthetic DNS destination is routed into the TUN interface. UDP payloads are forwarded to the selected resolver using a protected socket.
- Android Private DNS: the app copies the provider's DNS-over-TLS hostname and opens the system screen. Android owns validation and persistence.

Known MVP limits: UDP/IPv4 DNS only, one VPN at a time, no DNS-over-HTTPS inside the app, and no TCP fallback for oversized DNS responses. These limits are shown here rather than hidden behind a misleading switch.

## Reliability and lifecycle

- Long-running checks use a foreground service and visible notification.
- The service is cancelled on stop/destruction.
- CSV writes occur after every confirmed result.
- The next phase should add Room-backed sessions, pause/resume intervals, event annotations, export through Storage Access Framework, and reboot resumption only after explicit opt-in.

## Planned connection model

The current report answers whether the active Android network can reach the public internet. The next connection model will keep separate fields for:

- logical path: direct, this app's DNS-only VPN, or an external VPN;
- observable underlying transport: Wi-Fi, cellular, Ethernet, Bluetooth, or unknown;
- local state: interface address, default gateway, private-address classification, captive portal, and validation;
- cellular context: telephony availability, SIM readiness, and available/validated cellular networks;
- blocking context: airplane mode, missing network, or permission-limited information.

These fields prevent a VPN label from hiding the physical connection and allow Wi-Fi/LAN failure to be distinguished from public-internet failure. See [ROADMAP.md](ROADMAP.md) for staged implementation and privacy constraints.

## Security

- HTTPS is required for all application probes.
- Cleartext application traffic is disabled in the manifest.
- No certificate bypass or custom trust manager is used.
- Third-party binaries under `third-party/` are excluded from Git.
