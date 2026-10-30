# Research 326: separate opt-in V2 backup UI

Original Candidate 319 phone-tested V1 export button is preserved and
still saves identical manually added words plus accepted correction pairs.

A second button now exports V2, after explicitly warning that:
- the file is plaintext, may reveal saved words and usage patterns
- older versions cannot import V2
- user should retain a V1 backup when rolling back.

Import accepts both V1 and V2, validates them before showing the
existing explicit approval dialog, and merges context evidence only
through the tested V2 storage adapter. It never enables learning
automatically and retains the user's existing V1 import semantics.

No new permissions or networking. Code is not in a released APK;
requires Android compilation and emulator before Candidate promotion.
