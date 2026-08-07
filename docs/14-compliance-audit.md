# Compliance audit — secrets and licences

**Run 2026-08-07** against the full git history and the release dependency
graph. Both are Phase 9 release-gate items (`docs/08`).

Re-run both before submission — the point of writing the commands down is that
the answer changes as history grows.

---

## 1. Secrets across git history

Not "are they in the working tree" — **have they ever been committed**. A secret
removed in a later commit is still in the history and still extractable.

```bash
# Files that must never appear, in ANY commit
git log --all --name-only --pretty=format: | sort -u \
  | grep -Ei "keystore\.properties$|\.jks$|\.keystore$|local\.properties$|google-services\.json$|\.pem$|\.p12$"

# Credential-shaped strings in any blob, ever
git rev-list --all | while read c; do
  git grep -I -nE "(BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|AIza[0-9A-Za-z_-]{35}|storePassword=.+|keyPassword=.+)" "$c" --
done
```

### Result: ✅ clean

| Check | Result |
|---|---|
| `keystore.properties` / `*.jks` ever committed | none |
| `local.properties` ever committed | none |
| `google-services.json` ever committed | none |
| Private-key blocks, Google API keys, passwords | none |
| Real AdMob account ID (`7038016776482334`) | **never committed** — it lives only in the git-ignored `local.properties` |

`keystore.properties.example` is present and correct: keys with no values.

---

## 2. Dependency licences

Every third-party artifact on `releaseRuntimeClasspath` — 199 artifacts across
60 groups.

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

### Result: ✅ no copyleft, nothing needing review

| Licence | Groups |
|---|---|
| Apache-2.0 | all 45 `androidx.*`, all `org.jetbrains*`, Guava, j2objc, okio, javax.inject, `com.google.android.datatransport` |
| Android SDK License (Google) | `com.android.billingclient`, `com.google.android.gms`, `com.google.android.play`, `com.google.android.ump` |
| Apache-2.0 / Google APIs ToS | `com.google.firebase` |
| MIT | `org.checkerframework` |
| BSD | `com.google.code.findbugs` (jsr305 only) |

**🔴 No GPL, AGPL or LGPL anywhere.** That matters for a closed-source paid app:
a copyleft dependency would oblige source disclosure, and finding one after
publishing is expensive to undo.

The Google SDK licences are not open-source licences but explicitly permit
distribution in an app, which is what they exist for.

### Why the list is 199 artifacts for an app with ~12 declared dependencies

Almost all of it is transitive, and the two big contributors are the ones this
project accepted deliberately:

- **Compose** pulls the whole `androidx.compose.*` tree
- **Play Services Ads + Firebase** pull GMS, datatransport, protobuf and the
  Privacy Sandbox libraries

`docs/02` §9's allowed list governs *declared* dependencies, and nothing outside
it was declared. The transitive graph is the price of AdMob and Crashlytics,
and it is reflected in the release APK at 4.1 MB against a 12 MB budget.

---

## Still to do before submission

- [ ] Re-run both checks on the final release commit
- [ ] Confirm `targetSdk = 36` **in the built AAB**, not just in Gradle
- [ ] Open-source licences screen in About *(the artifacts are all
      attribution-requiring; a licences list is the normal way to satisfy that)*
