/// How API values read to a player, the same words as the web app's labels.ts.
library;

import '../core/api_types.dart';
import 'booking_model.dart';

extension SkillLevelLabel on SkillLevel {
  String get label => switch (this) {
        SkillLevel.beginner => 'Beginner',
        SkillLevel.intermediate => 'Intermediate',
        SkillLevel.advanced => 'Advanced',
        SkillLevel.pro => 'Pro',
      };
}

extension PositionLabel on Position {
  String get label => switch (this) {
        Position.goalkeeper => 'Goalkeeper',
        Position.defender => 'Defender',
        Position.midfielder => 'Midfielder',
        Position.forward => 'Forward',
        Position.any => 'Anywhere',
      };
}

extension PreferredFootLabel on PreferredFoot {
  String get label => switch (this) {
        PreferredFoot.left => 'Left',
        PreferredFoot.right => 'Right',
        PreferredFoot.both => 'Both',
      };
}

extension TeamRoleLabel on TeamRole {
  String get label => switch (this) {
        TeamRole.captain => 'Captain',
        TeamRole.viceCaptain => 'Vice-captain',
        TeamRole.member => 'Player',
      };
}

extension TournamentFormatLabel on TournamentFormat {
  String get label => switch (this) {
        TournamentFormat.knockout => 'Knockout',
        TournamentFormat.league => 'League',
        TournamentFormat.groupKnockout => 'Groups, then knockout',
      };
}

extension TournamentStatusLabel on TournamentStatus {
  String get label => switch (this) {
        TournamentStatus.draft => 'Draft',
        TournamentStatus.registrationOpen => 'Taking teams',
        TournamentStatus.registrationClosed => 'Entries closed',
        TournamentStatus.inProgress => 'Under way',
        TournamentStatus.completed => 'Finished',
        TournamentStatus.cancelled => 'Cancelled',
      };
}

extension StageLabel on Stage {
  String get label => switch (this) {
        Stage.group => 'Group stage',
        Stage.league => 'League',
        Stage.roundOf32 => 'Round of 32',
        Stage.roundOf16 => 'Round of 16',
        Stage.quarterFinal => 'Quarter-finals',
        Stage.semiFinal => 'Semi-finals',
        Stage.thirdPlace => 'Third place',
        Stage.finalMatch => 'Final',
      };
}

extension EntryStatusLabel on EntryStatus {
  String get label => switch (this) {
        EntryStatus.pendingPayment => 'Pay the entry fee',
        EntryStatus.confirmed => 'Confirmed',
        EntryStatus.waitlisted => 'On the waitlist',
        EntryStatus.withdrawn => 'Withdrawn',
        EntryStatus.rejected => 'Not accepted',
      };
}

extension SplitRuleLabel on SplitRule {
  String get label => switch (this) {
        SplitRule.equal => 'Split equally',
        SplitRule.losersPayAll => 'Losers pay all',
        SplitRule.losersPayPercent => 'Losers pay more',
        SplitRule.losersPayCourt => 'Losers pay the court',
      };

  String get detail => switch (this) {
        SplitRule.equal => 'Everyone pays the same, whoever wins.',
        SplitRule.losersPayAll => 'Baazi: the losing side pays everything.',
        SplitRule.losersPayPercent => 'Baazi: the losing side pays a share you choose, the winners the rest.',
        SplitRule.losersPayCourt => 'Baazi: losers pay for the court, winners for the extras.',
      };
}

/// "10 games", "15% off every game", and for how long.
String planTerms(MembershipType type, {int? gamesIncluded, double? discountPercent, int? validityDays}) {
  final what = type == MembershipType.gamePack
      ? '$gamesIncluded games'
      : '${percent(discountPercent ?? 0)}% off every game';
  return validityDays != null ? '$what, for $validityDays days' : what;
}

/// "Reds won", "Draw".
String resultLabel(MatchResult result, String sideA, String sideB) =>
    result == MatchResult.draw ? 'Draw' : '${result == MatchResult.aWon ? sideA : sideB} won';

/// "1 player", "3 players".
String count(int n, String one, [String? many]) => '$n ${n == 1 ? one : many ?? '${one}s'}';
