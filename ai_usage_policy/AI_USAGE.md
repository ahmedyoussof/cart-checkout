# AI Usage Disclosure

How AI assistance (Anthropic Claude) was used in this submission, per the assignment's AI usage policy.

The full back-and-forth that produced the design lives in [BRAIN_STORMING_CONVERSATION.md](BRAIN_STORMING_CONVERSATION.md); this document is the audit trail.

---

## 1. Summary

| Aspect | Details |
|---|---|
| **AI tool** | Anthropic Claude (chat + Claude Code) |
| **Used heavily for** | Drafting [DESIGN.md](DESIGN.md), [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md), and most of the prose / diagrams in this folder; surfacing implicit invariants (optimistic locking, transactional boundaries, the two-layer webhook guard); generating the sequence and state-machine diagrams; producing significant chunks of the Java implementation under my direction and review |
| **My role** | Drove every conversation, set the constraints, made the architectural calls, corrected the AI when it overstepped, and reviewed every artifact before accepting it. The decisions are mine; a lot of the *wording* and *rendering* is the AI's |
| **Where the line sits** | The AI sometimes surfaced a consideration I would not have raised unprompted (e.g. the explicit two-layer idempotency framing). I decided what to do about it. The design is not "AI-generated"; it's also not "AI-untouched" |

---

## 2. My Own Architectural Decisions

Decisions I owned. Where the AI suggested alternatives, I overrode them.

| Decision | Notes |
|---|---|
| Four bounded contexts (Cart, Order, Payment, Provider) | AI initially proposed a fifth "Service" context — I corrected: service/repository are layers, not contexts |
| One-way dependency direction `Cart ← Order ← Payment → Provider` | Stated explicitly in my prompt |
| Order knows Cart by ID; Payment knows Order by ID; Order does NOT know Payment | Held the one-way line when AI offered bidirectional |
| Payment as a separate aggregate from Order; one Payment per attempt | Confirmed with AI as a good fit for extensibility |
| Cart `lock()` is idempotent (no-op on already-locked) | My call. *(Revised during implementation — see [IMPLEMENTATION_DECISIONS.md §4](IMPLEMENTATION_DECISIONS.md).)* |
| No item merging on duplicate productId | Chose simplicity |
| `outcome` belongs to the Provider webhook payload, not the Payment aggregate | Corrected the AI's initial placement |
| Currency as a label-invariant on Money, not stored independently | My phrasing |
| No timestamps on Order | Time isn't a concern for this design |
| `CANCELLED` excluded from this iteration | Design must accommodate it later without breaking invariants |
| `PaymentId` as idempotency key sent to provider | My design |
| JPA + H2, no auth, no webhook signature | Conscious scope decisions |
| Reject `payment/start` when order already `PENDING_PAYMENT` | AI surfaced that the Order state machine enforces this naturally — no extra field |
| Webhook idempotency via terminal-status check (no event-id table) | AI presented both options; I chose terminal-status |

## 3. AI-Assisted Contributions

| Contribution | What the AI did | What I did |
|---|---|---|
| Hard-constraint extraction | Enumerated explicit + implicit constraints from the brief | Reviewed, supplied the missing decisions (tech, auth, signature, lock behavior, etc.) |
| Sequence and state-machine diagrams | Rendered the mermaid/SVG diagrams from my flow specs | Verified they matched my design |
| Design-document drafting | Wrote the prose of `DESIGN.md` from the decisions made across the conversation | Approved the structure; content reflects my decisions |
| Surfacing implicit invariants | Pointed out optimistic locking, transactional boundaries, "no setters on state" | Accepted — they follow from the "real money" constraint |
| Flagging sub-questions | Asked about cart-merge, currency invariant, CANCELLED, timestamps, etc. | Made the calls one by one |

## 4. Things I Corrected the AI On

1. **"Service" as a bounded context** — I clarified it's a layer, not a context.
2. **`outcome` as a Payment field** — moved it to the Provider context as a webhook-payload field.
3. **Timestamps on Order** (`paidAt`, `createdAt`, `updatedAt`) — rejected; time isn't a concern.
4. **Including `CANCELLED` from day one** — excluded for this iteration.
5. **Order → Payment back-reference** — picked one-way and held the line.

During the implementation-planning session (see §6 below) I additionally corrected:

6. **Package layout** — forced layer-first (`domain/`, `repository/`, `service/`, `api/` at top level) over the AI's original context-first nesting.
7. **Build order** — forced vertical use-case slices over horizontal layer phases.
8. **Top-level `exceptions/`** and flattening `infrastructure/mock/` to `infrastructure/`.

---

## 5. How and Why I Used AI — Honestly

- **As a sounding board** — every choice was challenged by a "did you think about X?" prompt. Some I accepted (currency invariant, transactional boundary, the explicit two-layer idempotency framing), some I rejected (timestamps, Service-as-context, `outcome` on Payment). The friction was genuinely useful, and I think some of the implicit invariants I would not have surfaced as cleanly on my own.
- **As a drafting and rendering assistant** — once decisions were made, the AI produced the prose of [DESIGN.md](DESIGN.md), [DESIGN_ARCH_TRADEOFFS.md](DESIGN_ARCH_TRADEOFFS.md), and the sequence/state-machine diagrams faster than I could. I read and edited rather than wrote from scratch.
- **As a coding assistant** — the Java implementation was produced through Claude Code under my direction, with each step reviewed and corrected. The deltas between the AI's initial plan and what was actually shipped are tracked in [IMPLEMENTATION_DECISIONS.md](IMPLEMENTATION_DECISIONS.md).
- **Not as the designer of record** — I never asked "what should I do?" without first having a position. When the AI proposed alternatives, I evaluated and chose; I did not accept by default. But I'm not going to claim the design would look identical without AI involvement: it would cover the same invariants, but the framing, the section structure, and several of the explicitly-called-out trade-offs are clearer because the AI pushed on them.

The choices that matter — bounded contexts, state machine inside the aggregate, separate Payment aggregate, terminal-status idempotency, `paymentId` as the idempotency key, one-way dependency direction — were mine. What I cannot honestly claim is that I would have produced this *documentation* and *visualization* to the same standard, or in this timeframe, without AI. That is the trade-off the policy is asking me to be upfront about.

---

## 6. Planning-Session Notes (Claude Code)

A separate Claude Code planning session turned [DESIGN.md](DESIGN.md) into [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md). The corrections I gave during that session are listed in §4 (items 6–8). No code was written during planning; the plan was iterated to its final state (layer-first packages, vertical use-case slices, 10 build steps) before plan mode was exited.

Deltas between the initial design/plan and the actual implementation are tracked in [IMPLEMENTATION_DECISIONS.md](IMPLEMENTATION_DECISIONS.md); the originals carry `↪ Superseded` markers pointing to the relevant entry.
