// core-patch.js - Global TV Cursor and Smooth Scroll Engine
console.log("TV Browser: core-patch.js loaded.");

(function() {
    // 1. Create Cursor Element if it doesn't exist
    if (!document.getElementById('tv-mouse-cursor')) {
        const cursor = document.createElement('div');
        cursor.id = 'tv-mouse-cursor';
        cursor.style.position = 'fixed';
        cursor.style.left = '50%';
        cursor.style.top = '50%';
        cursor.style.width = '20px';
        cursor.style.height = '20px';
        cursor.style.backgroundColor = 'rgba(0, 230, 118, 0.9)';
        cursor.style.border = '2px solid white';
        cursor.style.borderRadius = '50%';
        cursor.style.pointerEvents = 'none';
        cursor.style.zIndex = '999999';
        cursor.style.display = 'none';
        cursor.style.transform = 'translate(-50%, -50%)';
        cursor.style.boxShadow = '0 0 10px rgba(0,0,0,0.5)';
        
        if (document.body) {
            document.body.appendChild(cursor);
        } else {
            document.addEventListener('DOMContentLoaded', () => {
                document.documentElement.appendChild(cursor);
            });
        }
    }

    // 2. Global Visibility Controller (called by MainActivity)
    window.setCursorVisible = function(visible) {
        const cursor = document.getElementById('tv-mouse-cursor');
        if (cursor) {
            cursor.style.display = visible ? 'block' : 'none';
        }
    };

    // 3. Click Simulator (called by MainActivity on D-Pad Center)
    window.clickCursor = function() {
        const cursor = document.getElementById('tv-mouse-cursor');
        if (cursor) {
            cursor.style.backgroundColor = '#ff5252';
            setTimeout(() => cursor.style.backgroundColor = 'rgba(0, 230, 118, 0.9)', 150);
        }
        
        const x = window.innerWidth / 2;
        const y = window.innerHeight / 2;
        const target = document.elementFromPoint(x, y);
        
        if (target) {
            const opts = { bubbles: true, cancelable: true, clientX: x, clientY: y };
            target.dispatchEvent(new MouseEvent('mouseover', opts));
            target.dispatchEvent(new MouseEvent('mousedown', opts));
            target.dispatchEvent(new MouseEvent('mouseup', opts));
            target.dispatchEvent(new MouseEvent('click', opts));
            if (typeof target.focus === 'function') target.focus();
        }
    };

    // 4. Smooth Scroll Engine (called by MainActivity on D-Pad arrows)
    window.tvScrollBy = function(dx, dy) {
        window.scrollBy({
            left: dx,
            top: dy,
            behavior: 'auto' // Use 'auto' or 'smooth' for D-pad navigation
        });
    };
})();
