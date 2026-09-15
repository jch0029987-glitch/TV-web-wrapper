(function() {
    if (!window.location.hostname.includes('facebook.com') && !window.location.hostname.includes('messenger.com')) return;
    if (window.__fbMobileSessionInjected) return;
    window.__fbMobileSessionInjected = true;

    // Use a high-compatibility Mobile Chrome UA
    const targetUA = 'Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36';
    const targetPlatform = 'Linux armv8l';

    function overrideProp(obj, prop, value) {
        try {
            Object.defineProperty(obj, prop, {
                get: () => value,
                configurable: true,
                enumerable: true
            });
        } catch (e) {}
    }

    overrideProp(navigator, 'userAgent', targetUA);
    overrideProp(navigator, 'appVersion', targetUA.replace('Mozilla/', ''));
    overrideProp(navigator, 'platform', targetPlatform);
    overrideProp(navigator, 'vendor', 'Google Inc.');
    overrideProp(navigator, 'maxTouchPoints', 5);
    overrideProp(navigator, 'webdriver', false);

    // Enforce mobile viewport scaling
    function enforceMobileViewport() {
        let viewport = document.querySelector('meta[name="viewport"]');
        if (!viewport) {
            viewport = document.createElement('meta');
            viewport.name = 'viewport';
            document.head.appendChild(viewport);
        }
        viewport.content = 'width=device-width, initial-scale=1.0, maximum-scale=2.0, user-scalable=yes';
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', enforceMobileViewport);
    } else {
        enforceMobileViewport();
    }

    // Force-unhide checkpoint elements and scale layout to reveal hidden alternate options
    const styleFixes = document.createElement('style');
    styleFixes.innerHTML = `
        div[id*="checkpoint"], div[class*="checkpoint"], form {
            max-height: none !important;
            overflow: visible !important;
        }
        a[role="link"], button {
            visibility: visible !important;
            opacity: 1 !important;
        }
        body {
            zoom: 0.85;
        }
    `;
    if (document.head) {
        document.head.appendChild(styleFixes);
    } else {
        document.addEventListener('DOMContentLoaded', () => document.head.appendChild(styleFixes));
    }

    // Neutralize history-based loops
    try {
        const preventLoop = (fnName) => {
            const original = history[fnName];
            history[fnName] = function(...args) {
                return;
            };
        };
        preventLoop('replaceState');
        preventLoop('pushState');
    } catch (e) {}

    // Neutralize programmatic page reloads
    try {
        Object.defineProperty(window.location, 'reload', {
            value: function() { return; },
            writable: false,
            configurable: true
        });
    } catch (e) {}

    console.log("Facebook Checkpoint Unblocker Initialized.");
})();
