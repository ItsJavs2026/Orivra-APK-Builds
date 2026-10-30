# Research 325: portable V2 backend adapter, V1 preserved

- Existing exportText/contextless V1 format is unchanged.
- New separate opt-in exportV2 can emit short, bounded context usage
  records after filtering private/unknown words. Plaintext warning will
  be required when a V2 UI button is added.
- previewV2 validates the entire supplied V1 or V2 document, confirms
  known/saved targets and capacity without writing.
- importV2Confirmed imports V1 words/corrections through the existing
  tested path, then merges only higher local context counts. Reimport
  does not inflate repeated learning, and the Learning toggle remains
  unchanged.
- Two synthetic Android tests protect V1 format, context V2 roundtrip,
  opt-out state, repeat imports, and rejection before any manual words
  are written.
- No IME code changes or package ID changes; no new APK or UI button.
- SharedPreferences across three stores are not transactionally atomic;
  partial commits are possible on unusual storage failures and should
  never be described as fully atomic.

After emulator qualification, an explicit optional V2 export UI and
version-aware import preview can be added in a separate reviewable branch.
