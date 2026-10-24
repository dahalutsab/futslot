# FutSlot on free plans: Render, Neon, Cloudinary and Vercel

A free way to run the demo at `futslot.dahalutsab.com.np`, no credit card needed. The other way, one small server for
everything, is in the main README under "Deploying".

| Part | Where | Free plan |
|------|-------|-----------|
| API (Spring Boot) | Render, `futslot-api.onrender.com` | 512 MB, sleeps after 15 idle minutes, 750 hours a month, 5 GB out a month |
| Database (PostgreSQL 18) | Neon, Singapore | 0.5 GB, 100 compute hours a month, sleeps after 5 idle minutes |
| Venue photos | Cloudinary | 25 credits a month (1 credit = 1 GB stored or 1 GB served) |
| Player app, owner dashboard, admin, venue websites | Vercel | Static files; it passes the API's paths to Render |

| Address | Vercel project (Root Directory) |
|---------|---------------------------------|
| `futslot.dahalutsab.com.np` | `player`: the player app, with FutSlot's public pages, `/api`, photos and venue sites (`/sites/<subdomain>`) from Render |
| `owner.futslot.dahalutsab.com.np` | `dashboard` |
| `admin.futslot.dahalutsab.com.np` | `admin` |
| `dhuku.futslot.dahalutsab.com.np`, ... | `deploy/render/sites`: every path from Render, which tells venues apart by address |

The Vercel settings are in each project's `vercel.json`, the Render ones in `render.yaml`.

## Setting it up

1. **Neon**: create a project with PostgreSQL 18 in AWS Asia Pacific (Singapore). In its compute settings, set
   autoscaling to 0.25–0.25 CU so it never uses more than the free hours allow. From **Connect**, with connection
   pooling off, take the host, user and password; the JDBC address is
   `jdbc:postgresql://<host>/neondb?sslmode=require`.
2. **Cloudinary**: sign up, and copy the **API environment variable** from the dashboard
   (`cloudinary://<key>:<secret>@<cloud>`). Photos go in its `futslot` folder.
3. **Render**: **New > Blueprint**, pick this repo and branch, Blueprint path `deploy/render/render.yaml`. Fill in
   `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `CLOUDINARY_URL`; the secrets are generated. The first build takes
   several minutes. Done when `https://futslot-api.onrender.com/actuator/health/liveness` says `{"status":"UP"}`.
   If Render gives it another address, put that in the `vercel.json` files:
   ```bash
   grep -rl futslot-api.onrender.com player/vercel.json dashboard/vercel.json admin/vercel.json deploy/render/sites \
     | xargs sed -i 's#futslot-api.onrender.com#<your address>#'
   ```
4. **Keep the API awake**: Vercel waits at most 30 seconds for it, and waking takes about a minute. With a free
   [UptimeRobot](https://uptimerobot.com) HTTP monitor on the liveness address above, every 5 minutes, it never sleeps.
   That uses Render's 750 free hours on this one service, so don't run another free service in the same workspace.
   The liveness check doesn't touch the database, so Neon still sleeps between the API's background jobs.
5. **Vercel**: import this repo four times, once per row of the table above, with that **Root Directory** and the
   framework preset **Other** (`vercel.json` has the build). Under **Settings > Git**, set the production branch to
   the one deployed. Under **Settings > Domains**, add each project's address: one per venue for the venue websites
   project (`dhuku`, `kickarena` and `thimi` for the demo ones). Vercel only takes `*.futslot` when it runs the
   domain's DNS, so each venue's address is added by hand.
6. **DNS** (Cloudflare, for `dahalutsab.com.np`): the `CNAME` records Vercel asks for, for `futslot`,
   `owner.futslot`, `admin.futslot` and each venue, set to **DNS only** (grey cloud) so Vercel can get the certificates.
7. **Demo data**: on your own computer, in `deploy/`, make a `.env` with `FUTSLOT_DOMAIN=futslot.dahalutsab.com.np`
   and `DEMO_PASSWORD=...`, then run `./seed-demo.sh` (needs curl and jq). It makes the demo venues and the
   admin, owner and player accounts. Add venue photos from the owner dashboard.

## What to expect

- Every push to the deployed branch rebuilds what changed: Render only for `backend/`, Vercel per app.
- After a deploy or a restart, the API takes a minute or two to start on Render's small share of a CPU. Pages that
  need it fail until then.
- Background jobs (tidying up unpaid holds, booking reminders) run every 20 minutes instead of every few, so Neon
  can sleep. Slots show as free as soon as a hold runs out either way; reminders come up to 20 minutes late.
- Venues' own domains (not `*.futslot`) aren't served.
- If a venue website shows FutSlot's front page instead, the venue's address isn't reaching the API: check that
  Render passes Vercel's `X-Forwarded-Host` header through.
