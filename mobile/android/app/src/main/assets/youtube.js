(function () {
  if (window.__braveliteAdSkip) return;
  window.__braveliteAdSkip = true;

  // Force high-bitrate audio in YouTube & YouTube Music
  try {
    if (window.localStorage) {
      localStorage.setItem('yt-player-bandwidth', '50000000'); // 50MB/s bandwidth estimate
      localStorage.setItem('ytmusic-audio-quality', '"HIGH"');
    }
    if (window.sessionStorage) {
      sessionStorage.setItem('yt-player-bandwidth', '50000000');
      sessionStorage.setItem('ytmusic-audio-quality', '"HIGH"');
    }
  } catch (e) {}

  try {
    if (navigator.connection) {
      Object.defineProperty(navigator.connection, 'downlink', { get: function () { return 100; }, configurable: true });
      Object.defineProperty(navigator.connection, 'effectiveType', { get: function () { return '4g'; }, configurable: true });
      Object.defineProperty(navigator.connection, 'rtt', { get: function () { return 10; }, configurable: true });
      Object.defineProperty(navigator.connection, 'saveData', { get: function () { return false; }, configurable: true });
    }
  } catch (e) {}

  // Intercept MediaSession action handlers registered by YouTube Music / YouTube
  try {
    window.__blMediaHandlers = window.__blMediaHandlers || {};
    if (navigator && navigator.mediaSession && navigator.mediaSession.setActionHandler) {
      var origSetActionHandler = navigator.mediaSession.setActionHandler.bind(navigator.mediaSession);
      navigator.mediaSession.setActionHandler = function (action, handler) {
        if (action && typeof handler === 'function') {
          window.__blMediaHandlers[String(action).toLowerCase()] = handler;
        }
        return origSetActionHandler(action, handler);
      };
    }
  } catch (e) {}

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
    Object.defineProperty(document, 'onvisibilitychange', { get: function () { return null; }, set: function () {}, configurable: true });
    Object.defineProperty(window, 'onblur', { get: function () { return null; }, set: function () {}, configurable: true });
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
    if (!document.body || location.hostname.includes('music.youtube.com')) return;

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
    return document.querySelector('video');
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

    if (tick % 6 === 0) dismissYTMusicPromos();

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

  // ---- YouTube & YT Music Pull-Down-to-Refresh Controller ----
  var refreshEl = null;
  var refreshSvg = null;
  var touchStartY = 0;
  var touchStartX = 0;
  var isPulling = false;
  var isRefreshing = false;
  var canPull = false;

  function initPullToRefresh() {
    if (document.getElementById('bl-pull-refresh')) return;
    refreshEl = document.createElement('div');
    refreshEl.id = 'bl-pull-refresh';
    refreshEl.style.cssText =
      'position:fixed;top:-50px;left:50%;transform:translateX(-50%);z-index:99999;' +
      'width:40px;height:40px;border-radius:20px;background:#212121;box-shadow:0 3px 10px rgba(0,0,0,0.5);' +
      'display:flex;align-items:center;justify-content:center;pointer-events:none;' +
      'transition:transform 0.1s ease-out, opacity 0.2s ease;' +
      'opacity:0;border:1px solid rgba(255,255,255,0.15);';

    refreshSvg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    refreshSvg.setAttribute('viewBox', '0 0 24 24');
    refreshSvg.setAttribute('width', '22');
    refreshSvg.setAttribute('height', '22');
    refreshSvg.style.cssText = 'fill:#ffffff;transform-origin:center;transition:transform 0.1s linear;';
    refreshSvg.innerHTML =
      '<path d="M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>';

    refreshEl.appendChild(refreshSvg);
    (document.body || document.documentElement).appendChild(refreshEl);
  }

  function getScrollTop() {
    var doc = document.documentElement;
    var body = document.body;
    var scrollY = window.pageYOffset || (doc ? doc.scrollTop : 0) || (body ? body.scrollTop : 0) || 0;
    var inner = document.querySelector('ytmusic-browse-response, ytm-app, #contents');
    var innerScroll = inner ? inner.scrollTop : 0;
    return Math.max(scrollY, innerScroll);
  }

  window.addEventListener('touchstart', function (e) {
    if (isRefreshing) return;
    var playerPage = document.querySelector('ytmusic-player-page');
    if (playerPage && playerPage.offsetHeight > 300) {
      canPull = false;
      return;
    }
    if (e.touches && e.touches.length === 1) {
      touchStartY = e.touches[0].clientY;
      touchStartX = e.touches[0].clientX;
      canPull = getScrollTop() <= 2;
      isPulling = false;
    }
  }, { passive: true });

  window.addEventListener('touchmove', function (e) {
    if (!canPull || isRefreshing) return;
    if (!refreshEl) initPullToRefresh();
    if (!e.touches || e.touches.length !== 1) return;

    var curY = e.touches[0].clientY;
    var curX = e.touches[0].clientX;
    var dy = curY - touchStartY;
    var dx = Math.abs(curX - touchStartX);

    if (dy > 12 && dy > dx * 1.4 && getScrollTop() <= 2) {
      isPulling = true;
      var pullDist = Math.min(dy * 0.42, 85);
      var progress = Math.min(pullDist / 60, 1.0);
      var rotation = (dy * 2.5) % 360;

      refreshEl.style.opacity = String(progress);
      refreshEl.style.transform = 'translate(-50%, ' + (pullDist + 15) + 'px)';
      refreshSvg.style.transform = 'rotate(' + rotation + 'deg)';
    } else if (dy < 0) {
      canPull = false;
      if (refreshEl && !isRefreshing) {
        refreshEl.style.opacity = '0';
        refreshEl.style.transform = 'translate(-50%, 0px)';
      }
    }
  }, { passive: true });

  window.addEventListener('touchend', function (e) {
    if (!isPulling || isRefreshing) return;
    isPulling = false;
    canPull = false;
    var curY = (e.changedTouches && e.changedTouches[0]) ? e.changedTouches[0].clientY : 0;
    var dy = curY - touchStartY;

    if (dy * 0.42 >= 55) {
      isRefreshing = true;
      if (refreshEl) {
        refreshEl.style.opacity = '1';
        refreshEl.style.transform = 'translate(-50%, 75px)';
        refreshSvg.style.animation = 'blSpin 0.7s linear infinite';
      }
      setTimeout(function () {
        location.reload();
      }, 350);
    } else {
      if (refreshEl) {
        refreshEl.style.opacity = '0';
        refreshEl.style.transform = 'translate(-50%, 0px)';
      }
    }
  }, { passive: true });

  try {
    var kf = document.createElement('style');
    kf.textContent = '@keyframes blSpin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }';
    (document.head || document.documentElement).appendChild(kf);
  } catch (e) {}

  function dismissYTMusicPromos() {
    var dialog = document.querySelector('ytmusic-mealbar-promo-renderer, ytmusic-upsell-dialog-renderer, tp-yt-paper-dialog[opened]');
    if (!dialog) return;
    var btn = dialog.querySelector('button[aria-label*="Dismiss" i], button[aria-label*="No thanks" i], button[aria-label*="Close" i], .dismiss-button');
    if (btn && btn.offsetWidth > 0) {
      try { btn.click(); } catch(e) {}
    }
  }

  // =======================================================
  // Song / Video Toggle Management (like official YouTube Music)
  // =======================================================
  var currentAVMode = 'song';
  try {
    currentAVMode = localStorage.getItem('vibeon_av_mode') || 'song';
  } catch(e) {}

  function applyAVMode(mode) {
    currentAVMode = mode;
    try {
      localStorage.setItem('vibeon_av_mode', mode);
    } catch(e) {}

    var songBtn = document.getElementById('vibeon-btn-song');
    var videoBtn = document.getElementById('vibeon-btn-video');

    if (mode === 'video') {
      document.body.classList.add('vibeon-video-mode');
      document.body.classList.remove('vibeon-song-mode');

      if (songBtn) songBtn.classList.remove('active');
      if (videoBtn) videoBtn.classList.add('active');

      // Click native Video tab if available
      var vidTabs = document.querySelectorAll(
        '#song-video-toggle [role="tab"], #av-toggle [role="tab"]'
      );
      for (var i = 0; i < vidTabs.length; i++) {
        var tab = vidTabs[i];
        var text = (tab.textContent || tab.getAttribute('aria-label') || '').trim().toLowerCase();
        if (text.includes('video') && tab.getAttribute('aria-selected') === 'false') {
          try { tab.click(); } catch(e) {}
          break;
        }
      }

      // Restore playback quality to auto/highres
      var p = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
      if (p) {
        try {
          if (typeof p.setPlaybackQualityRange === 'function') p.setPlaybackQualityRange('auto', 'highres');
          else if (typeof p.setPlaybackQuality === 'function') p.setPlaybackQuality('auto');
        } catch(e) {}
      }
    } else {
      document.body.classList.add('vibeon-song-mode');
      document.body.classList.remove('vibeon-video-mode');

      if (songBtn) songBtn.classList.add('active');
      if (videoBtn) videoBtn.classList.remove('active');

      // Click native Song tab if available
      var songTabs = document.querySelectorAll(
        '#song-video-toggle [role="tab"], #av-toggle [role="tab"]'
      );
      for (var i = 0; i < songTabs.length; i++) {
        var tab = songTabs[i];
        var text = (tab.textContent || tab.getAttribute('aria-label') || '').trim().toLowerCase();
        if (text.includes('song') && tab.getAttribute('aria-selected') === 'false') {
          try { tab.click(); } catch(e) {}
          break;
        }
      }

      // Clamp video quality to tiny (144p) to save data while audio plays at maximum quality
      var p = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
      if (p) {
        try {
          if (typeof p.setPlaybackQualityRange === 'function') p.setPlaybackQualityRange('tiny', 'tiny');
          else if (typeof p.setPlaybackQuality === 'function') p.setPlaybackQuality('tiny');
        } catch(e) {}
      }
    }
  }

  function ensureAVToggle() {
    if (!location.hostname.includes('music.youtube.com')) return;

    var toggleContainer = document.getElementById('vibeon-av-toggle-container');

    // Only show the Song/Video capsule when the user is actively on the full player page
    var playerPage = document.querySelector('ytmusic-player-page');
    var isPlayerOpen = false;
    if (playerPage) {
      var uiState = playerPage.getAttribute('player-ui-state');
      if (uiState === 'FULLSCREEN' || uiState === 'PLAYER_PAGE_OPEN') {
        isPlayerOpen = true;
      } else {
        var rect = playerPage.getBoundingClientRect();
        if (rect.height > 300 && rect.top < 250) {
          isPlayerOpen = true;
        }
      }
    }
    if (!isPlayerOpen && (location.pathname || '').includes('/watch')) {
      isPlayerOpen = true;
    }

    if (!isPlayerOpen) {
      if (toggleContainer) toggleContainer.style.display = 'none';
      return;
    }

    if (currentAVMode === 'video' && !document.body.classList.contains('vibeon-video-mode')) {
      document.body.classList.add('vibeon-video-mode');
      document.body.classList.remove('vibeon-song-mode');
    } else if (currentAVMode === 'song' && !document.body.classList.contains('vibeon-song-mode')) {
      document.body.classList.add('vibeon-song-mode');
      document.body.classList.remove('vibeon-video-mode');
    }

    if (!toggleContainer) {
      toggleContainer = document.createElement('div');
      toggleContainer.id = 'vibeon-av-toggle-container';
      toggleContainer.innerHTML =
        '<div id="vibeon-av-toggle">' +
          '<button type="button" id="vibeon-btn-song" class="vibeon-av-btn' + (currentAVMode === 'song' ? ' active' : '') + '">' +
            '<svg viewBox="0 0 24 24"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>' +
            '<span>Song</span>' +
          '</button>' +
          '<button type="button" id="vibeon-btn-video" class="vibeon-av-btn' + (currentAVMode === 'video' ? ' active' : '') + '">' +
            '<svg viewBox="0 0 24 24"><path d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"/></svg>' +
            '<span>Video</span>' +
          '</button>' +
        '</div>';

      (document.body || document.documentElement).appendChild(toggleContainer);

      var songBtn = document.getElementById('vibeon-btn-song');
      var videoBtn = document.getElementById('vibeon-btn-video');

      if (songBtn) {
        songBtn.addEventListener('click', function(ev) {
          ev.preventDefault();
          ev.stopPropagation();
          applyAVMode('song');
        });
      }
      if (videoBtn) {
        videoBtn.addEventListener('click', function(ev) {
          ev.preventDefault();
          ev.stopPropagation();
          applyAVMode('video');
        });
      }
    } else {
      toggleContainer.style.display = 'flex';
      if (toggleContainer.parentElement !== document.body && document.body) {
        document.body.appendChild(toggleContainer);
      }
    }
  }

  setInterval(function () {
    killAd();
    if (tick % 6 === 0) ensureAVToggle();
  }, 60);
})();

