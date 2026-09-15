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
    cursor.style.display = 'none';
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
            cursor.style.backgroundColor = 'rgba(0, 255, 0, 0.9)';
        } else {
            cursor.style.backgroundColor = 'rgba(255, 0, 0, 0.8)';
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
            activeEl.isContentEditable ||
            activeEl.getAttribute('role') === 'textbox'
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

    // 4. Click Simulation for Login Inputs
    window.clickCursor = function() {
        if (!window.isCursorActive) return;

        cursor.style.backgroundColor = 'rgba(255, 255, 255, 1)';
        setTimeout(() => {
            cursor.style.backgroundColor = 'rgba(255, 0, 0, 0.8)';
        }, 150);

        const target = document.elementFromPoint(cursorX, cursorY);
        if (target) {
            const inputTarget = target.matches('input, textarea, [contenteditable="true"], [role="textbox"]') 
                ? target 
                : target.querySelector('input, textarea, [contenteditable="true"], [role="textbox"]') 
                || target.closest('input, textarea, [contenteditable="true"], [role="textbox"]');

            const actualTarget = inputTarget || target;

            // Touch + Mouse event sequence to trigger React's event listeners
            const touchObj = new Touch({
                identifier: Date.now(),
                target: actualTarget,
                clientX: cursorX,
                clientY: cursorY,
                screenX: cursorX,
                screenY: cursorY,
                pageX: cursorX + window.pageXOffset,
                pageY: cursorY + window.pageYOffset,
                radiusX: 10,
                radiusY: 10,
                force: 1
            });

            ['touchstart', 'touchend'].forEach(eventType => {
                const touchEvent = new TouchEvent(eventType, {
                    bubbles: true,
                    cancelable: true,
                    view: window,
                    touches: eventType === 'touchend' ? [] : [touchObj],
                    targetTouches: eventType === 'touchend' ? [] : [touchObj],
                    changedTouches: [touchObj]
                });
                actualTarget.dispatchEvent(touchEvent);
            });

            ['mousedown', 'mouseup', 'click'].forEach(eventType => {
                const event = new MouseEvent(eventType, {
                    view: window,
                    bubbles: true,
                    cancelable: true,
                    clientX: cursorX,
                    clientY: cursorY,
                    button: 0
                });
                actualTarget.dispatchEvent(event);
            });

            if (inputTarget) {
                inputTarget.focus();
                setTimeout(() => {
                    inputTarget.focus();
                    if (typeof inputTarget.select === 'function' && (/^(text|search|url|tel|password|email)$/i.test(inputTarget.type) || !inputTarget.type)) {
                        inputTarget.select();
                    }
                }, 50);

                if (window.nativeBridge && typeof window.nativeBridge.requestWebViewFocus === 'function') {
                    window.nativeBridge.requestWebViewFocus();
                }
            } else if (typeof target.focus === 'function') {
                target.focus();
            }
        }
    };

    window.handleEnterPress = function() {
        if (window.isCursorActive) {
            window.clickCursor();
        }
    };

    document.addEventListener('focusin', function(event) {
        const target = event.target;
        if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable || target.getAttribute('role') === 'textbox')) {
            if (window.nativeBridge && typeof window.nativeBridge.requestWebViewFocus === 'function') {
                window.nativeBridge.requestWebViewFocus();
            }
        }
    }, true);

    // 5. Intelligent Keyboard & Refresh Prevention Routing
    window.addEventListener('keydown', function(event) {
        if (event.key === 'F5' || ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'r')) {
            event.preventDefault();
            return;
        }

        const activeEl = document.activeElement;
        const isTextField = activeEl && (
            activeEl.tagName === 'INPUT' || 
            activeEl.tagName === 'TEXTAREA' || 
            activeEl.isContentEditable ||
            activeEl.getAttribute('role') === 'textbox'
        );

        if (isTextField) {
            return; 
        }

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
