# Reference Projects

No source code from the projects below is vendored in this repository. They were used to compare features, Android constraints, monitoring behavior, or UI concepts.

| Project | License observed | How it informed this app |
|---|---|---|
| [NetCheckMonitor](https://github.com/ahui3c/NetCheckMonitor) | Source repository unavailable during development; local v0.9.8 output guide supplied by project owner | Multiple independent HTTPS probes, 60-second checks, 5-second retry, consecutive-failure confirmation, CSV evidence |
| [fastly-hosts-adjust](https://github.com/AFAlimru/fastly-hosts-adjust) | MIT | Fastly-backed service list, route-quality framing, reversible configuration principle |
| [AndroidNetworkTools](https://github.com/tejmagar/AndroidNetworkTools) | GPL-2.0 | Network-tool taxonomy only; no code copied |
| [network-checker](https://github.com/mirarr-app/network-checker) | GPL-3.0 | DNS latency and edge-route diagnostic ideas only; no code copied |
| [AndroidNetworkWatcher](https://github.com/kdroidFilter/AndroidNetworkWatcher) | MIT | Real-time transport/connectivity state model |
| [network-traffic-analysis](https://github.com/farazulhoda/network-traffic-analysis) | Check upstream before reuse | DNS traffic-analysis concepts only |
| [Network_Monitoring_Tool](https://github.com/saeed205/Network_Monitoring_Tool) | MIT | Success-rate, latency, and jitter metric definitions |
| [network-traffic-monitor](https://github.com/sam18p/network-traffic-monitor) | No license identified in repository overview; treat as all rights reserved | Threshold/alert concepts only; no code copied |
| [network-monitor](https://github.com/Debatej2299q/network-monitor) | MIT | Mobile dashboard metrics and presentation concepts |
| [keepitup](https://github.com/ibbaa/keepitup) | Apache-2.0 | Foreground monitoring, configurable tasks, failure/recovery notification concepts |
| [PCAPdroid](https://github.com/emanuele-f/PCAPdroid) | GPL (see upstream COPYING) | Mature no-root VPN architecture and local-processing expectations; not embedded |

Before importing any future dependency, pin a version, re-check its exact license at that revision, record notices, and review transitive dependencies.
