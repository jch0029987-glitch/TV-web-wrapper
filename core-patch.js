// core-patch.js - Modular TV Navigation & Scroll Engine
console.log("TV Browser: core-patch.js loaded successfully via manifest.");

(function() {
    // 1. Visual Activation Badge
    if (!document.getElementById('tv-extension-badge')) {
        const badge = document.createElement('div');
        badge.id = 'tv-extension-badge';
        badge.innerText = '✨ Modular Extension Active';
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

    // 2. Track Free-Roaming Cursor Position State
    if (window.tvCursorX === undefined) {
        window.tvCursorX = window.innerWidth / 2;
        window.tvCursorY = window.innerHeight / 2;
    }

    // Update cursor element position style dynamically
    const cursor = document.getElementById('tv-mouse-cursor');
    if (cursor) {
        cursor.style.left = window.tvCursorX + 'px';
        cursor.style.top = window.tvCursorY + 'px';
    }

    // 3. Robust TV Scroll & Cursor Movement Engine
    window.tvScrollBy = function(dx, dy) {
        if (!cursor) {
            window.scrollBy(dx, dy);
            return;
        }

        // Move the visual cursor across the screen first
        window.tvCursorX = Math.max(10, Math.min(window.innerWidth - 10, window.tvCursorX + dx));
        window.tvCursorY = Math.max(10, Math.min(window.innerHeight - 10, window.tvCursorY + dy));
        
        cursor.style.left = window.tvCursorX + 'px';
        cursor.style.top = window.tvCursorY + 'px';

        // If cursor hits screen boundary, scroll the underlying page content
        if (window.tvCursorX <= 15 || window.tvCursorX >= window.innerWidth - 15 ||
            window.tvCursorY <= 15 || window.tvCursorY >= window.innerHeight - 15) {
            window.scrollBy(dx * 2, dy * 2);
            
            // Also attempt to scroll active containers
            const activeEl = document.activeElement;
            if (activeEl && activeEl !== document.body) {
                activeEl.scrollBy({ top: dy * 2, left: dx * 2, behavior: 'smooth' });
            }
        }
    };

    // 4. Enhanced Click Dispatcher at Current Cursor Coordinates
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
            if (target.tagName === 'A' && target.href) {
                window.location.href = target.href;
            }
        }
    };

    console.log("TV Browser: core-patch.js movement engine registered.");
})();
