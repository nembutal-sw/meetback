window.MeetBack = (() => {

    let accessToken =
        localStorage.getItem("accessToken");


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
        escapeHtml,
        formatDateTime

    };

})();