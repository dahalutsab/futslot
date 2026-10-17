import { planTerms, resultLabel } from './labels';

describe('labels', () => {
  it('says what a membership gives', () => {
    expect(planTerms({ type: 'GAME_PACK', gamesIncluded: 10, discountPercent: null }, 60)).toBe('10 games, for 60 days');
    expect(planTerms({ type: 'MONTHLY_PASS', gamesIncluded: null, discountPercent: 15 })).toBe('15% off every game');
  });

  it('names the winning side', () => {
    expect(resultLabel('A_WON', 'Reds', 'Blues')).toBe('Reds won');
    expect(resultLabel('B_WON', 'Reds', 'Blues')).toBe('Blues won');
    expect(resultLabel('DRAW', 'Reds', 'Blues')).toBe('Draw');
  });
});
