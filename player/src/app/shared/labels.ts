import {
  Amenity, EntryStatus, MatchResult, Position, PreferredFoot, SkillLevel, SplitRule, Stage, TeamRole, TournamentFormat,
  TournamentStatus,
} from '../core/api.types';
import { l } from '../i18n/i18n';

/** How each API value reads to a player, in the app's language. */

export const skillLabels = (): Record<SkillLevel, string> => ({
  BEGINNER: l().skillBeginner, INTERMEDIATE: l().skillIntermediate, ADVANCED: l().skillAdvanced, PRO: l().skillPro,
});

export const positionLabels = (): Record<Position, string> => ({
  GOALKEEPER: l().positionGoalkeeper, DEFENDER: l().positionDefender, MIDFIELDER: l().positionMidfielder,
  FORWARD: l().positionForward, ANY: l().positionAny,
});

export const footLabels = (): Record<PreferredFoot, string> => ({ LEFT: l().footLeft, RIGHT: l().footRight, BOTH: l().footBoth });

export const roleLabels = (): Record<TeamRole, string> => ({
  CAPTAIN: l().roleCaptain, VICE_CAPTAIN: l().roleViceCaptain, MEMBER: l().roleMember,
});

export const formatLabels = (): Record<TournamentFormat, string> => ({
  KNOCKOUT: l().formatKnockout, LEAGUE: l().formatLeague, GROUP_KNOCKOUT: l().formatGroupKnockout,
});

export const tournamentStatusLabels = (): Record<TournamentStatus, string> => ({
  DRAFT: l().tournamentDraft, REGISTRATION_OPEN: l().tournamentOpen, REGISTRATION_CLOSED: l().tournamentClosed,
  IN_PROGRESS: l().tournamentInProgress, COMPLETED: l().tournamentCompleted, CANCELLED: l().tournamentCancelled,
});

export const stageLabels = (): Record<Stage, string> => ({
  GROUP: l().stageGroup, LEAGUE: l().stageLeague, ROUND_OF_32: l().stageRoundOf32, ROUND_OF_16: l().stageRoundOf16,
  QUARTER_FINAL: l().stageQuarterFinal, SEMI_FINAL: l().stageSemiFinal, THIRD_PLACE: l().stageThirdPlace, FINAL: l().stageFinal,
});

export const entryStatusLabels = (): Record<EntryStatus, string> => ({
  PENDING_PAYMENT: l().entryPendingPayment, CONFIRMED: l().entryConfirmed, WAITLISTED: l().entryWaitlisted,
  WITHDRAWN: l().entryWithdrawn, REJECTED: l().entryRejected,
});

export const splitRules = (): Record<SplitRule, { label: string; detail: string }> => ({
  EQUAL: { label: l().ruleEqual, detail: l().ruleEqualDetail },
  LOSERS_PAY_ALL: { label: l().ruleLosersPayAll, detail: l().ruleLosersPayAllDetail },
  LOSERS_PAY_PERCENT: { label: l().ruleLosersPayPercent, detail: l().ruleLosersPayPercentDetail },
  LOSERS_PAY_COURT: { label: l().ruleLosersPayCourt, detail: l().ruleLosersPayCourtDetail },
});

/** "10 games", "15% off every game", and for how long. */
export function planTerms(plan: { type: string; gamesIncluded: number | null; discountPercent: number | null }, validityDays?: number): string {
  const what = plan.type === 'GAME_PACK' ? l().games(plan.gamesIncluded ?? 0) : l().planDiscount(plan.discountPercent ?? 0);
  return validityDays ? l().planFor(what, l().days(validityDays)) : what;
}

/** "Reds won", "Draw". */
export function resultLabel(result: MatchResult, sideA: string, sideB: string): string {
  return result === 'DRAW' ? l().resultDraw : l().sideWon(result === 'A_WON' ? sideA : sideB);
}

/** The fixed amenities in the app's language; any other keeps the name the venue gave it. */
export function amenityLabel(amenity: Amenity): string {
  const words = l();
  const labels: Record<string, string> = {
    PARKING: words.amenityParking, CHANGING_ROOM: words.amenityChangingRoom, SHOWER: words.amenityShower,
    DRINKING_WATER: words.amenityDrinkingWater, CAFE: words.amenityCafe, FLOODLIGHTS: words.amenityFloodlights, WIFI: words.amenityWifi,
    FIRST_AID: words.amenityFirstAid, EQUIPMENT_RENTAL: words.amenityEquipmentRental, SPECTATOR_SEATING: words.amenitySpectatorSeating,
  };
  return labels[amenity.code] ?? amenity.name;
}
