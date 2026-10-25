# FutSlot design rules: Broadsheet

FutSlot is set like a newspaper front page: near-black Source Serif 4 on paper white, two spot inks used sparingly. Apply these rules to every screen (player web, public pages, dashboard, admin, mobile). Reference mockups: `design_handoff_broadsheet/reference/`.

## Layout
- Left-aligned and asymmetric. Headings flush left; whitespace on the right.
- No boxes, no dividers between sections. Separate sections with 56–72px of space.
- Use `.card` / a surface fill only for genuinely discrete items (a listing in a picker, a dialog). Never to group page sections.
- Rules appear only as front-page furniture: the thick-thin head rule (2px + 1px, full ink) around a dateline row, 1px ink rules closing an index, and `--color-divider` hairlines between list rows.
- Lists of times/prices are an *index*: label · dotted leader · value.
- Don't tighten spacing. Touch targets ≥ 44px on phones.

## Type
- Source Serif 4 for everything, including buttons, nav and labels. No sans-serif.
- Hierarchy from size and weight: display 72–84px, H2 28–40px, H3 21–24px, body 16–19px, labels 13px uppercase with 0.08em tracking.
- Italic (true italic, weight 400) for emphasis and the second half of a headline: "Tonight, 18:00 *at Dhuku.*"
- Tabular figures for times and prices.
- Muted text = text color at 70% (small) / 78–82% (body). Never below 4.5:1 for small text.

## Color
- Paper `#f3f2f2`, ink `#201e1d`.
- Cyan `#0088b0` = interactive (primary buttons, links via the 700 step `#006786`) and "settled/confirmed".
- Magenta `#d6006c` (text: `#aa0b56`) = rarer; "needs your action".
- Never both accents in one small component. Process yellow is for print treatments only.
- Status: Confirmed → cyan tag; Held / share due / no-show → magenta tag; Checking / waitlist / played / cancelled → neutral tag.

## Night edition (dark)
The handoff had no dark surfaces; this is FutSlot's own, the same page printed for a dark room. It follows the device
unless the reader picks Light or Dark (player app: Me → Appearance; public pages: the moon/sun in the masthead). One
choice, `localStorage` `futslot.theme`, covers the app and the public pages on the same address; `.dark` on `<html>`
switches the tokens (player `src/styles.css`, `site-assets/futslot.css` + `theme.js`), set before first paint.
- Paper `#151413` (the ink's own warm hue), ink `#e6e4e1`. Muted ink keeps its alpha steps over `--ink-rgb`.
- Surfaces step lighter as they rise: card / chosen segment `#211f1e`, track / hover `#1a1918`.
- Cyan lifted to `#2aa9d6` for fills, with dark type on it; links and pressed `#63c4e8`; tints `#0e2a35` / `#143a48`
  with `#b3e5f7` type. Magenta `#f0559b`, text `#ff86b8`, tint `#35141f`. Every pairing is at least 6.5:1.
- Plates add instead of multiply (`.cmyk-num` screen blend: a pale core with cyan and magenta fringes); photographs
  sit at 85% brightness.
- Never pure black or white, and no new colors: every value is a step of an existing ink.

## Imagery and numerals
- Hero photos: `.cmyk` (four misregistered process plates). Listing/interface photos: `.halftone`.
- Big moment numbers (countdown, step numbers): `.cmyk-num`.

## Icons
Phosphor, duotone weight, inheriting text color. Use them in navigation and action lists; leave them out of headings and prose.

## States
- Hover: accent 600 or a 7–10% tint; pressed: accent 700 or 14–18% tint.
- Focus: `outline: 2px solid #0088b0; outline-offset: 2px`. Never the browser default.
- Disabled: 45% opacity.

## Product rules (unchanged from player-web.md)
- Money is spelled out before it's committed: court, extras, discount, total, paid now, left for the venue, and the exact cancellation fee.
- Rules in plain second-person words.
- Browsing never needs an account.
- Every string goes through i18n (en + ne).
