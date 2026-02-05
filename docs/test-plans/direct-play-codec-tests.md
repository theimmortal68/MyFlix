# Direct Play Codec Test Matrix

Generated: 2026-02-04

## Video Codec Tests

| Status | Video Codec | Movie | Audio | ID |
|--------|-------------|-------|-------|-----|
| [ ] | AV1 DOVIInvalid | Ballerina | TRUEHD 8ch | `9a3c64a628232d732fb39ea7b52a723a` |
| [ ] | AV1 DOVIWithHDR10 | The Abyss | None 0ch | `9f7e8114de49689da7210d6a753d334c` |
| [ ] | AV1 SDR | 13 Going on 30 | OPUS 6ch | `7e6beee14bb109601942ef8169928cd0` |
| [ ] | H264 SDR | 9 Bullets | DTS 6ch | `460b3a93d833f953bed8f534a1ad85e4` |
| [ ] | HEVC DOVI | Ant-Man | TRUEHD 8ch | `10a1d03b806cd76e267989884d080923` |
| [ ] | HEVC DOVIInvalid | American Psycho | TRUEHD 8ch | `62f9c70548a25660ef01b1234f696bb6` |
| [ ] | HEVC DOVIWithEL | 10 Cloverfield Lane | TRUEHD 8ch | `552c63995eb17fa3a50b5ba0908db0b1` |
| [ ] | HEVC DOVIWithELHDR10Plus | 28 Years Later | TRUEHD 8ch | `836ae0fb3af40c06ed09869e7b3684fa` |
| [ ] | HEVC DOVIWithHDR10 | 2 Guns | DTS 8ch | `c181c0a4cc9f42ad6e1d16f193f9a2aa` |
| [ ] | HEVC DOVIWithHDR10Plus | 2 Fast 2 Furious | DTS 8ch | `c65dc10721944d4d1ae90c24921b88eb` |
| [ ] | HEVC HDR10 | (500) Days of Summer | DTS 6ch | `a62451b6ecccd209c0b1897613aa9507` |
| [ ] | HEVC HDR10Plus | 300: Rise of an Empire | None 0ch | `a7a76fa0f745a688d0ddfd12fb8cc925` |
| [ ] | HEVC SDR | 2:22 | AC3 6ch | `17815a666368ee692695cd0585d2baa3` |
| [ ] | MPEG4 SDR | Buzz Lightyear of Star Command: The Adventure Begins | None 0ch | `d4f9f1306d7c59115f207736ab2a7cda` |
| [ ] | VC1 SDR | 16 Blocks | DTS 6ch | `296652996f3917232783bada084eb613` |
| [ ] | VP9 SDR | Avengers: Infinity War | OPUS 2ch | `60f229bb6c5f1e174d70a56ed629325a` |

## Audio Codec Tests

| Status | Audio Codec | Movie | Video | ID |
|--------|-------------|-------|-------|-----|
| [ ] | AAC 2ch | The Air Up There | HEVC SDR | `f89529d084d76976ed237415c1c9df2e` |
| [ ] | AAC 6ch | About Time | HEVC SDR | `9b60c0fbb09a8b4eb9bd17a6e3406556` |
| [ ] | AC3 2ch | An American Tail: Mystery of Night Monster | HEVC SDR | `be523815b0921ee73f1684deaeedc28c` |
| [ ] | AC3 6ch | 2:22 | HEVC SDR | `17815a666368ee692695cd0585d2baa3` |
| [ ] | DTS 2ch | 12 Angry Men | HEVC DOVIWithEL | `6d8d69b96086a578376c976e15673ab9` |
| [ ] | DTS 5ch | Billy Madison | HEVC DOVIWithEL | `ea4b23c778efb1bc39f60806ae9c6fce` |
| [ ] | DTS 6ch | (500) Days of Summer | HEVC HDR10 | `a62451b6ecccd209c0b1897613aa9507` |
| [ ] | DTS 7ch | Blade: Trinity | HEVC SDR | `8876be2ddf227b26c936e5d47927da9c` |
| [ ] | DTS 8ch | 2 Fast 2 Furious | HEVC DOVIWithHDR10Plus | `c65dc10721944d4d1ae90c24921b88eb` |
| [ ] | EAC3 2ch | Aloha Scooby-Doo! | HEVC SDR | `7ca0a98e1cd7a2aee17d0c1197dae0bb` |
| [ ] | EAC3 6ch | 28 Weeks Later | HEVC SDR | `c47821cc9423928d3ac97e6e3639f906` |
| [ ] | EAC3 6ch Atmos | 6 Underground | HEVC DOVIWithHDR10Plus | `c6f91bdd6dbaaf0cea7d1a4e4a0e7cd3` |
| [ ] | EAC3 8ch Atmos | Baywatch | HEVC DOVIWithHDR10Plus | `701a217a234dacdc211e11d78e589762` |
| [ ] | FLAC 1ch | The Breakfast Club | HEVC DOVIWithEL | `1cd9abae6cc0a8754dd2deebc9b42c1e` |
| [ ] | OPUS 2ch | Avengers: Infinity War | VP9 SDR | `60f229bb6c5f1e174d70a56ed629325a` |
| [ ] | OPUS 6ch | 13 Going on 30 | AV1 SDR | `7e6beee14bb109601942ef8169928cd0` |
| [ ] | TRUEHD 6ch | The 6th Day | HEVC SDR | `9078b9a510a810c3bc84f0a03c5ccfd0` |
| [ ] | TRUEHD 8ch | Abigail | HEVC DOVIWithHDR10 | `408a787aab8d141b8afbe93d2cae89e2` |
| [ ] | TRUEHD 8ch Atmos | The 5th Wave | HEVC DOVIWithHDR10 | `5e13cc4097112be86b75ce94636e5e93` |

## Priority Test Cases

### Dolby Vision (Most Important)
1. **HEVC DOVIWithHDR10** - 2 Guns (`c181c0a4cc9f42ad6e1d16f193f9a2aa`)
2. **HEVC DOVI** - Ant-Man (`10a1d03b806cd76e267989884d080923`)
3. **HEVC DOVIWithEL** - 10 Cloverfield Lane (`552c63995eb17fa3a50b5ba0908db0b1`)

### HDR10/HDR10+
4. **HEVC HDR10** - (500) Days of Summer (`a62451b6ecccd209c0b1897613aa9507`)
5. **HEVC HDR10Plus** - 300: Rise of an Empire (`a7a76fa0f745a688d0ddfd12fb8cc925`)

### AV1 (Newer Codec)
6. **AV1 SDR** - 13 Going on 30 (`7e6beee14bb109601942ef8169928cd0`)
7. **AV1 DOVIWithHDR10** - The Abyss (`9f7e8114de49689da7210d6a753d334c`)

### Audio Passthrough
8. **TRUEHD 8ch Atmos** - The 5th Wave (`5e13cc4097112be86b75ce94636e5e93`)
9. **EAC3 8ch Atmos** - Baywatch (`701a217a234dacdc211e11d78e589762`)
10. **DTS 8ch** - 2 Fast 2 Furious (`c65dc10721944d4d1ae90c24921b88eb`)

## Notes

- DOVIInvalid = Profile 5 (MEL) without enhancement layer - may play as HDR10 fallback
- DOVIWithHDR10 = Profile 8 with HDR10 fallback layer
- DOVIWithEL = Profile 7 with enhancement layer (full Dolby Vision)
- Items with "None 0ch" audio have no default audio track set
