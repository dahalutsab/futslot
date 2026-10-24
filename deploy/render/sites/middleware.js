import { rewrite } from '@vercel/functions';

// Every venue website (dhuku.futslot.dahalutsab.com.np/...) is the API on Render. Vercel doesn't tell Render which
// address was visited, so the venue's name goes at the start of the path: /site-host/dhuku/book. Photos and the
// sites' stylesheets are the same for every venue, so they pass as they are.
const API = 'https://futslot-api.onrender.com';

export default function middleware(request) {
  const url = new URL(request.url);
  const site = url.hostname.split('.')[0];
  const shared = url.pathname.startsWith('/media/') || url.pathname.startsWith('/site-assets/');
  const path = shared ? url.pathname : `/site-host/${site}${url.pathname}`;
  return rewrite(new URL(path + url.search, API));
}
