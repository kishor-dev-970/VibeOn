(function () {
  if (window.__braveliteAdSkip) return;
  window.__braveliteAdSkip = true;

  // Spoof Page Visibility so YouTube's own scripts never pause playback
  // when the app is backgrounded or the screen is turned off.
  try {
    ['hidden', 'webkitHidden'].forEach(function (k) {
      Object.defineProperty(document, k, { get: function () { return false; }, configurable: true });
      Object.defineProperty(Document.prototype, k, { get: function () { return false; }, configurable: true });
    });
    ['visibilityState', 'webkitVisibilityState'].forEach(function (k) {
      Object.defineProperty(document, k, { get: function () { return 'visible'; }, configurable: true });
      Object.defineProperty(Document.prototype, k, { get: function () { return 'visible'; }, configurable: true });
    });
    document.hasFocus = function () { return true; };
  } catch (e) {}

  // rAF shim: real animation frames stop firing when the page is genuinely
  // hidden; YouTube uses that stall as a background signal. A timer-backed
  // fallback keeps frames "arriving" (~4fps worst case under throttling).
  try {
    if (!window.__braveliteRafShim) {
      window.__braveliteRafShim = true;
      var __origRaf = window.requestAnimationFrame.bind(window);
      window.requestAnimationFrame = function (cb) {
        var fired = false;
        function run(t) {
          if (fired) return;
          fired = true;
          try { cb(t); } catch (e) {}
        }
        try { __origRaf(run); } catch (e) {}
        setTimeout(run, 250);
        return 0;
      };
    }
  } catch (e) {}

  // Block future visibility/blur pause handlers from binding; existing ones
  // already see the spoofed state above.
  try {
    var BLOCKED_EVENTS = { visibilitychange: 1, webkitvisibilitychange: 1, blur: 1 };
    var origAddEventListener = EventTarget.prototype.addEventListener;
    EventTarget.prototype.addEventListener = function (type, fn, opts) {
      if (BLOCKED_EVENTS[String(type).toLowerCase()]) return;
      return origAddEventListener.call(this, type, fn, opts);
    };
  } catch (e) {}

  var BL_BLOCKED = typeof window.__braveliteBlocked === 'number' ? window.__braveliteBlocked : null;

  var tick = 0;
  var savedMuted = null;

  var OPEN_APP_SEL =
    'ytm-open-app-banner-renderer,' +
    'ytd-open-app-banner-renderer,' +
    'ytm-smart-app-banner,' +
    '#open-app-banner,' +
    '.open-app-banner-renderer';

  var SKIP_SEL =
    '.ytp-skip-ad-button, ' +
    '.ytp-ad-skip-button, ' +
    '.ytp-ad-skip-button-modern, ' +
    '.videoAdUiSkipButton, ' +
    'button[aria-label^="Skip ad"], ' +
    'button[aria-label^="Skip Ads"]';

  // ---- Compact chrome (top bar, bottom nav, chips) ----
  // Applied as inline !important styles so they always win over YouTube's own
  // (later-loaded) stylesheets. Re-applied on a timer to survive re-renders.
  var COMPACT_TOP_SEL =
    'ytd-masthead,' +
    'ytm-masthead,' +
    'tp-yt-app-header,' +
    '#masthead,' +
    '.ytd-masthead,' +
    'ytd-searchbox';
  var COMPACT_BOTTOM_SEL =
    'ytd-mini-guide-renderer,' +
    'ytd-guide-renderer,' +
    'ytm-pivot-bar,' +
    'ytm-pivot-bar-renderer,' +
    'ytm-bottom-bar,' +
    'ytd-multi-page-menu,[role="navigation"],' +
    '#guide-skeleton';
  var COMPACT_CHIP_SEL =
    'ytd-chip-cloud-renderer,' +
    'ytm-chip-cloud-renderer,' +
    'ytm-feed-filter-chip-bar-renderer,' +
    'ytd-chip-cloud-chip-renderer,' +
    'ytm-chip-cloud-chip-renderer,' +
    '#filter-chip-bar,' +
    '#home-chips,' +
    '#chips';
  var COMPACT_ITEM_SEL =
    'ytm-pivot-bar-item-renderer,' +
    'ytd-mini-guide-entry-renderer,' +
    'ytd-chip-cloud-chip-renderer,' +
    'ytm-chip-cloud-chip-renderer';

  function setImportant(el, prop, val) {
    try {
      el.style.setProperty(prop, val, 'important');
    } catch (e) {}
  }

  function compactChrome() {
    if (!document.body) return;

    document.querySelectorAll(COMPACT_TOP_SEL).forEach(function (el) {
      var r = el.getBoundingClientRect();
      if (r.height && r.height > 60) return;
      setImportant(el, 'height', '40px');
      setImportant(el, 'min-height', '40px');
      setImportant(el, 'max-height', '40px');
    });

    // Slim icons/buttons inside the top bar.
    document.querySelectorAll(COMPACT_TOP_SEL + ' a, ' + COMPACT_TOP_SEL + ' yt-icon-button, ' + COMPACT_TOP_SEL + ' tp-yt-paper-icon-button, ' + COMPACT_TOP_SEL + ' #menu-icon').forEach(function (el) {
      setImportant(el, 'width', 'auto');
      setImportant(el, 'min-width', '30px');
      setImportant(el, 'height', '32px');
      setImportant(el, 'min-height', '32px');
      setImportant(el, 'padding', '0 8px');
    });
    document.querySelectorAll(COMPACT_TOP_SEL + ' yt-icon').forEach(function (el) {
      setImportant(el, 'width', '18px');
      setImportant(el, 'height', '18px');
      setImportant(el, 'min-width', '18px');
      setImportant(el, 'min-height', '18px');
    });

    // Bottom tab bar: only shrink bars sitting at the bottom edge.
    document.querySelectorAll(COMPACT_BOTTOM_SEL).forEach(function (el) {
      try {
        var r = el.getBoundingClientRect();
        var nearBottom = r.top >= window.innerHeight * 0.75 || el.classList.contains('middle');
        if (!nearBottom && r.bottom > 0 && r.top < window.innerHeight) return;
      } catch (e) {}
      setImportant(el, 'height', '42px');
      setImportant(el, 'min-height', '42px');
      setImportant(el, 'max-height', '42px');
      setImportant(el, 'padding-top', '0');
      setImportant(el, 'padding-bottom', '0');
      setImportant(el, 'overflow', 'hidden');
    });
    document.querySelectorAll(COMPACT_ITEM_SEL).forEach(function (el) {
      try {
        var r = el.getBoundingClientRect();
        if (r.bottom > window.innerHeight) return;
      } catch (e) {}
      setImportant(el, 'padding', '0');
      setImportant(el, 'min-height', '42px');
      setImportant(el, 'margin', '0');
    });

    // Chips row just under the header.
    document.querySelectorAll(COMPACT_CHIP_SEL).forEach(function (el) {
      var t = String(el.tagName);
      if (t.indexOf('CHIP-RENDERER') !== -1 || el.id === 'filter-chip-bar' || el.id === 'chips' || el.id === 'home-chips') {
        setImportant(el, 'height', '32px');
        setImportant(el, 'min-height', '32px');
        setImportant(el, 'max-height', '32px');
        setImportant(el, 'padding-top', '0');
        setImportant(el, 'padding-bottom', '0');
      } else {
        setImportant(el, 'height', '28px');
        setImportant(el, 'min-height', '28px');
        setImportant(el, 'max-height', '28px');
      }
    });
    document.querySelectorAll('ytm-feed-filter-chip-bar-renderer > div, ytd-rich-grid-renderer > #chip-bar').forEach(function (el) {
      setImportant(el, 'height', '32px');
      setImportant(el, 'min-height', '32px');
      setImportant(el, 'max-height', '32px');
      setImportant(el, 'padding-top', '0');
      setImportant(el, 'padding-bottom', '0');
    });
    document.querySelectorAll(COMPACT_CHIP_SEL + ' a, ' + COMPACT_CHIP_SEL + ' yt-button-shape, ' + COMPACT_CHIP_SEL + ' yt-formatted-string').forEach(function (el) {
      setImportant(el, 'font-size', '12px');
      setImportant(el, 'line-height', '24px');
      setImportant(el, 'padding-top', '0');
      setImportant(el, 'padding-bottom', '0');
      setImportant(el, 'min-height', '24px');
    });

    // Hide big empty padding on the home feed.
    document.querySelectorAll('ytd-rich-grid-renderer, ytm-rich-grid-renderer, #contents.ytd-rich-grid-renderer').forEach(function (el) {
      setImportant(el, 'padding-top', '0');
    });
  }

  setInterval(compactChrome, 300);

  function player() {
    return document.getElementById('movie_player');
  }

  function videoEl() {
    return document.querySelector('video.html5-main-video');
  }

  function hideEl(el) {
    el.style.display = 'none';
  }

  function killOpenAppBanner() {
    var found = document.querySelectorAll(OPEN_APP_SEL);
    for (var i = 0; i < found.length; i++) hideEl(found[i]);

    // Text sweep is throttled: every 4th tick (~1s).
    if (tick % 4 !== 0) return;
    var leaves = document.querySelectorAll('span, div, a, button');
    for (var j = 0; j < leaves.length; j++) {
      var el = leaves[j];
      if (el.childElementCount > 0) continue;
      if (el.style.display === 'none') continue;
      var t = (el.textContent || '').trim();
      if (t.length > 0 && t.length <= 20 && /open\s+app/i.test(t)) {
        var anc = el.closest(
          'ytm-open-app-banner-renderer, ytd-open-app-banner-renderer, header, div'
        );
        var target = el;
        if (anc) {
          var r = anc.getBoundingClientRect();
          if (r.height > 0 && r.height < 180 && r.top < 80) target = anc;
        }
        hideEl(target);
      }
    }
  }

  function patchBrandText() {
    if (!document.body) return;
    var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
    var n;
    while ((n = walker.nextNode())) {
      var v = n.nodeValue;
      if (!v) continue;
      if (/youtube,\s*a\s*google\s*company/i.test(v)) {
        n.nodeValue = v.replace(/youtube,\s*a\s*google\s*company/i, 'App developed by KK');
      }
    }
  }

  function patchSettingsCount() {
    if (BL_BLOCKED === null) return;
    if (!/^\/settings/.test(location.pathname)) return;
    var el = document.getElementById('bravelite-blocked-count');
    if (!el) {
      el = document.createElement('div');
      el.id = 'bravelite-blocked-count';
      el.style.cssText =
        'padding:12px 16px;font-size:14px;color:#f1f1f1;' +
        'background:#212121;border-bottom:1px solid #383838;';
      var host = document.querySelector('ytm-settings') || document.body;
      host.prepend(el);
    }
    el.textContent = 'Ads blocked: ' + BL_BLOCKED;
  }

  function killAd() {
    tick++;

    killOpenAppBanner();
    patchSettingsCount();
    if (tick % 8 === 0) patchBrandText();

    var p = player();
    var v = videoEl();
    var adShowing = !!(p &&
      (p.classList.contains('ad-showing') || p.classList.contains('ad-interrupting')));
    var adUi = document.querySelector('.ytp-ad-player-overlay, .ytp-ad-player-overlay-skip-or-preview');
    if (adUi) adUi.style.visibility = 'hidden';

    if (adShowing) {
      document
        .querySelectorAll('.ytp-ad-overlay-close-button, .ytp-ad-overlay-close-container')
        .forEach(function (b) {
          b.click();
        });

      var skip = document.querySelector(SKIP_SEL);
      if (skip) skip.click();

      if (v) {
        if (savedMuted === null) savedMuted = v.muted;
        v.muted = true;
        if (
          Number.isFinite(v.duration) &&
          v.duration > 0 &&
          Math.abs(v.currentTime - v.duration) > 0.5
        ) {
          try {
            v.currentTime = v.duration;
          } catch (e) {}
        }
      }
    } else if (v && savedMuted !== null) {
      v.muted = savedMuted;
      savedMuted = null;
    }
  }

  setInterval(killAd, 60);
})();
