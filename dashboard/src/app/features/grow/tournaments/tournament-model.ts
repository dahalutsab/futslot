import { Stage, TournamentFormat, TournamentMatch, TournamentStatus } from '../../../core/api.types';

export const FORMATS: { value: TournamentFormat; label: string; detail: string }[] = [
  { value: 'KNOCKOUT', label: 'Knockout', detail: 'Lose and you’re out. Quick: 8 teams need 7 matches.' },
  { value: 'LEAGUE', label: 'League', detail: 'Everyone plays everyone once; the table decides. 6 teams need 15 matches.' },
  { value: 'GROUP_KNOCKOUT', label: 'Groups, then knockout', detail: 'Round-robin groups, then the top two of each play a knockout.' },
];

export const STATUS_LABELS: Record<TournamentStatus, string> = {
  DRAFT: 'Draft',
  REGISTRATION_OPEN: 'Taking teams',
  REGISTRATION_CLOSED: 'Registration closed',
  IN_PROGRESS: 'Under way',
  COMPLETED: 'Finished',
  CANCELLED: 'Cancelled',
};

const STAGE_LABELS: Record<Stage, string> = {
  GROUP: 'Group',
  LEAGUE: 'League',
  ROUND_OF_32: 'Round of 32',
  ROUND_OF_16: 'Round of 16',
  QUARTER_FINAL: 'Quarter-finals',
  SEMI_FINAL: 'Semi-finals',
  THIRD_PLACE: 'Third place',
  FINAL: 'Final',
};

const STAGE_ORDER: Stage[] = ['GROUP', 'LEAGUE', 'ROUND_OF_32', 'ROUND_OF_16', 'QUARTER_FINAL', 'SEMI_FINAL', 'THIRD_PLACE', 'FINAL'];

export function formatLabel(format: TournamentFormat): string {
  return FORMATS.find((f) => f.value === format)?.label ?? format;
}

export interface MatchSection {
  key: string;
  title: string;
  matches: TournamentMatch[];
}

/**
 * Fixtures in the order they're played: group or league rounds first ("Group A, round 1"), then each
 * knockout stage. Byes are left out; they're not matches anyone plays.
 */
export function sections(matches: TournamentMatch[]): MatchSection[] {
  const played = matches.filter((m) => !m.bye);
  const byKey = new Map<string, MatchSection>();
  const sorted = [...played].sort((a, b) => STAGE_ORDER.indexOf(a.stage) - STAGE_ORDER.indexOf(b.stage)
    || a.round - b.round || (a.groupName ?? '').localeCompare(b.groupName ?? '') || a.matchNumber - b.matchNumber);
  for (const match of sorted) {
    const knockout = match.stage !== 'GROUP' && match.stage !== 'LEAGUE';
    const key = knockout ? match.stage : `${match.stage}-${match.round}-${match.groupName ?? ''}`;
    const title = knockout ? STAGE_LABELS[match.stage]
      : match.stage === 'GROUP' ? `Group ${match.groupName}, round ${match.round}` : `Round ${match.round}`;
    let section = byKey.get(key);
    if (!section) {
      section = { key, title, matches: [] };
      byKey.set(key, section);
    }
    section.matches.push(match);
  }
  return [...byKey.values()];
}

export function isKnockout(stage: Stage): boolean {
  return stage !== 'GROUP' && stage !== 'LEAGUE';
}

/** Whether a result has been recorded. */
export function decided(match: TournamentMatch): boolean {
  return match.status === 'COMPLETED' || match.status === 'WALKOVER';
}

/** "3 – 1", "2 – 2 (4–3 pens)", or null before the result. */
export function score(match: TournamentMatch): string | null {
  if (match.homeScore === null || match.awayScore === null) {
    return null;
  }
  const pens = match.homePenalties !== null && match.awayPenalties !== null ? ` (${match.homePenalties}–${match.awayPenalties} pens)` : '';
  return `${match.homeScore} – ${match.awayScore}${pens}`;
}

/** Groups are done when every group match has a result; then the knockout can be drawn. */
export function groupsFinished(matches: TournamentMatch[]): boolean {
  const groups = matches.filter((m) => m.stage === 'GROUP' && !m.bye);
  return groups.length > 0 && groups.every(decided) && !matches.some((m) => isKnockout(m.stage));
}
