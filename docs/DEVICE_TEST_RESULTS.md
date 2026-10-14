# Device Test Results

This file records observed runs, not inferred support. A scenario remains open until its expected result is backed by an exported console log and, where useful, a screenshot.

## 2026-10-14 — Android 17, Wi-Fi

- Build: commit `bc73699` (`fix: save DNS selection safely`).
- DNS screen: opened and changed selection without the earlier activity-state crash.
- VPN consent/start: Google DNS-only VPN started with upstream `8.8.8.8` at 09:10:46; the screen showed Google as active and exposed the stop action.
- External checks before DNS activation: Google and Cloudflare returned HTTP 204; GitHub Raw, X images, and Twitch returned HTTP 206.
- Invalid probe discovered: `www.hinet.net` returned NXDOMAIN even though unrelated DNS and HTTPS traffic worked. Public verification reproduced NXDOMAIN through Cloudflare and Google resolvers, so the app probe was corrected to `hinet.net`.

Not established by this run: consent cancellation, rapid stop/start, occupied VPN slot, network loss while forwarding, process termination, external VPN interaction, cellular-only access, captive portal, dual-SIM behavior, or vendor builds other than the tested device.
