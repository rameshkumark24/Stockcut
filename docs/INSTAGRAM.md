# Instagram: does a meme-led strategy fit StockCut?

**Date:** 2026-09-27 · **Owner:** Rameshkumar · **Status:** a recommendation plus an 8-week experiment. Nothing has been started.
**Upstream:** [`01-prd.md`](01-prd.md) · [`00-phase-0-scope-feasibility.md`](00-phase-0-scope-feasibility.md) §4 · [`15-free-launch-and-paywall-plan.md`](15-free-launch-and-paywall-plan.md) · [`16-store-listing.md`](16-store-listing.md)

> **Missing inputs.** The brief named `docs/PRD.md`, `docs/COMPETITORS.md` and
> `docs/GROWTH.md`. The PRD exists, as `01-prd.md`. The other two do not exist
> anywhere in the repo, on any branch. The USP and competitor facts here come from
> `00-phase-0` §4 and `16-store-listing.md`. Where the audience is was researched
> for this document (§1.6–1.7), and the five numbers are defined in §7.6. If a
> GROWTH.md is ever written with different numbers, reconcile the two there.

---

## 0. The verdict

**The content fit is good. The acquisition fit is poor. Instagram cannot be
StockCut's scalable acquisition channel.**

Run it as a capped 8-week experiment with kill rules (§7). Treat it as a lab for
finding which message works, then move the winners into channels that convert
better (§9). Stop if the numbers in §7.6 do not show up.

### Why the content fits

- **The pain is physical and visual, and every trade has it.** The offcut rack,
  the piece 20 mm short, the blade width forgotten ten times over, fractions
  added up on the back of a delivery note. Trade meme culture already exists and
  it is large: `@hilarious.construction` has about 517K followers, and the welder
  meme pages range from 4K to 81K.
- **The product's output is itself the punchline.** The cut-plan screen packs
  pieces into bars like Tetris, and its numbers read like a joke's payoff:
  `offcut 1`, `7 bars · 2.0% waste`.
- **The CTA has no friction.** The app is free, with no sign-up and nothing
  locked, so there is nothing to sell.

### Why the channel does not fit: five leaks no meme can fix

| # | Leak | What it costs |
|---|---|---|
| 1 | **Most Tier-1 viewers cannot install.** StockCut is Android-only. iOS holds about 60% of the US, Canada and Australia, and about 54% of the UK. | A meme reaches everyone, but more than half of the Tier-1 viewers it reaches have nothing to install. |
| 2 | **The viewers who *can* install are in the markets that pay least.** English-language trade content on Instagram also reaches large Android-first markets. Per Phase 0 §3, India's interstitial eCPM is about $1.88, against $14.32 in the US. | Instagram installs will skew toward low-eCPM countries. That is fine for users and ratings, and poor for revenue. |
| 3 | **Every install is worth cents** (Appendix A: about $0.05–0.25 per US install over its lifetime, and a tenth of that in India). | You can never pay for reach: no boosts, no paid creators, no ads. Your time is the only cost, and it has to be capped. |
| 4 | **Instagram makes the click hard.** Links in Reels captions do not work. Clickable caption links are only a Meta Verified test. The path is Reel → profile → bio link → Play. | Typical rates: 8–15% of profile visits tap the bio link, and 2–5% of Story viewers tap a link sticker. |
| 5 | **Measurement stops at the install.** There is no analytics SDK, by design (TRD §2, CLAUDE.md "Do not"). Play Console sees UTM-tagged visitors and installs. Nothing sees whether an Instagram user ran a job or came back. | Activation, retention and revenue **cannot be measured per channel**. This document does not propose changing that. |

Two more risks, neither of them fatal:

- **Age.** Dave, the primary persona, is 42. Instagram skews young: people aged
  35–54 make up about 26% of its users, against about 35% on Facebook. Mike (35)
  and apprentices are on Instagram, and Dave is more often on Facebook.
- **Authenticity.** You do not work a trade, and this audience spots a fake
  tradesman within one clip: a wrong word, a missing glove, a saw used wrong.
  That is why a working tradesperson who vets every post is a precondition
  (§7.1, gate 0), not a nice-to-have.

### The order of magnitude

These rates are assumptions, used only to show the size of the result:

```
100,000 reach  (a strong month for a new account)
  ×1%   →  1,000 profile visits
  ×10%  →    100 bio-link taps
  ×45%  →     45 Android users reach the listing   (the rest are on iPhone)
  ×30%  →    ~15 installs
  ×60%  →     ~8 run a first optimize
```

**Expect tens of installs per 100,000 reach, not thousands.** Better execution
can change these rates, but it will not change the order of magnitude.

### What Instagram *is* good for here

1. **A message lab.** Sends and saves show which pain lands hardest. Test the
   winning lines where installs are decided, in Play Console **store listing
   experiments** (free A/B tests on the listing).
2. **An asset factory.** Every Reel made here can be reposted unchanged to
   Facebook Reels (the older audience, where Dave is), YouTube Shorts, and TikTok
   where it is available.
3. **A branded-search halo.** Some people who see the name will search
   "stockcut" in Play instead of tapping a link. Play Console shows search terms,
   so this lift can be measured (§7.5).

### Better channels, in order: the detail is in §9

1. **Put the app's name on the shared plan image.** This is the loop the
   product already has. A plan shared on WhatsApp reaches exactly the right
   person, and right now it does not say where it came from.
2. **ASO and store listing experiments**, fed by the messages that win on
   Instagram.
3. **Reddit**: r/metalworking (1.1M members), r/Carpentry (~608K),
   r/Construction (~589K) and r/Welding (~528K). Answer cut-list questions,
   within each subreddit's self-promotion rules.
4. **YouTube**: search-intent videos such as "how to work out cuts from 20 ft
   sticks".
5. **Facebook groups and Facebook Reels**, which match Dave's age group.
6. **Instagram**, as the capped experiment below.

---

## 1. The fit

### 1.1 Which problems make relatable memes

| Problem | Why it works as a meme | Emotion | How directly the product answers it |
|---|---|---|---|
| **The offcut rack.** "Might need it." Years of 300 mm bits. | Every shop has one. It is visual and self-mocking. | Relatability, "this is literally me" | **Strong.** The rack is where waste hides. |
| **Cut the long piece from the wrong bar.** Every remaining bar is now too short. | The tape was right and the plan was wrong, which is the unfair kind of mistake. | Frustration, humour | **Strong.** "Which piece comes off which bar" is the whole product. |
| **Forgetting the kerf.** Ten cuts at 3 mm is 30 mm, and it always comes off the last piece. | Most people have never done the sum, so it is a genuine "wait, what". | Surprise | **Strongest.** Kerf is the credibility signal in the store listing. |
| **Adding fractions in your head,** such as `2' 10 1/2"` plus `1 5/16"`. | "The math ain't mathing" is already a meme phrase. | Frustration, humour | **Strong** for the US. Fractional input is a Phase 0 wedge. |
| **The cut list on the back of a delivery note,** a 2×4 or cardboard. | It is universal and easy to photograph. | Relatability | **Strong.** It sets up a before/after. |
| **Ordered 7 bars and needed 8.** The drive back to the supplier. | A lost morning everyone remembers. | Frustration | **Strong.** The plan says how many bars to buy (US-06). |
| **The apprentice cuts from the old scribble.** | Crew humour, sent straight to the crew. | Humour | **Medium–strong.** The plan is shared as an image (US-09). |
| **No signal in a steel shed.** A cracked, dusty phone. Safety glasses on. | The setting is instantly recognisable. | Relatability | **Medium.** The app works offline and has large type. |
| **A tight nest:** `offcut 1`. | An oddly-satisfying clip. | Satisfaction | **Strong.** It is the result screen. |

**Avoid** job costing, quoting, offcut inventory and **2D sheet or plywood
nesting**. None of these are in the product. Attracting the 2D audience is
worse than attracting no one, because it brings "doesn't do sheets" one-star
reviews.

### 1.2 Which emotions to use

In order of usefulness:

1. **Relatability.** "This is literally me" is what makes people send a post.
   A meme gets sent to the mate who does the thing, and Instagram weights sends
   above likes, comments and watch time.
2. **Satisfaction.** A tight nest, a clean cut, `offcut 1`.
3. **Surprise.** The kerf sum. The bar that fits after all.
4. **Frustration.** Useful as the set-up, but it gets tiring if it is all you post.
5. **Humour.** This is how the other four are delivered, not a separate emotion.
6. **FOMO. Do not use it.** The app is free forever and nothing is scarce.
   Manufactured urgency is a lie, and this audience will call it out.

### 1.3 Which situations connect to the product

The product fits naturally only in situations about **planning the cut**: before
the saw, at the saw, or at the supplier's counter. Memes about bosses, pay,
weather, weld quality or site toilets reach the same people. But they build a
following of general blue-collar humour fans who will never install.

🔴 **Rule: every post is about cutting from stock lengths, even the ones with no
product in them.** Reach from off-topic posts is growth the funnel cannot use.

### 1.4 Formats that can become recurring series

| Series | Format | Why it can repeat |
|---|---|---|
| **Offcut of the Week** | A follower's offcut rack, reposted with credit | Endless supply, and people want to be featured |
| **The Kerf Tax** | One number fact per carousel | Blade width × cut count gives endless variations |
| **The Math Ain't Mathing** | A fractional-inch job, guess the answer, then the real run | Comment-bait with a real answer |
| **Guess the Waste** | Slide 1 is the job, slide 2 is the real result | A weekly quiz habit |
| **Check Our Maths** | One bar from a plan, added up in public | Builds trust, and this audience checks numbers |
| **Your Job, Solved** | A reply Reel that runs a follower's job on camera | The product working on strangers' real jobs |

### 1.5 Which trends can be adapted

See §3: a trend is used only when its score says so, and the reasons are given
there.

### 1.6 Niche audiences that overlap with the users

| Audience | Match | Notes |
|---|---|---|
| Welding and fabrication | **Core** (Dave) | Metric in the UK, AU and India, imperial in the US. Tube, box section, angle. |
| Framing, deck building, carpentry | **Core** (Mike) | US fractional inches. 8/10/12/16 ft studs. |
| General blue-collar humour | **Reach, low intent** | Large, and mostly people who do not cut stock. Borrow its formats, not its topics. |
| Garage fabricators, makers, DIY metalwork | **Secondary** (Raj) | They leave reviews, which ASO depends on (PRD §3). |
| Woodworking hobbyists | ⚠️ **Mismatch risk** | Mostly sheet goods. Show bars, tube and studs so this audience can tell the app isn't for sheets. |
| Aluminium windows and doors, shopfitting, balustrades | **Good** | Extrusion cut from fixed lengths. |
| Fencing, rebar and steel fixing | **Good** | Rails, posts and bar, all cut from stock lengths. |
| Electricians (conduit), plumbers (pipe) | **Untested** | The listing names conduit and pipe. Check with a working electrician before targeting them. |
| Apprentices and trade students | **Medium** | Young and Instagram-native, but US teens and young adults skew heavily toward iPhone. |

### 1.7 Accounts, pages and creators that already have this audience

Follower counts were taken from search results in September 2026. **Check them
before contacting anyone.**

| Account | What it is | Followers |
|---|---|---|
| `@hilarious.construction` | Construction humour page | ~517K |
| `@thebluecollarbarbie` (Lindsey Hover) | Welder and fabricator creator | ~160K |
| `@the.fabricator` (The Fabrication Series) | Fabrication creator, also on YouTube | ~98K |
| Dabs Wellington (Sean Flottmann) | Welded stainless steel art | ~86K |
| `@weldermemesss` | Welding meme page | ~81K |
| `@ram_nation58` (Bob Moffatt) | Welder, fabricator and instructor | ~36K |
| `@weldermemes__` · `@welder.memes` | Welding meme pages | ~10K · ~3.6K |
| `@memesofwoodworking` | Woodworking meme page | ~2.6K |

Tool and welder manufacturers' own brand accounts are also where this audience
gathers.

**How to approach them:** do it the way this audience does, with a real
comment on their work, and never a link in someone else's comments. Once
StockCut has a few posts worth sharing, offer a **Collab post**, which shows on
both accounts, to one fabrication creator. Offer only the free tool. Paying a
creator can never pay back at cents per install (Appendix A). An instructor such
as `@ram_nation58` is the best fit: teaching how to plan cuts is already their
job.

### 1.8 Which content formats work in this niche

| Format | Strength | Use it for |
|---|---|---|
| **Reels**, raw and shot on a phone, with **shop sound** | Reach to non-followers and sends | Problem, POV, reaction, satisfying and before/after posts |
| **Carousels** | About 2× the saves of a Reel per impression, and about 3–4× on educational posts | Kerf maths, 10/10 habits, "check our maths", quizzes |
| **Stories** | The only in-feed link (the sticker); reach followers only | Weekly link sticker, polls, "guess the waste" |
| **Single images** | The weakest format | Only for two-panel or starter-pack memes |

### 1.9 How to introduce the product without the post feeling like an ad

1. **Posting mix: about 50 / 35 / 15.** Half the posts have no product in them.
   About a third have the product as the punchline, and about one in seven is
   led by the product. The experiment in §7 settles the real ratio.
2. **The product is the resolution, never the hook.** The first 1.5 seconds, or
   the first slide, is always the problem.
3. **Real UI, real numbers.** Every plan shown is a real run of the app, the same
   rule as `16-store-listing.md`. 🔴 **Never mock a result.** This audience reads
   numbers for a living and will add them up.
4. **No logo stickers and no "download now" on the image.** The name appears
   once: inside the UI itself, or in the last line of the caption.
5. **Say "Android" in every CTA.** It saves iPhone users a wasted tap, and
   stops a thread of "where's the iPhone version".
6. **Captions follow the app's voice** ([`04-uiux-brief.md`](04-uiux-brief.md) §1):
   blunt, like a colleague, no exclamation marks, no emoji. The humour comes
   from the situation, not the punctuation.
7. **The account is visibly StockCut.** 🔴 Never run an anonymous meme page that
   pushes the app. That is covert advertising under the FTC endorsement guides
   and UK ASA/CAP rules. It will also be found out.
8. **Only claims you can defend:**

| ✅ Say (verifiable) | ❌ Never say |
|---|---|
| Counts the blade on every cut | "Optimal" or "perfect" plan. The optimizer is best-fit-decreasing plus an improvement pass, and it does not promise an optimum. |
| Works with no signal | "Zero waste" |
| Fractional inches: type `1 5/16"` | "Saves you £200 every job." That is a savings claim with no data behind it. |
| Free, no sign-up, nothing locked | "#1", "best", "the only" |
| Tells you when a piece won't fit | Anything about a named competitor |

---

## 2. The meme content framework

| Type | What it does | StockCut example | Product role |
|---|---|---|---|
| **Problem meme** | Names the pain before the product exists | The offcut rack as a retirement plan (C01) | None |
| **Before vs after** | The manual way, then the product | Delivery-note scrawl, then the bar-by-bar plan (C05) | The "after" |
| **POV** | Puts the viewer in the moment | "POV: you finally sorted the cut list…" (C10) | The twist or payoff |
| **Relatable situation** | An everyday moment in the trade | "Tell me you cut steel for a living…" (C08) | None |
| **Reaction** | A recognisable face at a recognisable pain | The stare at "mm, metres, feet" in one job (C18) | The payoff |
| **Alternative** | The frustrating old workflow against ours. 🔴 **Only generic workflows** (pencil, spreadsheet, "browser tools with no signal"), **never a named product.** OptiCutter, Cutlist Evolution and the rest stay in the Phase 0 doc. | No-signal shed vs airplane mode (C11) | The contrast |
| **Product-integrated** | Real UI or screenshots inside the meme | Check our maths (C06), offcut 1 mm (C12) | The subject |
| **User-generated** | A format followers can copy and tag you in | Offcut of the Week (C20), Your Job, Solved (C16) | The reason to join in |

---

## 3. Trend research: September 2026

### 3.1 The scoring rule

Each trend is scored 1–5 on three factors:

- **Trend:** is it alive right now?
- **Audience:** does this audience recognise it?
- **Product:** does it carry a StockCut situation without strain?

A trend is **used** only if the product of the three scores is ≥ 48 **and** the
product score is ≥ 4. Below that, borrow the *idea* in an original format, or
skip it.

### 3.2 Current trends, scored

| Trend (Sept 2026) | How it works | T·A·P | Score | Verdict and why | Rights |
|---|---|---|---|---|---|
| **Flop-core** | Post your failures instead of your highlight reel | 4·4·5 | **80** | **Use.** Every cut-list mistake is a flop this audience has made. A carousel needs no audio. → C13 | Original, no issue |
| **"10/10 habits"** | List the habits you genuinely recommend on one narrow topic | 4·4·4 | **64** | **Use.** It is practical rather than preachy, and it is a carousel, which is what drives saves. → C14 | Original, no issue |
| **"Bad Dream"** | A product suffers a disaster, then wakes up safe in bed | 4·3·4 | **48** | **Use, once.** "The last piece was short… it was a dream." → C19 | 🔴 The trend audio is a brand's own track. Recreate it with your own sound. |
| **"Potential-maxxing"** | Proving progress with hard numbers instead of affirmations | 3·3·5 | 45 | **Idea only.** "Numbers, not vibes" is already the brand voice. Use real plan numbers, without the trend's label or creator-culture framing. | Original |
| **"Holy f–ing airball"** | A confident guess that misses completely | 4·3·3 | 36 | **Idea only.** Use it as text, e.g. "eyeballed the cut list: airball". Skip the audio: the clip's rights are unclear and it swears, which is off-voice. | ⚠️ Source clip |
| **"What did my husband pray for?"** | A calm partner, then a chaotic reveal | 5·2·1 | 10 | **Skip.** | 🔴 Built on a Taylor Swift track |
| **"Kinda chic to…"** | Quiet pride in something unrelated to money | 4·1·2 | 8 | **Skip.** The register is wrong for this audience. | Original |
| **"Hallelujah" gratitude lists** | Listing things you're grateful for | 3·2·2 | 12 | **Skip.** | — |
| **Throwback audio ("Timber", "September", "Right Round")** | Fast-cut edits to a nostalgic track | — | — | **Skip.** "Timber" for timber framers is exactly the tempting trap. | 🔴 Commercial tracks, not licensed for a brand account |

### 3.3 Evergreen structures

These are *structures*, not images. Build every one from your own footage,
photos or plain text.

| Structure | Fit | Build it from |
|---|---|---|
| **POV:** … | High | Your own shop footage with text over it |
| **Nobody: / Me:** | High | Footage of the offcut rack |
| **Expectation vs reality** | High | Office laptop vs a dusty phone |
| **Starter pack** | High | Your own photos: pencil stub, delivery note, bent tape hook |
| **Tell me you're a ___ without telling me** | High (it drives comments) | A montage of your own footage |
| **The math ain't mathing** | High | Plain text, a phrase meme |
| **Oddly satisfying** | High | Chop saw sound plus the plan screen |
| **Quiz / guess** | High | Carousel: the job, then the real result |
| **Reply-to-comment Reel** | High (UGC loop) | Instagram's built-in reply sticker plus a screen recording |
| **How it started / how it's going** | Medium | Two photos |
| **Tier list** | Medium | Ranking ways to write a cut list |

### 3.4 🔴 Formats a brand account must not use, and the original alternative

Instagram's music library licenses tracks for *personal* use. Business accounts
are limited to the **Meta Sound Collection** (about 14,000 tracks cleared for
commercial use). As of 2026, audio fingerprinting flags reused sound within
seconds. A brand account promoting a product is the kind of account that gets
takedowns and lawsuits. *This is a list of risks, not legal advice.*

| Tempting format | The problem | Original alternative |
|---|---|---|
| Drake "Hotline Bling" two-panel | Music video still | The same two-panel, using your own hand gestures over two shop photos |
| Distracted boyfriend | Licensed stock photo of real people, whose likeness has publicity rights | Three labels over three of your own photos: *you* / *the pencil list* / *the plan* |
| "This is fine" dog | KC Green's comic. The artist has objected to commercial use. | Your own photo of a van dashboard buried in delivery notes, with different wording |
| Surprised Pikachu | Nintendo / The Pokémon Company IP | Your own reaction shot |
| Spider-Man pointing | Marvel / Sony | Two identical offcuts photographed side by side |
| Gru's plan (4-panel) | Film stills (Illumination / Universal) | Four of your own photos: plan, plan, plan, 30 mm short |
| "They're the same picture" (The Office) | NBCUniversal | Two of your own photos |
| Hide the Pain Harold | A real person's likeness, used in advertising | Skip it |
| Trending songs | Personal-use licence only | **Shop sound** (chop saw, grinder, tape snap), a voiceover, or the Meta Sound Collection |

**Shop sound is the best answer.** It is original, it cannot be fingerprinted,
it sounds like the trade, and "saw ASMR" is a genre in its own right.

### 3.5 Hashtags and search

- **Five hashtags at most.** Since December 2025, a post with more than five
  hashtags is excluded from Explore, hashtag pages and Reels recommendations.
  Tags in the caption and the first comment count together.
- **The pattern is one tag from each slot:** trade · problem · humour · material ·
  (optional) region. Tags seen in this niche: `#welding` `#fabrication`
  `#weldlife` `#weldinglife` `#weldernation` `#tigwelding` `#weldporn`
  `#bluecollarhumor` `#carpentry` `#framing`, plus `#cutlist` for the problem.
  Before adopting one, check the tag page's recent posts to confirm it is live
  and relevant. Rotate them.
- **Instagram search reads words, not just tags.** Put "cut list", "offcut",
  "kerf", "steel tube" and "20 ft sticks" in the on-screen text, the first line
  of the caption and the alt text.
- **The profile name field is searchable.** Set it to `StockCut | Cut list
  optimizer` (29 of 30 characters).

### 3.6 Topics in the news that connect

- **September means new apprentices starting.** First-week apprentice memes
  (kindly ones) land now.
- **Material prices.** When steel or lumber prices or tariffs are in the news,
  waste is money that week. Post the waste-in-currency angle, but as a
  hypothetical ("on a £2,000 job, 10% is £200"), never as a savings claim.
- **The "Gen Z into the trades" wave.** Blue-collar creators are recruiting
  young people into the trades. Tips that sound like one colleague teaching
  another fit that.

---

## 4. The content system

### 4.1 Five pillars

| Pillar | Purpose | Audience | Meme formats | Concepts | CTA strategy | Share of posts |
|---|---|---|---|---|---|---|
| **P1 · The Offcut Bin**: the pain | Reach and sends; establish that "we get it" | Everyone who cuts stock | Problem, relatable, flop-core, Nobody/Me, starter pack | C01 C02 C08 C09 C13 C15 | Mostly **soft**. **Problem** CTA only when the post names a fixable mistake. | ~35% |
| **P2 · Kerf & the Maths**: the hidden arithmetic | Saves and credibility; teach the thing the app does | Fabricators (kerf), US framers (fractions) | Carousels, "math ain't mathing", 10/10 habits, reaction | C03 C04 C14 C18 | **Problem** or **curiosity**. The product is always the last slide. | ~20% |
| **P3 · Pencil vs Plan**: the transformation | Profile visits and link taps | People already feeling the pain | Before/after, oddly satisfying, trend adaptation, check the maths | C05 C06 C12 C19 | **Curiosity** or **direct**. Pin the best one. | ~20% |
| **P4 · Site Reality**: the conditions | Sends to the crew; shows the app is phone-native | Site workers, apprentices | POV, expectation vs reality, alternative | C10 C11 C17 | **Problem** or **curiosity** | ~15% |
| **P5 · Your Job**: participation | Comments, UGC, and the closest thing to activation you can see | Engaged followers | Quiz, reply Reels, UGC reposts | C07 C16 C20 | **Soft** (join in) or **direct** (reply Reels) | ~10% |

### 4.2 Twenty meme concepts

Screenshot files are in [`store/screenshots/`](../store/screenshots/). Every
number shown comes from a real run of the app. `01-cut-plan.png` is 6000 mm
bars with a 3 mm blade.

---

**C01 · The 312 mm retirement plan** · P1 · Problem/relatable · Reel, 7–10 s
- **User problem:** Waste hides in the offcut rack. Nobody counts it as waste, because "it'll get used".
- **Hook (0–1.5 s):** A slow pan along a rack of short offcuts, each one labelled in marker.
- **Format:** "Nobody: / Me:" as text over your own footage. Shop sound only.
- **On-image:** `Nobody:` / `Me, keeping 312 mm of 50×50 for "a job that's coming"`
- **Caption:** "Eleven years. Still waiting for the job that needs exactly 312 mm. What's the oldest offcut in your rack?"
- **CTA:** Soft. It asks a question people answer in the comments, and it gets sent to the mate with the worst rack.
- **Product connection:** None on screen. It plants the idea that later posts pay off: the rack is where waste hides.
- **Objective:** Reach, sends, comments.

**C02 · Measured twice, cut from the wrong bar** · P1 · Problem/reaction · Reel
- **User problem:** The tape was right but the cutting order was wrong, so no bar is left long enough for the last long piece.
- **Hook:** A finished piece held up to its gap, 40 mm short. A long pause.
- **Format:** A reaction: an original face (with consent) looks at the gap, then at the rack. The only sound is one sigh.
- **On-image:** `Measured twice. Cut once. Cut the long one from the wrong bar.`
- **Caption:** "The tape was right. The order wasn't. Every short bit in that rack was once a long bar. Send this to whoever did it last."
- **CTA:** Soft. It asks people to send it.
- **Product connection:** Implied. Which piece comes off which bar is the whole product.
- **Objective:** Sends.

**C03 · The kerf tax** · P2 · Problem, then product · Carousel, 5 slides
- **User problem:** Hand-written cut lists forget the blade.
- **Hook (slide 1):** `10 cuts. 3 mm blade. Where did 30 mm go?`
- **Format:** 2: a bar drawn with ten dark slivers between the pieces. 3: `Your pieces add up to 6000. So does the bar. It still won't fit.` 4: `The blade took 30 mm. It always comes off the last piece.` 5: the real Setup tab with the kerf set, then the plan.
- **Caption:** "Every cut eats a blade width. Ten cuts at 3 mm is 30 mm, and it comes off the last piece every time. StockCut counts the blade on every cut. Free on Android, link in bio."
- **CTA:** Problem.
- **Product connection:** Kerf, the credibility signal in the store listing.
- **Objective:** Saves, profile visits.

**C04 · The math ain't mathing** · P2 · Relatable, then product · Reel, 12–15 s · US imperial
- **User problem:** Adding feet, inches and fractions in your head.
- **Hook:** A pencil writes `2' 10 1/2" ×4 · 1' 10 3/4" ×6 · 1 5/16" ×12` on a 2×4. Text: `How many 8 ft sticks?`
- **Format:** Text over footage, then a screen recording of the "Workbench frame" job (`05-fractional-inches.png`): tap Optimize, show the answer.
- **On-image:** `How many 8 ft sticks?` → `the math ain't mathing` → the real count.
- **Caption:** "Type 1 5/16" and it takes it. No decimals, no converting. Guess the stick count in the comments before the end."
- **CTA:** Curiosity.
- **Product connection:** Fractional-inch entry, the Phase 0 wedge. ⚠️ Run the job against the stock shown on screen and post the real count.
- **Objective:** Comments, profile visits.

**C05 · The back of the delivery note** · P3 · Before/after · Reel
- **User problem:** Cut lists live on scraps of paper and get lost or misread.
- **Hook:** A crumpled delivery note covered in pencil sums, with two crossings-out.
- **Format:** Before, then a hard cut on a chop-saw sound, then after. **Build it from the `Example: gate frame` job**, which the app seeds on first launch ([`03-app-flow.md`](03-app-flow.md)). The first screen a new install sees then matches the meme that brought them in.
- **On-image:** `Before: the back of a delivery note` / `After: bar by bar, cut by cut`
- **Caption:** "Same job both times. One of them you can read at the saw. Free on Android, and the gate frame's already in there when you open it."
- **CTA:** Curiosity.
- **Product connection:** The core output, plus the seeded example that helps activation.
- **Objective:** Profile visits, link taps.

**C06 · Check our maths** · P3 · Product-integrated trust post · Carousel, 5 slides · **pin it**
- **User problem:** Tradesmen don't trust a plan from an app they don't know (Phase 0 risk A1).
- **Hook (slide 1):** `Don't trust an app. Check it.` over bar 1 from `01-cut-plan.png`: `2400 · 2400 · 1190 → offcut 1`.
- **Format:** 2: `2400 + 2400 + 1190 = 5990`. 3: `+ 3 cuts × 3 mm blade = 9 → 5999`. 4: `+ 1 mm offcut = 6000. The bar.` 5: `Every bar in every plan has to add back up to the full length. The app checks that itself before it shows you anything.`
- **Caption:** "Pick any bar in any plan: pieces, plus one blade width per cut, plus offcut and any end trim, equals the bar. If you ever find one that doesn't add up, tell us. StockCut is free on Android, link in bio."
- **CTA:** Direct. Someone checking the maths is already evaluating the app.
- **Product connection:** The kerf invariant (CLAUDE.md rule 3), made as a public promise.
- **Objective:** Saves, profile visits, link taps.

**C07 · Guess the waste** · P5 · Interactive quiz · Carousel, 2 slides, weekly
- **User problem:** Nobody knows how much a job wastes until the rack fills up.
- **Hook:** `6 m bars. These pieces. 3 mm blade. Guess the waste.` with the parts list from a real job.
- **Format:** Slide 2 is the real result card from the app.
- **On-image:** `Guess the waste %` → the real figure, e.g. `2.0%`.
- **Caption:** "Closest guess in the comments gets bragging rights. The answer is on slide 2, so no peeking."
- **CTA:** Curiosity.
- **Product connection:** The result screen is the answer key.
- **Objective:** Comments, saves, a weekly habit. ⚠️ Offer no real prize. A prize makes it a promotion, with legal rules in every country.

**C08 · Tell me you cut steel for a living** · P1 · Relatable/UGC starter · Reel
- **User problem:** The whole culture of improvised cut lists.
- **Hook:** A cut list written in pen on the cuff of a work glove.
- **Format:** "Tell me you're a ___ without telling me", as a montage of your own shots: a pencil worn to 2 cm, numbers written on the saw table, a cracked phone covered in metal dust.
- **On-image:** `Tell me you cut steel for a living without telling me`
- **Caption:** "Your turn. The best one in the comments gets its own video."
- **CTA:** Soft, asking people to join in.
- **Product connection:** None. The replies become reply Reels (C16 style).
- **Objective:** Comments, sends, UGC.

**C09 · The hand-written cut list starter pack** · P1 · Relatable · Single image, your own photos
- **User problem:** Every tool of the manual method in one frame.
- **Hook:** The starter-pack grid itself.
- **Format:** Six of your own photos: a pencil stub, a delivery note, a tape with a bent hook, a calculator showing `1.3125`, a rack of short bits, and a caption tile reading `one bar short, again`.
- **On-image:** `The hand-written cut list starter pack`
- **Caption:** "Missing anything? It's the calculator showing 1.3125, isn't it."
- **CTA:** Soft.
- **Product connection:** None. Every item is a pain that later posts resolve.
- **Objective:** Sends, comments.

**C10 · POV: the apprentice cut from the old list** · P4 · POV · Reel
- **User problem:** The plan has to leave the phone (US-09), and the scribbled version goes wrong.
- **Hook:** `POV: you finally sorted the cut list` over a worker at the chop saw, filmed with consent and wearing PPE.
- **Format:** The twist: `…and the apprentice cut from the old one`, with a close-up of `1190` misread as `1910`. The payoff is WhatsApp showing the shared plan image.
- **On-image:** As above. Blame the scribble, not the apprentice.
- **Caption:** "Send the picture, not the scribble. StockCut shares the plan as an image, bar by bar. Free on Android."
- **CTA:** Problem.
- **Product connection:** Share as image.
- **Objective:** Sends, straight into crew chats.

**C11 · No signal in the unit** · P4 · Alternative/POV · Reel
- **User problem:** Browser tools need signal, and steel sheds block it.
- **Hook:** A phone held up to the roof of a steel shed, showing one bar of signal.
- **Format:** Two parts: `Browser cut list tools in a steel shed:` with a loading spinner, then `This one:` with the plan loaded and the airplane-mode icon in the status bar, as on the real store screenshots.
- **On-image:** As above. Generic "browser tools" only, never a name.
- **Caption:** "Everything runs on the phone. Works in airplane mode, in a container, in a basement. The full demo is pinned on the profile."
- **CTA:** Curiosity.
- **Product connection:** Works offline.
- **Objective:** Profile visits.

**C12 · Offcut: 1 mm** · P3 · Satisfying, product-integrated · Reel, 6–8 s, loops
- **User problem:** The flip side of waste: the good feeling of a tight nest.
- **Hook:** A chop saw cut and a clean drop, then a slow zoom into `Bars 1–2 · 2400 · 2400 · 1190 → offcut 1`.
- **Format:** Oddly satisfying. Your own audio: the cut, then silence.
- **On-image:** `offcut: 1 mm`
- **Caption:** "Two bars. One millimetre left on each. We'll take it."
- **CTA:** Soft.
- **Product connection:** The result screen is the whole post.
- **Objective:** Sends, saves, rewatches.

**C13 · Cut list flops, ranked** · P1 · **Trend: flop-core** · Carousel
- **User problem:** Every classic mistake, each one a feature the app covers.
- **Hook (slide 1):** `Cut list flops. We've done all of them.`
- **Format:** One flop per slide: `Cut all the long ones first. Ran out of long bars.` · `Forgot the blade. Last piece 18 mm short.` · `Ordered 7 bars. Needed 8.` · `Drawing in mm. Supplier in metres. Client in feet.` · `Wrote 1190. Read 1910.` · `Which one's yours?`
- **Caption:** "Flop season. Number in the comments. It's usually all of them."
- **CTA:** Soft.
- **Product connection:** None on the slides. Each flop maps to a feature: bar assignment, kerf, bars needed, units, a readable plan.
- **Objective:** Sends, comments.

**C14 · 10/10 habits for cutting from stock lengths** · P2 · **Trend: 10/10 habits** · Carousel, 11 slides
- **User problem:** Nobody teaches cut planning; people pick it up by making mistakes.
- **Hook (slide 1):** `10/10 habits for cutting from stock lengths`
- **Format:** One habit per slide: write the blade width on the saw · count cuts, not just pieces · place the longest pieces first, then fill the gaps · trim the damaged end before you plan, not after · keep offcuts you'll really use and scrap the rest · label pieces as they come off · buy for the plan, not the guess · send the crew a picture, not a scribble · check the last bar before you cut the first · let the phone do the packing (the only product slide).
- **Caption:** "Save it for the next job. Which one would you add?"
- **CTA:** Problem. The habits name the problems, and the last slide answers them.
- **Product connection:** The final slide only.
- **Objective:** Saves.

**C15 · One bar short** · P1 · Problem · Reel
- **User problem:** Ordering by guesswork, then losing a morning to the drive back.
- **Hook:** A van dashboard on the motorway, with the text `40 minutes to the supplier. For one bar.`
- **Format:** Text over your own footage.
- **On-image:** `When you ordered 7 and needed 8`
- **Caption:** "The plan tells you how many bars before you order, not after. Free on Android, link in bio."
- **CTA:** Problem.
- **Product connection:** Bars needed per stock size (US-06).
- **Objective:** Profile visits, link taps.

**C16 · Your job, solved** · P5 · UGC, product-integrated · Reel replying to a comment
- **User problem:** "Would it work on *my* job?"
- **Hook:** Instagram's reply-to-comment sticker showing a follower's job.
- **Format:** A screen recording that enters their stock, pieces and blade, taps Optimize, and shows the plan.
- **On-image:** The comment sticker, then `Here's your plan.`
- **Caption:** "Drop your job in the comments: stock length, pieces and quantities, blade width. We run the ones we can, on camera."
- **CTA:** Direct.
- **Product connection:** The whole product, on a stranger's real job.
- **Objective:** Comments, profile visits, link taps. It is the closest thing to activation Instagram can show.
- 🔴 **Public comments only.** Never ask for jobs by DM and never keep them. The account follows the app's collect-nothing stance, even though CLAUDE.md rule 11 is about the app itself.

**C17 · Cut list tools: expectation vs reality** · P4 · Alternative · Reel
- **User problem:** Most tools assume an office, a keyboard and Wi-Fi.
- **Hook:** `Expectation:` a tidy desk, a laptop and a spreadsheet.
- **Format:** Then `Reality:` one glove off, thumb-typing on a dusty phone with safety glasses on. The payoff is StockCut on that phone, with its large type and big buttons.
- **On-image:** `Cut list tools: expectation` / `reality`
- **Caption:** "Built for the second one. Large type, big buttons, no sign-up, no signal needed. How it works is pinned on the profile."
- **CTA:** Curiosity.
- **Product connection:** Built for the phone, and it works at maximum font size.
- **Objective:** Profile visits.

**C18 · Three units, one job** · P2 · Reaction · Reel
- **User problem:** Mixed units: the drawing in mm, the supplier in metres, the client in feet.
- **Hook:** `Drawing: mm. Supplier: metres. Client: feet and inches.`
- **Format:** An original reaction shot of a long stare into the middle distance, then the unit picker in the job's setup.
- **On-image:** As above.
- **Caption:** "Pick the unit per job: mm, cm, m, decimal or fractional inches. The plan comes out in the same one."
- **CTA:** Curiosity.
- **Product connection:** The app's five unit systems. It does *not* mix units within one job, so do not imply that it does.
- **Objective:** Sends (the mixed-unit reality of the UK, AU and CA), profile visits.

**C19 · The bad dream** · P3 · **Trend: "Bad Dream"** · Reel
- **User problem:** The dread of the last piece coming up short.
- **Hook:** A dramatic slow-motion shot of the last piece coming up short, with a drone sound you made yourself.
- **Format:** Cut to someone jolting awake in the van at 5 am. The phone shows the plan's `offcut 28`.
- **On-image:** `the last piece was short` → `it was a dream. the plan had 28 mm spare.`
- **Caption:** "Sleep better. Free on Android."
- **CTA:** Soft.
- **Product connection:** The plan as the reassurance.
- **Objective:** Reach, sends. 🔴 Use your own audio, never the brand track the trend started with.

**C20 · Offcut of the Week** · P5 · UGC series · A credited repost, weekly
- **User problem:** The rack is a shared joke, so let people show theirs.
- **Hook:** A follower's rack photo.
- **Format:** Reposted with the owner's consent and credit, using a Collab post or Instagram's built-in repost.
- **On-image:** `Offcut of the week: @handle · 11 years of 50×50`
- **Caption:** "Rack of the week. Tag us in yours."
- **CTA:** Soft, asking people to take part.
- **Product connection:** Indirect.
- **Objective:** Comments, UGC, a reason to follow.
- 🔴 **Keep reposts a minority, at one per week at most.** Since April 2026, an account whose posts in a rolling 30-day window are mostly other people's content is removed from recommendations. Aggregators have reported reach drops of 60–80%.

**CTA mix across the 20:** soft 8 · problem 4 · curiosity 6 · direct 2.
**Product presence across the 20:** none in 6 · punchline in 9 · product-led in 5.
This is a menu. The posting calendar follows the 50 / 35 / 15 rule in §1.9.

---

## 5. The funnel

| Stage | What should happen | Your lever | Metric | Source | Leak to watch |
|---|---|---|---|---|---|
| **Reach** | Non-followers who cut stock see the post | Pillars, on-topic rule (§1.3), 5 hashtags, words in on-screen text | Reach, share of reach from non-followers | Instagram Insights | Reach from off-topic audiences who never install |
| **Engagement** | They send it to a mate, or save it | Relatability; carousels for saves | **Sends per 1,000 reach**, saves | Insights | Likes without sends: it amused people but did not land |
| **Profile visit** | "Who made this?" | Product as the punchline; curiosity CTAs | **Profile visits per 1,000 reach** | Insights | Product-free posts rarely produce visits, so balance the mix |
| **Bio / CTA** | The profile explains itself in 3 seconds, and they tap the link | Name field `StockCut \| Cut list optimizer`. Bio: "Cut lists for steel, tube and timber. Counts the saw blade. Works with no signal. Free on Android. No sign-up." (110 of 150 characters). **Pin C06, C04 and C05.** Highlights: *How it works* · *Check the maths* · *Your jobs* · *Offcuts* | External link taps; Story sticker taps | Insights | iPhone users tapping through; "Android" in the bio prevents this |
| **Store listing** | The listing continues the same message | The listing is already done (`16-store-listing.md`). Its screenshot 1 shows the same plan screen as the memes. | Store listing visitors, `utm_source=instagram` | Play Console acquisition, tracked channels (UTM) | A mismatch between meme and listing; changing the listing mid-experiment (§7.7) |
| **Install** | Android users in the target trades install | Nothing more on Instagram. This is the listing's job. | Store listing acquisitions by UTM campaign **and by country** | Play Console | A low Tier-1 share (leak 2) |
| **Activation** | The first optimize | The seeded `Example: gate frame` job. Posts that mirror it (C05) show people exactly what to tap first. | **Not measurable per channel.** Aggregate proxy: AdMob interstitial impressions (one per 5th optimize, at least 10 minutes apart) | AdMob, aggregate only | You cannot tell Instagram users apart from anyone else |
| **Retention** | A second job within 30 days (PRD metric 1) | Followers see the account between jobs, which reminds them the tool exists | **Not measurable per channel.** Aggregate only | Play Console user metrics, aggregate | The same; do not add an SDK to fix this (§8) |

---

## 6. CTA strategy

### 6.1 The four types

| Type | When | Where | Example lines |
|---|---|---|---|
| **Soft** | Very viral, relatable posts with no product in them | The last line of the caption | "What's the oldest offcut in your rack?" · "Send this to whoever did it last." · "Which one's yours?" · "Tag us in yours." |
| **Problem** | The meme names a mistake the app prevents | The caption, then "link in bio" | "The plan tells you how many bars before you order, not after." · "Every cut eats a blade width. StockCut counts it." · "Send the picture, not the scribble." |
| **Curiosity** | The post shows *part* of the product | The caption, pointing to a pinned post or slide 2 | "Guess before the end." · "The full demo is pinned on the profile." · "The gate frame's already in there when you open it." |
| **Direct** | High-intent posts: check-the-maths, reply Reels, demos | The caption, plus a Story link sticker the same day | "Free on Android. No sign-up. Link in bio." · "Try to break it: link in bio." |

### 6.2 Rotation rules

- **Never use the same CTA type twice in a row.**
- **At most one direct CTA in every four posts**, and only on product-led posts.
- **Only problem, curiosity and direct CTAs say "link in bio".** A soft post
  ends on its question, so the comments stay about the joke.
- **Every link CTA says "Android".**
- **Story link sticker:** once or twice a week, on the same day as a direct or
  problem post. The sticker is the only tappable link in the Instagram feed.
- **"Comment CUT and we'll DM the link"** (keyword-triggered DMs). Links sent by
  DM get tapped far more often than links in the feed (roughly 15–25% against
  1–4%). This is an **experiment cell** in weeks 7–8 (§7.3), not a default.
  - Use only an official Meta-API tool, and never hand over the account password.
    ManyChat's free plan dropped to 25 contacts in March 2026; others offer
    more. Check the terms before signing up.
  - The tool sends one link and keeps nothing. That keeps it consistent with the
    app's collect-nothing stance.

---

## 7. The posting experiment: 8 weeks

### 7.1 Week 0: setup and gates

- **Gate 0: one working tradesperson.** A fabricator or framer who reads every
  post before it goes out and lets you film their shop and offcut rack. This is
  the same open item as Phase 0 §3. 🔴 **Without this gate, do not start.**
  Content that gets the trade wrong damages the brand more than posting nothing.
- **Account:** a professional Business account with the handle `stockcut`, or
  the nearest free variant (check availability), and the name field and bio from
  §5.
  - Business accounts are limited to the Meta Sound Collection. Plan on shop
    sound anyway.
  - Trial Reels, which test a Reel on non-followers first, need **1,000
    followers**. Do not count on reaching that within 8 weeks.
- **Link scheme:**
  `https://play.google.com/store/apps/details?id=com.measure.stockcut&utm_source=instagram&utm_medium=<bio|story|dm>&utm_campaign=<wNN-cell>`
  - The bio link can only change **per week**. Set it to that week's test cell.
  - Story and DM links can change **per post**.
  - 🔴 **Test the link before week 1.** Tap it from the Instagram bio on an
    Android phone, and confirm the visit appears in Play Console under tracked
    channels (UTM) the next day or two. Play Console may delay or hide very small
    numbers, so check before relying on it.
- **Freeze the store listing** for the 8 weeks (§7.7).
- **Scheduling:** use Instagram's built-in scheduler, so you are not posting at
  01:30 IST.

### 7.2 Cadence and time budget

- **3 feed posts a week**: 2 Reels and 1 carousel.
- **2–3 Stories a week**, one of them with the link sticker.
- **15 minutes a day** replying to comments.
- **One 2-hour shoot every two weeks** at the tradesperson's shop, which yields
  6–8 clips.
- 🔴 **Hard cap: 4 hours a week, logged.** Hours are the real cost and the
  denominator of the verdict.

### 7.3 Phases

| Weeks | Goal | What varies | What stays fixed |
|---|---|---|---|
| **1–4 · Discovery** | Find which pillars land | Pillar and format. Each pillar is posted at least twice. | CTA by type (soft on product-free posts, problem on the rest), time slot rotated, 3 posts a week |
| **Week 4 checkpoint** | The content-fit gate (§7.7) | — | — |
| **5–6 · Exploit + hooks** | Double down on the best two pillars | **Hook:** problem statement vs question vs number-first (`30 mm`) vs POV | Pillar mix, CTA |
| **7–8 · Exploit + CTAs** | Find the CTA that moves people to Play | **CTA:** problem vs curiosity vs direct vs DM keyword | Pillar mix, hook style |
| **Week 8 checkpoint** | The channel gate (§7.7) | — | — |

### 7.4 The test matrix

**With installs in the tens, nothing here will reach statistical significance.**
Compare cumulative totals, normalised per 1,000 reach, with at least three posts
per arm. Decide by the rules written in advance in §7.7, not by the best week.

| Variable | Arms | How | Judged by |
|---|---|---|---|
| **Frequency** | 3 a week (weeks 1–4) vs 5 a week (weeks 5–8, **only if the week 4 gate passes and hours allow**) | Sequential, which is confounded by account growth; say so when reporting | Sends per post, **UTM installs per logged hour** |
| **Meme format** | Reel · carousel · single image | Rotate within each pillar | Sends per 1,000 reach (Reels), saves per 1,000 (carousels), profile visits per 1,000 |
| **Hook** | Problem · question · number-first · POV | Weeks 5–6, same pillars | 3-second hold (Insights), sends per 1,000 |
| **Caption** | One line vs a 4–6 line story | Alternate | Profile visits per 1,000, comments |
| **CTA** | Soft · problem · curiosity · direct · DM keyword | Weeks 7–8. The bio link's `utm_campaign` changes weekly, and each DM or Story gets its own. | Link taps, **UTM visitors and installs** |
| **Posting time** | Audience-local **6:30 am** (before site) · **12:30 pm** (lunch) · **8 pm** (evening). This is a hypothesis to test. | Rotate the three slots through the UK and US Eastern windows | Reach in the first 24 h, share of reach from Tier-1 (Insights, audience countries) |
| **Original vs trend** | Evergreen structure vs a scored trend (§3.2) | C13, C14 and C19 against originals from the same pillar | Sends per 1,000, non-follower reach share |
| **Product-heavy vs light** | None · punchline · product-led | Follow the 50/35/15 mix, then compare | **Profile visits and UTM installs per 1,000 reach.** Product-free posts will win on sends and lose on installs. The question is by how much. |

**Posting times in IST:**

| Audience slot | UK (BST, until 25 Oct) | UK (GMT, from 25 Oct) | US Eastern (EDT, until 1 Nov) | US Eastern (EST, from 1 Nov) |
|---|---|---|---|---|
| 6:30 am | 11:00 | 12:00 | 16:00 | 17:00 |
| 12:30 pm | 17:00 | 18:00 | 22:00 | 23:00 |
| 8:00 pm | 00:30 (next day) | 01:30 (next day) | 05:30 (next day) | 06:30 (next day) |

### 7.5 What is tracked, and where

🔴 **Likes and views are not recorded anywhere, not even in the sheet.** They
are not decision metrics. Reach is kept only as the denominator.

| Metric | Source | Grain | Attributable to Instagram? | Role |
|---|---|---|---|---|
| Reach | Instagram Insights | Per post | Yes | Denominator only |
| **Shares (sends)** | Insights | Per post | Yes | **Decision:** content fit |
| Saves | Insights | Per post | Yes | Diagnostic: reference value |
| Comments | Insights | Per post | Yes | Diagnostic |
| **Profile visits** | Insights | Per post | Yes | **Decision** |
| **Link clicks** (bio taps, sticker taps) | Insights | Per post or week | Yes | **Decision** |
| **Store visits** | Play Console, tracked channels (UTM) | Per week or campaign | Yes, through UTM | **Decision** |
| **Installs**, by country | Play Console, UTM store listing acquisitions | Per week or campaign | Yes, through UTM | **Decision** |
| **Branded search installs** (search term `stockcut`) | Play Console acquisition, search terms | Per week | Partly: the halo, which also has other causes | **Decision**, compared with the 4 weeks before |
| Activated users | *No per-channel measure exists.* Aggregate proxy: AdMob interstitial impressions | Per week | **No** | Context only |
| Retention | *No per-channel measure.* Play Console aggregate user metrics | Per month | **No** | Context only |
| Revenue | AdMob estimated earnings by country | Per week | **No** | Context only. At cents per install it cannot decide anything here. |
| Rating count and average | Play Console | Per week | No | Guardrail: ≥ 4.3 (PRD §7) |
| **Hours spent** | Your log | Per week | — | **Decision:** the denominator |

**The tracking sheet has one row per post.** Columns:

- date and slot
- pillar, concept ID, format, hook type, caption length, CTA type
- trend or original; product: none, punchline or led
- reach, sends, saves, comments, profile visits, link taps

It also has **one row per week**: UTM visitors, UTM installs (Tier-1 / other),
branded-search installs, AdMob interstitial impressions, rating count, hours,
and anything else that changed that week.

### 7.6 The five numbers

| # | Number | The question it answers |
|---|---|---|
| 1 | **Sends per 1,000 reach** | Does the meme land with people who know the pain? |
| 2 | **Profile visits per 1,000 reach** | Does it make them wonder who made it? |
| 3 | **UTM store listing visitors per week** | Do they take the step off Instagram? |
| 4 | **UTM installs per week, split Tier-1 (US/UK/CA/AU) vs other** | Do they install, and where are they? |
| 5 | **Installs per logged hour** (UTM installs plus the lift in branded-search installs) | Is this worth your time compared with §9? |

### 7.7 Decision rules, written before any data exists

These thresholds are this document's own, not industry benchmarks. They are set
so that Instagram has to earn roughly **three installs per hour of work** to
keep its time.

**Week 4, the content-fit gate:**
- **Continue** if the best pillar's median is **≥ 10 sends and ≥ 5 profile
  visits per 1,000 reach**.
- **If no pillar reaches 3 sends per 1,000 reach, the memes are not landing.**
  Make one reshoot of the weakest pillar with the tradesperson's input, then
  stop if it still misses.

**Week 8, the channel gate:**

| Result | Condition | Action |
|---|---|---|
| **Scale** | ≥ 100 UTM installs over the 8 weeks **and** ≥ 3 installs per logged hour **and** weeks 6–8 each above the weeks 1–3 average | Keep it, still capped at 4 h a week. Cross-post every Reel to Facebook Reels. Try Trial Reels at 1,000 followers. |
| **Maintenance** | 30–99 UTM installs, or 1–3 per hour | Cut to one post a week, **repurposed** from content made for §9 channels, never made specially |
| **Kill** | < 30 UTM installs over the 8 weeks, or no single week with ≥ 5 | Stop posting. Leave the account up with the pinned posts and the bio link, since a dormant profile still serves anyone who searches for it. |

**Tier-1 share is reported, not a gate.** If fewer than 20% of Instagram
installs are Tier-1, the channel is building users but not revenue. That is
acceptable under the "users first" decision in `15`, but it has to be stated
plainly in the week 8 write-up.

**Protect the reading from other causes:**
- **Keep the store listing frozen** for these 8 weeks, since a listing change
  shifts the install rate for all traffic, including Instagram's.
- **Run store listing experiments *after* week 8,** using the hooks that won
  here.
- Log every other change in the weekly row: a release, a Reddit post, a spike
  in reviews.

---

## 8. Rules that hold whatever the numbers say

1. **Never pay for reach.** No boosts, no ads, no paid creators, no bought
   followers, no engagement pods. At cents per install (Appendix A), none of it
   pays back.
2. **Never name a competitor in a negative light.** The competitor names stay in
   the Phase 0 doc.
3. **Never show a plan the app didn't produce.**
4. **Make only verifiable claims** (§1.9).
5. **No unsafe work on camera.** Glasses, gloves, guards on, and no angle
   grinder with its guard removed. The comments will notice, and so will a
   lawyer.
6. **Get consent from everyone filmed.** Never film a client's site or drawings
   without permission.
7. **No copyrighted audio or film stills** (§3.4).
8. **Most posts must be original** in any rolling 30-day window, to satisfy the
   originality rule.
9. **No anonymous meme page** promoting the app.
10. 🔴 **Nothing in the app changes to support this channel.** No analytics SDK,
    no referral codes, no install IDs, no "how did you hear about us". That
    follows TRD §2, CLAUDE.md rule 11 and "Do not". The UTM sits on the store
    link, outside the app. If activation per channel stays unknowable, that is
    the accepted price of an app that collects nothing.

---

## 9. What to do instead, or alongside

Ranked by the expected installs per hour of your time, from highest to lowest.

| # | Channel | Why it beats Instagram for this product | First step |
|---|---|---|---|
| 1 | **Put the app's name on the shared plan image** | Every plan shared on WhatsApp reaches exactly the right person: the apprentice, or the next fabricator on site. [`PlanRenderer.kt`](../app/src/main/kotlin/com/stockcut/ui/result/PlanRenderer.kt) currently draws the job name, the summary line and the bars, **with no app name anywhere**. One plain line such as "Planned with StockCut, free on Google Play" turns every share into a recommendation from a colleague. | A code change, **not made here** because this brief was docs only. It needs a decision and its own PR. Keep it plain text: no tracking link, no identifier (rule 11). |
| 2 | **ASO + store listing experiments** | This is where installs are decided, and it costs ₹0. `16` already calls ASO "the entire funnel". | After the week 8 gate, A/B test the short description and screenshot 1 caption against the two best hooks from Instagram. |
| 3 | **Reddit**: r/metalworking (~1.1M), r/Carpentry (~608K), r/Construction (~589K), r/Welding (~528K) | People there are already asking the question the app answers. Text posts, and replies that last for months. | Read each subreddit's self-promotion rules first. Answer "how do I work out cuts from 20 ft sticks" threads with the method, and mention the tool only where the rules allow. |
| 4 | **YouTube**: search-intent tutorials | People search "how to calculate a cut list" when they have the problem, which is the opposite of scrolling past a meme. | One 3–5 minute video: the kerf sum by hand, then the same job in the app. The C03 and C06 carousels are its script. |
| 5 | **Facebook groups and Facebook Reels** | They match Dave's age group, and UK/AU trade groups are active. Cross-posting Reels costs almost nothing. | Cross-post every Instagram Reel. Join two trade groups and contribute before posting. |
| 6 | **Instagram** | The message lab and asset factory, as above | §7 |

---

## Appendix A: what an install is worth

These are **estimates** built from this repo's numbers and stated assumptions.
They could be off by 2–3×. They are not off by 100×.

**Ad rules in the app:**
- An interstitial shows on **every 5th optimize**, at least **10 minutes
  apart** (`Limits.INTERSTITIAL_EVERY`, `INTERSTITIAL_MIN_GAP_MILLIS`).
- The **only banner** is on the Projects screen.
- A user who runs one job with three optimizes and never returns sees **no
  interstitial at all**.

**Assumptions:**
- 60% of installs run a first optimize.
- 30% of those come back, which is the PRD target. So about 18% of installs
  become repeat users.
- A repeat user makes 10–20 optimizes a month, which is about 2–4
  interstitials, and sees about 10 banner impressions a month.
- A repeat user stays for 6–12 months.

**The US calculation:**
- Interstitial eCPM is $14.32 (Phase 0 §3). Banner eCPM is assumed to be
  about $1.
- A repeat user is worth about 2–4 × $0.0143 plus 10 × $0.001, which is roughly
  **$0.04–0.07 a month**.
- Over 6–12 months that is about $0.23–0.80.
- Multiplied by the 18% repeat rate, that is **about $0.05–0.15 per install**.
- One-off users add almost nothing.

**Other markets:**
- **India** (interstitial eCPM $1.88): about **$0.01–0.02 per install**.
- **Upper bound:** a heavier user who is active for a full year pushes the US
  figure toward **$0.25**.

**Conclusion.** Any paid install, whether a boost, an ad or a creator fee,
almost certainly costs more than an install earns over its lifetime. Instagram
is viable only as unpaid work with a time cap. Its return is **users and
ratings**, which is the job `15` gives v1, not money.

---

## Sources

Checked 27 September 2026. Follower counts, member counts and platform rules
change, so recheck any figure before acting on it.

- Instagram ranking signals, sends per reach: [Later](https://later.com/blog/how-instagram-algorithm-works/) · [Sprout Social](https://sproutsocial.com/insights/instagram-algorithm/) · [Socialync on Mosseri and shares](https://www.socialync.io/blog/adam-mosseri-shares-instagram-algorithm-2026) · [Dataslayer](https://www.dataslayer.ai/blog/instagram-algorithm-2025-complete-guide-for-marketers)
- April 2026 originality and aggregator rule: [Tubefilter](https://www.tubefilter.com/2026/04/30/instagram-removes-algorithm-recommendations-repost-content-aggregator/) · [PetaPixel](https://petapixel.com/2026/04/30/new-instagram-policies-target-reposted-content/) · [Sotrender, reach data](https://www.sotrender.com/blog/2026/08/instagram-reach-is-down-what-the-data-actually-says/)
- Five-hashtag limit: [Later](https://later.com/blog/ultimate-guide-to-using-instagram-hashtags/) · [Social Media Today](https://www.socialmediatoday.com/news/instagram-implements-new-limits-on-hashtag-use/808309/)
- Trial Reels: [Instagram for Creators](https://creators.instagram.com/blog/instagram-trial-reels) · [Social Champ](https://www.socialchamp.com/blog/instagram-trial-reels/)
- Links: [Engadget, caption-link test](https://www.engadget.com/social-media/meta-is-testing-clickable-links-in-instagram-captions-for-verified-subscribers-184555406.html) · [Social Media Examiner](https://www.socialmediaexaminer.com/what-clickable-reels-links-and-hashtag-limits-mean-for-your-2026-instagram-strategy/) · [Creator Lane, link sticker](https://creatorlanehq.com/blog/instagram-story-link-sticker-2026) · [SmartMarketingTips, CTR](https://smartmarketingtips.com/what-is-the-best-instagram-ctr/) · [CreatorFlow, bio link vs DM](https://creatorflow.so/blog/bio-link-vs-dm-automation-affiliate-clicks/)
- DM automation: [ManyChat, rules](https://manychat.com/blog/instagram-dm-automation-rules/) · [Flowgent, tools and pricing](https://flowgent.ai/blog/instagram-dm-automation-tool)
- Music rights for business accounts: [Meta Business Help, licensed music](https://www.facebook.com/business/help/402084904469945) · [Audiodrome](https://audiodrome.net/for-creators/instagram-music-library/) · [SRIPLAW](https://sriplaw.com/blog/instagram-business-accounts-and-copyright/)
- September 2026 trends: [Newengen](https://newengen.com/insights/instagram-trends/) · [SocialBee](https://socialbee.com/blog/latest-instagram-trends/) · [Mean CEO](https://blog.mean.ceo/instagram-trends-september-2026/) · [Later, Reels trends](https://later.com/blog/instagram-reels-trends/) · [SocialPilot](https://www.socialpilot.co/blog/instagram-reels-trends)
- Carousels vs Reels: [Metricool 2026 study](https://metricool.com/press-release-instagram-study-2026/) · [Contentdrips](https://contentdrips.com/blog/2026/06/instagram-reels-vs-carousels-2026-guide/)
- Demographics: [Hootsuite, Instagram demographics](https://blog.hootsuite.com/instagram-demographics/) · [Statista, US Instagram users by age](https://www.statista.com/statistics/398166/us-instagram-user-age-distribution/)
- iOS / Android share: [SQ Magazine](https://sqmagazine.co.uk/iphone-vs-android-statistics/) · [MobiLoud](https://www.mobiloud.com/blog/android-vs-ios-market-share/)
- Trade accounts and creators: [@hilarious.construction](https://www.instagram.com/hilarious.construction/) · [@weldermemesss](https://www.instagram.com/weldermemesss/) · [@weldermemes__](https://www.instagram.com/weldermemes__/) · [@welder.memes](https://www.instagram.com/welder.memes/) · [@memesofwoodworking](https://www.instagram.com/memesofwoodworking/) · [@the.fabricator](https://www.instagram.com/the.fabricator/) · [Welding influencers list](https://influencers.uppromote.com/social-media/instagram/welding-influencers/) · [Yahoo Finance on blue-collar influencers](https://finance.yahoo.com/small-business/articles/blue-collar-influencers-recruit-170000400.html)
- Subreddit sizes: [reddapi r/welding](https://reddapi.dev/subreddits/welding/insights) · [reddapi r/construction](https://reddapi.dev/subreddits/construction/insights) · [GummySearch r/metalworking](https://gummysearch.com/r/metalworking/) · [GummySearch r/Carpentry](https://gummysearch.com/r/Carpentry/)
- Tradespeople on social media: [ServiceTitan](https://www.servicetitan.com/blog/social-media-for-tradesmen) · [Tradesman Saver](https://www.tradesmansaver.co.uk/tradesman-insights/tiktok-tradespeople-how-social-media-is-changing-the-industry/)
- Trade culture (offcuts, board stretcher): [Arc Life, the good scrap pile](https://blog.weldsupportparts.com/2026/08/08/good-scrap-pile-welders-see-possibility/) · [JC, board stretcher](https://jimconnors.net/interesting-things-with-jc/2025/4/11/1249-what-is-a-carpenters-board-stretcher)
- Play Console UTM and acquisition reporting: [Play Console, acquisition reporting](https://play.google.com/console/about/acquisitionreporting/) · [Play Console Help](https://support.google.com/googleplay/android-developer/answer/6263332?hl=en-GB) · [AppRadar guide](https://appradar.com/academy/google-play-console-guide)
