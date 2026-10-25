#!/usr/bin/env bash
# Fills a fresh FutSlot with demo venues, through the API as an owner would. Run from deploy/ on the server once
# `docker compose up` is serving the domain, with SHOW_SIGN_IN_CODES=true (sign-up needs the code). Needs curl and jq.
# For a database that isn't in this compose project (Neon), set DATABASE_URL to it, and psql is needed too.
#
# Makes three accounts, all with DEMO_PASSWORD:
#   admin   +977 9800000001   approves venues, top-ups and payouts at admin.<domain>
#   owner   +977 9800000002   runs the demo venues (demo-venues.json) at owner.<domain>
#   player  +977 9800000101   has a couple of bookings
set -euo pipefail
cd "$(dirname "$0")"
# Read, not sourced: values are written for docker compose, not the shell
setting() { grep -m1 "^$1=" .env | cut -d= -f2-; }
FUTSLOT_DOMAIN=${FUTSLOT_DOMAIN:-$(setting FUTSLOT_DOMAIN)}
DEMO_PASSWORD=$(setting DEMO_PASSWORD)
: "${DEMO_PASSWORD:?set DEMO_PASSWORD in .env}"
API="${API_URL:-https://$FUTSLOT_DOMAIN}/api/v1"

call() { # method path token [json]
	local out
	# CURL_FLAGS: e.g. -k when trying it locally against Caddy's own certificates
	out=$(curl -sS ${CURL_FLAGS:-} -X "$1" "$API$2" -H 'Content-Type: application/json' ${3:+-H "Authorization: Bearer $3"} \
		${4:+--data "$4"} -w '\n%{http_code}')
	local status=${out##*$'\n'} body=${out%$'\n'*}
	if [[ $status != 2* ]]; then
		echo "$1 $2 failed ($status): $body" >&2
		return 1
	fi
	printf '%s' "$body"
}

account() { # phone name -> access token
	local code
	code=$(call POST /auth/codes "" "{\"phone\":\"$1\",\"purpose\":\"SIGN_UP\"}" | jq -r '.code // empty')
	[[ -n $code ]] || { echo "No code came back: is SHOW_SIGN_IN_CODES=true?" >&2; exit 1; }
	call POST /auth/sign-up "" "{\"phone\":\"$1\",\"code\":\"$code\",\"fullName\":\"$2\",\"password\":\"$DEMO_PASSWORD\"}" \
		| jq -r .accessToken
}

sign_in() { # phone -> access token
	call POST /auth/sign-in "" "{\"phone\":\"$1\",\"password\":\"$DEMO_PASSWORD\"}" | jq -r .accessToken
}

if [[ $(call GET /venues "" | jq .totalElements) != 0 ]]; then
	echo "There are venues already; this is for a fresh install." >&2
	exit 1
fi

account 9800000001 "FutSlot Admin" > /dev/null
GRANT="INSERT INTO user_roles (user_id, role) SELECT id, 'ADMIN' FROM users WHERE phone = '+9779800000001' ON CONFLICT DO NOTHING;"
if [[ -n ${DATABASE_URL:-} ]]; then
	psql -q "$DATABASE_URL" -c "$GRANT" # a database elsewhere, such as Neon's
else
	docker compose exec -T db psql -q -U futslot -d futslot -c "$GRANT"
fi
ADMIN=$(sign_in 9800000001)
OWNER=$(account 9800000002 "Demo Owner")
PLAYER=$(account 9800000101 "Aarav Shrestha")

HOURS=$(jq -nc '[("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY")
	| {day: ., openTime: "06:00", closeTime: "21:00"}]')

venue() { # one entry of venues.json -> its id
	local v=$1 id base
	id=$(call POST /owner/venues "$OWNER" "$(jq -c '{name, area, city, addressLine: .street, latitude: .lat, longitude: .lng,
		description}' <<< "$v")" | jq -r .id)
	base=/owner/venues/$id
	call PUT "$base/operating-hours" "$OWNER" "$HOURS" > /dev/null
	call PUT "$base/amenities" "$OWNER" "$(jq -c '{codes: .amenities}' <<< "$v")" > /dev/null
	call PUT "$base/policy" "$OWNER" "{\"advancePercent\":0,\"bookingHoldMinutes\":30,\"maxAdvanceBookingDays\":30,
		\"cancellationTiers\":[{\"withinHours\":12,\"feePercent\":25},{\"withinHours\":3,\"feePercent\":50}],
		\"strikeLimit\":3,\"strikeAdvancePercent\":50,\"depositRequired\":$(jq .deposit <<< "$v")}" > /dev/null
	jq -c '.courts[]' <<< "$v" | while read -r court; do
		local court_id rules
		court_id=$(call POST "$base/courts" "$OWNER" "$(jq -c '{name, format, surface: "ARTIFICIAL_TURF",
			slotDurationMinutes: 60}' <<< "$court")" | jq -r .id)
		rules=$base/courts/$court_id/pricing-rules
		jq -c '.prices[]' <<< "$v" | while read -r rule; do call POST "$rules" "$OWNER" "$rule" > /dev/null; done
	done
	call POST "$base/submit" "$OWNER" > /dev/null
	call POST "/admin/venues/$id/approve" "$ADMIN" > /dev/null
	call PUT "$base/site" "$OWNER" "$(jq -c '{template, subdomain, accentColor: .accent, headline}' <<< "$v")" > /dev/null
	call POST "$base/site/publish" "$OWNER" > /dev/null
	echo "$id"
}

# Real futsal grounds around the valley, from public listings. Prices and free hours are made up for the demo, and
# FutSlot has nothing to do with the venues themselves: each description says so.
DHUKU=
while read -r v; do
	id=$(venue "$v")
	echo "$(jq -r .name <<< "$v"): https://$(jq -r .subdomain <<< "$v").$FUTSLOT_DOMAIN"
	[[ $(jq -r .subdomain <<< "$v") == dhuku ]] && DHUKU=$id
done < <(jq -c '.[]' demo-venues.json)

# Two bookings for the demo player at Dhuku, tomorrow evening
SLUG=$(call GET "/owner/venues/$DHUKU" "$OWNER" | jq -r .slug)
TOMORROW=$(date -d tomorrow +%F)
call GET "/venues/$SLUG/availability?date=$TOMORROW" "" \
	| jq -c '.courts[0] as $c | $c.slots | map(select(.status == "AVAILABLE" and (.startsAt | test("T1[78]:"))))[:2][]
		| {courtId: $c.courtId, startsAt}' \
	| while read -r booking; do call POST /bookings "$PLAYER" "$booking" > /dev/null; done

echo "Done."
