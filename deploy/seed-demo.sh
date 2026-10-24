#!/usr/bin/env bash
# Fills a fresh FutSlot with demo venues, through the API as an owner would. Run from deploy/ on the server once
# `docker compose up` is serving the domain, with SHOW_SIGN_IN_CODES=true (sign-up needs the code). Needs curl and jq.
#
# Makes three accounts, all with DEMO_PASSWORD:
#   admin   +977 9800000001   approves venues, top-ups and payouts at admin.<domain>
#   owner   +977 9800000002   runs the three demo venues at owner.<domain>
#   player  +977 9800000101   has a couple of bookings
set -euo pipefail
cd "$(dirname "$0")"
# Read, not sourced: values are written for docker compose, not the shell
setting() { grep -m1 "^$1=" .env | cut -d= -f2-; }
FUTSLOT_DOMAIN=$(setting FUTSLOT_DOMAIN)
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
docker compose exec -T db psql -q -U futslot -d futslot \
	-c "INSERT INTO user_roles (user_id, role) SELECT id, 'ADMIN' FROM users WHERE phone = '+9779800000001' ON CONFLICT DO NOTHING;"
ADMIN=$(sign_in 9800000001)
OWNER=$(account 9800000002 "Demo Owner")
PLAYER=$(account 9800000101 "Aarav Shrestha")

HOURS=$(jq -nc '[("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY")
	| {day: ., openTime: "06:00", closeTime: "21:00"}]')

venue() { # name area city lat lng description amenities(json) deposit(true/false) template subdomain accent headline street
	local id base
	id=$(call POST /owner/venues "$OWNER" "$(jq -nc --arg n "$1" --arg a "$2" --arg c "$3" --arg d "$6" --arg s "${13}" \
		--argjson lat "$4" --argjson lng "$5" \
		'{name: $n, area: $a, city: $c, addressLine: $s, latitude: $lat, longitude: $lng, description: $d}')" \
		| jq -r .id)
	base=/owner/venues/$id
	call PUT "$base/operating-hours" "$OWNER" "$HOURS" > /dev/null
	call PUT "$base/amenities" "$OWNER" "{\"codes\":$7}" > /dev/null
	call PUT "$base/policy" "$OWNER" "{\"advancePercent\":0,\"bookingHoldMinutes\":30,\"maxAdvanceBookingDays\":30,
		\"cancellationTiers\":[{\"withinHours\":12,\"feePercent\":25},{\"withinHours\":3,\"feePercent\":50}],
		\"strikeLimit\":3,\"strikeAdvancePercent\":50,\"depositRequired\":$8}" > /dev/null
	for court in "Court A:FIVE_A_SIDE" "Court B:SEVEN_A_SIDE"; do
		local court_id rules
		court_id=$(call POST "$base/courts" "$OWNER" "{\"name\":\"${court%%:*}\",\"format\":\"${court##*:}\",
			\"surface\":\"ARTIFICIAL_TURF\",\"slotDurationMinutes\":60}" | jq -r .id)
		rules=$base/courts/$court_id/pricing-rules
		call POST "$rules" "$OWNER" '{"startTime":"06:00","endTime":"16:00","price":1200}' > /dev/null
		call POST "$rules" "$OWNER" '{"label":"PEAK","startTime":"16:00","endTime":"21:00","price":1800,"priority":1}' > /dev/null
	done
	call POST "$base/submit" "$OWNER" > /dev/null
	call POST "/admin/venues/$id/approve" "$ADMIN" > /dev/null
	call PUT "$base/site" "$OWNER" "$(jq -nc --arg t "$9" --arg s "${10}" --arg c "${11}" --arg h "${12}" \
		'{template: $t, subdomain: $s, accentColor: $c, headline: $h}')" > /dev/null
	call POST "$base/site/publish" "$OWNER" > /dev/null
	echo "$id"
}

DHUKU=$(venue "Dhuku Futsal" "New Baneshwor" "Kathmandu" 27.6915 85.3420 \
	"A demo venue: two floodlit turf courts off the main road, with parking and a small cafe. Book a slot and see how FutSlot works." \
	'["PARKING","CHANGING_ROOM","DRINKING_WATER","CAFE","FLOODLIGHTS"]' false TURF dhuku "#1f7a3a" "Evening games under the lights" \
	"Shantinagar Marg")
venue "Jhamsikhel Kick Arena" "Jhamsikhel" "Lalitpur" 27.6788 85.3070 \
	"A demo venue that needs the FutSlot booking deposit, so you can try topping it up (no real money)." \
	'["PARKING","CHANGING_ROOM","SHOWER","DRINKING_WATER","FLOODLIGHTS","WIFI"]' true FLOODLIGHT kickarena "#f59e0b" \
	"Late-night futsal in Lalitpur" "Jhamsikhel Road" > /dev/null
venue "Thimi Futsal Hub" "Madhyapur Thimi" "Bhaktapur" 27.6780 85.3870 \
	"A demo venue east of the valley with seating for supporters." \
	'["PARKING","DRINKING_WATER","FIRST_AID","SPECTATOR_SEATING","EQUIPMENT_RENTAL"]' false TURF thimi "#2563eb" \
	"Your weekly game in Bhaktapur" "Bode Road" > /dev/null

# Two bookings for the demo player at Dhuku, tomorrow evening
SLUG=$(call GET "/owner/venues/$DHUKU" "$OWNER" | jq -r .slug)
TOMORROW=$(date -d tomorrow +%F)
call GET "/venues/$SLUG/availability?date=$TOMORROW" "" \
	| jq -c '.courts[0] as $c | $c.slots | map(select(.status == "AVAILABLE" and (.startsAt | test("T1[78]:"))))[:2][]
		| {courtId: $c.courtId, startsAt}' \
	| while read -r booking; do call POST /bookings "$PLAYER" "$booking" > /dev/null; done

echo "Done. Venue websites: https://dhuku.$FUTSLOT_DOMAIN, https://kickarena.$FUTSLOT_DOMAIN, https://thimi.$FUTSLOT_DOMAIN"
