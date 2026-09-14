// extensions.js - Dynamic Remote Script for TV-web-wrapper
console.log("TV Browser: extensions.js loaded successfully from remote/cache.");

// Visual indicator to confirm the script executed live on your TV screen
(function() {
    if (!document.getElementById('tv-extension-badge')) {
        const badge = document.createElement('div');
        badge.id = 'tv-extension-badge';
        badge.innerText = '✨ Extensions Active';
        badge.style.position = 'fixed';
        badge.style.bottom = '10px';
        badge.style.right = '10px';
        badge.style.backgroundColor = 'rgba(0, 0, 0, 0.75)';
        badge.style.color = '#00e676';
        badge.style.padding = '6px 12px';
        badge.style.borderRadius = '6px';
        badge.style.fontSize = '12px';
        badge.style.zIndex = '999999';
        badge.style.pointerEvents = 'none';
        badge.style.fontFamily = 'sans-serif';
        badge.style.border = '1px solid #00e676';
        document.documentElement.appendChild(badge);

        // Fade out after 4 seconds so it doesn't stay permanently on screen
        setTimeout(() => {
            badge.style.transition = 'opacity 1s ease';
            badge.style.opacity = '0';
            setTimeout(() => badge.remove(), 1000);
        }, 4000);
    }
})();
