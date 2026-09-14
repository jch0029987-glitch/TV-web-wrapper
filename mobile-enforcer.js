// mobile-enforcer.js - Forces mobile site redirection for TV
console.log("TV Browser: mobile-enforcer.js loaded.");

(function() {
    const host = window.location.hostname;
    const href = window.location.href;

    // 1. YouTube: Redirect desktop to m.youtube.com
    if (host.includes('youtube.com') && !host.startsWith('m.')) {
        console.log("TV Enforcer: Redirecting YouTube to mobile version.");
        window.location.replace(href.replace('www.youtube.com', 'm.youtube.com'));
    }
    
    // 2. Facebook & Messenger Web: Redirect desktop to m.facebook.com
    else if (host.includes('facebook.com') && !host.startsWith('m.') && !host.startsWith('touch.')) {
        console.log("TV Enforcer: Redirecting Facebook/Messages to mobile version.");
        window.location.replace(href.replace('facebook.com', 'm.facebook.com'));
    }
})();
