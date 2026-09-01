(function () {
    "use strict";

    const STORAGE_KEY = "meetback-theme";
    const root = document.documentElement;
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    let feedbackTimer = null;
    let feedbackConfirmResolve = null;
    let feedbackEscapeHandler = null;

    function ensureFeedbackToast() {
        let toast = document.querySelector(".mb-feedback-toast");

        if (!toast) {
            const region = document.createElement("div");
            region.className = "mb-feedback-toast-region";

            toast = document.createElement("div");
            toast.className = "mb-feedback-toast";
            toast.innerHTML = [
                '<div class="mb-feedback-toast__message"></div>',
                '<div class="mb-feedback-toast__actions"></div>'
            ].join("");

            region.appendChild(toast);
            document.body.appendChild(region);
        }

        return {
            toast: toast,
            message: toast.querySelector(".mb-feedback-toast__message"),
            actions: toast.querySelector(".mb-feedback-toast__actions")
        };
    }

    function settleFeedbackConfirm(result) {
        const resolve = feedbackConfirmResolve;
        feedbackConfirmResolve = null;

        if (resolve) {
            resolve(result);
        }
    }

    function hideFeedbackToast(result = false) {
        if (feedbackTimer) {
            clearTimeout(feedbackTimer);
            feedbackTimer = null;
        }

        if (feedbackEscapeHandler) {
            document.removeEventListener("keydown", feedbackEscapeHandler);
            feedbackEscapeHandler = null;
        }

        const toast = document.querySelector(".mb-feedback-toast");
        if (toast) {
            toast.classList.remove("is-visible");
            toast.removeAttribute("role");
            toast.removeAttribute("aria-modal");

            const actions = toast.querySelector(".mb-feedback-toast__actions");
            if (actions) {
                actions.replaceChildren();
            }
        }

        settleFeedbackConfirm(result);
    }

    function showFeedbackToast(message, type = "info", duration = 3500) {
        hideFeedbackToast(false);

        const ui = ensureFeedbackToast();
        const normalizedType = ["success", "warning", "error", "info"].includes(type)
            ? type
            : "info";

        ui.message.textContent = String(message || "알림");
        ui.actions.replaceChildren();
        ui.toast.className = "mb-feedback-toast mb-feedback-toast--" + normalizedType;
        ui.toast.setAttribute("role", normalizedType === "error" ? "alert" : "status");

        requestAnimationFrame(function () {
            ui.toast.classList.add("is-visible");
        });

        if (duration > 0) {
            feedbackTimer = window.setTimeout(function () {
                hideFeedbackToast(false);
            }, duration);
        }
    }

    function showFeedbackConfirm(message, options = {}) {
        hideFeedbackToast(false);

        return new Promise(function (resolve) {
            const ui = ensureFeedbackToast();
            const cancelButton = document.createElement("button");
            const confirmButton = document.createElement("button");

            feedbackConfirmResolve = resolve;
            ui.message.textContent = String(message || "계속 진행하시겠습니까?");
            ui.toast.className = "mb-feedback-toast mb-feedback-toast--confirm";
            ui.toast.setAttribute("role", "alertdialog");
            ui.toast.setAttribute("aria-modal", "false");

            cancelButton.type = "button";
            cancelButton.className = "mb-feedback-toast__button mb-feedback-toast__button--cancel";
            cancelButton.textContent = options.cancelText || "취소";
            cancelButton.addEventListener("click", function () {
                hideFeedbackToast(false);
            });

            confirmButton.type = "button";
            confirmButton.className = "mb-feedback-toast__button mb-feedback-toast__button--confirm";
            confirmButton.textContent = options.confirmText || "확인";
            confirmButton.addEventListener("click", function () {
                hideFeedbackToast(true);
            });

            ui.actions.replaceChildren(cancelButton, confirmButton);

            feedbackEscapeHandler = function (event) {
                if (event.key === "Escape") {
                    event.preventDefault();
                    hideFeedbackToast(false);
                }
            };
            document.addEventListener("keydown", feedbackEscapeHandler);

            requestAnimationFrame(function () {
                ui.toast.classList.add("is-visible");
                cancelButton.focus();
            });
        });
    }

    window.MeetBackToast = Object.freeze({
        show: showFeedbackToast,
        confirm: showFeedbackConfirm,
        hide: hideFeedbackToast
    });

    function readSavedTheme() {
        try {
            const saved = window.localStorage.getItem(STORAGE_KEY);
            return saved === "light" || saved === "dark" ? saved : null;
        } catch (error) {
            return null;
        }
    }

    function saveTheme(theme) {
        try {
            window.localStorage.setItem(STORAGE_KEY, theme);
        } catch (error) {
            // The theme still applies for this page when storage is unavailable.
        }
    }

    function preferredTheme() {
        return readSavedTheme() || (media.matches ? "dark" : "light");
    }

    function applyTheme(theme) {
        const dark = theme === "dark";
        const previousTheme = root.dataset.theme;

        root.dataset.theme = theme;
        root.classList.toggle("dark", dark);
        root.style.colorScheme = theme;

        const toggle = document.querySelector("[data-theme-toggle]");
        if (toggle) {
            const nextLabel = dark ? "라이트 모드" : "다크 모드";
            toggle.setAttribute("aria-label", nextLabel);
            toggle.setAttribute("title", nextLabel);
            toggle.setAttribute("aria-pressed", String(dark));
            toggle.innerHTML = dark
                ? '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="4"></circle><path d="M12 2v2M12 20v2M4.93 4.93l1.42 1.42M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.42-1.42M17.66 6.34l1.41-1.41"></path></svg>'
                : '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20.2 15.1A8.5 8.5 0 0 1 8.9 3.8 8.5 8.5 0 1 0 20.2 15.1Z"></path></svg>';
        }

        if (previousTheme !== theme) {
            window.dispatchEvent(new CustomEvent("meetback:themechange", {
                detail: { theme: theme }
            }));
        }
    }

    function mountSiteHeader() {
        const body = document.body;

        if (!body || !body.matches(".mb-app-page:not(.page-home), .page-feed-list")) {
            return null;
        }

        let header = document.querySelector(".mb-site-header");

        if (!header) {
            const active = body.classList.contains("page-quick-meetings")
                ? "quick"
                : body.classList.contains("page-feed-list")
                    ? "review"
                    : "home";

            header = document.createElement("header");
            header.className = "mb-site-header";
            header.innerHTML = [
                '<div class="mb-site-header__inner">',
                '  <a class="mb-site-brand" href="/home" aria-label="MeetBack 홈">',
                '    <span class="mb-site-brand__icon" aria-hidden="true"></span>',
                '    <span class="mb-site-brand__name">MeetBack</span>',
                '  </a>',
                '  <nav class="mb-site-nav" aria-label="주요 메뉴">',
                `    <a href="/home"${active === "home" ? ' aria-current="page"' : ""}>홈</a>`,
                `    <a href="/quick-meetings"${active === "quick" ? ' aria-current="page"' : ""}>번개</a>`,
                `    <a href="/feed"${active === "review" ? ' aria-current="page"' : ""}>후기</a>`,
                '  </nav>',
                '  <div class="mb-site-header__actions">',
                '    <a class="mb-site-create" href="/home#meetingStartSection"><span aria-hidden="true">＋</span> 모임 만들기</a>',
                '  </div>',
                '</div>'
            ].join("");

            body.insertBefore(header, body.firstChild);
            body.classList.add("mb-has-site-header");
        }

        return header.querySelector(".mb-site-header__actions");
    }

    function mountToggle() {
        const siteHeaderActions = mountSiteHeader();
        let toggle = document.querySelector("[data-theme-toggle]");

        if (!toggle) {
            toggle = document.createElement("button");
            toggle.type = "button";
            toggle.className = "mb-theme-toggle";
            toggle.dataset.themeToggle = "";

            const homeActions = document.querySelector(".home-header-actions");
            const loginContainer = document.querySelector(".page-login .login-container");
            if (siteHeaderActions) {
                siteHeaderActions.insertBefore(toggle, siteHeaderActions.firstChild);
            } else if (homeActions) {
                homeActions.insertBefore(toggle, homeActions.firstChild);
            } else if (loginContainer) {
                loginContainer.insertBefore(toggle, loginContainer.firstChild);
            } else {
                document.body.appendChild(toggle);
            }
        }

        toggle.addEventListener("click", function () {
            const next = root.dataset.theme === "dark" ? "light" : "dark";
            saveTheme(next);
            applyTheme(next);
        });

        applyTheme(root.dataset.theme || preferredTheme());
    }

    applyTheme(preferredTheme());

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", mountToggle, { once: true });
    } else {
        mountToggle();
    }

    const handleSystemThemeChange = function (event) {
        if (!readSavedTheme()) {
            applyTheme(event.matches ? "dark" : "light");
        }
    };

    if (typeof media.addEventListener === "function") {
        media.addEventListener("change", handleSystemThemeChange);
    } else if (typeof media.addListener === "function") {
        media.addListener(handleSystemThemeChange);
    }

    window.addEventListener("storage", function (event) {
        if (event.key === STORAGE_KEY) {
            applyTheme(preferredTheme());
        }
    });

    window.addEventListener("pageshow", function () {
        applyTheme(preferredTheme());
    });
}());
