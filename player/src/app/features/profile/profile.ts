import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { PlayerCard, Position, PreferredFoot, SkillLevel } from '../../core/api.types';
import { l } from '../../i18n/i18n';
import { footLabels, positionLabels, skillLabels } from '../../shared/labels';

function options<T extends string>(labels: Record<T, string>): { value: T; label: string }[] {
  return (Object.keys(labels) as T[]).map((value) => ({ value, label: labels[value] }));
}

/** What other players see when the player asks to join a game or a team: position, level, foot, city. */
@Component({
  selector: 'fs-profile',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-xl px-4 py-6 lg:py-10">
      <h1 class="text-2xl font-semibold tracking-tight">{{ l().meProfile }}</h1>
      <p class="mt-1 text-muted">{{ l().profileIntro }}</p>
      @if (card(); as c) {
        <p class="mt-3 text-sm text-muted">{{ l().gamesPlayedOnFutSlot(l().games(c.gamesPlayed)) }}</p>
        <form class="mt-5 space-y-5 rounded-xl border border-border bg-surface p-5" (ngSubmit)="save()">
          @for (group of groups(); track group.key) {
            <fieldset>
              <legend class="mb-1.5 font-medium">{{ group.label }}</legend>
              <div class="flex flex-wrap gap-2">
                @for (o of group.options; track o.value) {
                  <button type="button" (click)="pick(group.key, o.value)" [attr.aria-pressed]="value(group.key) === o.value"
                    class="h-10 rounded-full border px-4 text-sm font-medium"
                    [class]="value(group.key) === o.value ? 'border-ink bg-ink text-ink-fg' : 'border-border text-muted hover:text-text'">{{ o.label }}</button>
                }
              </div>
            </fieldset>
          }
          <div>
            <label for="city" class="mb-1.5 block font-medium">{{ l().city }}</label>
            <input id="city" name="city" type="text" maxlength="60" class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base"
              [ngModel]="city()" (ngModelChange)="city.set($event)" />
          </div>
          <div>
            <label for="bio" class="mb-1.5 block font-medium">{{ l().aboutYou }} <span class="font-normal text-subtle">{{ l().optional }}</span></label>
            <textarea id="bio" name="bio" rows="3" maxlength="500" [placeholder]="l().aboutYouHint"
              class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-base" [ngModel]="bio()" (ngModelChange)="bio.set($event)"></textarea>
          </div>
          <label class="flex min-h-11 cursor-pointer items-center gap-3">
            <input type="checkbox" name="isPublic" class="size-5" [ngModel]="isPublic()" (ngModelChange)="isPublic.set($event)" />
            <span>{{ l().profilePublic }}</span>
          </label>
          @if (message(); as m) {
            <p [class]="m.ok ? 'text-confirmed-fg' : 'text-danger'" role="status">{{ m.text }}</p>
          }
          <button type="submit" [disabled]="saving()" class="h-11 rounded-lg bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">{{ saving() ? l().saving : l().save }}</button>
        </form>
      } @else if (!message()) {
        <div class="mt-6 h-64 animate-pulse rounded-xl bg-surface" [attr.aria-label]="l().loading"></div>
      }
    </div>
  `,
})
export class Profile {
  protected readonly l = l;

  private readonly api = inject(Api);

  protected readonly groups = computed(() => [
    { key: 'position' as const, label: l().position, options: options<Position>(positionLabels()) },
    { key: 'skillLevel' as const, label: l().level, options: options<SkillLevel>(skillLabels()) },
    { key: 'preferredFoot' as const, label: l().strongerFoot, options: options<PreferredFoot>(footLabels()) },
  ]);

  protected readonly card = signal<PlayerCard | null>(null);

  protected readonly choices = signal<{ position?: Position; skillLevel?: SkillLevel; preferredFoot?: PreferredFoot }>({});

  protected readonly city = signal('');

  protected readonly bio = signal('');

  protected readonly isPublic = signal(true);

  protected readonly saving = signal(false);

  protected readonly message = signal<{ ok: boolean; text: string } | null>(null);

  constructor() {
    this.api.myProfile().then((card) => this.show(card), (error) => this.message.set({ ok: false, text: problemOf(error).message }));
  }

  protected value(key: 'position' | 'skillLevel' | 'preferredFoot'): string | undefined {
    return this.choices()[key];
  }

  /** Tapping the chosen one again clears it. */
  protected pick(key: 'position' | 'skillLevel' | 'preferredFoot', value: string): void {
    this.choices.update((c) => ({ ...c, [key]: c[key] === value ? undefined : value }));
  }

  protected async save(): Promise<void> {
    this.saving.set(true);
    this.message.set(null);
    try {
      this.show(await this.api.saveProfile({ ...this.choices(), city: this.city().trim() || undefined, bio: this.bio().trim() || undefined, isPublic: this.isPublic() }));
      this.message.set({ ok: true, text: l().saved });
    }
    catch (error) {
      this.message.set({ ok: false, text: problemOf(error).message });
    }
    finally {
      this.saving.set(false);
    }
  }

  private show(card: PlayerCard): void {
    this.card.set(card);
    this.choices.set({ position: card.position ?? undefined, skillLevel: card.skillLevel ?? undefined, preferredFoot: card.preferredFoot ?? undefined });
    this.city.set(card.city ?? '');
    this.bio.set(card.bio ?? '');
    this.isPublic.set(card.isPublic ?? true);
  }
}
