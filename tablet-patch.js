// tablet-patch.js - Forces widescreen tablet layout scaling for TVs
console.log("TV Browser: tablet-patch.js loaded.");

(function() {
    // 1. Force a widescreen tablet viewport width (1280px)
    let meta = document.querySelector('meta[name="viewport"]');
    if (!meta) {
        meta = document.createElement('meta');
        meta.name = 'viewport';
        document.head.appendChild(meta);
    }
    
    // Setting width to 1280 forces YouTube and Facebook to render their wide tablet/desktop grids
    meta.content = 'width=1280, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes';

    // 2. Inject widescreen layout adjustments
    if (!document.getElementById('tv-tablet-styles')) {
        const style = document.createElement('style');
        style.id = 'tv-tablet-styles';
        style.innerHTML = `
            /* Ensure containers expand to fill the TV screen width */
            body, html {
                width: 100% !important;
                max-width: 100% !important;
                overflow-x: hidden !important;
            }
            /* Widen main content feeds on mobile/tablet views */
            [role="main"], .mobile-container, #page-container {
                max-width: 100% !important;
                width: 100% !important;
            }
        `;
        document.head.appendChild(style);
    }

    console.log("TV Enforcer: Widescreen tablet viewport applied.");
})();
