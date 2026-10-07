import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ChevronsUpDown, Ellipsis, LogOut, LucideAngularModule, Monitor, Moon, Search, Sun, X } from 'lucide-angular';

import { problemOf } from '../core/api-error';
import { Session } from '../core/session';
import { Theme, ThemeChoice } from '../core/theme';
import { VenueContext } from '../core/venue-context';
import { Badges } from './badges';
import { ALL_ITEMS, NAVIGATION, NavGroup, NavItem } from './navigation';

/**
 * The frame around every screen: a sidebar on wide screens, a top bar and bottom bar on phones. Only what the
 * user may do at the selected venue is shown.
 */
@Component({
  selector: 'fs-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.html',
})
export class Shell implements OnInit {
  protected readonly session = inject(Session);

  protected readonly venues = inject(VenueContext);

  protected readonly theme = inject(Theme);

  protected readonly badges = inject(Badges);

  private readonly router = inject(Router);

  protected readonly icons = { ChevronsUpDown, Ellipsis, LogOut, Monitor, Moon, Search, Sun, X };

  protected readonly loading = signal(true);

  protected readonly loadError = signal<string | null>(null);

  protected readonly moreOpen = signal(false);

  protected readonly groups = computed<NavGroup[]>(() =>
    NAVIGATION.map((group) => ({ ...group, items: group.items.filter((item) => this.allowed(item)) })).filter(
      (group) => group.items.length > 0,
    ),
  );

  protected readonly phoneBar = computed(() => ALL_ITEMS.filter((item) => item.onPhoneBar && this.allowed(item)));

  protected readonly moreItems = computed(() => ALL_ITEMS.filter((item) => !item.onPhoneBar && this.allowed(item)));

  protected readonly themes: { value: ThemeChoice; label: string }[] = [
    { value: 'light', label: 'Light' },
    { value: 'dark', label: 'Dark' },
    { value: 'system', label: 'Auto' },
  ];

  async ngOnInit(): Promise<void> {
    try {
      const venues = await this.venues.load();
      if (venues.length === 0) {
        await this.router.navigateByUrl('/no-venue');
      }
    }
    catch (error) {
      this.loadError.set(problemOf(error).message);
    }
    finally {
      this.loading.set(false);
    }
  }

  protected badge(item: NavItem): number {
    if (item.badge === 'payments') {
      return this.badges.payments();
    }
    return item.badge === 'messages' ? this.badges.messages() : 0;
  }

  protected switchVenue(event: Event): void {
    this.venues.select((event.target as HTMLSelectElement).value);
    void this.router.navigateByUrl('/schedule');
  }

  protected retry(): void {
    this.loading.set(true);
    this.loadError.set(null);
    void this.ngOnInit();
  }

  private allowed(item: NavItem): boolean {
    if (item.needs === null) {
      return true;
    }
    return item.needs === 'OWNER' ? this.venues.isOwner() : this.venues.can(item.needs);
  }
}
