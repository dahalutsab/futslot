import {
  EntryStatus, MatchResult, Position, PreferredFoot, SkillLevel, SplitRule, Stage, TeamRole, TournamentFormat,
  TournamentStatus,
} from '../core/api.types';

/** How each API value reads to a player. */

export const SKILL: Record<SkillLevel, string> = {
  BEGINNER: 'Beginner', INTERMEDIATE: 'Intermediate', ADVANCED: 'Advanced', PRO: 'Pro',
};

export const POSITION: Record<Position, string> = {
  GOALKEEPER: 'Goalkeeper', DEFENDER: 'Defender', MIDFIELDER: 'Midfielder', FORWARD: 'Forward', ANY: 'Anywhere',
};

export const FOOT: Record<PreferredFoot, string> = { LEFT: 'Left', RIGHT: 'Right', BOTH: 'Both' };

export const ROLE: Record<TeamRole, string> = { CAPTAIN: 'Captain', VICE_CAPTAIN: 'Vice-captain', MEMBER: 'Player' };

export const FORMAT: Record<TournamentFormat, string> = {
  KNOCKOUT: 'Knockout', LEAGUE: 'League', GROUP_KNOCKOUT: 'Groups, then knockout',
};

export const TOURNAMENT_STATUS: Record<TournamentStatus, string> = {
  DRAFT: 'Draft', REGISTRATION_OPEN: 'Taking teams', REGISTRATION_CLOSED: 'Entries closed',
  IN_PROGRESS: 'Under way', COMPLETED: 'Finished', CANCELLED: 'Cancelled',
};

export const STAGE: Record<Stage, string> = {
  GROUP: 'Group stage', LEAGUE: 'League', ROUND_OF_32: 'Round of 32', ROUND_OF_16: 'Round of 16',
  QUARTER_FINAL: 'Quarter-finals', SEMI_FINAL: 'Semi-finals', THIRD_PLACE: 'Third place', FINAL: 'Final',
};

export const ENTRY_STATUS: Record<EntryStatus, string> = {
  PENDING_PAYMENT: 'Pay the entry fee', CONFIRMED: 'Confirmed', WAITLISTED: 'On the waitlist',
  WITHDRAWN: 'Withdrawn', REJECTED: 'Not accepted',
};

export const SPLIT_RULE: Record<SplitRule, { label: string; detail: string }> = {
  EQUAL: { label: 'Split equally', detail: 'Everyone pays the same, whoever wins.' },
  LOSERS_PAY_ALL: { label: 'Losers pay all', detail: 'Baazi: the losing side pays everything.' },
  LOSERS_PAY_PERCENT: { label: 'Losers pay more', detail: 'Baazi: the losing side pays a share you choose, the winners the rest.' },
  LOSERS_PAY_COURT: { label: 'Losers pay the court', detail: 'Baazi: losers pay for the court, winners for the extras.' },
};

/** "10 games", "15% off every game", and for how long. */
export function planTerms(plan: { type: string; gamesIncluded: number | null; discountPercent: number | null }, validityDays?: number): string {
  const what = plan.type === 'GAME_PACK' ? `${plan.gamesIncluded} games` : `${plan.discountPercent}% off every game`;
  return validityDays ? `${what}, for ${validityDays} days` : what;
}

/** "Reds won", "Draw". */
export function resultLabel(result: MatchResult, sideA: string, sideB: string): string {
  return result === 'DRAW' ? 'Draw' : `${result === 'A_WON' ? sideA : sideB} won`;
}
