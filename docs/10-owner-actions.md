# Owner Actions — everything that must happen outside the code

> ⚠️ **Largely superseded — read
> [`13-remaining-to-launch.md`](13-remaining-to-launch.md) instead.**
>
> This doc was written when the app was going to be paid. That changed on
> 2026-08-08: **StockCut ships completely free, earning only from AdMob**
> ([`15-free-launch-and-paywall-plan.md`](15-free-launch-and-paywall-plan.md)).
>
> So everything here about the **in-app product, license testers, the billing
> test matrix, and the payments profile does not apply**. It applies only if a
> paid unlock is ever added.
>
> Still current, and all covered in `13`: the Play Console account, the keystore,
> the store listing, the real-device testing, and the **12-tester / 14-day closed
> test**, which a new personal developer account still requires.

**Why this doc exists:** Phases 3–5 are built. Phase 6 is billing and ads, and
almost none of it can be *tested* until a Play Console account, an in-app
product, and license testers exist. Those are account and paperwork tasks that
only the owner can do.

Work top to bottom — the order is dependency order, not preference. Two tracks
run in parallel and are marked ⏱ because they are calendar time, not effort.

---

## 0. 🔴 The gate that outranks everything — validate with a real tradesman

**Status: still open.** `docs/07` W0 makes this a stop-or-continue gate: *"if the
paper test fails, stop the project here."*

The app was built ahead of this gate. That was a choice, and it is worth being
honest about what it means: if a tradesman looks at a cut plan and shrugs, the
sunk cost is now several weeks of code rather than one afternoon.

**The good news: this is now easier than the doc assumed.** W0 asked you to
produce a cut plan *by hand on paper* because no app existed. One does now.

1. Find one person who cuts from linear stock — welder, fabricator, fencing
   contractor, timber framer, shopfitter. Not a cabinetmaker (that is 2D, and a
   different product).
2. Ask for one **real job** they have coming up: the stock lengths they buy and
   the pieces they need.
3. Enter it into the app. Show them the plan.
4. Ask three questions, in this order, and write down the answers verbatim:
   - *"Is this plan right?"* — correctness is the whole product
   - *"Would you cut to this?"* — trust is separate from correctness
   - *"What units do you actually work in?"* — this decides how much the
     fractional-inch work matters

**If they say the plan is wrong,** that job becomes oracle case `O-10` in
`docs/06` §2.5 — a permanent regression test, and the most valuable test case
this project will ever get. Send it to me and I will add it.

**Cost:** one conversation. **Blocks:** nothing technically, but it is the M0/M4
gate and the reason the project exists.

---

## 1. 🔴 Decide the app name and package name

**This blocks steps 2, 5, 6 and 7. Do it first.**

The package name is **permanent once published** — it cannot be changed, ever,
without shipping a different app and losing every install and review.

✅ **Settled 2026-08-07:** `com.measure.stockcut`, title
**StockCut — Cut List Optimizer**. Verified free on Play before committing.
Set in one place, [`app/build.gradle.kts`](../app/build.gradle.kts).

1. Pick 3 candidate names.
2. Search each on the Play Store. `docs/00-phase-0` §10 warns that **"CutList" is
   heavily used** — it collides with SmartCut, CutList Optimizer, Cutlistor,
   CutListCalc and Cutlist Evolution. Do not build a title around it.
3. Resolve the ASO tension deliberately: the title needs the search keyword
   (`cut list`, `cutting`, `optimizer`) but must be distinctive. The pattern
   `docs/00-phase-0` proposes is **`StockCut — Cut List Optimizer`**: distinctive
   brand, keyword tail.
4. Pick the package name to match, in reverse-domain form — e.g.
   `com.yourname.stockcut`. Using a domain you control is conventional; it does
   not have to be a live website.
5. Tell me the final string and I will change it in the one place it appears.

**Cost:** free. **Time:** an evening of searching.

---

## 2. Create the Play Console account

**Blocks: the in-app product, license testers, the closed test, and therefore all
of Phase 6 testing.**

1. Sign up at the Google Play Console with the Google account you intend to own
   this app **permanently**. Moving an app between accounts later is painful.
2. Pay the **$25 one-time** registration fee.
3. Complete identity verification. This can take a few days — start it early.
4. Create the app entry using the package name from step 1.

⚠️ **The account age itself is not a gate, but the 12-tester closed test is** —
and that clock cannot start until the account and an uploaded build exist. See
step 6.

**Cost:** $25 once (~₹2,100). This is the *only* unavoidable spend in the whole
project; everything else is ₹0.

---

## 3. ⏱ Start recruiting testers — today, in parallel

**This is the longest-lead-time item and the only thing that can hard-block
launch.** It is calendar time, not work, so it must run alongside everything
else.

Google requires a **closed test with ≥ 12 testers opted in for 14 continuous
days** before a personal developer account can publish to production.

1. Open a spreadsheet. Collect **15 Gmail addresses**, not 12 — people drop out,
   and the count must hold *continuously*. If someone opts out on day 9, the
   clock breaks.
2. Target **≥ 3 real tradesmen** among them. `docs/06` §10 makes this a release
   gate, and it is also the only way you learn whether the metal-trades wedge is
   the right bet.
3. 🔴 **Never buy testers.** Paid tester services get accounts terminated — you
   lose the $25 and the account permanently.
4. They must be Gmail addresses, and each person must actually **opt in via the
   link** and open the app. Google asks how you tested.

**Cost:** free. **Time:** weeks of asking. Start now.

---

## 4. Write the privacy policy and put it on a public URL

**Required by Play whenever any SDK collects data — and AdMob does.**

Host it free on **GitHub Pages**. No domain needed.

It must cover **AdMob** (advertising ID, per UMP consent) and **Crashlytics**
(crash logs and device state). That is the whole list — the app collects nothing
from users itself, so there is no submitted data to declare.

✅ **Already written**: [`privacy-policy.html`](../privacy-policy.html) at the
repo root, with your contact details filled in. Publish it via GitHub Pages
(Settings → Pages → branch `main`, folder `/(root)`).

---

## 5. ~~Feedback form and redirect page~~ — no longer needed

🔴 **Dropped 2026-08-07.** The app collects nothing from its users, so there is
no form to create and no redirect page to host. If you already made a form, you
can delete it. See [`09-feedback-channel.md`](09-feedback-channel.md).

The privacy policy still needs publishing (step 4) — AdMob and Crashlytics make
that mandatory regardless.

## 6. Create the in-app product and license testers *(the actual Phase 6 blocker)*

Needs steps 1 and 2 done first.

1. In Play Console → Monetise → In-app products, create **one** product:
   - Type: **one-time purchase, non-consumable** — *not* a subscription
   - Price: **$4.99**
   - Note the product ID you choose and send it to me.
2. Add **license testers** (Setup → License testing). These accounts can walk the
   entire purchase flow without spending real money. Without them, the billing
   matrix in `docs/06` §5 cannot be tested at all.
3. Set up **payments**: bank account and tax details. For India, confirm the
   export-of-service treatment of foreign app revenue **with a CA** before
   revenue arrives (`docs/00-gap-audit` §B4).
4. Verify Play's current service fee tier in the Console — the docs say 15% on
   the first $1M/year, but verify rather than assume.

---

## 7. Create the AdMob account and ad units

1. Sign up for AdMob with the same Google account.
2. Add the app (it can be added before it is live on Play).
3. Create **one banner** and **one interstitial** ad unit. Send me both IDs.
4. They go in `local.properties` and are injected via `buildConfigField` — never
   committed (`docs/02` §7).

🔴 **Google's test ad unit IDs are used in every non-production build, always.**
Clicking a live ad in your own app **terminates the AdMob account permanently and
forfeits earnings**. This is a hard rule in `CLAUDE.md` rule 8, not a convention.

---

## 8. Firebase project for Crashlytics

Free. Create a Firebase project, add an Android app with the package name from
step 1, download `google-services.json`.

**Do not commit it** — it is in `.gitignore` already.

---

## 9. Generate the upload keystore — and back it up twice

Needed before the first upload, and it is the one unrecoverable mistake.

> 🔴 **Losing the upload keystore means the app can never be updated again.** Not
> "hard to recover" — impossible.

1. Generate `upload-keystore.jks` locally.
2. Copy `keystore.properties.example` → `keystore.properties` and fill it in.
   Neither the `.jks` nor `keystore.properties` is ever committed; both are in
   `.gitignore`.
3. Back up the keystore **and its passwords** in **two places off this machine** —
   e.g. a password manager and an encrypted cloud folder.
4. Enrol in **Play App Signing** at first upload, so Google holds the app signing
   key and you hold only the upload key.

---

## The short version, in order

| # | Do this | Cost | Unblocks |
|---|---|---|---|
| 0 | 🔴 Show the app to one real tradesman | free | the reason to continue |
| 1 | 🔴 Decide app name + package name | free | 2, 5, 6, 7, 8 |
| 2 | Create Play Console account | **$25** | 6, and the closed test |
| 3 | ⏱ Start recruiting 15 testers | free | launch itself |
| 4 | Privacy policy on GitHub Pages | free | store submission |
| ~~5~~ | ~~Google Form + redirect~~ — **dropped** | — | — |
| 6 | In-app product + license testers | free | **Phase 6 billing** |
| 7 | AdMob account + 2 ad unit IDs | free | **Phase 6 ads** |
| 8 | Firebase project | free | Crashlytics |
| 9 | Keystore + two off-machine backups | free | first upload, forever |

**Steps 0, 1, 2, 3 are the critical path.** 6 and 7 are what Phase 6 actually
waits on.

## What I can do while you do this

Say the word on any of these — they are writing, not code, and none of them need
an account:

- Draft the **privacy policy** text
- Draft the **store listing**: title, 80-char short description, long
  description — `docs/00-gap-audit` §B8 calls ASO the entire distribution channel
- Write the **billing and ads code** against the API, ready to test the moment
  license testers exist
- Add **oracle case `O-10`** from the tradesman's real job
