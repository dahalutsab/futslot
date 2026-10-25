// The night edition of FutSlot's public pages (futslot.css, :root.dark): the reader's choice, kept under the same key
// as the player app's (player/src/app/core/theme.ts) so the front page and the app match, else the device's setting.
// Loaded in <head> without defer, so a dark page never flashes paper-white first.
(() => {
  const KEY = 'futslot.theme';
  const media = matchMedia('(prefers-color-scheme: dark)');
  let choice = null;
  try {
    choice = localStorage.getItem(KEY);
  } catch {}

  const apply = () => {
    const dark = choice === 'dark' || (choice !== 'light' && media.matches);
    document.documentElement.classList.toggle('dark', dark);
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', dark ? '#151413' : '#f3f2f2');
    document.querySelectorAll('.theme-switch').forEach((button) => button.setAttribute('aria-pressed', String(dark)));
  };

  media.addEventListener('change', apply);
  // Another tab, or the app on this address, changed it
  addEventListener('storage', (e) => {
    if (e.key === KEY) {
      choice = e.newValue;
      apply();
    }
  });
  document.addEventListener('DOMContentLoaded', apply);
  // The masthead's switch: the other edition from the one showing, remembered
  document.addEventListener('click', (e) => {
    if (!e.target.closest?.('.theme-switch')) return;
    choice = document.documentElement.classList.contains('dark') ? 'light' : 'dark';
    try {
      localStorage.setItem(KEY, choice);
    } catch {}
    apply();
  });
  apply();
})();
