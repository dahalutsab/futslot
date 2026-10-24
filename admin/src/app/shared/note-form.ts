import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

/**
 * The second step of an action that needs words: a reason to reject or suspend, or the reference of a transfer
 * FutSlot sent. Keeps the action next to the item it's about.
 */
@Component({
  selector: 'fs-note-form',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="space-y-2" (ngSubmit)="submit()">
      <label [for]="id()" class="block font-medium">{{ label() }}@if (optional()) {<span class="font-normal text-subtle"> (optional)</span>}</label>
      <input [id]="id()" name="note" type="text" [attr.maxlength]="maxLength()" [placeholder]="placeholder()"
        class="h-11 w-full rounded-md border border-border bg-surface px-3 text-base" [ngModel]="text()" (ngModelChange)="text.set($event)" />
      @if (problem(); as message) {
        <p class="text-sm text-danger" role="alert">{{ message }}</p>
      }
      <div class="flex gap-2">
        <button type="submit" [disabled]="busy()" class="h-11 rounded-md px-4 font-medium disabled:opacity-60"
          [class]="danger() ? 'bg-danger text-white' : 'bg-ink text-ink-fg'">{{ action() }}</button>
        <button type="button" (click)="cancelled.emit()" class="h-11 rounded-md px-4 font-medium">Back</button>
      </div>
    </form>
  `,
})
export class NoteForm {
  readonly id = input('note');

  readonly label = input.required<string>();

  readonly action = input.required<string>();

  readonly placeholder = input('');

  readonly optional = input(false);

  readonly danger = input(false);

  readonly maxLength = input(500);

  readonly busy = input(false);

  readonly done = output<string>();

  readonly cancelled = output<void>();

  protected readonly text = signal('');

  protected readonly problem = signal<string | null>(null);

  protected submit(): void {
    const text = this.text().trim();
    if (!this.optional() && text.length < 3) {
      this.problem.set('Write a few words.');
      return;
    }
    this.problem.set(null);
    this.done.emit(text);
  }
}
