import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { Check, Copy, LoaderCircle, LucideAngularModule, UserPlus } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { StaffMember, StaffRole, VenuePermission } from '../../core/api.types';
import { Session } from '../../core/session';
import { VenueContext } from '../../core/venue-context';
import { ago, phone } from '../../shared/format';
import { Sheet } from '../../shared/sheet';

type Grantable = Exclude<VenuePermission, 'MANAGE_STAFF'>;

const PERMISSIONS: { value: Grantable; label: string; short: string; detail: string }[] = [
  { value: 'MANAGE_BOOKINGS', short: 'Bookings', label: 'Bookings and payments', detail: 'Book, take and check payments, cancel, mark played, reply to messages' },
  { value: 'MANAGE_BLOCKS', short: 'Blocks', label: 'Block court time', detail: 'For repairs or private events' },
  { value: 'VIEW_CUSTOMERS', short: 'Customers', label: 'Customers', detail: 'See customers, their history and your notes' },
  { value: 'VIEW_REPORTS', short: 'Reports', label: 'Reports', detail: 'Takings, busy hours and exports' },
  { value: 'MANAGE_VENUE', short: 'Settings', label: 'Venue settings', detail: 'Hours, rules, courts, prices, extras and photos' },
];

/** Matches the API's defaults for each role. */
const ROLES: { value: StaffRole; label: string; detail: string; permissions: Grantable[] }[] = [
  { value: 'RECEPTIONIST', label: 'Receptionist', detail: 'Runs the counter: bookings, payments, blocks and customers.',
    permissions: ['MANAGE_BOOKINGS', 'MANAGE_BLOCKS', 'VIEW_CUSTOMERS'] },
  { value: 'MANAGER', label: 'Manager', detail: 'Everything except staff and your FutSlot plan.',
    permissions: ['MANAGE_BOOKINGS', 'MANAGE_BLOCKS', 'VIEW_CUSTOMERS', 'VIEW_REPORTS', 'MANAGE_VENUE'] },
];

interface Editing {
  member: StaffMember | null;
  phone: string;
  name: string;
  role: StaffRole;
  permissions: Grantable[];
  active: boolean;
}

/**
 * Who else works at the venue and what each can do. Staff sign in to this dashboard with their own phone
 * number; only the owner manages staff and billing.
 */
@Component({
  selector: 'fs-staff',
  imports: [LucideAngularModule, Sheet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-3xl px-4 py-4 lg:px-8 lg:py-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="text-3xl lg:text-4xl">Staff</h1>
          <p class="mt-1 text-muted">People who help run {{ venueName() }}. Each signs in with their own phone number.</p>
        </div>
        @if (members()) {
          <button type="button" (click)="add()" class="flex h-11 items-center gap-2 rounded-md bg-ink px-4 font-medium text-ink-fg hover:opacity-90">
            <lucide-icon [img]="icons.UserPlus" [size]="18" />Add staff
          </button>
        }
      </div>

      @if (invite(); as message) {
        <section class="mt-5 rounded-md border border-border bg-surface p-4" role="status">
          <p class="flex items-center gap-2 font-medium"><lucide-icon [img]="icons.Check" [size]="18" class="text-confirmed-fg" />{{ invitedName() }} can sign in now</p>
          <p class="mt-1 text-sm text-muted">Send them this so they know where to go:</p>
          <p class="mt-2 rounded-md bg-surface-2 px-3 py-2">{{ message }}</p>
          <div class="mt-3 flex gap-2">
            <button type="button" (click)="copy(message)" class="flex h-10 items-center gap-2 rounded-md border border-border px-3 text-sm font-medium hover:bg-surface-2">
              <lucide-icon [img]="copied() ? icons.Check : icons.Copy" [size]="16" />{{ copied() ? 'Copied' : 'Copy message' }}
            </button>
            <button type="button" (click)="invite.set(null)" class="h-10 rounded-md px-3 text-sm font-medium hover:bg-surface-2">Done</button>
          </div>
        </section>
      }

      @if (error(); as message) {
        <div class="mt-6 rounded-md border border-border bg-surface px-6 py-8 text-center">
          <p class="text-muted">{{ message }}</p>
          <button type="button" (click)="retry()" class="mt-3 h-11 rounded-md border border-border px-4 font-medium hover:bg-surface-2">Try again</button>
        </div>
      } @else if (members(); as list) {
        <ul class="mt-5 divide-y divide-border overflow-hidden rounded-md border border-border bg-surface">
          <li class="flex items-center gap-3 px-4 py-3">
            <span class="grid size-10 shrink-0 place-items-center rounded-full bg-surface-2 font-semibold text-muted" aria-hidden="true">{{ initial(session.user()?.fullName) }}</span>
            <span class="min-w-0 flex-1">
              <span class="block truncate font-medium">{{ session.user()?.fullName }} <span class="font-normal text-subtle">(you)</span></span>
              <span class="tabular block text-sm text-muted">{{ fmt.phone(session.user()?.phone) }}</span>
            </span>
            <span class="text-sm text-muted">Owner</span>
          </li>
          @for (member of list; track member.id) {
            <li>
              <button type="button" (click)="edit(member)" class="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-surface-2">
                <span class="grid size-10 shrink-0 place-items-center rounded-full bg-surface-2 font-semibold text-muted" aria-hidden="true">{{ initial(member.fullName) }}</span>
                <span class="min-w-0 flex-1">
                  <span class="block truncate font-medium" [class.text-muted]="!member.active">{{ member.fullName }}</span>
                  <span class="block truncate text-sm text-muted"><span class="tabular">{{ fmt.phone(member.phone) }}</span> · {{ summary(member) }}</span>
                </span>
                <span class="shrink-0 text-right text-sm">
                  <span class="block" [class.text-muted]="!member.active">{{ roleLabel(member.role) }}</span>
                  @if (!member.active) {
                    <span class="block text-subtle">Can't sign in</span>
                  }
                </span>
              </button>
            </li>
          }
        </ul>
        @if (list.length === 0) {
          <p class="mt-3 text-muted">No staff yet. Add a receptionist so the counter can take bookings without your phone.</p>
        }
      } @else {
        <div class="mt-6 h-40 animate-pulse rounded-md bg-surface" aria-label="Loading"></div>
      }
    </div>

    @if (editing(); as e) {
      <fs-sheet [title]="e.member ? e.member.fullName : 'Add staff'" [subtitle]="e.member ? fmt.phone(e.member.phone) + ' · added ' + fmt.ago(e.member.createdAt) : null"
        (closed)="editing.set(null)">
        <form id="staff-form" class="space-y-5" (submit)="$event.preventDefault(); save()">
          @if (!e.member) {
            <div>
              <label for="staff-phone" class="mb-1.5 block font-medium">Their phone number</label>
              <input id="staff-phone" type="tel" inputmode="numeric" autocomplete="off" placeholder="98XXXXXXXX" [value]="e.phone"
                (input)="patch({ phone: $any($event.target).value })" class="tabular h-12 w-full rounded-md border border-border bg-surface px-3 text-base" />
              <p class="mt-1.5 text-sm text-subtle">They sign in with this number and a code sent to it.</p>
            </div>
            <div>
              <label for="staff-name" class="mb-1.5 block font-medium">Name</label>
              <input id="staff-name" type="text" maxlength="100" autocomplete="off" [value]="e.name"
                (input)="patch({ name: $any($event.target).value })" class="h-12 w-full rounded-md border border-border bg-surface px-3 text-base" />
            </div>
          }
          <fieldset>
            <legend class="mb-1.5 font-medium">Role</legend>
            <div class="space-y-2">
              @for (role of roles; track role.value) {
                <label class="flex cursor-pointer items-start gap-3 rounded-md border p-3" [class]="e.role === role.value ? 'border-ink bg-surface-2' : 'border-border'">
                  <input type="radio" name="role" class="mt-1 size-4 accent-current" [checked]="e.role === role.value" (change)="pickRole(role.value)" />
                  <span>
                    <span class="block font-medium">{{ role.label }}</span>
                    <span class="text-sm text-muted">{{ role.detail }}</span>
                  </span>
                </label>
              }
            </div>
          </fieldset>
          <fieldset>
            <legend class="mb-1.5 font-medium">What they can do</legend>
            <div class="divide-y divide-border rounded-md border border-border">
              @for (permission of permissions; track permission.value) {
                <label class="flex cursor-pointer items-start gap-3 p-3">
                  <input type="checkbox" class="mt-1 size-4 accent-current" [checked]="e.permissions.includes(permission.value)" (change)="togglePermission(permission.value)" />
                  <span>
                    <span class="block font-medium">{{ permission.label }}</span>
                    <span class="text-sm text-muted">{{ permission.detail }}</span>
                  </span>
                </label>
              }
            </div>
            <p class="mt-1.5 text-sm text-subtle">Choosing a role sets these to its usual list; change any you like.</p>
          </fieldset>
          @if (e.member) {
            <label class="flex cursor-pointer items-start gap-3 rounded-md border border-border p-3">
              <input type="checkbox" class="mt-1 size-4 accent-current" [checked]="e.active" (change)="patch({ active: !e.active })" />
              <span>
                <span class="block font-medium">Can sign in</span>
                <span class="text-sm text-subtle">Untick while they're on leave. Their name stays on bookings they handled.</span>
              </span>
            </label>
            <div class="border-t border-border pt-4">
              @if (confirmRemove()) {
                <p class="font-medium">Remove {{ e.member.fullName }}?</p>
                <p class="text-sm text-muted">They lose access to {{ venueName() }} straight away. You can add them again later.</p>
                <div class="mt-3 flex gap-2">
                  <button type="button" (click)="remove()" [disabled]="saving()" class="h-11 rounded-md bg-danger px-4 font-medium text-surface disabled:opacity-60">Remove</button>
                  <button type="button" (click)="confirmRemove.set(false)" class="h-11 rounded-md border border-border px-4 font-medium">Keep</button>
                </div>
              } @else {
                <button type="button" (click)="confirmRemove.set(true)" class="h-11 rounded-md px-1 font-medium text-danger hover:underline">Remove from staff</button>
              }
            </div>
          }
        </form>
        @if (formError(); as message) {
          <p class="mt-4 rounded-md bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
        }
        <div sheet-footer class="border-t border-border px-5 py-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
          <button type="submit" form="staff-form" [disabled]="saving()"
            class="flex h-12 w-full items-center justify-center gap-2 rounded-md bg-ink font-medium text-ink-fg disabled:opacity-60">
            @if (saving()) {
              <lucide-icon [img]="icons.LoaderCircle" [size]="18" class="animate-spin" />
            }
            {{ e.member ? 'Save' : 'Add to staff' }}
          </button>
        </div>
      </fs-sheet>
    }
  `,
})
export class Staff {
  private readonly http = inject(HttpClient);

  private readonly venues = inject(VenueContext);

  protected readonly session = inject(Session);

  protected readonly icons = { Check, Copy, LoaderCircle, UserPlus };

  protected readonly fmt = { ago, phone };

  protected readonly roles = ROLES;

  protected readonly permissions = PERMISSIONS;

  protected readonly members = signal<StaffMember[] | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly editing = signal<Editing | null>(null);

  protected readonly saving = signal(false);

  protected readonly formError = signal<string | null>(null);

  protected readonly confirmRemove = signal(false);

  protected readonly invite = signal<string | null>(null);

  protected readonly invitedName = signal('');

  protected readonly copied = signal(false);

  protected readonly venueName = computed(() => this.venues.current()?.venue.name ?? 'your venue');

  private readonly venueId = computed(() => this.venues.current()?.venue.id ?? null);

  constructor() {
    effect(() => {
      const id = this.venueId();
      if (id) {
        untracked(() => void this.load(id));
      }
    });
  }

  protected initial(name: string | null | undefined): string {
    return (name ?? '?').charAt(0).toUpperCase();
  }

  protected roleLabel(role: StaffRole): string {
    return ROLES.find((r) => r.value === role)?.label ?? role;
  }

  /** What they can do, in short. */
  protected summary(member: StaffMember): string {
    const granted = PERMISSIONS.filter((p) => member.permissions.includes(p.value)).map((p) => p.short);
    return granted.length ? granted.join(', ') : 'Nothing yet';
  }

  protected add(): void {
    this.open({ member: null, phone: '', name: '', role: 'RECEPTIONIST', permissions: [...ROLES[0].permissions], active: true });
  }

  protected edit(member: StaffMember): void {
    this.open({ member, phone: member.phone, name: member.fullName, role: member.role,
      permissions: member.permissions.filter((p): p is Grantable => p !== 'MANAGE_STAFF'), active: member.active });
  }

  protected patch(change: Partial<Editing>): void {
    this.editing.update((e) => (e ? { ...e, ...change } : e));
  }

  protected pickRole(role: StaffRole): void {
    this.patch({ role, permissions: [...ROLES.find((r) => r.value === role)!.permissions] });
  }

  protected togglePermission(permission: Grantable): void {
    const current = this.editing()?.permissions ?? [];
    this.patch({ permissions: current.includes(permission) ? current.filter((p) => p !== permission) : [...current, permission] });
  }

  protected async save(): Promise<void> {
    const e = this.editing();
    const id = this.venueId();
    if (!e || !id || this.saving()) {
      return;
    }
    if (!e.member && e.phone.replace(/\D/g, '').length < 10) {
      this.formError.set('Enter their 10-digit mobile number, e.g. 98XXXXXXXX.');
      return;
    }
    if (!e.member && !e.name.trim()) {
      this.formError.set('Enter their name.');
      return;
    }
    this.saving.set(true);
    this.formError.set(null);
    const base = `/api/v1/owner/venues/${id}/staff`;
    try {
      if (e.member) {
        const saved = await firstValueFrom(this.http.put<StaffMember>(`${base}/${e.member.id}`,
          { role: e.role, permissions: e.permissions, active: e.active }));
        this.members.update((list) => (list ?? []).map((m) => (m.id === saved.id ? saved : m)));
      }
      else {
        const saved = await firstValueFrom(this.http.post<StaffMember>(base,
          { phone: e.phone.trim(), fullName: e.name.trim(), role: e.role, permissions: e.permissions }));
        this.members.update((list) => [...(list ?? []).filter((m) => m.id !== saved.id), saved]);
        this.invitedName.set(saved.fullName);
        this.copied.set(false);
        this.invite.set(`You've been added to ${this.venueName()} on FutSlot as ${this.roleLabel(saved.role).toLowerCase()}. `
          + `Sign in at ${location.origin} with ${phone(saved.phone)}.`);
      }
      this.editing.set(null);
    }
    catch (error) {
      this.formError.set(problemOf(error).message);
    }
    finally {
      this.saving.set(false);
    }
  }

  protected async remove(): Promise<void> {
    const e = this.editing();
    const id = this.venueId();
    if (!e?.member || !id) {
      return;
    }
    this.saving.set(true);
    try {
      await firstValueFrom(this.http.delete(`/api/v1/owner/venues/${id}/staff/${e.member.id}`));
      this.members.update((list) => (list ?? []).filter((m) => m.id !== e.member!.id));
      this.editing.set(null);
    }
    catch (error) {
      this.formError.set(problemOf(error).message);
    }
    finally {
      this.saving.set(false);
    }
  }

  protected async copy(text: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(text);
      this.copied.set(true);
    }
    catch {
      // Clipboard blocked; the text is on screen to copy by hand
    }
  }

  protected retry(): void {
    const id = this.venueId();
    if (id) {
      void this.load(id);
    }
  }

  private open(editing: Editing): void {
    this.formError.set(null);
    this.confirmRemove.set(false);
    this.editing.set(editing);
  }

  private async load(id: string): Promise<void> {
    try {
      this.members.set(await firstValueFrom(this.http.get<StaffMember[]>(`/api/v1/owner/venues/${id}/staff`)));
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
