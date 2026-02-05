# Plan for Incomplete Tests (Cat1 + Cat3)

**Created:** 2026-02-05
**Total Outstanding:** 3 PARTIAL, 4 SKIPPED

---

## Summary

| ID | Test | Status | Blocker | Solution |
|----|------|--------|---------|----------|
| AD-03 | Audio delay range limits | PARTIAL | Need to test UI clamping | Test in player during playback |
| ES-06 | Invalid subtitle file | PARTIAL | Need malformed subtitle | Create test files, use external subtitle feature |
| ES-02 | Load ASS file | SKIPPED | No ASS content in library | Find/add ASS subtitle content |
| ES-03 | Load VTT file | SKIPPED | No VTT content in library | Find/add VTT subtitle content |
| ES-04 | Encoding handling | PARTIAL | Only UTF-8 tested | Test with Latin-1, CJK encoded files |
| TA-BUTTON | Trailer button | SKIPPED | Not tested | Navigate to detail, click trailer button |
| OS-01/02/03 | OpenSubtitles UI | PARTIAL | UI not exercised | Search for subtitles from player menu |

---

## Plan A: Tests Completable on Emulator (No Content Changes)

### 1. AD-03: Audio Delay Range Limits
**Effort:** Low (5 min)
**Environment:** Emulator

**Steps:**
1. Launch emulator, install app
2. Play any video content
3. Open player menu, find audio delay control
4. Test boundary values:
   - Increase delay to maximum (should cap at +2000ms or similar)
   - Decrease delay to minimum (should cap at -2000ms or similar)
5. Verify UI prevents exceeding limits

**Pass Criteria:** UI enforces range limits, no crash on boundary values

---

### 2. TA-BUTTON: Trailer Button
**Effort:** Low (5 min)
**Environment:** Emulator

**Steps:**
1. Navigate to any movie with trailers (e.g., "500 Days of Summer")
2. On detail screen, locate and click the Trailer button
3. Verify trailer plays (YouTube or local)
4. Press back to return to detail screen

**Pass Criteria:** Trailer launches and plays, returns cleanly

---

### 3. OS-01/02/03: OpenSubtitles UI
**Effort:** Medium (10 min)
**Environment:** Emulator + OpenSubtitles account

**Steps:**
1. Play a video without subtitles (or with wrong language)
2. Open player menu > Subtitles > Search OpenSubtitles
3. **OS-01:** Verify search UI appears, shows results
4. **OS-02:** Test language filter dropdown
5. **OS-03:** Select and download a subtitle, verify it loads

**Pass Criteria:** Search works, filter works, download applies subtitle to player

**Note:** Requires OpenSubtitles API credentials configured in app

---

## Plan B: Tests Requiring Test Content

### 4. ES-06: Invalid Subtitle File
**Effort:** Medium (15 min)
**Environment:** Emulator + test subtitle files

**Preparation:**
Create test subtitle files in scratchpad:

```bash
# malformed.srt - invalid timestamps
cat > malformed.srt << 'EOF'
1
NOT_A_TIMESTAMP --> ALSO_INVALID
This subtitle has broken timing

2
99:99:99,999 --> 99:99:99,999
Impossible timestamp values
EOF

# empty.srt - empty file
touch empty.srt

# truncated.srt - cut off mid-file
cat > truncated.srt << 'EOF'
1
00:00:01,000 --> 00:00:04,000
First subtitle

2
00:00:05,000 -->
EOF
```

**Steps:**
1. Host files on local HTTP server or push to device
2. Play video, select "Add External Subtitle"
3. Load each malformed file
4. Verify: video continues playing, no crash, appropriate error/warning

**Pass Criteria:** Graceful degradation - video plays, subtitles silently fail or show error toast

---

### 5. ES-02: Load ASS File
**Effort:** Medium (15 min)
**Environment:** Emulator + ASS subtitle content

**Options to get ASS content:**
1. **OpenSubtitles:** Search for anime content (commonly has ASS)
2. **Convert existing:** Use ffmpeg to convert SRT to ASS
3. **Create test file:** Minimal ASS file for testing

**Minimal ASS test file:**
```ass
[Script Info]
Title: Test ASS Subtitle
ScriptType: v4.00+

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,2,2,10,10,10,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
Dialogue: 0,0:00:01.00,0:00:05.00,Default,,0,0,0,,This is an ASS subtitle test
Dialogue: 0,0:00:06.00,0:00:10.00,Default,,0,0,0,,{\b1}Bold text{\b0} and {\i1}italic{\i0}
```

**Pass Criteria:** ASS subtitles render with styling (bold, italic if supported)

---

### 6. ES-03: Load VTT File
**Effort:** Low (10 min)
**Environment:** Emulator + VTT subtitle content

**Minimal VTT test file:**
```vtt
WEBVTT

00:00:01.000 --> 00:00:05.000
This is a WebVTT subtitle test

00:00:06.000 --> 00:00:10.000
Second cue with <b>bold</b> text
```

**Options:**
1. Use ffmpeg to convert: `ffmpeg -i subtitle.srt subtitle.vtt`
2. Create minimal test file above
3. Find VTT content via OpenSubtitles

**Pass Criteria:** VTT subtitles render correctly

---

### 7. ES-04: Encoding Handling
**Effort:** Medium (15 min)
**Environment:** Emulator + multi-encoded subtitle files

**Test files needed:**
1. **UTF-8:** Already tested (PASS)
2. **Latin-1 (ISO-8859-1):** European characters (é, ñ, ü)
3. **Windows-1252:** Common for old SRT files
4. **CJK (GB2312/Shift-JIS):** Chinese/Japanese characters

**Create test files:**
```bash
# Latin-1 encoded
echo -e "1\n00:00:01,000 --> 00:00:05,000\nCafé résumé naïve" | iconv -t ISO-8859-1 > latin1.srt

# Check if app auto-detects or needs BOM
```

**Pass Criteria:** Non-UTF8 subtitles display correctly (auto-detect or fallback)

---

## Execution Order (Recommended)

### Session 1: Quick Wins (20 min)
1. ✅ AD-03 - Audio delay limits
2. ✅ TA-BUTTON - Trailer button
3. ✅ OS-01/02/03 - OpenSubtitles UI (if credentials available)

### Session 2: Subtitle Formats (30 min)
1. Prepare test subtitle files (ASS, VTT, malformed)
2. ✅ ES-02 - ASS file
3. ✅ ES-03 - VTT file
4. ✅ ES-06 - Invalid subtitle handling

### Session 3: Edge Cases (15 min)
1. ✅ ES-04 - Encoding variations

---

## Prerequisites Checklist

- [ ] Emulator available (Sony_85_TV)
- [ ] Latest APK built and installed
- [ ] OpenSubtitles credentials configured (for OS-* tests)
- [ ] Test subtitle files created (ASS, VTT, malformed, encoded)
- [ ] Local HTTP server for hosting test files (optional)

---

## Notes

- **ES-02/ES-03:** Could be tested via OpenSubtitles download if library lacks content
- **ES-06:** ExoPlayer handles gracefully by design; test confirms no regression
- **OS-*:** Requires network access and valid OpenSubtitles API key
