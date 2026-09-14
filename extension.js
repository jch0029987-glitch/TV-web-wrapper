// extensions.js - Upgraded Mouse & Scroll Patch for TV-web-wrapper
console.log("TV Browser: extensions.js patch loaded.");

(function() {
    // 1. Ensure a visual test badge is present so you know the script fetched successfully
    if (!document.getElementById('tv-extension-badge')) {
        const badge = document.createElement('div');
        badge.id = 'tv-extension-badge';
        badge.innerText = '🔧 Mouse/Scroll Patch Active';
        badge.style.position = 'fixed';
        badge.style.bottom = '10px';
        badge.style.right = '10px';
        badge.style.backgroundColor = 'rgba(0, 0, 0, 0.85)';
        badge.style.color = '#00e676';
        badge.style.padding = '6px 12px';
        badge.style.borderRadius = '6px';
        badge.style.fontSize = '12px';
        badge.style.zIndex = '999999';
        badge.style.pointerEvents = 'none';
        badge.style.fontFamily = 'sans-serif';
        badge.style.border = '1px solid #00e676';
        document.documentElement.appendChild(badge);

        setTimeout(() => {
            badge.style.transition = 'opacity 1s ease';
            badge.style.opacity = '0';
            setTimeout(() => badge.remove(), 1000);
        }, 5000);
    }

    // 2. Robust Scroll Override: Fixes sites where window.scrollBy gets ignored
    window.tvScrollBy = function(dx, dy) {
        // Try standard window scroll first
        window.scrollBy(dx, dy);

        // Also look for focused or major scrollable containers in modern web apps
        const activeEl = document.activeElement;
        if (activeEl && activeEl !== document.body && activeEl !== document.documentElement) {
            activeEl.scrollBy({ top: dy, left: dx, behavior: 'smooth' });
        }

        // Fallback: scroll primary content containers if they exist
        const containers = document.querySelectorAll('main, [role="main"], .scrollable, overflow-y');
        containers.forEach(container => {
            if (container.scrollHeight > container.clientHeight) {
                container.scrollTop += dy;
                container.scrollLeft += dx;
            }
        });
    };

    // 3. Enhanced Cursor Feedback
    const cursor = document.getElementById('tv-mouse-cursor');
    if (cursor) {
        cursor.style.border = '2px solid #00e676';
        cursor.style.boxShadow = '0 0 10px #00e676';
    }
    
    console.log("TV Browser: Custom tvScrollBy override registered.");
})();
