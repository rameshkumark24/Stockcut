# R8 rules for the release build (TRD §13.1).
#
# Room, Compose and kotlinx ship their own consumer rules, so this file is
# intentionally near-empty. Anything added here needs a written reason —
# a blanket -keep clause silently undoes the shrinking that keeps the AAB
# under the 12 MB NFR-3 budget.
