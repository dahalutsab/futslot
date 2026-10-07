import { HttpClient, HttpEventType } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ChevronLeft, ChevronRight, ImagePlus, LoaderCircle, LucideAngularModule, Star, Trash2 } from 'lucide-angular';
import { firstValueFrom, lastValueFrom, tap } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { VenuePhoto } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { shrink } from '../../shared/image';

const MAX_PHOTOS = 15;

interface Upload {
  key: number;
  name: string;
  /** 0–100 while sending. */
  progress: number;
  error: string | null;
}

/**
 * The venue's photos as players see them in the app. The cover leads search results; the rest show in order
 * on the venue page. Photos are shrunk in the browser before upload.
 */
@Component({
  selector: 'fs-photos',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(dragover)': '$event.preventDefault()', '(drop)': 'dropped($event)' },
  template: `
    <div class="mx-auto max-w-5xl px-4 py-4 lg:px-8 lg:py-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="flex items-baseline gap-3 text-xl font-semibold tracking-tight lg:text-2xl">
            Photos
            @if (photos(); as list) {
              <span class="tabular text-base font-normal text-muted">{{ list.length }} of {{ max }}</span>
            }
          </h1>
          <p class="mt-1 text-muted">Bright, wide shots of the court in daylight work best. The cover shows first in the app.</p>
        </div>
        @if (photos() && room() > 0) {
          <label class="flex h-11 cursor-pointer items-center gap-2 rounded-lg bg-ink px-4 font-medium text-ink-fg hover:opacity-90 has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-ring">
            <lucide-icon [img]="icons.ImagePlus" [size]="18" />Add photos
            <input type="file" accept="image/*" multiple class="sr-only" (change)="picked($event)" />
          </label>
        }
      </div>

      @if (notice(); as message) {
        <p class="mt-4 rounded-lg bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
      }

      @if (error(); as message) {
        <div class="mt-6 rounded-xl border border-border bg-surface px-6 py-8 text-center">
          <p class="text-muted">{{ message }}</p>
          <button type="button" (click)="retry()" class="mt-3 h-11 rounded-lg border border-border px-4 font-medium hover:bg-surface-2">Try again</button>
        </div>
      } @else if (photos(); as list) {
        @if (list.length === 0 && uploads().length === 0) {
          <label class="mt-5 flex cursor-pointer flex-col items-center rounded-xl border-2 border-dashed border-border bg-surface px-6 py-12 text-center hover:bg-surface-2">
            <lucide-icon [img]="icons.ImagePlus" [size]="32" class="text-subtle" />
            <span class="mt-3 font-medium">Add photos of your venue</span>
            <span class="mt-1 text-muted">Drop them here or choose from your phone or computer. Up to {{ max }}.</span>
            <input type="file" accept="image/*" multiple class="sr-only" (change)="picked($event)" />
          </label>
        }
        <ul class="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          @for (photo of list; track photo.id; let i = $index, last = $last) {
            <li class="overflow-hidden rounded-xl border border-border bg-surface">
              <div class="relative aspect-[4/3] bg-surface-2">
                <img [src]="photo.url" [alt]="photo.caption || 'Venue photo ' + (i + 1)" loading="lazy" class="size-full object-cover" />
                @if (photo.cover) {
                  <span class="absolute left-2 top-2 flex items-center gap-1 rounded-full bg-ink px-2.5 py-1 text-xs font-medium text-ink-fg">
                    <lucide-icon [img]="icons.Star" [size]="12" />Cover
                  </span>
                }
              </div>
              <div class="space-y-2 p-3">
                <label [for]="'caption-' + photo.id" class="sr-only">Caption</label>
                <input [id]="'caption-' + photo.id" type="text" maxlength="200" placeholder="Add a caption" [value]="photo.caption ?? ''"
                  (change)="caption(photo, $any($event.target).value)" (keydown.enter)="$any($event.target).blur()"
                  class="h-10 w-full rounded-lg border border-transparent bg-transparent px-2 text-sm hover:border-border focus:border-border" />
                <div class="flex items-center gap-1">
                  @if (!photo.cover) {
                    <button type="button" (click)="makeCover(photo)" [disabled]="busy()" class="h-10 rounded-lg px-2 text-sm font-medium hover:bg-surface-2 disabled:opacity-60">Make cover</button>
                  }
                  <span class="flex-1"></span>
                  <!-- The cover always shows first; the rest follow in the owner's order -->
                  @if (!photo.cover) {
                    <button type="button" (click)="move(i, -1)" [disabled]="i === 0 || list[i - 1].cover || busy()" aria-label="Move earlier"
                      class="grid size-10 place-items-center rounded-lg text-muted hover:bg-surface-2 disabled:opacity-30">
                      <lucide-icon [img]="icons.ChevronLeft" [size]="18" />
                    </button>
                    <button type="button" (click)="move(i, 1)" [disabled]="last || busy()" aria-label="Move later"
                      class="grid size-10 place-items-center rounded-lg text-muted hover:bg-surface-2 disabled:opacity-30">
                      <lucide-icon [img]="icons.ChevronRight" [size]="18" />
                    </button>
                  }
                  @if (confirming() === photo.id) {
                    <button type="button" (click)="remove(photo)" [disabled]="busy()" class="h-10 rounded-lg bg-danger px-3 text-sm font-medium text-surface disabled:opacity-60">Delete</button>
                    <button type="button" (click)="confirming.set(null)" class="h-10 rounded-lg px-2 text-sm font-medium hover:bg-surface-2">Keep</button>
                  } @else {
                    <button type="button" (click)="confirming.set(photo.id)" aria-label="Delete photo"
                      class="grid size-10 place-items-center rounded-lg text-muted hover:bg-surface-2 hover:text-danger">
                      <lucide-icon [img]="icons.Trash2" [size]="18" />
                    </button>
                  }
                </div>
              </div>
            </li>
          }
          @for (upload of uploads(); track upload.key) {
            <li class="overflow-hidden rounded-xl border border-border bg-surface">
              <div class="grid aspect-[4/3] place-items-center bg-surface-2 px-4 text-center">
                @if (upload.error) {
                  <p class="text-sm text-danger" role="alert">{{ upload.error }}</p>
                } @else {
                  <div class="w-full max-w-48" role="status">
                    <lucide-icon [img]="icons.LoaderCircle" [size]="24" class="mx-auto animate-spin text-subtle" />
                    <div class="mt-3 h-1.5 overflow-hidden rounded-full bg-border">
                      <div class="h-full rounded-full bg-ink transition-[width]" [style.width.%]="upload.progress"></div>
                    </div>
                    <p class="mt-2 text-sm text-muted">{{ upload.progress < 100 ? 'Uploading' : 'Finishing' }}…</p>
                  </div>
                }
              </div>
              <div class="flex items-center gap-2 p-3">
                <p class="min-w-0 flex-1 truncate text-sm text-muted">{{ upload.name }}</p>
                @if (upload.error) {
                  <button type="button" (click)="dismiss(upload)" class="h-10 rounded-lg px-2 text-sm font-medium hover:bg-surface-2">Dismiss</button>
                }
              </div>
            </li>
          }
        </ul>
      } @else {
        <div class="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3" aria-label="Loading">
          @for (n of [1, 2, 3]; track n) {
            <div class="aspect-[4/3] animate-pulse rounded-xl bg-surface"></div>
          }
        </div>
      }
    </div>
  `,
})
export class Photos {
  private readonly http = inject(HttpClient);

  private readonly venues = inject(VenueContext);

  protected readonly icons = { ChevronLeft, ChevronRight, ImagePlus, LoaderCircle, Star, Trash2 };

  protected readonly max = MAX_PHOTOS;

  protected readonly photos = signal<VenuePhoto[] | null>(null);

  protected readonly uploads = signal<Upload[]>([]);

  protected readonly error = signal<string | null>(null);

  protected readonly notice = signal<string | null>(null);

  protected readonly busy = signal(false);

  protected readonly confirming = signal<string | null>(null);

  /** Photos that can still be added, counting ones on their way up. */
  protected readonly room = computed(() => MAX_PHOTOS - (this.photos()?.length ?? 0) - this.uploads().filter((u) => !u.error).length);

  private readonly venueId = computed(() => this.venues.current()?.venue.id ?? null);

  private nextKey = 0;

  constructor() {
    effect(() => {
      const id = this.venueId();
      if (id) {
        untracked(() => void this.load(id));
      }
    });
  }

  protected picked(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.add([...(input.files ?? [])]);
    input.value = '';
  }

  protected dropped(event: DragEvent): void {
    event.preventDefault();
    this.add([...(event.dataTransfer?.files ?? [])].filter((file) => file.type.startsWith('image/')));
  }

  protected dismiss(upload: Upload): void {
    this.uploads.update((list) => list.filter((u) => u.key !== upload.key));
  }

  protected async caption(photo: VenuePhoto, value: string): Promise<void> {
    if (value.trim() === (photo.caption ?? '')) {
      return;
    }
    await this.update(photo, { caption: value.trim() });
  }

  protected makeCover(photo: VenuePhoto): void {
    void this.update(photo, { cover: true });
  }

  /** Moves a photo one place earlier or later, renumbering only the photos whose place changed. */
  protected async move(index: number, step: -1 | 1): Promise<void> {
    const list = [...(this.photos() ?? [])];
    const target = index + step;
    if (target < 0 || target >= list.length || this.busy()) {
      return;
    }
    [list[index], list[target]] = [list[target], list[index]];
    this.photos.set(list);
    const changed = list.map((photo, order) => ({ photo, order })).filter(({ photo, order }) => photo.sortOrder !== order);
    this.busy.set(true);
    this.notice.set(null);
    try {
      let latest: VenuePhoto[] | null = null;
      for (const { photo, order } of changed) {
        latest = await firstValueFrom(this.http.put<VenuePhoto[]>(this.url(photo.id), { sortOrder: order }));
      }
      if (latest) {
        this.photos.set(latest);
      }
    }
    catch (error) {
      this.notice.set(problemOf(error).message);
      await this.load(this.venueId()!);
    }
    finally {
      this.busy.set(false);
    }
  }

  protected async remove(photo: VenuePhoto): Promise<void> {
    this.busy.set(true);
    this.notice.set(null);
    try {
      this.photos.set(await firstValueFrom(this.http.delete<VenuePhoto[]>(this.url(photo.id))));
      this.confirming.set(null);
    }
    catch (error) {
      this.notice.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  protected retry(): void {
    const id = this.venueId();
    if (id) {
      void this.load(id);
    }
  }

  private add(files: File[]): void {
    this.notice.set(null);
    const room = this.room();
    if (files.length > room) {
      this.notice.set(room === 0 ? `You already have ${MAX_PHOTOS} photos. Delete one to add another.`
        : `Only ${room} more ${room === 1 ? 'photo fits' : 'photos fit'}, so the first ${room} were added.`);
    }
    for (const file of files.slice(0, Math.max(0, room))) {
      void this.upload(file);
    }
  }

  private async upload(file: File): Promise<void> {
    const venueId = this.venueId();
    if (!venueId) {
      return;
    }
    const key = this.nextKey++;
    const patch = (change: Partial<Upload>) => this.uploads.update((list) => list.map((u) => (u.key === key ? { ...u, ...change } : u)));
    this.uploads.update((list) => [...list, { key, name: file.name, progress: 0, error: null }]);
    try {
      const blob = await shrink(file);
      const form = new FormData();
      form.append('file', blob, blob === file ? file.name : `${file.name.replace(/\.[^.]+$/, '')}.jpg`);
      await lastValueFrom(this.http.post<VenuePhoto>(`/api/v1/owner/venues/${venueId}/photos`, form, { reportProgress: true, observe: 'events' })
        .pipe(tap((event) => {
          if (event.type === HttpEventType.UploadProgress && event.total) {
            patch({ progress: Math.round((event.loaded / event.total) * 100) });
          }
        })));
      this.dismiss({ key, name: file.name, progress: 100, error: null });
      // The list decides the cover and order, so take it from the API
      this.photos.set(await firstValueFrom(this.http.get<VenuePhoto[]>(`/api/v1/owner/venues/${venueId}/photos`)));
    }
    catch (error) {
      patch({ error: error instanceof Error && !('status' in error) ? error.message : problemOf(error).message });
    }
  }

  private async update(photo: VenuePhoto, change: { caption?: string; cover?: boolean }): Promise<void> {
    this.busy.set(true);
    this.notice.set(null);
    try {
      this.photos.set(await firstValueFrom(this.http.put<VenuePhoto[]>(this.url(photo.id), change)));
    }
    catch (error) {
      this.notice.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private url(photoId: string): string {
    return `/api/v1/owner/venues/${this.venueId()}/photos/${photoId}`;
  }

  private async load(id: string): Promise<void> {
    try {
      this.photos.set(await firstValueFrom(this.http.get<VenuePhoto[]>(`/api/v1/owner/venues/${id}/photos`)));
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
