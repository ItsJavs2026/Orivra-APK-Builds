# Orivra Part 5 — Candidate 303 English wordpack trial checkpoint

**2026-10-29 — CI-qualified PRERELEASE, not phone-confirmed or production stable.** The protected currently working Samsung baseline remains **Candidate 300**, with its Google Keep prediction fix confirmed by the user on their S26 Ultra Android 17. Never overwrite Candidate 300, Preview #178, Build 262, or Candidate 293/298.

## Exact verified release
- Tag: `candidate-303-20261029`
- Release: https://github.com/ItsJavs2026/Orivra-APK-Builds/releases/tag/candidate-303-20261029
- APK: https://github.com/ItsJavs2026/Orivra-APK-Builds/releases/download/candidate-303-20261029/Orivra-Candidate-303.apk
- Filename: `Orivra-Candidate-303.apk`
- APK SHA-256: `d6f3314b864b715c5bebd826abcc62860617b69c843f34351720e2ec5b1b6b63`
- Actual packaged application ID: `com.chroomareborn.keyboard.build303trial`, versionCode `303`, versionName `1.0.22-build303-offline-language-trial`.
- Debug-signed via GitHub Actions, with ephemeral debug signer. Do not confuse it with a production stable build; upgrades may require uninstalling only Candidate 303. Existing installed keyboard is never overwritten.
- Verified GitHub release file and SHA-256 checksum file exist. APK final size 460,571 bytes (slightly under 0.5 MB).

## Changes vs Candidate 300
- Protected Candidate 300 English SentenceFlow / Build 294 UI, Safe Enter/Backspace, and the user-confirmed Google Keep fix remain in source. Candidate 303 introduces an additional independently sourced, optional English vocabulary tier from the official ESDB/SCOWL v2 American-English size-60 list. The experimental tier helps retain valid whole words and conservatively improves some missing-space reconstructions.
- Deterministic gzip-compressed ESDB `esdb-en-US-60.esdbpack` embedded as a 280KiB asset, and full upstream `ESDB_COPYRIGHT.txt` license notice embedded. Canonical wordlist SHA-256 `a5d2f75b95d3530485e539352dc01f9dd9079e03fdaca37466292bf1a360c600`. 78,942 distinct ASCII lowercase words verified.
- `OfflineDictionaryActivationPolicy`: default **OFF**. The user can opt in with the **Experimental offline English dictionary** button in Candidate 303 launcher Settings. Reopen a normal text editor after changing it.
- `RebornImeService`: checks the opt-in at editor start; does not load in protected password/email/URL/non-text inputs; opens a low-priority background worker, retains the original predictor until verified pack is ready; clears optional pack on disable, protected editor or service destruction. No user content is stored or uploaded.
- Experimental Spanish and gesture-glide typing are NOT part of this release.

## Build / QA evidence
- Research [Build 303 CI run 40974663679](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40974663679) **SUCCESS**: Java policy/security suite, 23/23 Android 35 emulator tests, reproducible ESDB checks, Android asset preflight.
- Final separate-package [Candidate 303 release CI run 40975546792](https://github.com/ItsJavs2026/Orivra-APK-Builds/actions/runs/40975546792) **SUCCESS**: Java regression tests including 41 opt-in policy assertions, 43 Google Keep/privacy-policy assertions, seven compressed-wordpack fallback tests, 11 segmentation assertions; Android 35 Pixel 6 emulator **23 tests started / 23 finished / BUILD SUCCESSFUL**; app package and versionCode, asset and license, Android permissions (NO INTERNET), debug APK signing verified; SHA256 of generated APK exactly matches release asset.
- One initial release workflow failed on a mistaken strict source compare between separate trial package IDs; corrected to compare only intentional package/version/label changes. A subsequent Android test hard-coded Candidate 300's package ID; changed it to Candidate 303 before final successful release. These were CI packaging QA errors, not Samsung field failures.
- In earlier 301 **synthetic** offline 667-case A/B English benchmarks, new dictionary tier improved 100-case top1 from 33 to 47 with zero correct-to-wrong changes in individually checked 80+100 cases. This is not real-world typing accuracy.
- Earlier 302 Android 35 **emulator** measured cold pack load 478 ms and warm load 45 ms, and synthetic warm predictor p95 ~211 ms baseline versus ~194 ms experimental in one noisy run. These are not Samsung S26 Ultra p95/heap measurements. The loader is off the UI/touch path.

## Manual Samsung Android 17 acceptance
1. Install Candidate 303 side by side with Candidate 300. Do NOT uninstall or replace Candidate 300.
2. First activate 303 keyboard with experimental dictionary **OFF**, verify Safe Enter/Backspace/predictions in scratch Samsung Notes and Google Keep title/body. No real passwords/messages.
3. Open the Candidate 303 launcher, tap **Experimental offline English dictionary** to ON, and reopen the editor. Allow first async load to complete during ordinary typing; first prediction(s) may still use baseline until loaded.
4. Compare joined exact-word strings, multiple misspellings and no-space phrases using same examples ON/OFF; test `thetrainarrivedbeforesunrise`, `yourheadphonessoundveryclear`, `thegardenneedsfreshwater` and `thepaxkaxearrivedearliertoday`. For ambiguous compounds such as freshwater/fresh water, preserved baseline is desirable.
5. Recheck Google Keep normal notes and intentional private editor suppression. No network or special permissions needed.
6. If lag, crashes or false corrections occur, toggle OFF and reopen editor (restores Candidate 300 engine); switch back to installed Candidate 300. Record screenshots/cases in the project chat.

## Future development
- Wait for user's actual phone acceptance before calling Candidate 303 stable or enabling the pack by default.
- Improve independent, truly unseen language benchmarks, measure on-device cold/warm latency/heap and double-check lifecycle cancellation.
- Only after English engine qualification, continue isolated Spanish auto-routing and Unicode work.

## Physical Samsung user feedback — after Candidate 303 install
The user confirmed installing Candidate 303 on their Galaxy S26 Ultra Android 17. After receiving a combined test checklist covering Google Keep, prediction ON/OFF comparisons, Backspace, Safe Enter, and responsiveness, the user replied **"All good"**. Record this as **positive user-reported handset acceptance / no problems reported**. The reply did not itemize test-case results or quantify per-case accuracy, so **do not claim all individual test scenarios passed independently, 78,942-word pack improved on-device scores, or that production stability is proven**. Candidate 303 is now the newest positively tested handset candidate. Preserve Candidate 300 as the confirmed Keep-fix rollback option, and continue language improvements in a separate branch.
