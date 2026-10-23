# G clear

- Visible name `G clear`; package `cn.g.clear`. Keep original signing identity for updates and increment version code for releases.
- Read README before changing matching or lifecycle behavior. All APP protection opt-in; jump return is experimental and separately confirmed.
- Keep screen content ephemeral. Never add network, screenshots, coordinate tapping, broad permissions or hidden automation without an explicit requirement.
- Count only accepted actions; do not describe an accepted click as a confirmed ad removal.
- Keep protected system/home/IME boundaries, bounded scans and node recycling. Core rule changes require false-positive and state-transition checks.
- Build with `sh build.sh`; verify with `sh tests/check.sh` and `sh tests/device-check.sh` in Android Termux. Separate build/runtime checks from installed device UX validation.
- Never commit or archive `tools/`, `build/`, signing keys/passwords, private backups or SDK binaries. Source archives use an explicit allowlist. Scan staged files before GitHub pushes.
