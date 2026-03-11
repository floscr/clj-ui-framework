// CSS Live Reload — polls /theme.css and hot-swaps the stylesheet on change.
// No full page reload needed. Included automatically in dev pages.
(function() {
  'use strict';
  var POLL_MS = 500;
  var link = document.querySelector('link[rel="stylesheet"][href*="theme"]');
  if (!link) return;

  var prevHash = 0;

  function djb2(str) {
    var h = 5381;
    for (var i = 0; i < str.length; i++) {
      h = ((h << 5) + h + str.charCodeAt(i)) | 0;
    }
    return h;
  }

  function check() {
    fetch('/theme.css', { cache: 'no-store' })
      .then(function(r) { return r.text(); })
      .then(function(css) {
        var h = djb2(css);
        if (prevHash && h !== prevHash) {
          // Clone link and swap — avoids FOUC
          var next = link.cloneNode();
          next.href = '/theme.css?t=' + Date.now();
          link.parentNode.insertBefore(next, link.nextSibling);
          next.onload = function() {
            link.parentNode.removeChild(link);
            link = next;
          };
          console.log('[css-live-reload] theme.css updated');
        }
        prevHash = h;
      })
      .catch(function() { /* ignore network errors */ });
  }

  check();
  setInterval(check, POLL_MS);
})();
