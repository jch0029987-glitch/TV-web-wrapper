(function() {
    // Prevent multiple injection initializations
    if (window.__tvWrapperInjected) return;
    window.__tvWrapperInjected = true;

    window.isCursorActive = false;
    let cursorX = window.innerWidth / 2;
    let cursorY = window.innerHeight / 2;
    let motionIntervalX = null;
    let motionIntervalY = null;

    // 1. Create and style the Virtual Mouse Cursor Element
    const cursor = document.createElement('div');
    cursor.id = 'tv-virtual-cursor';
    cursor.style.position = 'fixed';
    cursor.style.left = `${cursorX}px`;
    cursor.style.top = `${cursorY}px`;
    cursor.style.width = '24px';
    cursor.style.height = '24px';
    cursor.style.backgroundColor = 'rgba(255, 0, 0, 0.8)';
    cursor.style.border = '2px solid white';
    cursor.style.borderRadius = '50%';
    cursor.style.zIndex = '999999';
    cursor.style.pointerEvents = 'none';
    cursor.style.transform = 'translate(-50%, -50%)';
    cursor.style.display = 'none'; // Hidden by default until toggled ON
    cursor.style.transition = 'background-color 0.1s ease';

    if (document.body) {
        document.body.appendChild(cursor);
    } else {
        document.addEventListener('DOMContentLoaded', () => {
            document.body.appendChild(cursor);
        });
    }

    // 2. Toggle Cursor Visibility from Kotlin
    window.setCursorVisible = function(visible) {
        window.isCursorActive = visible;
        cursor.style.display = visible ? 'block' : 'none';
    };

    // 3. Continuous Motion Engine (Bypasses text input locks when cursor mode is active)
    function executeMotion(dx, dy) {
        cursorX = Math.max(0, Math.min(window.innerWidth - 12, cursorX + dx));
        cursorY = Math.max(0, Math.min(window.innerHeight - 12, cursorY + dy));
        
        cursor.style.left = `${cursorX}px`;
        cursor.style.top = `${cursorY}px`;

        // Highlight element currently underneath the cursor for precise feedback
        const target = document.elementFromPoint(cursorX, cursorY);
        if (target) {
            cursor.style.backgroundColor = 'rgba(0, 255, 0, 0.9)'; // Green when hovering an interactive element
        } else {
            cursor.style.backgroundColor = 'rgba(255, 0, 0, 0.8)'; // Red otherwise
        }
    }

    window.tvStartMotion = function(dx, dy) {
        // If our virtual mouse cursor is active, always allow movement everywhere (even inside text inputs)
        if (window.isCursorActive) {
            if (dx !== 0 && !motionIntervalX) {
                motionIntervalX = setInterval(() => executeMotion(dx, 0), 25);
            }
            if (dy !== 0 && !motionIntervalY) {
                motionIntervalY = setInterval(() => executeMotion(0, dy), 25);
            }
            return;
        }

        // Otherwise, check if a text input is actively focused for standard web navigation
        const activeEl = document.activeElement;
        const isTextField = activeEl && (
            activeEl.tagName === 'INPUT' || 
            activeEl.tagName === 'TEXTAREA' || 
            activeEl.isContentEditable
        );

        // Only allow motion if not trapped inside a text field
        if (!isTextField) {
            if (dx !== 0 && !motionIntervalX) {
                motionIntervalX = setInterval(() => executeMotion(dx, 0), 25);
            }
            if (dy !== 0 && !motionIntervalY) {
                motionIntervalY = setInterval(() => executeMotion(0, dy), 25);
            }
        }
    };

    window.tvStopMotion = function(axis) {
        if (axis === 'x' && motionIntervalX) {
            clearInterval(motionIntervalX);
            motionIntervalX = null;
        }
        if (axis === 'y' && motionIntervalY) {
            clearInterval(motionIntervalY);
            motionIntervalY = null;
        }
    };

    // 4. Click Simulation at Cursor Position
    window.clickCursor = function() {
        if (!window.isCursorActive) return;

        cursor.style.backgroundColor = 'rgba(255, 255, 255, 1)';
        setTimeout(() => {
            cursor.style.backgroundColor = 'rgba(255, 0, 0, 0.8)';
        }, 150);

        const target = document.elementFromPoint(cursorX, cursorY);
        if (target) {
            ['mousedown', 'mouseup', 'click'].forEach(eventType => {
                const event = new MouseEvent(eventType, {
                    view: window,
                    bubbles: true,
                    cancelable: true,
                    clientX: cursorX,
                    clientY: cursorY,
                    button: 0
                });
                target.dispatchEvent(event);
            });

            // If it's a focusable input or button, give it direct focus
            if (typeof target.focus === 'function') {
                target.focus();
            }
        }
    };

    // Fallback enter press handler
    window.handleEnterPress = function() {
        if (window.isCursorActive) {
            window.clickCursor();
        }
    };

})();
