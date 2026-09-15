(function() {
    if (window.__tvWrapperInjected) return;
    window.__tvWrapperInjected = true;

    window.isCursorActive = false;
    let cursorX = window.innerWidth / 2;
    let cursorY = window.innerHeight / 2;
    let motionIntervalX = null;
    let motionIntervalY = null;
    let forcedTextField = null; // Tracks explicit user input selection

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

        if (!isAnyTextFieldActive()) {
            if (dx !== 0 && !motionIntervalX) motionIntervalX = setInterval(() => executeMotion(dx, 0), 25);
            if (dy !== 0 && !motionIntervalY) motionIntervalY = setInterval(() => executeMotion(0, dy), 25);
        }
    };

    window.tvStopMotion = function(axis) {
        if (axis === 'x' && motionIntervalX) { clearInterval(motionIntervalX); motionIntervalX = null; }
        if (axis === 'y' && motionIntervalY) { clearInterval(motionIntervalY); motionIntervalY = null; }
    };

    // 3. Reliable Input Detection Helper
    function isAnyTextFieldActive() {
        const activeEl = document.activeElement;
        const isFocusedInput = activeEl && (
            activeEl.tagName === 'INPUT' || 
            activeEl.tagName === 'TEXTAREA' || 
            activeEl.isContentEditable ||
            activeEl.getAttribute('role') === 'textbox'
        );
        // Fallback to forcedTextField if WebView window focus dropped the activeElement state
        return isFocusedInput || (forcedTextField && document.contains(forcedTextField));
    }

    // 4. Bulletproof Click Simulation for Login Fields
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

            // Dispatch realistic touch and mouse sequences
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
                forcedTextField = inputTarget; // Lock this field down manually
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
                forcedTextField = null;
                if (typeof target.focus === 'function') target.focus();
            }
        }
    };

    window.handleEnterPress = function() {
        if (window.isCursorActive) window.clickCursor();
    };

    // Clear manual lock if user clicks somewhere else standard
    document.addEventListener('click', (e) => {
        if (!e.target.matches('input, textarea, [contenteditable="true"], [role="textbox"]')) {
            forcedTextField = null;
        }
    }, true);

    // 5. Hardened Keystroke Routing Guard
    window.addEventListener('keydown', function(event) {
        if (event.key === 'F5' || ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'r')) {
            event.preventDefault();
            return;
        }

        // If an input field is active or manually tracked, let the web page handle the key completely
        if (isAnyTextFieldActive()) {
            return; 
        }

        // Otherwise, safely pipe into the native toolbar
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
