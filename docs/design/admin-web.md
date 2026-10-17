# Admin web app: design direction

For FutSlot staff, mostly at a laptop, working through queues. It shares the owner dashboard's tokens and rules
(see [inspirations.md](inspirations.md)): calm surfaces, ink buttons, colour only for meaning, 44px targets.

- **Queues first.** The overview leads with what's waiting (venues to approve, payments to check, deposits due
  to be sent, payouts to venues); the same counts sit in the navigation. Each queue item carries its own action
  so a list can be cleared top to bottom.
- **Money is sent by hand, so the item shows exactly where to send it** (the player's wallet number, the venue's
  account) and asks for the transfer's reference once it's done. Deposits can only be sent once their 3-day wait
  is over, so a late fee can still come out first.
- **Anything that tells someone "no" asks for a reason**, and says that they'll see it.
- **Light on charts.** Numbers and tables; the overview is six figures and a top-venues table.
