(function () {
    "use strict";

    function readNickname() {
        try {
            const nickname = localStorage.getItem("nickname");
            return nickname && nickname.trim() ? nickname.trim() : null;
        } catch (error) {
            return null;
        }
    }

    function mountNickname() {
        const nicknameElement = document.getElementById("homeNickname");
        const userIdElement = document.getElementById("userId");

        if (!nicknameElement || !userIdElement) {
            return;
        }

        function syncNickname() {
            const nickname = readNickname();
            nicknameElement.textContent = nickname ? nickname + "님" : "회원님";
            nicknameElement.title = nickname ? nickname + "님의 계정" : "로그인된 계정";
        }

        syncNickname();

        const observer = new MutationObserver(syncNickname);
        observer.observe(userIdElement, {
            childList: true,
            characterData: true,
            subtree: true
        });

        window.addEventListener("storage", function (event) {
            if (event.key === "nickname") {
                syncNickname();
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", mountNickname, { once: true });
    } else {
        mountNickname();
    }
})();
