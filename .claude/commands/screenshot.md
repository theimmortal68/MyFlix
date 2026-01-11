Capture a screenshot from an Android emulator or device.

## Usage
```
/screenshot [platform]
```

Where `[platform]` is:
- `tv` - Capture from Android TV emulator
- `mobile` - Capture from mobile emulator/device
- (no argument) - List available devices and prompt for selection

## Implementation

### Step 1: List Available Devices
```bash
adb devices -l
```

This shows all connected devices with details. Look for:
- TV emulators: Usually contain "tv" or "Television" in the model
- Mobile: Usually phone model names or "sdk_gphone"

### Step 2: Identify Device Serial
Common patterns:
- Emulators: `emulator-5554`, `emulator-5556`, etc.
- Physical devices: Alphanumeric serial numbers

### Step 3: Capture Screenshot
```bash
# For specific device (replace SERIAL with actual device serial)
adb -s SERIAL exec-out screencap -p > screenshot-tv-$(date +%Y%m%d-%H%M%S).png

# Examples:
adb -s emulator-5554 exec-out screencap -p > screenshot-tv-20260111-143022.png
adb -s emulator-5556 exec-out screencap -p > screenshot-mobile-20260111-143022.png
```

### Step 4: View the Screenshot
After capturing, view the image to analyze the UI.

## Quick Reference

**List devices:**
```bash
adb devices -l
```

**Capture from TV (assuming first emulator):**
```bash
adb -s emulator-5554 exec-out screencap -p > screenshot-tv-$(date +%Y%m%d-%H%M%S).png
```

**Capture from Mobile (assuming second emulator):**
```bash
adb -s emulator-5556 exec-out screencap -p > screenshot-mobile-$(date +%Y%m%d-%H%M%S).png
```

## Device Identification Tips

Run `adb devices -l` and look at the output:
```
emulator-5554  device product:sdk_google_atv_x86 model:sdk_google_atv_x86 → TV
emulator-5556  device product:sdk_gphone64_x86_64 model:Pixel_6 → Mobile
R3CN90XXXXX    device product:beyond1 model:SM_G973U → Physical phone
```

- `atv` in product name = Android TV
- `gphone` or phone model = Mobile
- Physical devices show manufacturer model

## Workflow Example

```
User: /screenshot tv

Claude:
1. Runs: adb devices -l
2. Identifies TV emulator (e.g., emulator-5554 with atv in name)
3. Runs: adb -s emulator-5554 exec-out screencap -p > screenshot-tv-20260111-143022.png
4. Views the captured image
5. Provides analysis of the current UI state
```
