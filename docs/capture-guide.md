# Capturing DJI Mimo live view (Android + PCAPdroid)

Goal: record the WiFi traffic DJI Mimo sends when it turns on live view, so we
can isolate the "start live view" command and the video stream. We already have
the BLE pairing and camera Wi-Fi join — **live-view enable and the video stream
are what PCAPdroid is for**.

## One-time setup (Android phone)

1. Install **DJI Mimo** and **PCAPdroid** (Play Store or F-Droid).
2. In PCAPdroid:
   - **Target app → DJI Mimo only.** This drops all the noise so the diff is clean.
   - **Dump PCAP → to file** (Settings → "PCAP dump"). Leave capture mode at the
     default root-less (VPN) mode.

## Capture the controlled sequence

The diff only works if we have a "before" and an "after," so narrate a fixed script:

1. Start the PCAPdroid capture, then open Mimo and connect to the Pocket as usual
   (it joins the camera's WiFi AP at `192.168.2.1`).
2. Sit on a **non-live-view** screen for ~10 s. This is the *idle* baseline.
3. Enter **live view** and leave it running ~10 s.
4. Exit live view, ~5 s.
5. Stop the capture. Export the `.pcap`.

Do it **2–3 times** — repeats cancel out unrelated chatter (heartbeats, telemetry).

Best of all: capture the idle stretch and the live-view stretch as **two separate
files** (`idle-1.pcap`, `live-1.pcap`). Then the diff below is trivial. If that's
awkward, one combined file per run is fine too — note roughly when you hit live view.

## Hand it to the tool

Drop the files in `captures/` (git-ignored) and run:

```bash
pip install scapy          # one time
python3 tools/duml_parse.py captures/live-1.pcap            # list everything
python3 tools/duml_parse.py --diff captures/idle-1.pcap captures/live-1.pcap
```

The `--diff` output has two sections that are what we're hunting:

- **Candidate enable commands** — DUML `set/cmd` pairs seen only during live view.
  One of these is the "start live view" command.
- **Candidate stream flows** — UDP/TCP endpoints seen only during live view. The
  fat one (most bytes) is the H.264/H.265 video stream — note its **port**.

The `--diff` output is the input for decoding the enable command payload and the
stream's packetization/codec. Keep the `.pcap`s in `captures/` — never commit them.

## Notes

- **Passphrase warning:** captures can contain your camera's WiFi password. `captures/`
  is git-ignored on purpose — don't commit raw pcaps.
- **BLE, if we ever need it:** Android → Developer Options → *Enable Bluetooth HCI
  snoop log*, reproduce, then pull `btsnoop_hci.log`. We shouldn't need this — the
  BLE handshake is already known — but it's here for completeness.
- **Sanity check the tool first:** `python3 tools/duml_parse.py --selftest` (no pcap,
  no scapy needed) confirms the DUML frame scanner is working.
