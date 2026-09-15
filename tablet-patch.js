// tablet-patch.js - Forces widescreen tablet layout scaling for TVs (Excluding Facebook)
console.log("TV Browser: tablet-patch.js loaded.");

(function() {
    const host = window.location.hostname;

    // Skip widescreen forcing on Facebook and Messenger so they render clean mobile inputs
    if (host.includes('facebook.com')) {
        console.log("TV Enforcer: Skipping tablet patch for Facebook to preserve mobile input compatibility.");
        return;
    }

    // 1. Force a widescreen tablet viewport width (1280px) for other sites
    let meta = document.querySelector('meta[name="viewport"]');
    if (!meta) {
        meta = document.createElement('meta');
        meta.name = 'viewport';
        document.head.appendChild(meta);
    }
    
    meta.content = 'width=1280, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes';

    // 2. Inject widescreen layout adjustments
    if (!document.getElementById('tv-tablet-styles')) {
        const style = document.createElement('style');
        style.id = 'tv-tablet-styles';
        style.innerHTML = `
            body, html {
                width: 100% !important;
                max-width: 100% !important;
                overflow-x: hidden !important;
            }
            [role="main"], .mobile-container, #page-container {
                max-width: 100% !important;
                width: 100% !important;
            }
        `;
        document.head.appendChild(style);
    }

    console.log("TV Enforcer: Widescreen tablet viewport applied.");
})();
