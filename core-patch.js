(function() {
    if (window.__tvWrapperInjected) return;
    window.__tvWrapperInjected = true;

    window.isCursorActive = false;
    window.isWebTypingActive = false; // Explicit toggle based on user click
    let cursorX = window.innerWidth / 2;
    let cursorY = window.innerHeight / 2;
    let motionIntervalX = null;
    let motionIntervalY = null;

    // 1. Virtual Mouse Cursor Setup
    const cursor = document.createElement('div');
    cursor.id = 'tv-virtual-cursor';
    cursor.style.cssText = 'position:fixed; width:24px; height:24px; background:rgba(255,0,0,0.8); border:2px solid white; border-radius:50%; z-index:999999; pointer-events:none; transform:translate(-50%,-50%); display:none; transition:background-color 0.1s ease;';
    
    if (document.body) {
        document.body.appendChild(cursor);
    } else {
        document.addEventListener('DOMContentLoaded', () => document.body.appendChild(cursor));
    }

    window.setCursorVisible = function(visible) {
        window.isCursorActive = visible;
        cursor.style.display = visible ? 'block' : 'none';
        if (!visible) {
            window.isWebTypingActive = false; // Reset typing mode when hiding cursor
        }
    };

    // 2. Motion Engine
    function executeMotion(dx, dy) {
        cursorX = Math.max(0, Math.min(window.innerWidth - 12, cursorX + dx));
        cursorY = Math.max(0, Math.min(window.innerHeight - 12, cursorY + dy));
        cursor.style.left = `${cursorX}px`;
        cursor.style.top = `${cursorY}px`;

        const target = document.elementFromPoint(cursorX, cursorY);
        cursor.style.backgroundColor = target ? 'rgba(0,255,0,0.9)' : 'rgba(255,0,0,0.8)';
    }

    window.tvStartMotion = function(dx, dy) {
        if (window.isCursorActive) {
            if (dx !== 0 && !motionIntervalX) motionIntervalX = setInterval(() => executeMotion(dx, 0), 25);
            if (dy !== 0 && !motionIntervalY) motionIntervalY = setInterval(() => executeMotion(0, dy), 25);
            return;
        }

        // If web typing mode is NOT active, allow D-pad scrolling/motion
        if (!window.isWebTypingActive) {
            if (dx !== 0 && !motionIntervalX) motionIntervalX = setInterval(() => executeMotion(dx, 0), 25);
            if (dy !== 0 && !motionIntervalY) motionIntervalY = setInterval(() => executeMotion(0, dy), 25);
        }
    };

    window.tvStopMotion = function(axis) {
        if (axis === 'x' && motionIntervalX) { clearInterval(motionIntervalX); motionIntervalX = null; }
        if (axis === 'y' && motionIntervalY) { clearInterval(motionIntervalY); motionIntervalY = null; }
    };

    // 3. Explicit Click-to-Activate Typing Mode
    window.clickCursor = function() {
        if (!window.isCursorActive) return;

        cursor.style.backgroundColor = 'rgba(255,255,255,1)';
        setTimeout(() => cursor.style.backgroundColor = 'rgba(255,0,0,0.8)', 150);

        const target = document.elementFromPoint(cursorX, cursorY);
        if (target) {
            const inputTarget = target.matches('input, textarea, [contenteditable="true"], [role="textbox"]') 
                ? target 
                : target.querySelector('input, textarea, [contenteditable="true"], [role="textbox"]') 
                || target.closest('input, textarea, [contenteditable="true"], [role="textbox"]');

            const actualTarget = inputTarget || target;

            // Dispatch touch and mouse events for framework compatibility
            const touchObj = new Touch({
                identifier: Date.now(), target: actualTarget,
                clientX: cursorX, clientY: cursorY, screenX: cursorX, screenY: cursorY,
                pageX: cursorX + window.pageXOffset, pageY: cursorY + window.pageYOffset
            });

            ['touchstart', 'touchend', 'mousedown', 'mouseup', 'click'].forEach(eventType => {
                const ev = eventType.startsWith('touch') 
                    ? new TouchEvent(eventType, { bubbles: true, cancelable: true, view: window, touches: [touchObj], targetTouches: [touchObj], changedTouches: [touchObj] })
                    : new MouseEvent(eventType, { bubbles: true, cancelable: true, view: window, clientX: cursorX, clientY: cursorY, button: 0 });
                actualTarget.dispatchEvent(ev);
            });

            if (inputTarget) {
                // Explicitly lock into web typing mode on click
                window.isWebTypingActive = true;
                inputTarget.focus();
                setTimeout(() => {
                    inputTarget.focus();
                    if (typeof inputTarget.select === 'function' && /^(text|search|url|tel|password|email)$/i.test(inputTarget.type || 'text')) {
                        inputTarget.select();
                    }
                }, 50);

                if (window.nativeBridge && typeof window.nativeBridge.requestWebViewFocus === 'function') {
                    window.nativeBridge.requestWebViewFocus();
                }
            } else {
                // Clicking non-input elements exits web typing mode
                window.isWebTypingActive = false;
                if (typeof target.focus === 'function') target.focus();
            }
        }
    };

    window.handleEnterPress = function() {
        if (window.isCursorActive) window.clickCursor();
    };

    // 4. Clean Keystroke Routing based purely on explicit click state
    window.addEventListener('keydown', function(event) {
        if (event.key === 'F5' || ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'r')) {
            event.preventDefault();
            return;
        }

        // If explicitly in web typing mode via click, let the web page handle keys completely
        if (window.isWebTypingActive) {
            return; 
        }

        // Otherwise, route straight to the native toolbar
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
