(function () {
    "use strict";

    /* Official NAVER Maps Style Editor example: native GL dark map style. */
    const DARK_STYLE_ID = "94230366-adba-4e0e-ac5a-e82a0e137b5e";
    const root = document.documentElement;
    let mapTheme = null;
    let reloadScheduled = false;

    if (!window.naver || !window.naver.maps || !window.naver.maps.Map) {
        return;
    }

    const NaverMap = window.naver.maps.Map;

    function isDarkTheme() {
        return root.dataset.theme === "dark" || root.classList.contains("dark");
    }

    function themedOptions(options) {
        const dark = isDarkTheme();
        const next = Object.assign({}, options || {});

        next.gl = true;
        next.background = dark ? "#151522" : "#f6f6f8";

        if (dark) {
            next.customStyleId = DARK_STYLE_ID;
        } else {
            delete next.customStyleId;
        }

        return next;
    }

    function MeetBackNaverMap(element, options) {
        mapTheme = isDarkTheme() ? "dark" : "light";
        return new NaverMap(element, themedOptions(options));
    }

    MeetBackNaverMap.prototype = NaverMap.prototype;
    Object.setPrototypeOf(MeetBackNaverMap, NaverMap);
    window.naver.maps.Map = MeetBackNaverMap;

    function syncOpenMapTheme() {
        const nextTheme = isDarkTheme() ? "dark" : "light";

        if (!mapTheme || nextTheme === mapTheme || reloadScheduled) {
            return;
        }

        /*
         * NAVER GL custom styles are fixed when a map instance is created.
         * Rebuilding through a page reload guarantees that light mode returns
         * to NAVER's default map instead of retaining the previous dark style.
         */
        reloadScheduled = true;
        window.location.reload();
    }

    window.addEventListener("meetback:themechange", syncOpenMapTheme);

    new MutationObserver(syncOpenMapTheme).observe(root, {
        attributes: true,
        attributeFilter: ["data-theme", "class"]
    });
}());
