import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, ElementRef, inject, signal, untracked, viewChild } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Check, ExternalLink, Globe, LoaderCircle, LucideAngularModule, Monitor, Smartphone } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { SiteSettings, SiteTemplate } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';

const TEMPLATES: { value: SiteTemplate; label: string; detail: string; accent: string }[] = [
  { value: 'TURF', label: 'Turf', detail: 'Light and photo-led. Your cover photo up top, then slots, prices and how to find you.', accent: '#15803d' },
  { value: 'FLOODLIGHT', label: 'Floodlight', detail: 'Dark and bold, like a night match. Today’s free slots come first.', accent: '#c6f432' },
];

/** Colours that work as a site's accent on both templates. */
const SWATCHES = ['#15803d', '#c6f432', '#1d4ed8', '#dc2626', '#f59e0b', '#7c3aed', '#0891b2', '#111827'];

interface Draft {
  template: SiteTemplate;
  subdomain: string;
  customDomain: string;
  accentColor: string;
  headline: string;
}

/**
 * The venue's own website: pick a template and colour, choose its address (and optionally your own domain),
 * preview it, and publish. Everything on the site comes from FutSlot, so it stays up to date by itself.
 */
@Component({
  selector: 'fs-website',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-5xl px-4 py-4 lg:px-8 lg:py-6">
      <h1 class="text-3xl lg:text-4xl">Website</h1>
      <p class="mt-1 text-muted">Your venue's own website, filled in from FutSlot: free slots, prices, photos, hours and reviews stay up to date by themselves.</p>

      @if (error(); as message) {
        <p class="mt-6 rounded-md border border-border bg-surface px-6 py-8 text-center text-muted">{{ message }}</p>
      } @else if (settings(); as s) {
        <!-- Live or not -->
        <section class="mt-5 flex flex-wrap items-center gap-4 rounded-md border border-border bg-surface p-4">
          <lucide-icon [img]="icons.Globe" [size]="24" class="shrink-0 text-muted" />
          <div class="min-w-0 flex-1">
            @if (s.published) {
              <p class="font-semibold">Your website is live</p>
              <a [href]="liveUrl(s)" target="_blank" rel="noopener" class="inline-flex items-center gap-1 break-all text-sm font-medium underline underline-offset-2">
                {{ liveUrl(s) }}<lucide-icon [img]="icons.ExternalLink" [size]="14" />
              </a>
            } @else if (!s.live) {
              <p class="font-semibold">Not live yet</p>
              <p class="text-sm text-muted">You can set it up now; it can go live once FutSlot has approved your venue.</p>
            } @else {
              <p class="font-semibold">Not live yet</p>
              <p class="text-sm text-muted">{{ s.saved ? 'Publish when you’re happy with the preview.' : 'Choose a look and an address, save, then publish.' }}</p>
            }
          </div>
          @if (s.published) {
            <button type="button" (click)="publish(false)" [disabled]="busy()" class="h-11 rounded-md border border-border px-4 font-medium hover:bg-surface-2 disabled:opacity-60">Take offline</button>
          } @else {
            <button type="button" (click)="publish(true)" [disabled]="busy() || !s.live || !s.saved || dirty()"
              class="h-11 rounded-md bg-ink px-5 font-medium text-ink-fg disabled:opacity-50">Publish</button>
          }
        </section>
        @if (!s.published && s.live && s.saved && dirty()) {
          <p class="mt-2 text-sm text-muted">Save your changes before publishing.</p>
        }

        <div class="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
          <form class="space-y-5" (submit)="$event.preventDefault(); save()">
            <section class="rounded-md border border-border bg-surface p-4" aria-labelledby="look-title">
              <h2 id="look-title" class="font-semibold">Look</h2>
              <div class="mt-3 grid grid-cols-2 gap-3" role="radiogroup" aria-label="Template">
                @for (t of templates; track t.value) {
                  <button type="button" role="radio" [attr.aria-checked]="draft().template === t.value" (click)="pickTemplate(t.value)"
                    class="overflow-hidden rounded-md border-2 text-left" [class]="draft().template === t.value ? 'border-ink' : 'border-border hover:border-muted'">
                    <span class="relative block h-28 overflow-hidden bg-surface-2">
                      <iframe [src]="thumbnail(t.value)" title="" tabindex="-1" aria-hidden="true" loading="lazy"
                        class="pointer-events-none absolute left-0 top-0 h-[560px] w-[1280px] origin-top-left scale-[0.2] border-0 lg:scale-[0.17]"></iframe>
                    </span>
                    <span class="block p-2.5">
                      <span class="flex items-center gap-1.5 font-medium">
                        @if (draft().template === t.value) {
                          <lucide-icon [img]="icons.Check" [size]="16" />
                        }
                        {{ t.label }}
                      </span>
                      <span class="block text-xs text-muted">{{ t.detail }}</span>
                    </span>
                  </button>
                }
              </div>

              <fieldset class="mt-4">
                <legend class="mb-1.5 text-sm font-medium">Colour</legend>
                <div class="flex flex-wrap items-center gap-2">
                  @for (color of swatches; track color) {
                    <button type="button" (click)="patch({ accentColor: color })" [attr.aria-label]="'Colour ' + color" [attr.aria-pressed]="draft().accentColor === color"
                      class="grid size-10 place-items-center rounded-full border border-border ring-offset-2 ring-offset-surface" [class.ring-2]="draft().accentColor === color" [class.ring-ink]="draft().accentColor === color"
                      [style.background]="color">
                    </button>
                  }
                  <label class="flex h-10 cursor-pointer items-center gap-2 rounded-full border border-border px-3 text-sm">
                    <input type="color" [value]="draft().accentColor" (input)="patch({ accentColor: $any($event.target).value })" class="size-5 cursor-pointer border-0 bg-transparent p-0" />
                    Other
                  </label>
                </div>
              </fieldset>

              <div class="mt-4">
                <label for="site-headline" class="mb-1.5 block text-sm font-medium">Headline <span class="font-normal text-subtle">(optional)</span></label>
                <input id="site-headline" type="text" maxlength="120" [value]="draft().headline" (input)="patch({ headline: $any($event.target).value })"
                  placeholder="e.g. Floodlit futsal in the heart of Baneshwor" class="h-11 w-full rounded-md border border-border bg-surface px-3 text-base" />
                <p class="mt-1 text-xs text-subtle">Shown under your name. Without one, your venue description is used.</p>
              </div>
            </section>

            <section class="rounded-md border border-border bg-surface p-4" aria-labelledby="address-title">
              <h2 id="address-title" class="font-semibold">Address</h2>
              <label for="site-subdomain" class="mb-1.5 mt-3 block text-sm font-medium">Your FutSlot address</label>
              <div class="flex h-11 items-stretch overflow-hidden rounded-md border border-border focus-within:ring-2 focus-within:ring-ring">
                <span class="flex items-center bg-surface-2 px-3 text-sm text-muted">{{ scheme() }}</span>
                <input id="site-subdomain" type="text" maxlength="40" autocomplete="off" spellcheck="false" [value]="draft().subdomain"
                  (input)="typedSubdomain($any($event.target).value)" class="min-w-0 flex-1 bg-surface px-2 text-base outline-none" aria-describedby="subdomain-help" />
                <span class="flex items-center bg-surface-2 px-3 text-sm text-muted">{{ domainSuffix() }}</span>
              </div>
              <p id="subdomain-help" class="mt-1 text-xs" [class]="subdomainProblem() ? 'text-danger' : 'text-subtle'">
                {{ subdomainProblem() ?? 'Lowercase letters, digits and hyphens. Short and easy to say works best.' }}
              </p>

              <details class="mt-4 rounded-md border border-border" [open]="!!draft().customDomain">
                <summary class="flex h-11 cursor-pointer items-center px-3 text-sm font-medium">Use your own domain <span class="ml-1 font-normal text-subtle">(optional)</span></summary>
                <div class="border-t border-border p-3">
                  <label for="site-domain" class="mb-1.5 block text-sm font-medium">Domain</label>
                  <input id="site-domain" type="text" autocomplete="off" spellcheck="false" [value]="draft().customDomain" (input)="patch({ customDomain: $any($event.target).value })"
                    placeholder="e.g. dhukufutsal.com" class="h-11 w-full rounded-md border border-border bg-surface px-3 text-base" />
                  <p class="mt-2 text-sm text-muted">At the company you bought the domain from, add a <strong>CNAME</strong> record pointing to
                    <code class="rounded bg-surface-2 px-1">{{ s.dnsTarget }}</code>. Once it points here, your site works on it with HTTPS, and search engines are told it's your main address.</p>
                </div>
              </details>
            </section>

            @if (saveError(); as message) {
              <p class="rounded-md bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
            }
            <div class="flex items-center gap-3">
              <button type="submit" [disabled]="busy() || !dirty()" class="flex h-11 items-center gap-2 rounded-md bg-ink px-5 font-medium text-ink-fg disabled:opacity-50">
                @if (busy()) {
                  <lucide-icon [img]="icons.LoaderCircle" [size]="18" class="animate-spin" />
                }
                Save
              </button>
              @if (dirty()) {
                <span class="text-sm text-muted">Unsaved changes</span>
              } @else if (savedNote()) {
                <span class="text-sm text-confirmed-fg" role="status">Saved</span>
              }
            </div>
          </form>

          <!-- Preview -->
          <section class="flex min-h-[560px] flex-col overflow-hidden rounded-md border border-border bg-surface" aria-labelledby="preview-title">
            <div class="flex items-center gap-2 border-b border-border px-3 py-2">
              <h2 id="preview-title" class="flex-1 text-sm font-medium">Preview</h2>
              <div class="flex rounded-md bg-surface-2 p-0.5" role="radiogroup" aria-label="Preview size">
                <button type="button" role="radio" [attr.aria-checked]="device() === 'phone'" (click)="device.set('phone')" aria-label="Phone"
                  class="grid size-9 place-items-center rounded-md text-muted" [class.bg-surface]="device() === 'phone'" [class.text-text]="device() === 'phone'">
                  <lucide-icon [img]="icons.Smartphone" [size]="16" />
                </button>
                <button type="button" role="radio" [attr.aria-checked]="device() === 'desktop'" (click)="device.set('desktop')" aria-label="Desktop"
                  class="grid size-9 place-items-center rounded-md text-muted" [class.bg-surface]="device() === 'desktop'" [class.text-text]="device() === 'desktop'">
                  <lucide-icon [img]="icons.Monitor" [size]="16" />
                </button>
              </div>
              <a [href]="previewHref()" target="_blank" rel="noopener" class="grid size-9 place-items-center rounded-md text-muted hover:bg-surface-2" aria-label="Open preview in a new tab">
                <lucide-icon [img]="icons.ExternalLink" [size]="16" />
              </a>
            </div>
            @if (s.live) {
              <div class="relative flex-1 overflow-hidden bg-surface-2">
                @if (device() === 'phone') {
                  <iframe [src]="preview()" title="Website preview, phone size" class="mx-auto my-4 block h-[640px] w-[375px] max-w-full rounded-md border border-border bg-white"></iframe>
                } @else {
                  <!-- A 1280px-wide page, scaled down to fit the panel -->
                  <div #stage class="absolute inset-0 overflow-hidden">
                    <iframe [src]="preview()" title="Website preview, desktop size" class="absolute left-0 top-0 w-[1280px] origin-top-left border-0"
                      [style.transform]="'scale(' + scale() + ')'" [style.height.px]="stageHeight() / scale()"></iframe>
                  </div>
                }
              </div>
            } @else {
              <p class="m-auto max-w-xs px-6 text-center text-muted">The preview shows once FutSlot has approved your venue.</p>
            }
          </section>
        </div>
      } @else {
        <div class="mt-6 h-96 animate-pulse rounded-md bg-surface" aria-label="Loading"></div>
      }
    </div>
  `,
})
export class Website {
  private readonly http = inject(HttpClient);

  private readonly sanitizer = inject(DomSanitizer);

  private readonly venues = inject(VenueContext);

  protected readonly icons = { Check, ExternalLink, Globe, LoaderCircle, Monitor, Smartphone };

  protected readonly templates = TEMPLATES;

  protected readonly swatches = SWATCHES;

  protected readonly settings = signal<SiteSettings | null>(null);

  protected readonly draft = signal<Draft>({ template: 'TURF', subdomain: '', customDomain: '', accentColor: TEMPLATES[0].accent, headline: '' });

  protected readonly error = signal<string | null>(null);

  protected readonly saveError = signal<string | null>(null);

  protected readonly busy = signal(false);

  protected readonly savedNote = signal(false);

  protected readonly subdomainProblem = signal<string | null>(null);

  protected readonly device = signal<'phone' | 'desktop'>('desktop');

  private readonly stage = viewChild<ElementRef<HTMLElement>>('stage');

  /** How much the desktop preview is shrunk to fit its panel. */
  protected readonly scale = signal(0.4);

  protected readonly stageHeight = signal(560);

  /** The preview updates a moment after the last change, not on every keystroke. */
  private readonly previewDraft = signal<Draft | null>(null);

  protected readonly dirty = computed(() => {
    const s = this.settings();
    if (!s) {
      return false;
    }
    if (!s.saved) {
      return true;
    }
    const d = this.draft();
    return d.template !== s.template || d.subdomain.trim() !== s.subdomain || d.customDomain.trim() !== (s.customDomain ?? '')
      || d.accentColor !== (s.accentColor ?? defaultAccent(s.template)) || d.headline.trim() !== (s.headline ?? '');
  });

  /** "http://" and ".localhost:8080" around the subdomain, from the address FutSlot gave. */
  protected readonly scheme = computed(() => (this.settings()?.url ?? 'https://').replace(/^(https?:\/\/).*$/, '$1'));

  protected readonly domainSuffix = computed(() => {
    const s = this.settings();
    return s ? s.url.slice(s.url.indexOf(s.subdomain) + s.subdomain.length) : '.futslot.com';
  });

  protected readonly preview = computed<SafeResourceUrl>(() => this.sanitizer.bypassSecurityTrustResourceUrl(this.previewHref()));

  private readonly venueId = computed(() => this.venues.current()?.venue.id ?? null);

  private checkTimer: ReturnType<typeof setTimeout> | null = null;

  private previewTimer: ReturnType<typeof setTimeout> | null = null;

  private readonly thumbnails = new Map<SiteTemplate, SafeResourceUrl>();

  constructor() {
    effect(() => {
      const id = this.venueId();
      if (id) {
        untracked(() => void this.load(id));
      }
    });
    effect(() => {
      const draft = this.draft();
      untracked(() => {
        if (this.previewTimer) {
          clearTimeout(this.previewTimer);
        }
        this.previewTimer = setTimeout(() => this.previewDraft.set(draft), this.previewDraft() ? 500 : 0);
      });
    });
    // Keep the desktop preview fitted to its panel as the window changes
    const observer = new ResizeObserver(([entry]) => {
      this.scale.set(Math.max(0.2, entry.contentRect.width / 1280));
      this.stageHeight.set(entry.contentRect.height);
    });
    effect(() => {
      const element = this.stage()?.nativeElement;
      observer.disconnect();
      if (element) {
        observer.observe(element);
      }
    });
    inject(DestroyRef).onDestroy(() => {
      observer.disconnect();
      if (this.checkTimer) {
        clearTimeout(this.checkTimer);
      }
      if (this.previewTimer) {
        clearTimeout(this.previewTimer);
      }
    });
  }

  protected previewHref(): string {
    const s = this.settings();
    const d = this.previewDraft() ?? this.draft();
    if (!s) {
      return 'about:blank';
    }
    const params = new URLSearchParams({ template: d.template, accent: d.accentColor.replace('#', '') });
    if (d.headline.trim()) {
      params.set('headline', d.headline.trim());
    }
    return `${s.previewPath}?${params}`;
  }

  /** A small live view of each template, with this venue's details. */
  protected thumbnail(template: SiteTemplate): SafeResourceUrl {
    let url = this.thumbnails.get(template);
    if (!url) {
      const s = this.settings();
      const accent = TEMPLATES.find((t) => t.value === template)!.accent.replace('#', '');
      url = this.sanitizer.bypassSecurityTrustResourceUrl(s ? `${s.previewPath}?template=${template}&accent=${accent}` : 'about:blank');
      this.thumbnails.set(template, url);
    }
    return url;
  }

  protected liveUrl(s: SiteSettings): string {
    return s.customDomain ? `https://${s.customDomain}` : s.url;
  }

  protected patch(change: Partial<Draft>): void {
    this.savedNote.set(false);
    this.draft.update((d) => ({ ...d, ...change }));
  }

  /** Switching template also switches to its colour, unless the venue picked its own. */
  protected pickTemplate(template: SiteTemplate): void {
    const current = this.draft();
    const usingDefault = TEMPLATES.some((t) => t.accent === current.accentColor);
    this.patch({ template, accentColor: usingDefault ? defaultAccent(template) : current.accentColor });
  }

  protected typedSubdomain(value: string): void {
    const cleaned = value.toLowerCase().replace(/[^a-z0-9-]/g, '');
    this.patch({ subdomain: cleaned });
    this.subdomainProblem.set(null);
    if (this.checkTimer) {
      clearTimeout(this.checkTimer);
    }
    this.checkTimer = setTimeout(() => void this.check(cleaned), 400);
  }

  protected async save(): Promise<void> {
    const id = this.venueId();
    const d = this.draft();
    if (!id || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.saveError.set(null);
    try {
      const saved = await firstValueFrom(this.http.put<SiteSettings>(`/api/v1/owner/venues/${id}/site`, {
        template: d.template,
        subdomain: d.subdomain.trim(),
        customDomain: d.customDomain.trim() || null,
        accentColor: d.accentColor,
        headline: d.headline.trim() || null,
      }));
      this.apply(saved);
      this.savedNote.set(true);
    }
    catch (error) {
      this.saveError.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  protected async publish(published: boolean): Promise<void> {
    const id = this.venueId();
    if (!id || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.saveError.set(null);
    try {
      this.apply(await firstValueFrom(this.http.post<SiteSettings>(`/api/v1/owner/venues/${id}/site/${published ? 'publish' : 'unpublish'}`, null)));
    }
    catch (error) {
      this.saveError.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private async check(subdomain: string): Promise<void> {
    const id = this.venueId();
    if (!id || subdomain.length < 3 || subdomain === this.settings()?.subdomain) {
      this.subdomainProblem.set(subdomain.length > 0 && subdomain.length < 3 ? 'Use at least 3 characters.' : null);
      return;
    }
    try {
      const result = await firstValueFrom(this.http.get<{ available: boolean; problem: string | null }>(
        `/api/v1/owner/venues/${id}/site/subdomain-check`, { params: { subdomain } }));
      if (this.draft().subdomain === subdomain) {
        this.subdomainProblem.set(result.available ? null : result.problem);
      }
    }
    catch {
      // Saving checks it again
    }
  }

  private apply(settings: SiteSettings): void {
    this.settings.set(settings);
    this.draft.set({
      template: settings.template,
      subdomain: settings.subdomain,
      customDomain: settings.customDomain ?? '',
      accentColor: settings.accentColor ?? defaultAccent(settings.template),
      headline: settings.headline ?? '',
    });
  }

  private async load(id: string): Promise<void> {
    try {
      this.thumbnails.clear();
      this.apply(await firstValueFrom(this.http.get<SiteSettings>(`/api/v1/owner/venues/${id}/site`)));
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}

function defaultAccent(template: SiteTemplate): string {
  return TEMPLATES.find((t) => t.value === template)!.accent;
}
