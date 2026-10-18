import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/api_types.dart';
import 'package:futslot/features/matches/match_summary.dart';
import 'package:futslot/features/tournaments/tournament_screen.dart';

Json _post({String type = 'NEED_PLAYERS', int? needed, int taken = 0, Json? team}) => {
      'id': 'p1',
      'type': type,
      'status': 'OPEN',
      'postedBy': {'userId': 'u1', 'name': 'Sita G.'},
      'team': team,
      'startsAt': '2026-10-20T18:00:00+05:45',
      'playersNeeded': needed,
      'placesTaken': taken,
    };

Json _fixture(String id, String stage, {String? group, bool bye = false, int? home, int? away}) => {
      'id': id,
      'stage': stage,
      'groupName': group,
      'status': 'SCHEDULED',
      'bye': bye,
      'homeScore': home,
      'awayScore': away,
      'scheduledAt': '2026-11-01T10:00:00+05:45',
    };

void main() {
  test('says how many players a game still needs', () {
    expect(matchHeadline(MatchPost.fromJson(_post(needed: 3, taken: 1))), '2 players needed');
    expect(matchHeadline(MatchPost.fromJson(_post(needed: 2, taken: 1))), '1 player needed');
    expect(matchHeadline(MatchPost.fromJson(_post(needed: 2, taken: 2))), 'Full');
    final opponent = _post(type: 'FIND_OPPONENT', team: {'id': 't1', 'name': 'Jhamsikhel Kings', 'slug': 'jk'});
    expect(matchHeadline(MatchPost.fromJson(opponent)), 'Jhamsikhel Kings want a game');
  });

  test('scores a fixture, with penalties, or gives the kick-off', () {
    expect(fixtureScore(Fixture.fromJson(_fixture('m1', 'FINAL', home: 2, away: 1))), '2–1');
    final penalties =
        Fixture.fromJson({..._fixture('m1', 'FINAL', home: 1, away: 1), 'homePenalties': 4, 'awayPenalties': 3});
    expect(fixtureScore(penalties), '1–1 (4–3 pens)');
    expect(fixtureScore(Fixture.fromJson(_fixture('m1', 'FINAL'))), '10 AM');
  });

  test('groups fixtures by stage and group, in order, without byes', () {
    final rounds = tournamentRounds([
      Fixture.fromJson(_fixture('a1', 'GROUP', group: 'A')),
      Fixture.fromJson(_fixture('b1', 'GROUP', group: 'B')),
      Fixture.fromJson(_fixture('a2', 'GROUP', group: 'A')),
      Fixture.fromJson(_fixture('q1', 'SEMI_FINAL', bye: true)),
      Fixture.fromJson(_fixture('f', 'FINAL')),
    ]);
    expect(rounds.map((r) => '${r.label}: ${r.fixtures.map((f) => f.id).join(', ')}'), [
      'Group stage · Group A: a1, a2',
      'Group stage · Group B: b1',
      'Final: f',
    ]);
  });
}
