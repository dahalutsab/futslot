import { TournamentMatch } from '../../../core/api.types';
import { groupsFinished, score, sections } from './tournament-model';

function match(change: Partial<TournamentMatch>): TournamentMatch {
  return { id: Math.random().toString(), stage: 'GROUP', groupName: 'A', round: 1, matchNumber: 1, home: null, away: null,
    court: null, scheduledAt: null, homeScore: null, awayScore: null, homePenalties: null, awayPenalties: null,
    winnerRegistrationId: null, status: 'SCHEDULED', bye: false, ...change };
}

describe('tournament model', () => {
  it('orders fixtures by stage, round and group, and drops byes', () => {
    const list = sections([
      match({ stage: 'FINAL', round: 1 }),
      match({ groupName: 'B', round: 1 }),
      match({ groupName: 'A', round: 2 }),
      match({ groupName: 'A', round: 1 }),
      match({ stage: 'SEMI_FINAL', round: 1, matchNumber: 2 }),
      match({ stage: 'SEMI_FINAL', round: 1, matchNumber: 1, bye: true }),
    ]);
    expect(list.map((s) => [s.title, s.matches.length])).toEqual([
      ['Group A, round 1', 1], ['Group B, round 1', 1], ['Group A, round 2', 1], ['Semi-finals', 1], ['Final', 1]]);
  });

  it('writes scores with penalties', () => {
    expect(score(match({}))).toBeNull();
    expect(score(match({ homeScore: 3, awayScore: 1 }))).toBe('3 – 1');
    expect(score(match({ homeScore: 2, awayScore: 2, homePenalties: 4, awayPenalties: 3 }))).toBe('2 – 2 (4–3 pens)');
  });

  it('knows when the groups are done', () => {
    const done = match({ status: 'COMPLETED', homeScore: 1, awayScore: 0 });
    expect(groupsFinished([done, match({})])).toBe(false);
    expect(groupsFinished([done, { ...done, id: 'b' }])).toBe(true);
    expect(groupsFinished([done, match({ stage: 'FINAL' })])).toBe(false);
  });
});
