(function() {
    if (window.__tvWrapperInjected) return;
    window.__tvWrapperInjected = true;

    window.isCursorActive = false;
    let cursorX = window.innerWidth / 2;
    let cursorY = window.innerHeight / 2;
    let motionIntervalX = null;
    let motionIntervalY = null;
    
    // Persistent sticky lock for inputs to survive React re-renders
    let lockedInputTarget = null; 

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
            lockedInputTarget = null; // Reset lock when cursor hides
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

        // If a text input is locked, prevent D-pad from scrolling away
        if (!lockedInputTarget) {
            if (dx !== 0 && !motionIntervalX) motionIntervalX = setInterval(() => executeMotion(dx, 0), 25);
            if (dy !== 0 && !motionIntervalY) motionIntervalY = setInterval(() => executeMotion(0, dy), 25);
        }
    };

    window.tvStopMotion = function(axis) {
        if (axis === 'x' && motionIntervalX) { clearInterval(motionIntervalX); motionIntervalX = null; }
        if (axis === 'y' && motionIntervalY) { clearInterval(motionIntervalY); motionIntervalY = null; }
    };

    // 3. Iframe-Aware & Sticky Input Click-to-Activate Logic
    window.clickCursor = function() {
        if (!window.isCursorActive) return;

        cursor.style.backgroundColor = 'rgba(255,255,255,1)';
        setTimeout(() => cursor.style.backgroundColor = 'rgba(255,0,0,0.8)', 150);

        let target = document.elementFromPoint(cursorX, cursorY);
        if (target) {
            let actualTarget = target;
            let cx = cursorX;
            let cy = cursorY;

            // Handle security iframes (like reCAPTCHA)
            if (target.tagName === 'IFRAME') {
                try {
                    const rect = target.getBoundingClientRect();
                    const frameDoc = target.contentDocument || target.contentWindow.document;
                    const innerEl = frameDoc.elementFromPoint(cx - rect.left, cy - rect.top);
                    if (innerEl) {
                        actualTarget = innerEl;
                    }
                } catch (e) {
                    // Cross-origin fallback: Target the iframe element directly and focus it
                    actualTarget = target;
                    if (typeof target.focus === 'function') {
                        target.focus();
                    }
                }
            } else {
                const inputTarget = target.matches('input, textarea, [contenteditable="true"], [role="textbox"]') 
                    ? target 
                    : target.querySelector('input, textarea, [contenteditable="true"], [role="textbox"]') 
                    || target.closest('input, textarea, [contenteditable="true"], [role="textbox"]');
                if (inputTarget) actualTarget = inputTarget;
            }

            // Dispatch touch and mouse events for framework compatibility
            const touchObj = new Touch({
                identifier: Date.now(), target: actualTarget,
                clientX: cx, clientY: cy, screenX: cx, screenY: cy,
                pageX: cx + window.pageXOffset, pageY: cy + window.pageYOffset
            });

            ['touchstart', 'touchend', 'mousedown', 'mouseup', 'click'].forEach(eventType => {
                const ev = eventType.startsWith('touch') 
                    ? new TouchEvent(eventType, { bubbles: true, cancelable: true, view: window, touches: [touchObj], targetTouches: [touchObj], changedTouches: [touchObj] })
                    : new MouseEvent(eventType, { bubbles: true, cancelable: true, view: window, clientX: cx, clientY: cy, button: 0 });
                actualTarget.dispatchEvent(ev);
            });

            if (actualTarget.matches('input, textarea, [contenteditable="true"], [role="textbox"]')) {
                lockedInputTarget = actualTarget; // Permanently lock onto this input
                actualTarget.focus();
                
                // Reinforce focus to counteract immediate React re-render drops
                setTimeout(() => {
                    if (lockedInputTarget) {
                        lockedInputTarget.focus();
                        if (typeof lockedInputTarget.select === 'function' && /^(text|search|url|tel|password|email)$/i.test(lockedInputTarget.type || 'text')) {
                            lockedInputTarget.select();
                        }
                    }
                }, 50);

                if (window.nativeBridge && typeof window.nativeBridge.requestWebViewFocus === 'function') {
                    window.nativeBridge.requestWebViewFocus();
                }
            } else {
                // If clicking outside an input or interacting with an iframe/button, release the lock
                lockedInputTarget = null;
                if (typeof actualTarget.focus === 'function') actualTarget.focus();
            }
        }
    };

    window.handleEnterPress = function() {
        if (window.isCursorActive) window.clickCursor();
    };

    // Listen globally: if user clicks a non-input element anywhere, drop the lock
    document.addEventListener('click', (e) => {
        const clickedInput = e.target.matches('input, textarea, [contenteditable="true"], [role="textbox"]') 
            || e.target.closest('input, textarea, [contenteditable="true"], [role="textbox"]');
        if (!clickedInput) {
            lockedInputTarget = null;
        }
    }, true);

    // 4. Bulletproof Keydown Routing
    window.addEventListener('keydown', function(event) {
        if (event.key === 'F5' || ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'r')) {
            event.preventDefault();
            return;
        }

        // If we have a locked input target attached to the document, trap all keystrokes locally
        if (lockedInputTarget && document.contains(lockedInputTarget)) {
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
