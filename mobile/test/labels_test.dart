import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/api_types.dart';
import 'package:futslot/l10n/l10n.dart';
import 'package:futslot/shared/labels.dart';

// The web app's labels.spec.ts cases, and the enum names the API uses.

void main() {
  test('says what a membership gives', () {
    expect(planTerms(MembershipType.gamePack, gamesIncluded: 10, validityDays: 60), '10 games, for 60 days');
    expect(planTerms(MembershipType.monthlyPass, discountPercent: 15), '15% off every game');
  });

  test('names the winning side', () {
    expect(resultLabel(MatchResult.aWon, 'Reds', 'Blues'), 'Reds won');
    expect(resultLabel(MatchResult.bWon, 'Reds', 'Blues'), 'Blues won');
    expect(resultLabel(MatchResult.draw, 'Reds', 'Blues'), 'Draw');
  });

  test('spells enum values the way the API does, both ways', () {
    expect(SplitRule.losersPayPercent.apiName, 'LOSERS_PAY_PERCENT');
    expect(Side.a.apiName, 'A');
    expect(TeamRole.viceCaptain.apiName, 'VICE_CAPTAIN');
    expect(Stage.from('FINAL'), Stage.finalMatch);
    expect(Stage.from('ROUND_OF_16'), Stage.roundOf16);

    final team = Team.fromJson({
      'id': 't1',
      'name': 'Jhamsikhel Kings',
      'slug': 'jhamsikhel-kings',
      'skillLevel': 'INTERMEDIATE',
      'memberCount': 2,
      'members': [
        {'userId': 'u1', 'name': 'Aarav', 'role': 'VICE_CAPTAIN', 'status': 'ACTIVE'},
      ],
      'myRole': 'VICE_CAPTAIN',
      'myStatus': 'ACTIVE',
    });
    expect(team.members.single.role, TeamRole.viceCaptain);
    expect(team.skillLevel, SkillLevel.intermediate);
    expect(team.leads, isTrue);
  });

  test('counts in words', () {
    expect(l.players(1), '1 player');
    expect(l.players(3), '3 players');
    expect(l.players(1200), '1,200 players');
  });
}
