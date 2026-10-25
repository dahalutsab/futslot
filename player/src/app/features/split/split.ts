import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ArrowLeftRight, Check, Icon, Trash2 } from '../../shared/icon';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { MatchResult, Side, Split, SplitPlayer, SplitRule, SplitRules } from '../../core/api.types';
import { digits, l } from '../../i18n/i18n';
import { dayLabel, localClock, phone, rupees } from '../../shared/format';
import { resultLabel, splitRules } from '../../shared/labels';

/**
 * Sharing a booking's cost among the players: equally, or baazi-style where the losers pay more. The booker
 * sets the rule, adds players to two sides, records the result and ticks off who has paid them; the other
 * players see what they owe and to whom.
 */
@Component({
  selector: 'fs-split',
  imports: [FormsModule, RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './split.html',
})
export class SplitPage {
  protected readonly l = l;

  private readonly api = inject(Api);

  readonly id = input.required<string>();

  protected readonly icons = { ArrowLeftRight, Check, Trash2 };

  protected readonly fmt = { rupees, dayLabel, localClock, phone };

  protected readonly rules = computed(() => Object.entries(splitRules()).map(([value, text]) => ({ value: value as SplitRule, ...text })));

  protected readonly ruleText = splitRules;

  protected readonly digits = digits;

  protected readonly resultLabel = resultLabel;

  /** Undefined while loading, null when the booker hasn't set one up. */
  protected readonly split = signal<Split | null | undefined>(undefined);

  protected readonly error = signal<string | null>(null);

  protected readonly busy = signal(false);

  protected readonly actionError = signal<string | null>(null);

  protected readonly editing = signal(false);

  // The rules form
  protected readonly rule = signal<SplitRule>('EQUAL');

  protected readonly percent = signal(70);

  protected readonly extras = signal(0);

  protected readonly extrasNote = signal('');

  protected readonly sideA = signal('');

  protected readonly sideB = signal('');

  protected readonly mySide = signal<Side>('A');

  // Adding a player
  protected readonly newName = signal('');

  protected readonly newPhone = signal('');

  protected readonly newSide = signal<Side>('A');

  protected readonly sides = computed(() => {
    const split = this.split();
    if (!split) {
      return [];
    }
    return (['A', 'B'] as Side[]).map((side) => ({
      side,
      name: side === 'A' ? split.sideAName : split.sideBName,
      players: split.players.filter((p) => p.side === side),
    }));
  });

  protected readonly me = computed(() => this.split()?.players.find((p) => p.you) ?? null);

  /** Whether the game has started, so the result can go in. */
  protected readonly started = computed(() => {
    const split = this.split();
    return split ? new Date(split.startsAt).getTime() <= Date.now() : false;
  });

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => void this.load(id));
    });
  }

  protected edit(): void {
    const split = this.split();
    if (split) {
      this.rule.set(split.rule);
      this.percent.set(split.losersPercent ?? 70);
      this.extras.set(split.extrasAmount);
      this.extrasNote.set(split.extrasNote ?? '');
      this.sideA.set(split.sideAName);
      this.sideB.set(split.sideBName);
    }
    this.editing.set(true);
  }

  protected saveRules(): Promise<void> {
    const rules: SplitRules = {
      rule: this.rule(),
      losersPercent: this.rule() === 'LOSERS_PAY_PERCENT' ? this.percent() : undefined,
      extrasAmount: this.extras() || 0,
      extrasNote: this.extrasNote().trim() || undefined,
      sideAName: this.sideA().trim() || undefined,
      sideBName: this.sideB().trim() || undefined,
      bookerSide: this.split() ? undefined : this.mySide(),
    };
    return this.act(async () => {
      this.split.set(await this.api.setUpSplit(this.id(), rules));
      this.editing.set(false);
    });
  }

  protected addPlayer(): Promise<void> {
    const name = this.newName().trim();
    const phoneNumber = this.newPhone().replace(/\D/g, '');
    if (!name && !phoneNumber) {
      this.actionError.set(l().giveNameOrPhone);
      return Promise.resolve();
    }
    return this.act(async () => {
      this.split.set(await this.api.addSplitPlayer(this.id(), { name: name || undefined, phone: phoneNumber || undefined, side: this.newSide() }));
      this.newName.set('');
      this.newPhone.set('');
    });
  }

  protected move(player: SplitPlayer): Promise<void> {
    return this.act(async () => this.split.set(await this.api.moveSplitPlayer(this.id(), player.id, player.side === 'A' ? 'B' : 'A')));
  }

  protected remove(player: SplitPlayer): Promise<void> {
    return this.act(async () => this.split.set(await this.api.removeSplitPlayer(this.id(), player.id)));
  }

  protected record(result: MatchResult | null): Promise<void> {
    return this.act(async () => this.split.set(await this.api.splitResult(this.id(), result)));
  }

  protected undoResult(): Promise<void> {
    return this.act(async () => this.split.set(await this.api.undoSplitResult(this.id())));
  }

  protected paid(player: SplitPlayer): Promise<void> {
    return this.act(async () => this.split.set(await this.api.splitPaid(this.id(), player.id, !player.settled)));
  }

  private async act(action: () => Promise<void>): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.actionError.set(null);
    try {
      await action();
    }
    catch (error) {
      this.actionError.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private async load(id: string): Promise<void> {
    try {
      this.split.set(await this.api.split(id));
    }
    catch (error) {
      const problem = problemOf(error);
      if (problem.code === 'SPLIT_NOT_FOUND') {
        this.split.set(null);
      } else {
        this.error.set(problem.message);
      }
    }
  }
}
