// core-patch.js - Accelerated TV Mouse & Pointer Engine
console.log("TV Browser: core-patch.js loaded with accelerated cursor.");

(function() {
    let cursorX = window.innerWidth / 2;
    let cursorY = window.innerHeight / 2;
    let cursorVisible = false;
    let cursorEl = null;

    // Movement velocity state for smooth D-pad holding
    let moveVelX = 0;
    let moveVelY = 0;
    let isMoving = false;
    let animFrameId = null;

    function getOrCreateCursor() {
        if (cursorEl && document.body && document.body.contains(cursorEl)) return cursorEl;

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
        cursorEl.style.boxShadow = '0 0 12px rgba(0,0,0,0.6)';
        
        if (document.body) {
            document.body.appendChild(cursorEl);
        } else {
            window.addEventListener('DOMContentLoaded', () => {
                document.documentElement.appendChild(cursorEl);
            });
        }
        return cursorEl;
    }

    getOrCreateCursor();

    window.setCursorVisible = function(visible) {
        cursorVisible = visible;
        const cursor = getOrCreateCursor();
        if (cursor) {
            cursor.style.display = visible ? 'block' : 'none';
            if (visible) {
                cursorX = Math.max(10, Math.min(window.innerWidth - 10, cursorX));
                cursorY = Math.max(10, Math.min(window.innerHeight - 10, cursorY));
                updateCursorPosition();
            } else {
                stopMotion();
            }
        }
    };

    function updateCursorPosition() {
        if (!cursorEl) return;
        cursorX = Math.max(5, Math.min(window.innerWidth - 5, cursorX));
        cursorY = Math.max(5, Math.min(window.innerHeight - 5, cursorY));
        cursorEl.style.left = cursorX + 'px';
        cursorEl.style.top = cursorY + 'px';
    }

    // Smooth continuous loop for D-pad navigation
    function stepMotion() {
        if (!cursorVisible) return;

        if (moveVelX !== 0 || moveVelY !== 0) {
            cursorX += moveVelX;
            cursorY += moveVelY;

            // Edge-scrolling activation
            const edgeThreshold = 80;
            let pageScrollX = 0;
            let pageScrollY = 0;

            if (cursorY > window.innerHeight - edgeThreshold && moveVelY > 0) {
                pageScrollY = moveVelY * 2;
                cursorY = window.innerHeight - edgeThreshold;
            } else if (cursorY < edgeThreshold && moveVelY < 0) {
                pageScrollY = moveVelY * 2;
                cursorY = edgeThreshold;
            }

            if (cursorX > window.innerWidth - edgeThreshold && moveVelX > 0) {
                pageScrollX = moveVelX * 2;
                cursorX = window.innerWidth - edgeThreshold;
            } else if (cursorX < edgeThreshold && moveVelX < 0) {
                pageScrollX = moveVelX * 2;
                cursorX = edgeThreshold;
            }

            if (pageScrollX !== 0 || pageScrollY !== 0) {
                window.scrollBy({ left: pageScrollX, top: pageScrollY, behavior: 'auto' });
            }

            updateCursorPosition();
        }

        if (isMoving) {
            animFrameId = requestAnimationFrame(stepMotion);
        }
    }

    window.tvStartMotion = function(dx, dy) {
        if (!cursorVisible) return;
        moveVelX = dx;
        moveVelY = dy;
        if (!isMoving) {
            isMoving = true;
            animFrameId = requestAnimationFrame(stepMotion);
        }
    };

    window.tvStopMotion = function(dir) {
        // Stop specific axis or full stop
        if (dir === 'x') moveVelX = 0;
        if (dir === 'y') moveVelY = 0;
        if (dir === 'all') {
            moveVelX = 0;
            moveVelY = 0;
            isMoving = false;
            if (animFrameId) cancelAnimationFrame(animFrameId);
        }
        if (moveVelX === 0 && moveVelY === 0) {
            isMoving = false;
        }
    };

    window.tvScrollBy = function(dx, dy) {
        if (cursorVisible) {
            window.tvStartMotion(dx, dy);
            setTimeout(() => window.tvStopMotion('all'), 200);
        } else {
            window.scrollBy({ left: dx, top: dy, behavior: 'auto' });
        }
    };

    window.clickCursor = function() {
        if (!cursorVisible) return;
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

    window.handleEnterPress = function() {
        if (!cursorVisible) return;
        const cursor = getOrCreateCursor();
        if (!cursor) return;

        cursor.style.display = 'none';
        const target = document.elementFromPoint(cursorX, cursorY);
        cursor.style.display = 'block';

        if (target) {
            if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
                target.focus();
                return;
            }
            window.clickCursor();
        }
    };
})();
