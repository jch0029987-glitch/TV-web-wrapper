(function() {
    // Only run on Facebook domains
    if (!window.location.hostname.includes('facebook.com')) return;
    if (window.__fbDesktopInjected) return;
    window.__fbDesktopInjected = true;

    // Pool of realistic desktop User-Agent strings
    const desktopUAs = [
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Safari/605.1.15',
        'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
    ];

    // Pick a stable desktop user agent (Windows Chrome by default for maximum compatibility)
    const chosenUA = desktopUAs[0];
    const chosenPlatform = chosenUA.includes('Windows') ? 'Win32' : (chosenUA.includes('Macintosh') ? 'MacIntel' : 'Linux x86_64');

    // Robust property redefinition for desktop spoofing
    function spoofProperty(target, prop, value) {
        try {
            Object.defineProperty(target, prop, {
                get: () => value,
                configurable: true,
                enumerable: true
            });
        } catch (e) {
            // Fallback if property is non-configurable
        }
    }

    spoofProperty(navigator, 'userAgent', chosenUA);
    spoofProperty(navigator, 'appVersion', chosenUA.replace('Mozilla/', ''));
    spoofProperty(navigator, 'platform', chosenPlatform);
    spoofProperty(navigator, 'vendor', 'Google Inc.');
    spoofProperty(navigator, 'maxTouchPoints', 0); // Disables mobile touch-first behaviors
    spoofProperty(navigator, 'webdriver', false);

    // Force desktop viewport dimensions to prevent mobile scaling loops
    function enforceDesktopViewport() {
        let viewport = document.querySelector('meta[name="viewport"]');
        if (!viewport) {
            viewport = document.createElement('meta');
            viewport.name = 'viewport';
            document.head.appendChild(viewport);
        }
        viewport.content = 'width=1280, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes';
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', enforceDesktopViewport);
    } else {
        enforceDesktopViewport();
    }

    console.log("Facebook Desktop Mode Script Initialized.");
})();
