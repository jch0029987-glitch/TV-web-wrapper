// core-patch.js - Fluid Free-Roaming TV Mouse & Scroll Engine
console.log("TV Browser: core-patch.js loaded.");

(function() {
    // 1. Visual Activation Badge
    if (!document.getElementById('tv-extension-badge')) {
        const badge = document.createElement('div');
        badge.id = 'tv-extension-badge';
        badge.innerText = '✨ Free-Movement Cursor Active';
        badge.style.position = 'fixed';
        badge.style.bottom = '10px';
        badge.style.right = '10px';
        badge.style.backgroundColor = 'rgba(0, 0, 0, 0.85)';
        badge.style.color = '#00E676';
        badge.style.padding = '6px 12px';
        badge.style.borderRadius = '6px';
        badge.style.fontSize = '12px';
        badge.style.zIndex = '999999';
        badge.style.pointerEvents = 'none';
        badge.style.fontFamily = 'sans-serif';
        badge.style.border = '1px solid #00E676';
        document.documentElement.appendChild(badge);

        setTimeout(() => {
            badge.style.transition = 'opacity 1s ease';
            badge.style.opacity = '0';
            setTimeout(() => badge.remove(), 1000);
        }, 4000);
    }

    // 2. Initialize Cursor Coordinates (Center Screen)
    if (window.tvCursorX === undefined) {
        window.tvCursorX = window.innerWidth / 2;
        window.tvCursorY = window.innerHeight / 2;
    }

    const cursor = document.getElementById('tv-mouse-cursor');
    if (cursor) {
        cursor.style.left = window.tvCursorX + 'px';
        cursor.style.top = window.tvCursorY + 'px';
    }

    // 3. Fluid Free-Movement & Scrolling Engine
    window.tvScrollBy = function(dx, dy) {
        if (!cursor) {
            window.scrollBy(dx, dy);
            return;
        }

        // Update cursor position smoothly in any direction (allows complete backwards/free movement)
        window.tvCursorX = Math.max(10, Math.min(window.innerWidth - 10, window.tvCursorX + dx));
        window.tvCursorY = Math.max(10, Math.min(window.innerHeight - 10, window.tvCursorY + dy));
        
        cursor.style.left = window.tvCursorX + 'px';
        cursor.style.top = window.tvCursorY + 'px';

        // If the cursor is pushed against the outer screen edges, scroll the page naturally
        const edgeBuffer = 20;
        if (window.tvCursorX <= edgeBuffer || window.tvCursorX >= window.innerWidth - edgeBuffer ||
            window.tvCursorY <= edgeBuffer || window.tvCursorY >= window.innerHeight - edgeBuffer) {
            window.scrollBy(dx * 1.5, dy * 1.5);
        }
    };

    // 4. Cursor Click Dispatcher
    window.clickCursor = function() {
        if (!cursor) return;
        
        cursor.style.backgroundColor = '#FF5252';
        setTimeout(() => cursor.style.backgroundColor = 'rgba(0, 230, 118, 0.85)', 150);

        const target = document.elementFromPoint(window.tvCursorX, window.tvCursorY);
        if (target) {
            const opts = { bubbles: true, cancelable: true, clientX: window.tvCursorX, clientY: window.tvCursorY };
            target.dispatchEvent(new MouseEvent('mouseover', opts));
            target.dispatchEvent(new MouseEvent('mousedown', opts));
            target.dispatchEvent(new MouseEvent('mouseup', opts));
            target.dispatchEvent(new MouseEvent('click', opts));
            
            if (typeof target.focus === 'function') {
                target.focus();
            }
        }
    };

    console.log("TV Browser: Free-movement engine registered.");
})();
