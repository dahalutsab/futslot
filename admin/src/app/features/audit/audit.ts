import { ChangeDetectionStrategy, Component, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { AuditEntry } from '../../core/api.types';
import { ago } from '../../shared/format';
import { actionLabel } from '../../shared/labels';

/** Who did what, newest first: venue staff and FutSlot admins. Narrowed to one venue from its page. */
@Component({
  selector: 'fs-audit',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-5xl px-4 py-6 lg:px-8 lg:py-8">
      <h1 class="text-3xl lg:text-4xl">Audit log</h1>
      @if (venue()) {
        <p class="mt-2 flex items-center gap-2 text-muted">
          Only <a [routerLink]="['/venues', venue()]" class="font-medium text-text hover:underline">{{ name() ?? 'one venue' }}</a>
          <a routerLink="/audit" class="ml-2 text-sm font-medium underline-offset-2 hover:underline">Show all</a>
        </p>
      }
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      }
      @if (entries(); as list) {
        @if (list.length === 0) {
          <p class="mt-6 rounded-md border border-dashed border-border bg-surface px-6 py-12 text-center text-muted">Nothing recorded yet.</p>
        } @else {
          <div class="mt-4 overflow-x-auto rounded-md border border-border bg-surface">
            <table class="w-full min-w-[640px] text-left text-sm">
              <thead class="border-b border-border text-muted">
                <tr><th scope="col" class="px-4 py-2.5 font-medium">When</th><th scope="col" class="px-4 py-2.5 font-medium">Who</th><th scope="col" class="px-4 py-2.5 font-medium">What</th><th scope="col" class="px-4 py-2.5 font-medium">Details</th></tr>
              </thead>
              <tbody>
                @for (e of list; track e.id) {
                  <tr class="border-b border-border align-top last:border-0">
                    <td class="whitespace-nowrap px-4 py-2.5 text-muted">{{ ago(e.createdAt) }}</td>
                    <td class="px-4 py-2.5">{{ e.actorName ?? 'FutSlot' }}@if (e.ipAddress) {<span class="tabular block text-xs text-subtle">{{ e.ipAddress }}</span>}</td>
                    <td class="px-4 py-2.5">
                      {{ actionLabel(e.action) }}
                      @if (e.venueId && !venue()) {
                        <a [routerLink]="['/venues', e.venueId]" class="block text-xs text-muted hover:underline">Venue</a>
                      }
                    </td>
                    <td class="px-4 py-2.5 text-muted">{{ details(e) }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          @if (hasMore()) {
            <div class="mt-4 text-center">
              <button type="button" (click)="load(page() + 1)" class="h-11 rounded-md border border-border bg-surface px-6 font-medium hover:bg-surface-2">Show older</button>
            </div>
          }
        }
      } @else if (!error()) {
        <div class="mt-6 h-48 animate-pulse rounded-md bg-surface" aria-label="Loading"></div>
      }
    </div>
  `,
})
export class Audit {
  private readonly api = inject(Api);

  /** A venue's id from the address, to see only its entries. */
  readonly venue = input<string | undefined>();

  readonly name = input<string | undefined>();

  protected readonly ago = ago;

  protected readonly actionLabel = actionLabel;

  protected readonly entries = signal<AuditEntry[] | null>(null);

  protected readonly page = signal(0);

  protected readonly hasMore = signal(false);

  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.venue();
      untracked(() => void this.load(0));
    });
  }

  protected details(entry: AuditEntry): string {
    return Object.entries(entry.changes ?? {}).map(([key, value]) => `${key}: ${value}`).join(' · ');
  }

  protected async load(page: number): Promise<void> {
    if (page === 0) {
      this.entries.set(null);
    }
    try {
      const result = await this.api.audit(this.venue(), page);
      this.entries.set(page === 0 ? result.content : [...(this.entries() ?? []), ...result.content]);
      this.page.set(page);
      this.hasMore.set(page + 1 < result.totalPages);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
