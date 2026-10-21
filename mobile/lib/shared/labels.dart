/// How API values read to a player, the same words as the web app's labels.ts, in the app's language.
library;

import '../core/api_types.dart';
import '../l10n/l10n.dart';
import 'booking_model.dart';

extension SkillLevelLabel on SkillLevel {
  String get label => switch (this) {
        SkillLevel.beginner => l.skillBeginner,
        SkillLevel.intermediate => l.skillIntermediate,
        SkillLevel.advanced => l.skillAdvanced,
        SkillLevel.pro => l.skillPro,
      };
}

extension PositionLabel on Position {
  String get label => switch (this) {
        Position.goalkeeper => l.positionGoalkeeper,
        Position.defender => l.positionDefender,
        Position.midfielder => l.positionMidfielder,
        Position.forward => l.positionForward,
        Position.any => l.positionAny,
      };
}

extension PreferredFootLabel on PreferredFoot {
  String get label => switch (this) {
        PreferredFoot.left => l.footLeft,
        PreferredFoot.right => l.footRight,
        PreferredFoot.both => l.footBoth,
      };
}

extension TeamRoleLabel on TeamRole {
  String get label => switch (this) {
        TeamRole.captain => l.roleCaptain,
        TeamRole.viceCaptain => l.roleViceCaptain,
        TeamRole.member => l.roleMember,
      };
}

extension TournamentFormatLabel on TournamentFormat {
  String get label => switch (this) {
        TournamentFormat.knockout => l.formatKnockout,
        TournamentFormat.league => l.formatLeague,
        TournamentFormat.groupKnockout => l.formatGroupKnockout,
      };
}

extension TournamentStatusLabel on TournamentStatus {
  String get label => switch (this) {
        TournamentStatus.draft => l.tournamentDraft,
        TournamentStatus.registrationOpen => l.tournamentOpen,
        TournamentStatus.registrationClosed => l.tournamentClosed,
        TournamentStatus.inProgress => l.tournamentInProgress,
        TournamentStatus.completed => l.tournamentCompleted,
        TournamentStatus.cancelled => l.tournamentCancelled,
      };
}

extension StageLabel on Stage {
  String get label => switch (this) {
        Stage.group => l.stageGroup,
        Stage.league => l.stageLeague,
        Stage.roundOf32 => l.stageRoundOf32,
        Stage.roundOf16 => l.stageRoundOf16,
        Stage.quarterFinal => l.stageQuarterFinal,
        Stage.semiFinal => l.stageSemiFinal,
        Stage.thirdPlace => l.stageThirdPlace,
        Stage.finalMatch => l.stageFinal,
      };
}

extension EntryStatusLabel on EntryStatus {
  String get label => switch (this) {
        EntryStatus.pendingPayment => l.entryPendingPayment,
        EntryStatus.confirmed => l.entryConfirmed,
        EntryStatus.waitlisted => l.entryWaitlisted,
        EntryStatus.withdrawn => l.entryWithdrawn,
        EntryStatus.rejected => l.entryRejected,
      };
}

extension SplitRuleLabel on SplitRule {
  String get label => switch (this) {
        SplitRule.equal => l.ruleEqual,
        SplitRule.losersPayAll => l.ruleLosersPayAll,
        SplitRule.losersPayPercent => l.ruleLosersPayPercent,
        SplitRule.losersPayCourt => l.ruleLosersPayCourt,
      };

  String get detail => switch (this) {
        SplitRule.equal => l.ruleEqualDetail,
        SplitRule.losersPayAll => l.ruleLosersPayAllDetail,
        SplitRule.losersPayPercent => l.ruleLosersPayPercentDetail,
        SplitRule.losersPayCourt => l.ruleLosersPayCourtDetail,
      };
}

/// An amenity in the app's language, by its code; one added on the server later shows as it's named there.
String amenityLabel(Amenity amenity) => switch (amenity.code) {
      'PARKING' => l.amenityParking,
      'CHANGING_ROOM' => l.amenityChangingRoom,
      'SHOWER' => l.amenityShower,
      'DRINKING_WATER' => l.amenityDrinkingWater,
      'CAFE' => l.amenityCafe,
      'FLOODLIGHTS' => l.amenityFloodlights,
      'WIFI' => l.amenityWifi,
      'FIRST_AID' => l.amenityFirstAid,
      'EQUIPMENT_RENTAL' => l.amenityEquipmentRental,
      'SPECTATOR_SEATING' => l.amenitySpectatorSeating,
      _ => amenity.name,
    };

/// "10 games", "15% off every game", and for how long.
String planTerms(MembershipType type, {int? gamesIncluded, double? discountPercent, int? validityDays}) {
  final what =
      type == MembershipType.gamePack ? l.games(gamesIncluded ?? 0) : l.planDiscount(percent(discountPercent ?? 0));
  return validityDays != null ? l.planFor(what, l.days(validityDays)) : what;
}

/// "Reds won", "Draw".
String resultLabel(MatchResult result, String sideA, String sideB) =>
    result == MatchResult.draw ? l.resultDraw : l.sideWon(result == MatchResult.aWon ? sideA : sideB);
