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

    // Neutralize Facebook's aggressive SPA refresh/redirect loops
    try {
        const preventLoop = (fnName) => {
            const original = history[fnName];
            history[fnName] = function(...args) {
                console.log(`Blocked history.${fnName} reload loop:`, args[2]);
                return;
            };
        };
        preventLoop('replaceState');
        preventLoop('pushState');
    } catch (e) {}

    console.log("Facebook Mobile-Auth Anti-Loop Mode Initialized.");
})();
