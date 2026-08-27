(function () {
    "use strict";

    function initializeTabs() {
        const tabs = Array.from(document.querySelectorAll("[data-room-tab]"));
        const panels = Array.from(document.querySelectorAll("[data-room-panel]"));
        const chatMessages = document.getElementById("chatMessages");

        if (!tabs.length || !panels.length) {
            return;
        }

        let chatScrollTop = 0;
        let chatWasAtBottom = true;

        function activateTab(tabName) {
            const currentChatPanel = document.querySelector('[data-room-panel="chat"]');

            if (currentChatPanel && !currentChatPanel.hidden && chatMessages) {
                const remainingScroll = chatMessages.scrollHeight
                    - chatMessages.clientHeight
                    - chatMessages.scrollTop;
                chatWasAtBottom = remainingScroll < 12;
                chatScrollTop = chatMessages.scrollTop;
            }

            tabs.forEach((tab) => {
                const selected = tab.dataset.roomTab === tabName;
                tab.setAttribute("aria-selected", String(selected));
                tab.tabIndex = selected ? 0 : -1;
            });

            panels.forEach((panel) => {
                panel.hidden = panel.dataset.roomPanel !== tabName;
            });

            if (tabName === "chat" && chatMessages) {
                requestAnimationFrame(() => {
                    chatMessages.scrollTop = chatWasAtBottom
                        ? chatMessages.scrollHeight
                        : chatScrollTop;
                });
            }
        }

        tabs.forEach((tab, index) => {
            tab.addEventListener("click", () => activateTab(tab.dataset.roomTab));
            tab.addEventListener("keydown", (event) => {
                if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") {
                    return;
                }

                event.preventDefault();
                const direction = event.key === "ArrowRight" ? 1 : -1;
                const nextIndex = (index + direction + tabs.length) % tabs.length;
                tabs[nextIndex].focus();
                activateTab(tabs[nextIndex].dataset.roomTab);
            });
        });

        activateTab("info");
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeTabs);
    } else {
        initializeTabs();
    }
})();
