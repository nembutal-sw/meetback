window.MeetBack = (() => {

    let accessToken =
        localStorage.getItem("accessToken");

    let authLogoutInProgress =
        false;


    const loginUrl = "/login";
    const refreshUrl = "/auth/refresh";
    const checkUrl = "/auth/check";


    async function refreshTokens() {

        const refreshToken =
            localStorage.getItem("refreshToken");

        if (!refreshToken) {
            return false;
        }

        try {

            const response =
                await fetch(
                    refreshUrl,
                    {
                        method: "POST",

                        headers: {
                            "Content-Type":
                                "application/json"
                        },

                        body: JSON.stringify({
                            refreshToken: refreshToken
                        })
                    }
                );


            if (!response.ok) {
                return false;
            }


            const data =
                await response.json();


            localStorage.setItem(
                "accessToken",
                data.accessToken
            );


            localStorage.setItem(
                "refreshToken",
                data.refreshToken
            );


            accessToken =
                data.accessToken;


            return true;

        }
        catch (error) {

            console.error(
                "[JWT REFRESH ERROR]",
                error
            );

            return false;
        }
    }

    async function checkServerRestart()
    {

        try
        {

            const response =
                await fetch(
                    "/auth/server-instance",
                    {
                        cache:
                            "no-store"
                    }
                );


            if (!response.ok)
            {

                console.error(
                    "[SERVER INSTANCE] 조회 실패",
                    response.status
                );


                return false;
            }


            const data =
                await response.json();


            const currentServerInstanceId =
                data.serverInstanceId;


            const savedServerInstanceId =
                localStorage.getItem(
                    "serverInstanceId"
                );


            console.log(
                "[SERVER INSTANCE]",
                {
                    current:
                    currentServerInstanceId,

                    saved:
                    savedServerInstanceId
                }
            );


            // =====================================================
            // 처음 기능을 적용한 최초 접속
            // =====================================================

            if (!savedServerInstanceId)
            {

                localStorage.setItem(
                    "serverInstanceId",
                    currentServerInstanceId
                );


                return false;
            }


            // =====================================================
            // Spring 백엔드 재시작 감지
            // =====================================================

            if (
                savedServerInstanceId
                !== currentServerInstanceId
            )
            {

                console.log(
                    "[SERVER RESTART DETECTED]"
                );


                /*
                 * 로그인 관련 정보만 제거
                 */
                localStorage.removeItem(
                    "accessToken"
                );

                localStorage.removeItem(
                    "refreshToken"
                );

                localStorage.removeItem(
                    "userId"
                );

                localStorage.removeItem(
                    "role"
                );

                localStorage.removeItem(
                    "nickname"
                );


                /*
                 * 새 Spring 실행 ID는 저장
                 */
                localStorage.setItem(
                    "serverInstanceId",
                    currentServerInstanceId
                );


                /*
                 * 현재 JS 메모리에 들고 있던
                 * Access Token도 제거
                 */
                accessToken =
                    null;


                window.location.replace(
                    "/login"
                );


                return true;
            }


            return false;

        }
        catch (error)
        {

            console.error(
                "[SERVER INSTANCE CHECK ERROR]",
                error
            );


            /*
             * 백엔드가 잠깐 꺼져 있는 것만으로
             * localStorage를 지우지는 않는다.
             */
            return false;
        }
    }


    async function checkLogin() {

        if (!accessToken) {

            const refreshed =
                await refreshTokens();

            if (!refreshed) {

                goLogin();

                return null;
            }
        }


        let response =
            await fetch(
                checkUrl,
                {
                    headers: {
                        "Authorization":
                            "Bearer " + accessToken
                    }
                }
            );


        if (response.status === 401) {

            const refreshed =
                await refreshTokens();

            if (!refreshed) {

                goLogin();

                return null;
            }


            response =
                await fetch(
                    checkUrl,
                    {
                        headers: {
                            "Authorization":
                                "Bearer " + accessToken
                        }
                    }
                );
        }


        if (!response.ok) {

            goLogin();

            return null;
        }


        const user =
            await response.json();


        localStorage.setItem(
            "userId",
            user.userId
        );


        localStorage.setItem(
            "role",
            user.role
        );


        return user;
    }


    async function authenticatedFetch(
        url,
        options = {}
    ) {

        options.headers =
            options.headers || {};


        options.headers["Authorization"] =
            "Bearer " + accessToken;


        let response =
            await fetch(
                url,
                options
            );


        if (response.status === 401) {

            const refreshed =
                await refreshTokens();


            if (!refreshed) {

                goLogin();

                return response;
            }


            options.headers["Authorization"] =
                "Bearer " + accessToken;


            response =
                await fetch(
                    url,
                    options
                );
        }


        return response;
    }


    function getMeetingId() {

        const params =
            new URLSearchParams(
                location.search
            );


        const value =
            params.get("meetingId");


        const meetingId =
            Number(value);


        if (
            !value
            ||
            Number.isNaN(meetingId)
            ||
            meetingId <= 0
        ) {

            return null;
        }


        return meetingId;
    }


    function goLogin() {

        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("userId");
        localStorage.removeItem("role");

        location.href =
            loginUrl;
    }

    function showAuthLogoutToast(message)
    {
        let toast =
            document.getElementById(
                "authLogoutToast"
            );

        if (!toast)
        {
            toast =
                document.createElement(
                    "div"
                );

            toast.id =
                "authLogoutToast";

            toast.style.cssText = `
            position: fixed;
            top: 24px;
            left: 50%;
            transform: translate(-50%, -12px);
            z-index: 99999;
            padding: 14px 20px;
            border-radius: 12px;
            background: #ef4444;
            color: white;
            font-weight: 700;
            box-shadow: 0 12px 30px rgba(0, 0, 0, 0.22);
            opacity: 0;
            transition: all 0.25s ease;
        `;

            document.body.appendChild(
                toast
            );
        }

        toast.textContent =
            message;

        requestAnimationFrame(
            () =>
            {
                toast.style.opacity =
                    "1";

                toast.style.transform =
                    "translate(-50%, 0)";
            }
        );
    }


    function handleAuthWebSocketClose(event) {

        const reason =
            String(event?.reason || "");

        const authInvalidated =
            Number(event?.code) === 4001
            || reason.startsWith("AUTH_INVALIDATED");

        if (!authInvalidated) {
            return false;
        }

        if (authLogoutInProgress) {
            return true;
        }

        authLogoutInProgress =
            true;

        accessToken =
            null;

        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("userId");
        localStorage.removeItem("role");
        localStorage.removeItem("nickname");

        sessionStorage.setItem(
            "authLogoutMessage",
            "다른 곳에서 새로 로그인되어 기존 로그인이 종료되었습니다."
        );

        location.replace(
            loginUrl
        );

        return true;
    }


    function escapeHtml(value) {

        if (value == null) {
            return "";
        }


        return String(value)

            .replaceAll("&", "&amp;")

            .replaceAll("<", "&lt;")

            .replaceAll(">", "&gt;")

            .replaceAll('"', "&quot;")

            .replaceAll("'", "&#039;");
    }


    function formatDateTime(value) {

        if (!value) {
            return "-";
        }


        const date =
            new Date(value);


        if (
            Number.isNaN(
                date.getTime()
            )
        ) {

            return value;
        }


        return date.toLocaleString(
            "ko-KR"
        );
    }


    function getAccessToken() {

        return accessToken;
    }


    return {

        checkLogin,
        authenticatedFetch,
        getMeetingId,
        getAccessToken,
        handleAuthWebSocketClose,
        escapeHtml,
        formatDateTime

    };

})();
