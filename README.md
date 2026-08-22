# Emberlink

An **offline** infrared remote for Fire TV Edition televisions, driven by your Android phone's IR blaster.

No Wi-Fi. No Bluetooth pairing. No Amazon account. No ADB. The app does not declare the `INTERNET` permission, so it is physically incapable of reaching a network — check `AndroidManifest.xml` yourself.

---

## Does this work with my device?

This is the part that decides everything, so read it before installing.

| Device | IR receiver | Works? |
| --- | --- | --- |
| **Fire TV Edition TVs** — Insignia `NS-*DF*`, Toshiba | Yes | **Yes** |
| **Amazon Omni / 4-Series / 2-Series TVs** | Yes | **Yes** |
| Fire TV Cube (gen 2/3) | Yes, but IR-learning only | Only with root — disabled in stock firmware |
| Fire TV Stick (any generation, Lite, 4K, 4K Max) | **None** | **No.** Bluetooth only, no receiver exists |

A Fire TV **Stick** cannot be controlled by infrared by any app, ever. There is no receiver on the board. If that is your device, this project cannot help you.

Your phone also needs an IR emitter. Emberlink checks `ConsumerIrManager.hasIrEmitter()` on launch and tells you plainly if there isn't one. Developed against a **POCO F3**; any Xiaomi, Redmi, or Poco with a blaster behaves the same.

---

## What it does

- **Full remote pad** laid out to match the physical Insignia/Toshiba Fire TV Edition remote — power, search, back/home/menu, D-pad ring with OK, transport, volume cluster, app row
- **Hold-to-repeat** on the arrows, volume and scan keys, with a haptic tick per shot
- **Phone volume rocker drives the TV**, not the phone
- **Screen stays awake** while the remote is open
- **Number pad, discrete HDMI inputs, discrete power on/off** on a second tab
- **Fire OS system menus** — settings, recent apps, network, Bluetooth, resolution, VoiceView, Alexa
- **Danger zone** — reboot and factory reset exist as real codes and fire with no prompt on the TV, so they sit behind a confirmation dialog here
- **Code Lab** — brute-force sweeper for any code not published for your panel
- **Export** your discovered codes as JSON *and* as a Flipper Zero `.ir` file

---

## Code Lab

Not every button is documented. Amazon never published these codes; everything below was recovered by the community, and a few keys on the physical remote still have no public code for this panel family.

Code Lab solves that. NEC has only **256 possible command bytes** per device address, so an exhaustive sweep is a couple of minutes rather than an afternoon.

1. Pick which button you're hunting from the dropdown
2. Point the phone at the TV and hit **Start sweep**
3. The moment the TV reacts, tap that code in the rolling log

The log is the important bit. You are never asked to hit "capture" at exactly the right instant — human reaction time makes that hopeless. Instead the last twelve codes stay on screen, and you tap whichever one caused the reaction. Two or three back is completely normal.

Discovered codes save to on-device JSON, survive reinstall, and light up the matching button on the remote immediately.

If a full sweep of `02 7D` finds nothing, try address `40 BF` — some model years put panel-level functions on NEC device 64.

---

## The codes

Protocol is **NECext** at **38 kHz**, address **`02 7D`** (device 2, subdevice 125).

Frame layout: 9000 µs mark / 4500 µs space header, then 32 bits LSB-first in the order `address, subAddress, command, ~command`, then a 560 µs trailer. A `0` bit is 560 on / 560 off; a `1` bit is 560 on / 1690 off.

| Function | Code | Function | Code |
| --- | --- | --- | --- |
| Power | `0x46` | Guide / Live TV | `0x14` |
| Home | `0x9F` | Channel up | `0x0F` |
| Menu | `0x45` | Channel down | `0x5A` |
| Back | `0x0D` | Recent apps | `0xB1` |
| Up | `0x48` | Netflix | `0x5F` |
| Down | `0x4D` | Prime Video | `0xA1` |
| Left | `0x4E` | Disney+ | `0xA2` |
| Right | `0x49` | Hulu | `0xA5` |
| OK / Select | `0x4A` | Freevee | `0xD2` |
| Volume up | `0x0C` | Alexa | `0xA0` |
| Volume down | `0x19` | Magnifier | `0xC8` |
| Mute | `0x4C` | VoiceView | `0xC0` |
| Rewind | `0x16` | Bluetooth menu | `0xA6` |
| Play / Pause | `0x5B` | Resolution | `0xCE` |
| Fast forward | `0x17` | Network menu | `0xCF` |
| Settings | `0x96` | Reboot | `0xCA` |
| | | Factory reset | `0xC9` |

⚠️ `0xC9` really does factory reset the television, with no confirmation on the TV itself. `0xCA` reboots it instantly. Both are behind a dialog in the app — leave them there.

**Not yet mapped**, and therefore Code Lab targets: Search, Live TV (the antenna key), HBO, PS Vue, discrete HDMI 1/2/3, Input, discrete Power On, discrete Power Off, digits 0–9, Dash.

### Sources

Codes cross-checked across three independent captures in [Lucaslhm/Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB), which agree byte for byte:

- `TVs/Amazon/FireTV_Omni_Series_4K.ir`
- `TVs/Toshiba/toshiba_firetv_v2.ir`
- `TVs/Insignia/Insignia_NS_RCFNA_21.ir`

Original brute-force discovery of the Fire TV Edition set was done by the JP1 / hifi-remote community.

---

## Building

CI does everything. Push to `main` and grab the APK from the workflow artifacts, or push a `v*` tag to cut a GitHub Release with the APK attached.

Locally, with JDK 17 and the Android SDK:

```
gradle assembleRelease
```

### About the signing key

The release key is generated by CI on first run and **committed to this repository** on purpose. That keeps every build signed with the same key, so new versions install over old ones instead of forcing an uninstall. This is a personal offline remote app — the key protects nothing worth protecting, and making it public means anyone can build a compatible APK.

---

## Range

A phone blaster is weaker and narrower than the plastic remote. Expect roughly 3–5 metres and a tighter aiming cone. That is a hardware limit, not something the app can fix.

---

## Licence

MIT.

Not affiliated with Amazon, Insignia, Toshiba, or any of the streaming services whose names appear on the app-launch keys. Those buttons exist because the codes exist.
