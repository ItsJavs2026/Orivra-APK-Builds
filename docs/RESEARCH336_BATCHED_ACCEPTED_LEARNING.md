# Research 336 — batch learned word updates on suggestion acceptance

The 335 phrase frequency logic is correct but writes one preference
update per individual word, up to twelve times per accepted suggestion.
That adds avoidable main-thread lookups/disk scheduling, counteracting
the smoother tap feel the user wants. Research 336 takes one snapshot,
preloads manually saved words once only when needed, increments each
approved distinct word by 2, and issues a SINGLE preference apply.
Retains 320-word storage limit, counter ceiling 1000, opt-in, and
unknown/private text rejection.

The split next-word tap path now also reinforces the newly accepted
known word after successful same-session editor commit; it already
reinforces adjacent context and retains those checks.

V3 all-in-one backup continues exporting these exact frequency counters,
with V1 and V2 unchanged. No new permissions, no APK release.
Full Java and Android emulator qualification required.
