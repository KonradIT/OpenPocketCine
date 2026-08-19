# Mimo settings (Pocket 4 Pro) — labeled GET/SET

Quit **OpenPocketCine**. Use **Mimo only**, live view up, phone USB to this Mac (`rvi0`).
In-session controls are **UDP `:9004`**, not BLE.

Captures from 2026-08-14 live sessions (`/tmp/mimo-*.pcapng`). App → camera `rcv=0x01`, flags `0x40`. ACK is the same opcode, flags `0xC0`, payload `00` unless noted.

Most controls have **no GET opcode**. Mimo writes, then reads a subscribe push. Exceptions with a real GET: audio channel (`0x8E`), Vocal Boost (`0x8E`), gimbal `0x04/0x50` (tilt lock param `04` + rotational speed param `05`).

Do **not** invent more `0xE1` values. Photo is `0x02/0xE1 [17]`. Do **not** 1 Hz enable.

## How to capture the next control

`rvi0` is usually already up (`rvictl -s <iphone-udid>`).

```bash
tcpdump -i rvi0 -w /tmp/mimo-<control>.pcapng 'udp port 9004 or tcp port 7001'
```

Say the control and the values you will hit. Pause ~1 s on each. Then decode with `tools/mimo_settings.py` or a focused `0x02` SET filter.

---

## Exposure mode — `0x02/0x1E`

| | payload |
|---|---|
| **SET Auto** | `01 00` |
| **SET Manual** | `04 00` |
| **GET** | none |

Read: `cam_expo_param` `@7` (`01` auto / `04` manual).

## Shutter — `0x02/0x28`

7-byte SET: `01` + u16-LE `(denom \| 0x8000)` + `00 00 00 40`.

| Example | payload |
|---|---|
| 1/4 | `01 04 80 00 00 00 40` |
| 1/50 | `01 32 80 00 00 00 40` |
| 1/1600 | `01 40 86 00 00 00 40` |
| 1/16000 | `01 80 BE 00 00 00 40` |

Sweep 1/4 … 1/16000 all ACK `00`. **GET:** none.

Read: `cam_expo_param` `@2–3` = `denom \| 0x8000` (not `@16` — that field is ISO).

## ISO — `0x02/0x2A`

1-byte **index**, not the ISO number. **GET:** none.

| Index | ISO |
|---|---|
| `00` | Auto (`@16` floats) |
| `03` | 100 |
| `04` | 200 |
| `05` | 400 |
| `06` | 800 |
| `07` | 1600 |
| `08` | 3200 |
| `09` | 6400 |
| `0A` | 12800 |
| `0B` | 25600 |

Color mode only changes which indices Mimo offers:

- **D-Log2:** `03`–`08` (100–3200)
- **D-Log:** `00`, `05`–`09` (Auto, 400–6400)
- **Normal / HDR:** `00`, `03`–`0B` (Auto, 100–25600)

Read: `cam_expo_param` `@5` = index, `@16` u16-LE = ISO number. **`@13` is not ISO** (stayed 200 through the ISO sweep).

## Color mode — `0x02/0x42`

| Mode | payload |
|---|---|
| Normal | `3F` |
| HDR | `3C` |
| D-Log | `17` |
| D-Log2 | `41` |

**GET:** none. Read: `cam_image_effect` `@2` (same byte).

## Focus mode — `0x02/0x24`

| Mode | SET | `cam_lens_state` `@0` |
|---|---|---|
| Single | `01` | `B1` |
| Continuous | `02` | `B2` |

**GET:** none.

## AF-C track — `0x02/0x8E` pid `0x003B`

`/tmp/mimo-afc-options-20260818.pcapng` (2026-08-18). Video FOCUS submenu under Continuous. Independent of `0x24` — the value persists while AF-S. Mimo polls GET ~1 Hz. SET ACK is the usual `0x8E` `00`.

| | bytes |
|---|---|
| **GET** | `00 01 3B 00` |
| **GET reply** | `00 00 01 3B 00 02 01 <mode>` |
| **SET** | `01 01 3B 00 02 01 <mode>` |

| Mode | `<mode>` |
|---|---|
| Default | `00` |
| Product Showcase | `01` |
| Subject Lock Tracking | `02` |
| Registered Subject Priority | `03` |

Walk (started on Default): Showcase `01` → Lock `02` → Priority `03` → Default `00` → Single (`0x24 01`) → Continuous (`0x24 02`, 3B left at `00`). Do not invent other `0x3B` values.

## White balance — `0x02/0x2C`

5 bytes: `[mode][kelvin/100 u16-LE][tint i16-LE]`.

| | payload |
|---|---|
| Auto | `00 00 00 00 00` |
| Custom 3000 K tint 0 | `06 1E 00 00 00` |
| Custom 2000 K tint −5 | `06 14 00 FB FF` |
| Custom 10000 K tint 100 | `06 64 00 64 00` |
| Custom 10000 K tint −100 | `06 64 00 9C FF` |

Kelvin 2000–10000 in 100 K steps. Tint −100…+100. **GET:** none.

Read: `cam_image_effect` `@4` = mode, `@5–6` = Kelvin/100, `@7–8` = tint. `cam_expo_param` does not carry WB.

## Audio channel — `0x02/0x8E` pid `0x0020`

This one **has a GET**. Mimo polls ~1 Hz.

| Mode | value |
|---|---|
| Stereo | `02` |
| Mono | `01` |
| Spatial Audio | `03` |

| | bytes |
|---|---|
| **GET** | `00 01 20 00` |
| **GET reply** | `00 00 01 20 00 01 <value>` |
| **SET** | `01 01 20 00 01 <value>` |
| **SET ACK** | `00` |

`cam_audio_status_v2` is meters, not the channel.

## Wind noise reduction — `0x02/0x9F` / GET `0x02/0xA0`

Not a one-byte toggle. GET `0xA0` (empty) → `00` + 26-byte blob. SET `0x9F` = that blob. ACK `00`.

On/Off in the labeled take is blob **`@2`**: `1A` On, `18` Off. Blob `@0` may rewrite (`C0` → `80`) after the first SET — do not invent the rest. GET `0xA0`, change `@2`, SET `0x9F`.

## Directional audio — same `0x9F` / `0xA0` blob

Same GET/SET as wind NR. Direction lives in blob **`@2`** (shares the byte with wind). Started on **All**.

| Mode | `@2` | SET (rest of blob unchanged in this take) |
|---|---|---|
| All | `DA` | `C0 04 DA 05 00…` |
| Front | `3A` | `80 04 3A 05 00…` |
| Front and back | `BA` | `80 04 BA 05 00…` |

GET `0xA0` echoed the same `@2` after each SET. Extra `0x9F` writes only flipped blob `@0` (`C0`/`80`) — ignore those.

Also in that capture: `0x8E` pid `0x004C` flipped `00`↔`01` — that is **Vocal Boost**, labeled below.

## Vocal Boost — `0x02/0x8E` pid `0x004C`

Defaults **Off**. Has a GET (Mimo polls it).

| State | value |
|---|---|
| Off | `00` |
| On | `01` |

| | bytes |
|---|---|
| **GET** | `00 01 4C 00` |
| **GET reply** | `00 00 01 4C 00 01 <value>` |
| **SET** | `01 01 4C 00 01 <value>` |
| **SET ACK** | `00` |

---

## Timecode — subscribe `timecode_info` (no GET)

`0x00/0x99` push, 8 bytes, ~1 Hz. **No GET.** SET (source / reset) not captured.

| Offset | Field |
|---|---|
| `@0–2` | `00` |
| `@3` | HH |
| `@4` | MM |
| `@5` | SS (increments) |
| `@6` | FF |
| `@7` | `00` |

Mimo 2026-08-14 23:58: `00 00 00 05 16 2F 12 00` → **05:22:47:18**. OpenPocketCine used to decode `@4–7` and showed `22:47:18:00`.

## Gimbal flip (front ↔ selfie) — `0x04/0x4C` `FE 09`

`0x04/0x4C` is **multi-purpose**, not flip-only (Follow / Tilt Locked / FPV use `02 08` / `01 08` — see gimbal modes; Mimo recenter-gimbal button is `FE 08`). Flip is a toggle, not an enum. Same SET both ways. Started **front**. OPC sends this on stick triple-tap (double-tap waits one window so a third tap can win).

| | |
|---|---|
| **SET** | `0x04/0x4C` payload `FE 09` |
| **ACK** | flags `0x80` (gimbal, not `0xC0`) payload `00` |
| **GET** | none |

Read: unsolicited `0x04/0x27` — `@2` bit `0x40` set = selfie, clear = front.

| Face | `0x04/0x27` |
|---|---|
| Front | `00 80 00 00 00` |
| Selfie | `00 80 40 00 00` |

`cam_smart_gimbal_mode` stayed `01 00` through the flip.

## Gimbal joystick — `0x04/0x01`

Mimo on-screen stick. **298 writes, 0 ACKs.** Flags `0x00` (not `0x40`). No GET. Sent only while the stick is held (~stream).

10 bytes: `[axis0 u16-LE] 00 00 [axis1 u16-LE] 00 80 22 00`

| | |
|---|---|
| Center | `0x0400` (1024) |
| Range | `0x01DA`…`0x0626` (474…1574) = **1024 ± 550** |
| Trailer | always `00 80 22 00` |

Example rest-ish: `00 04 00 00 00 04 00 80 22 00`. Axis0 and axis1 both reached min and max independently (two stick axes). Pan vs tilt not labeled — don’t guess.

Position echo is the existing `0x04/0x05` telemetry, not a GET.

## Gimbal rotational speed — `0x04/0x50`

This one **has a GET**. Mimo polls `01 04 05` ~1 Hz (easy to mistake for a heartbeat). That GET is **params `04` and `05` together**, not speed alone. Started **Default**. Path: Default → Fast → Slow → Default.

| Speed | param `05` |
|---|---|
| Fast | `00` |
| Default | `01` |
| Slow | `02` |

| | bytes |
|---|---|
| **GET** | `01 04 05` (params `04` + `05`) |
| **GET reply** | `00 01 04 01 <tilt> 05 01 <speed>` |
| **SET speed** | `00 05 01 <speed>` |
| **SET ACK** | flags `0x80` (gimbal, not `0xC0`) payload `00 00` |

`<tilt>` is param `04` (tilt lock) — not a fixed `00`. In the speed take it stayed `00` (Follow). See gimbal modes.

`0x04/0x27` did not change.

## Gimbal modes — `0x04/0x4C` + `0x04/0x50` param `04`

Started **Follow**. Path: Follow → Tilt Locked → FPV → Follow.

`0x04/0x4C` sets the Follow / FPV family. Tilt lock is a separate `0x50` param `04`. Mimo sends `0x4C` twice around the `0x50` write for Follow / Tilt Locked.

| UI mode | `0x04/0x4C` | `0x50` SET param `04` |
|---|---|---|
| Follow | `02 08` | `00 04 01 00` (tilt unlocked) |
| Tilt Locked | `02 08` | `00 04 01 01` (tilt locked) |
| FPV | `01 08` | none (this take) |

| | bytes |
|---|---|
| **GET** | same `0x50` poll `01 04 05` |
| **GET reply** | `00 01 04 01 <tilt> 05 01 <speed>` |
| **`0x4C` ACK** | flags `0x80` payload `00` |
| **`0x50` SET ACK** | flags `0x80` payload `00 00` |

GET reply in this take (speed stayed Default `01`):

| After | reply |
|---|---|
| Follow | `00 01 04 01 00 05 01 01` |
| Tilt Locked | `00 01 04 01 01 05 01 01` |
| FPV | `00 01 04 01 01 05 01 01` (param `04` leftover `01`) |
| Follow again | `00 01 04 01 00 05 01 01` |

During FPV, param `04` stayed `01` from Tilt Locked. **`0x50` alone cannot tell FPV from Tilt Locked.**

`cam_smart_gimbal_mode` stayed `01 00`. `0x04/0x27` stayed `00 80 00 00 00`. After leaving FPV, Mimo also sent `0x03/0xDA` `05 FF FF FF FF` (same seq retried) — accompanying, not the mode SET.

Flip remains `0x4C` `FE 09` (earlier take). Recenter is captured `FE 08` (below). Do not invent other `0x4C` first bytes.

## Gimbal recenter (Mimo button) — `0x04/0x4C` `FE 08`

`captures/mimo-gimbal-recenter-20260819.pcapng` (2026-08-19). Mimo live. Mimo has **no** on-screen stick double-tap; this take is the **recenter gimbal** button (three presses after off-center stick bursts). Hardware joystick double-press is local. OPC uses this write for stick double-tap. Each press was a single `0x04/0x4C` write after a stick burst, then ACK.

| Burst | stick pkts | SET pkt | payload |
|---|---|---|---|
| 1 | 2526–2757 | 3859 | `FE 08` |
| 2 | 5527–6790 | 7678 | `FE 08` |
| 3 | 9793–10568 | 11497 | `FE 08` |

| | |
|---|---|
| **SET** | `0x04/0x4C` payload `FE 08` (`rcv=0x04`, flags `0x40`) |
| **ACK** | flags `0x80` payload `00` |
| **GET** | none |

No other SET in the ±30-packet window. `0x03/0xDA` `05 FF FF FF FF` appears at session register (seq retried) and again near teardown — not on the double-tap. `0x04/0x50` `01 04 05` is the usual ~1 Hz poll. Flip stays `FE 09`.

## Tap to focus — `0x22` / `0x30` / `0x68` / `0x32`

`/tmp/mimo-tap-focus-20260818.pcapng` (2026-08-18). Seven taps: three in AF-S, then `0x24` `02` (AF-C), then four more. **AF-S and AF-C use the same four writes.** Each ACK is `00`. Normalized 0…1, float32 LE.

| Order | opcode | payload |
|---|---|---|
| 1 | `0x02/0x22` | `02` |
| 2 | `0x02/0x30` | `x y` + 13× `00` (21 B). 20 B → ACK `E3` |
| 3 | `0x02/0x68` | `08` |
| 4 | `0x02/0x32` | `00 02 01 00` + `x y` + 8× `00` |

Mimo pipelines `0x22` then `0x30` before the first ACK, then `0x68`, then `0x32` after `0x68` ACKs. Example: tap (0.772, 0.483) → `0x30` `87 B5 45 3F 2B 14 F7 3E 00…`.

Classic DUML names (o-gs `dji-dumlv1-camera.lua`): `0x22` = AE Meter Set (spot), `0x30` = Focus Region Set, `0x32` = AE Meter Region Set, `0x68` = AE Lock Status Set. **`0x68` is not App Glamour** — that is `0x8E` pid `0x0039` (below). OPC must send the full burst — `0x30`+`0x32` alone times out with no ACK.

## App Glamour Effects — `0x02/0x8E` pid `0x0039`

`/tmp/mimo-glamour-20260818.pcapng` (2026-08-18). Mimo sparkle sheet: **None / Smooth / Brighten / Slim / Eyes** + strength slider. **Not** `0x02/0x68`. Mimo polls GET ~1 Hz. 34 SETs in the labeled take; ACK `00`. Pid `0x003B` is also polled; no SETs — do not invent it.

| | bytes |
|---|---|
| **GET** | `00 01 39 00` |
| **GET reply** | `00 00 01 39 00 3E` + 62-byte blob |
| **SET** | `01 01 39 00 3E` + 62-byte blob |
| **SET ACK** | `00` |

Blob (`len = 0x3E` = 62):

| Offset | |
|---|---|
| `@0–3` | `0F 00 00 00` (u32-LE 15) |
| `@4` | `01` |
| **`@5` enable** | **`00` None / Off, `01` any effect on** |
| `@6…` | tagged strengths: `tag:u16-LE` + `01` + `u8` (0…~100) |

Tags seen moving with the slider (order of first change): `01`, `02`, `03`, `04`, `0C`, `05`, `06`, `07`, `0A`, `0B`, `0E`. Tags `08` / `09` / `0D` stayed `00`. Do not invent per-tag names — the sheet has four looks plus overflow sliders.

None writes the same blob with **`@5 = 00`** (strengths remembered). OPC must **never SET pid `0x0039` except to force Off** (GET, then SET with `@5 = 00`). On first live picture OPC GETs and, if enabled, SETs Off so a leftover Mimo look cannot stick.

Example Off GET (pkt#442): enable `@5 = 00`, tag `01` = 20, `02` = 25, `03` = 70, `04`/`05`/`06` = 50, `0A` = 20, others 0.

## Tracking box — `0x02/0xA6` + poll `0x02/0xA5`

Drag-to-define box. **GET** is `0xA5` (empty `00`); not a box readback.

| | opcode | payload |
|---|---|---|
| **SET box** | `0x02/0xA6` | `01 00 00` + u16-LE id + 4× float32 LE at `@5` |
| **Clear** | `0x02/0xA6` | 21× `00` |
| **ACK** | `0xA6` flags `0xC0` | `00` |
| **Poll** | `0x02/0xA5` | `00` |
| **Poll reply** | `0xA5` flags `0xC0` | `00 01 00 00` while tracking, `00 00 00 00` after clear |

The four floats are normalized 0…1 **centre + size** (not top-left origin — drawing origin-as-centre put the top-left on the face). Examples: `(0.418, 0.525, 0.484, 0.461)`, `(0.725, 0.568, 0.318, 0.473)`. 2026-08-18 take: `(0.481, 0.481, 0.403, 0.555)`, `(0.804, 0.419, 0.180, 0.323)`. `@3–4` increments per box (`26 27`, `D6 27`, `B3 28`, `ff 07`, `a4 08`…). Mimo rejects a drag whose shorter side is ≲ 0.09 ("Frame Too Small") and does not SET.

OPC: tap inside the on-device AF-C face box (Vision; Mimo does not send a face rect) SETs that rect as the tracking box. A face shorter than 0.09 is grown to the accept floor so the tap is not rejected as Frame Too Small.

**GET `0xA5` is not a box readback.** The on-target subject rect is a separate push.

## Live subject box — `0x02/0x89` notify

`/tmp/mimo-tracking-box-20260818.pcapng` (2026-08-18). Drag box in Mimo, then hold while the subject moved. 914 notifies, 0 app GETs. Stops after `0xA6` all-zero clear; resumes after the next SET.

| | |
|---|---|
| **Push** | `0x02/0x89` flags `0x00`, 23 B, ~15 Hz |
| **Header** | 5× `00` + 2-byte tag (`a0 41` 913×; `80 3f` once) |
| **Box** | 4× float32 LE @7 = **centre x, y, w, h** in 0…1 |

Example pkt#1: header `00 00 00 00 00 a0 41` + `(0.520, 0.385, 0.187, 0.395)`. After SET `(0.804, 0.419, 0.180, 0.323)` the push walked `0.818 → 0.725` on x as the subject moved. 661 unique boxes in the take. Height can be as small as `0.048`.

## Resolution / frame rate — `0x02/0x18`

One 5-byte SET: `[res][fps_idx] 00 00 00`. Res and fps are **one blob**, not separate opcodes. **GET:** none. No empty `0x18`. No `0x8E` SET in this take (pid `0x0009` never appeared). `0xE1` absent.

`/tmp/mimo-res-fps.pcapng` (2026-08-15). Path: start 1080p → fps 24, 25, 30, 48, 50, 60 → 4K → fps 24, 25, 30, 48, 50, 60.

| UI | SET |
|---|---|
| 1080p 24 | `0A 01 00 00 00` |
| 1080p 25 | `0A 02 00 00 00` |
| 1080p 30 | `0A 03 00 00 00` |
| 1080p 48 | `0A 04 00 00 00` |
| 1080p 50 | `0A 05 00 00 00` |
| 1080p 60 | `0A 06 00 00 00` |
| 4K 24 | `10 01 00 00 00` |
| 4K 25 | `10 02 00 00 00` |
| 4K 30 | `10 03 00 00 00` |
| 4K 48 | `10 04 00 00 00` |
| 4K 50 | `10 05 00 00 00` |
| 4K 60 | `10 06 00 00 00` |

| | bytes |
|---|---|
| **SET** | `0x02/0x18` `[res][fps_idx] 00 00 00` |
| **ACK** | flags `0xC0` payload `00` |
| **GET** | none |

`@0` res: `0A` 1080p / `10` 4K. `@1` fps index: `01`=24, `02`=25, `03`=30, `04`=48, `05`=50, `06`=60. `@2–4` stayed `00`. Other resolutions were not in this take — do not invent.

Subscribe already showed 1080p 24 before the first SET. Switching 1080p 60 → 4K sent `10 06 00 00 00` (new res, same fps), then the 4K fps sweep.

Read: `cam_video_param_v2` (10 B). First 5 bytes = the SET. `@5–9` stayed `02 01 00 11 01` the whole take — unknown.

Mimo sometimes retransmits the same seq before ACK. 4K 30 and 4K 50 also got a later same-payload write with a new seq.

`0x09/0xa8` after some changes is live-view enable, not the format SET. `cam_fov` moved once at the 1080→4K switch — no FOV write here. `cam_expo_param` / `camcap_shutter` moved with fps (shutter list), not the write. `0x04/0x50` stayed the `01 04 05` poll.

## Zoom — `0x02/0xb8` + subscribe `cam_fov`

Recapture `/tmp/mimo-zoom-recapture-20260815-b.pcapng` (2026-08-15, 43.6 s, 4K 60). Script: **1× → 3× → 1×** chips, then **pinch** in toward 12× and back toward wide. Mimo has **no 12× chip** — 12× is pinch-only. Session started already at 12×. `0xE1` absent. Pid `0x0009` never GET or SET. `0x8E` GETs only. Zero `0A 4E` bytes in the file.

**GET:** none.

**SET:** `0x02/0xb8` flags `0x40` `rcv=0x01`. ACK flags `0xC0` payload `00`. This take used **slew only** — one SET per pinch direction, no stream, no stop.

| Kind | payload | This take |
|---|---|---|
| **Slew tele** | `03 00 64 00` (100) | t=19.356s. ACK `00`. `cam_fov` 9.15× → 6.00× → **12.00×**; lens `@14` 651 → 600 → **217** |
| **Slew wide** | `03 00 2C 01` (300) | t=16.071s. ACK `00`. Started at 12×; `cam_fov` 12.00× → 7.79× → **9.15×**; lens `@14` 217 → 244 → 624 → **651** |
| **Slider** | `0A 4E` + u16-LE lens `@14` | **Absent.** Prior drag takes only (`mimo-zoom-partial.pcapng`) |
| **Stop** | `FF 00 00 00` | **Absent.** Earlier `mimo-zoom-stopped.pcapng` / `live1` only |

**Buttons vs pinch:** labeled take `/tmp/mimo-zoom-1x3x-20260817.pcapng` — slow **1× → 12×** pinch. **587 slider SETs, 0 slews, 0 stops**, 50 ms. Lens **217 → 2604**. 3× hop at lens **651**. `cam_fov` 12287 → 2341 (inverted vs `/1024`). OpenPocketCine: 1× = 217, 3× = 651, 12× = 2604, pinch = slider the whole way.

Chip recapture `/tmp/mimo-zoom-recapture-20260815-g.pcapng` (2026-08-15, ~120 s). Operator: Mimo **1× chip selected**, tap **3×**, tap **1×**. `cam_fov` at t=0 was already **12.00×** (raw 12287, lens `@14` 217) — the 1× chip highlight is not the optical readout. Four `0x02/0xb8` SETs, all slews, all ACK `00`. No `0A 4E`, no `0x8E` SET, no pid `0x0009`.

| t | SET | Then `cam_fov` |
|---|---|---|
| 86.105s | `03 00 2C 01` (wide 300) | 12.00× → 4.75× → **9.15×** (lens 651) |
| 89.973s | `03 00 64 00` (tele 100) | 9.15× → **12.00×** (lens 217) |
| 104.406s | `03 00 2C 01` | 12.00× → **9.15×** |
| 118.276s | `03 00 64 00` | 9.15× → **12.00×** |

Same as the pinch pair. 3× never lands. Next take must wait until the **numeric** zoom readout is ~1.0× / ~2.29× (raw 2341), not just the 1× chip.

| UI | SET `0x02/0xb8` |
|---|---|
| Pinch in / 12× chip | `03 00 64 00` (slew 100) |
| Pinch out / 1× chip | `03 00 2C 01` (slew 300) — wide-direction write; 1× chip itself was not isolated |
| 3× chip | prior slider `0A 4E 6E 07` (lens 1902) — drag take, not the chip/pinch recapture |

Read: `cam_fov` (25 B, ~1 Hz). u32-LE **`@0`** is the factor field (not a float, not an index). **`@4`** = `@0 × 9/16` (16:9 pair). `@8` stayed `01 00 00 00`. Display ≈ `@0 / 1024`. 12× = 12287 = `12×1024 − 1`.

| Take | `cam_fov` `@0` | `@0` / 1024 | `@4` | `cam_lens_state` `@14` |
|---|---|---|---|---|
| prior wide | `25 09 00 00` (2341) | 2.29 | `25 05 00 00` (1317) | `2C 0A` (2604) |
| recapture mid | `98 24 00 00` (9368) | 9.15 | `95 14 00 00` (5269) | `8B 02` (651) |
| **12×** (both) | `FF 2F 00 00` (12287) | 12.00 | `00 1B 00 00` (6912) | `D9 00` (217) |

| | `cam_fov` (25 B) |
|---|---|
| wide | `25 09 00 00 25 05 00 00 01 00 00 00 3B 3E 00 00 01 E8 12 00 00 AC 0A 00 00` |
| 12× | `FF 2F 00 00 00 1B 00 00 01 00 00 00 A8 1B 00 00 01 99 31 00 00 00 1C 00 00` |

`camcap_zoom` (40 B): u16-LE tenths **10–120** (1.0×–12.0×) and lens `@14` range **217–2604**. Display is `cam_fov` `@0 / 1024`. Chip 1×/3× and pinch below 9.15× write lens `@14`; 12× and the 9.15×…12× pinch band write the slew u16.

## Still not labeled

FOV mode (Wide / Natural — `0x8E` pid `0x0009` never seen), codec, other resolutions (only 1080p / 4K here), iris (Pocket has none — `cam_blur_aperture` is beauty), shooting-mode extras. `cam_video_param_v2` `@5–9` unread. Zoom **factor** read is `cam_fov` `@0 / 1024` (12287 = 12×, not 1×). 4K 60 wide readout stays ~2.29× — the 1× chip still writes lens 2604. True optical 1.0× at 1080p was not recaptured.

## Earlier capture notes

`captures/mimo-settings-1.pcapng` (2026-08-14 ~22:02) first proved exposure `0x1E`, shutter `0x28`, ISO `0x2A`, and `0x8E` GET polls. Tonight’s `/tmp/mimo-*` sessions labeled the values.

Photo: **`0x02/0xE1 [17]`**. Nano `[05]` answers `0xEE`.
