# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

The primary user is an **event host or event manager** producing invitations for one event — wedding, conference, gala, memorial, birthday, or similar. They arrive with a house list (or will build one), need a named card for each guest, and need a way to admit people at the door.

Secondary roles in the system, not the desk user:

- **ADMIN** — scanner and operational access
- **GUEST** — recipient of a card; not the person operating InviteFlow

## Product Purpose

InviteFlow is the production desk for invitations. An event manager sets a template, puts each guest on the stock, generates and sends the list (email, SMS, WhatsApp), and puts a scannable QR on the door. Success is a named card in each guest’s hand and a working check-in at the event — not a prettier marketing page.

## Positioning

A neighboring consumer invitation site sells a pretty card. InviteFlow runs the event through the desk: templates and designer fields, guests and bulk generate, multi-channel delivery, payments and receipts in the API, and door scan. The unit of work is a **named guest on a plate**, not a generic event flyer.

## Operating Context

Typical sitting:

1. Choose a sample card and a sitting (package) on the public landing; selection is held as intent until sign-in.
2. Sign in and work in the desk: events, guests (including list import), templates and field designer, bulk generate.
3. Send invitations and watch the delivery log; retry failed sends.
4. At the door, scan the QR and record check-in.

The signed-in product lives in the InviteFlow SPA (`frontend/InviteFlow`). The spine is a Spring Boot API on port 8080 (`/api/v1/...`). Local development uses Vite on port 5173. Auth on the landing path is currently a local session (`localStorage` key `inviteflow-session`); landing selection is `sessionStorage` key `inviteflow-intent`. Backend auth, payments, and receipts exist on the API; the SPA does not yet operate those as first-class desk screens.

Terminology that is product language, not decoration: **desk**, **sitting**, **plate**, **stock**, **job**, **house list**, **press**, **door**.

Occasions in the catalog: wedding, conference, birthday, corporate, memorial, expo.

## Capabilities and Constraints

Confirmed in the product:

- Events, guests (CRUD, search, CSV import), templates (upload, activate, designer fields, preview), bulk invitation generate, invitation records with unique tokens, multi-channel delivery (email / SMS / WhatsApp) and delivery logs, QR check-in and history, dashboard metrics.
- Public landing catalog of sample cards and priced sittings; continue holds template + sitting and routes unsigned visitors to register, then the templates desk.
- Roles: `ADMIN`, `EVENT_MANAGER`, `GUEST`.

Constraints later work must not break:

- Do not change backend fetch URLs or the domain behavior of the Spring API.
- Product name is **InviteFlow** (repository folder `InvitationSystem` is not the product name).
- Landing sittings and their displayed prices are a **frontend catalog**, not charged billing. Sign-in on the landing path is a **local stand-in**, not proof that `/api/v1/auth` is wired into the SPA.
- Do not invent customers, testimonials, live billing, or that packages are actually charged.

Undecided (record, do not invent):

- When and whether sittings become real billing.
- When SPA sign-in switches from local session to the API.
- Whether payments and PDF receipts become desk screens, or stay API-only until asked.

## Brand Commitments

- Name: **InviteFlow**.
- Voice: a stationer’s desk — sittings, plates, stock, job tickets — not generic SaaS.
- Copy: sentence case. No emojis. Icons from lucide only.

## Evidence on Hand

- Working desk screens: dashboard, guests, templates, template designer, bulk generate, door scanner.
- Sample invitation names, dates, and venues in `frontend/InviteFlow/src/lib/catalog.ts` are **fiction for the board**. They are not customers or case studies.
- No testimonials, press, or usage metrics exist. Future work must not fabricate them.
- Package prices on the landing are catalog copy, not a price list backed by payments.

## Product Principles

1. **The named guest is the unit of work.** Every card is a press for a person on the house list.
2. **Production through the door.** List, press, send, and scan is the job; the shopfront exists to start that sitting.
3. **Desk language, not product-speak.** Sittings and stock over plans and dashboards-as-brand.
4. **Do not claim what is not wired.** Catalog prices, local sign-in, and sample names are stand-ins; they are not proof, customers, or charges.
5. **The API is the spine.** Visual work may change the desk and the landing; it may not quietly retarget or invent endpoints.
