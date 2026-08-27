(function () {
    "use strict";

    function roleLabel(role) {
        return role === "ADMIN" || role === "ROLE_ADMIN"
            ? "관리자"
            : "일반 회원";
    }

    function render(user) {
        if (!user) {
            return;
        }

        const nickname = user.nickname || user.email || "MeetBack 사용자";
        const initial = nickname.trim().charAt(0).toUpperCase() || "M";

        document.querySelectorAll("[data-session-nickname]").forEach((element) => {
            element.textContent = nickname;
        });
        document.querySelectorAll("[data-session-role]").forEach((element) => {
            element.textContent = roleLabel(user.role);
        });
        document.querySelectorAll(
            "[data-session-avatar], [data-session-initial]"
        ).forEach((element) => {
            element.textContent = initial;
        });
    }

    window.MeetBackSessionHeader = { render };
})();
