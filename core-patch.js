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

    // 3. Continuous Motion Engine
    function executeMotion(dx, dy) {
        cursorX = Math.max(0, Math.min(window.innerWidth - 12, cursorX + dx));
        cursorY = Math.max(0, Math.min(window.innerHeight - 12, cursorY + dy));
        
        cursor.style.left = `${cursorX}px`;
        cursor.style.top = `${cursorY}px`;

        const target = document.elementFromPoint(cursorX, cursorY);
        if (target) {
            cursor.style.backgroundColor = 'rgba(0, 255, 0, 0.9)'; // Green when hovering an interactive element
        } else {
            cursor.style.backgroundColor = 'rgba(255, 0, 0, 0.8)'; // Red otherwise
        }
    }

    window.tvStartMotion = function(dx, dy) {
        if (window.isCursorActive) {
            if (dx !== 0 && !motionIntervalX) {
                motionIntervalX = setInterval(() => executeMotion(dx, 0), 25);
            }
            if (dy !== 0 && !motionIntervalY) {
                motionIntervalY = setInterval(() => executeMotion(0, dy), 25);
            }
            return;
        }

        const activeEl = document.activeElement;
        const isTextField = activeEl && (
            activeEl.tagName === 'INPUT' || 
            activeEl.tagName === 'TEXTAREA' || 
            activeEl.isContentEditable
        );

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

    // 4. Click Simulation & Smart Input Focusing
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

            // Explicitly focus text fields or interactive controls when clicked by cursor
            if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable || typeof target.focus === 'function') {
                target.focus();
            }
        }
    };

    window.handleEnterPress = function() {
        if (window.isCursorActive) {
            window.clickCursor();
        }
    };

    // 5. Intelligent Keyboard Routing
    window.addEventListener('keydown', function(event) {
        const activeEl = document.activeElement;
        const isTextField = activeEl && (
            activeEl.tagName === 'INPUT' || 
            activeEl.tagName === 'TEXTAREA' || 
            activeEl.isContentEditable
        );

        // IF A WEB TEXT FIELD IS FOCUSED: Allow typing directly into the web field (do not intercept)
        if (isTextField) {
            return; 
        }

        // FOR EVERYTHING ELSE: Route keystrokes to the native app toolbar (`etUrlBar`)
        if (window.nativeBridge && typeof window.nativeBridge.onKeyboardInput === 'function') {
            if (event.key === 'Backspace') {
                window.nativeBridge.onKeyboardInput('', true);
                event.preventDefault();
            } else if (event.key.length === 1 && !event.ctrlKey && !event.altKey && !event.metaKey) {
                window.nativeBridge.onKeyboardInput(event.key, false);
                event.preventDefault();
            } else if (event.key === 'Enter') {
                if (typeof window.nativeBridge.submitToolbar === 'function') {
                    window.nativeBridge.submitToolbar();
                }
                event.preventDefault();
            }
        }
    }, true);

})();
