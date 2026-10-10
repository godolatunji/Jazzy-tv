# Test fixtures

All fixtures are hand-written and synthetic. They are shaped like each site's API or markup
(copied from TorrentSearch's tests, or modelled on the row markup its selectors target) but are
not captured responses, and they only list Blender Foundation open movies (Big Buck Bunny,
Sintel, Tears of Steel, CC-BY) or invented titles. Don't add real copyrighted listings.

- `*.json`: hand-written in each API's shape (apibay, YTS, torrentapi, TorrentLeech).
- `paginated-*.html`, `detail-page.html`: generic pages for the base provider tests.
- `kickasstorrents-*.html`, `magnetdl-search.html`: from TorrentSearch; they keep the real sites'
  noise (header rows, sidebars, a decoy magnet, a commented-out template row) so selectors are
  tested against it.
- `1337x-search.html`, `torrent9-search.html`, `yggtorrent-search.html`: replace TorrentSearch's
  real 1080p snapshots with two synthetic rows each in the same markup. Pagination links point at
  `example.invalid`, so tests only parse page one.
- `limetorrents-*.html`, `torrentproject-*.html`, `eztv-search.html`: new, modelled on the
  selectors in the provider definitions.
