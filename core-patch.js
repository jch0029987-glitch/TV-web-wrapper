// core-patch.js - Global TV Cursor, Free-Roaming Mouse, and Edge-Scroll Engine
console.log("TV Browser: core-patch.js loaded.");

(function() {
    let cursorX = window.innerWidth / 2;
    let cursorY = window.innerHeight / 2;
    let cursorEl = null;

    // 1. Create Cursor Element if it doesn't exist
    function getOrCreateCursor() {
        if (cursorEl && document.body.contains(cursorEl)) return cursorEl;

        cursorEl = document.createElement('div');
        cursorEl.id = 'tv-mouse-cursor';
        cursorEl.style.position = 'fixed';
        cursorEl.style.left = cursorX + 'px';
        cursorEl.style.top = cursorY + 'px';
        cursorEl.style.width = '24px';
        cursorEl.style.height = '24px';
        cursorEl.style.backgroundColor = 'rgba(0, 230, 118, 0.9)';
        cursorEl.style.border = '2px solid white';
        cursorEl.style.borderRadius = '50%';
        cursorEl.style.pointerEvents = 'none';
        cursorEl.style.zIndex = '999999';
        cursorEl.style.display = 'none';
        cursorEl.style.transform = 'translate(-50%, -50%)';
        cursorEl.style.boxShadow = '0 0 10px rgba(0,0,0,0.5)';
        
        if (document.body) {
            document.body.appendChild(cursorEl);
        } else {
            document.addEventListener('DOMContentLoaded', () => {
                document.documentElement.appendChild(cursorEl);
            });
        }
        return cursorEl;
    }

    getOrCreateCursor();

    // 2. Global Visibility Controller (called by MainActivity)
    window.setCursorVisible = function(visible) {
        const cursor = getOrCreateCursor();
        if (cursor) {
            cursor.style.display = visible ? 'block' : 'none';
            if (visible) {
                cursorX = window.innerWidth / 2;
                cursorY = window.innerHeight / 2;
                updateCursorPosition();
            }
        }
    };

    function updateCursorPosition() {
        if (!cursorEl) return;
        // Absolute hard screen bounds fallback
        cursorX = Math.max(10, Math.min(window.innerWidth - 10, cursorX));
        cursorY = Math.max(10, Math.min(window.innerHeight - 10, cursorY));
        cursorEl.style.left = cursorX + 'px';
        cursorEl.style.top = cursorY + 'px';
    }

    // 3. Free-Roaming Cursor & Edge-Scrolling Engine
    window.tvScrollBy = function(dx, dy) {
        const cursor = getOrCreateCursor();
        if (cursor && cursor.style.display === 'block') {
            cursorX += dx;
            cursorY += dy;

            const edgeThreshold = 90; // Distance from edge in pixels to trigger page scrolling
            let pageScrollX = 0;
            let pageScrollY = 0;

            // Bottom edge scroll
            if (cursorY > window.innerHeight - edgeThreshold && dy > 0) {
                pageScrollY = dy * 1.5;
                cursorY = window.innerHeight - edgeThreshold;
            }
            // Top edge scroll
            else if (cursorY < edgeThreshold && dy < 0) {
                pageScrollY = dy * 1.5;
                cursorY = edgeThreshold;
            }

            // Right edge scroll
            if (cursorX > window.innerWidth - edgeThreshold && dx > 0) {
                pageScrollX = dx * 1.5;
                cursorX = window.innerWidth - edgeThreshold;
            }
            // Left edge scroll
            else if (cursorX < edgeThreshold && dx < 0) {
                pageScrollX = dx * 1.5;
                cursorX = edgeThreshold;
            }

            if (pageScrollX !== 0 || pageScrollY !== 0) {
                window.scrollBy({ left: pageScrollX, top: pageScrollY, behavior: 'auto' });
            }

            updateCursorPosition();
        } else {
            window.scrollBy({ left: dx, top: dy, behavior: 'auto' });
        }
    };

    // 4. Click Simulator at exact Cursor coordinates
    window.clickCursor = function() {
        const cursor = getOrCreateCursor();
        if (!cursor) return;

        cursor.style.backgroundColor = '#ff5252';
        setTimeout(() => cursor.style.backgroundColor = 'rgba(0, 230, 118, 0.9)', 150);
        
        cursor.style.display = 'none';
        const target = document.elementFromPoint(cursorX, cursorY);
        cursor.style.display = 'block';
        
        if (target) {
            const opts = { bubbles: true, cancelable: true, clientX: cursorX, clientY: cursorY, view: window };
            target.dispatchEvent(new MouseEvent('mouseover', opts));
            target.dispatchEvent(new MouseEvent('mousedown', opts));
            target.dispatchEvent(new MouseEvent('mouseup', opts));
            target.dispatchEvent(new MouseEvent('click', opts));
            if (typeof target.focus === 'function') target.focus();
        }
    };
})();
