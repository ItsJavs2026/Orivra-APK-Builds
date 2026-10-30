# Candidate 333 — short-word personalization plus V3 portable backup

An isolated test build based EXACTLY on pinned Research 332 commit
6269392c7108490e92f3055bfa704691bb799b72. Candidate 327 remains installed as a separate app
and is never overwritten. Candidate 333 has package ID
com.chroomareborn.keyboard.build333trial, versionCode 333.

Key goal: repeated typed "continue" should produce tap-to-accept
suggestions from con, cont, and after more use co. Short words such as
my may complete from m after strong evidence. Words accepted through a
tap are reinforced, and stable fully typed words in chat fields can
learn even if not followed by Space.

Existing Google Search/Keep compatibility, smooth key touch, banking
numeric keypad, privacy protections, and V1/V2 learning backups remain.
V3 is new and contains aggregate individual-word frequencies so learning
survives new test installs. Older versions cannot import V3; export
and retain V1 as rollback. Imports never enable learning automatically.

No INTERNET permission; APK must pass Android 35 full emulator tests,
package isolation, signer verification and checksum. Real Samsung phone
behavior and banking app acceptance require user testing.
