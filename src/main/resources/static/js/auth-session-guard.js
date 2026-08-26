(function ()
{

    // =========================================================
    // 로그인 상태 확인 주기
    //
    // 프로젝트 테스트용:
    // 3초마다 한 번 확인
    // =========================================================

    const CHECK_INTERVAL_MS =
        3000;


    let checking =
        false;


    let timerId =
        null;


    // =========================================================
    // 로그인 정보 삭제
    // =========================================================

    function clearLoginStorage()
    {

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
            "chatMeetingId"
        );
    }


    // =========================================================
    // 강제 로그아웃
    // =========================================================

    function forceLogout()
    {

        console.log(
            "[SESSION INVALIDATED]"
        );


        sessionStorage.setItem(
            "authNotice",
            "다른 기기에서 로그인되어 기존 로그인이 종료되었습니다."
        );


        clearLoginStorage();


        window.location.replace(
            "/login"
        );
    }


    // =========================================================
    // Refresh Token 재발급
    //
    // Access Token이 단순 만료된 경우에는
    // 정상적으로 Refresh해서 로그인 유지
    //
    // 다른 기기 로그인 때문에 Refresh Token까지
    // 무효화됐다면 false
    // =========================================================

    async function refreshTokens()
    {

        const refreshToken =
            localStorage.getItem(
                "refreshToken"
            );


        if (!refreshToken)
        {
            return false;
        }


        try
        {

            const response =
                await fetch(
                    "/auth/refresh",
                    {

                        method:
                            "POST",


                        headers: {

                            "Content-Type":
                                "application/json"

                        },


                        body:
                            JSON.stringify(
                                {
                                    refreshToken:
                                    refreshToken
                                }
                            )

                    }
                );


            if (!response.ok)
            {
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


            return true;

        }
        catch (error)
        {

            /*
             * 단순 네트워크 장애 때문에
             * 강제 로그아웃하지 않도록
             * 여기서는 false만 반환
             */

            console.error(
                "[SESSION REFRESH ERROR]",
                error
            );


            return null;
        }
    }


    // =========================================================
    // 현재 로그인 세션 검사
    // =========================================================

    async function checkCurrentSession()
    {

        if (checking)
        {
            return;
        }


        const accessToken =
            localStorage.getItem(
                "accessToken"
            );


        const refreshToken =
            localStorage.getItem(
                "refreshToken"
            );


        /*
         * 애초에 로그인하지 않은 상태라면
         * 아무것도 하지 않는다.
         */
        if (
            !accessToken
            &&
            !refreshToken
        )
        {
            return;
        }


        checking =
            true;


        try
        {

            let currentAccessToken =
                localStorage.getItem(
                    "accessToken"
                );


            // =================================================
            // Access Token 자체가 없다면 Refresh 시도
            // =================================================

            if (!currentAccessToken)
            {

                const refreshed =
                    await refreshTokens();


                if (refreshed === false)
                {

                    forceLogout();


                    return;
                }


                if (refreshed === null)
                {
                    return;
                }


                currentAccessToken =
                    localStorage.getItem(
                        "accessToken"
                    );
            }


            // =================================================
            // 현재 Access Token 검증
            // =================================================

            let response =
                await fetch(
                    "/auth/check",
                    {

                        method:
                            "GET",


                        headers: {

                            "Authorization":
                                "Bearer "
                                + currentAccessToken

                        },


                        cache:
                            "no-store"

                    }
                );


            // =================================================
            // 401 / 403
            //
            // 1. Access Token 자연 만료일 수도 있음
            // 2. 다른 기기 로그인으로 tokenVersion이
            //    바뀐 것일 수도 있음
            //
            // 따라서 Refresh Token으로 한 번 확인
            // =================================================

            if (
                response.status === 401
                ||
                response.status === 403
            )
            {

                const refreshed =
                    await refreshTokens();


                /*
                 * Refresh까지 실패
                 *
                 * → 기존 Refresh Token도 DB에서 무효화됨
                 * → 다른 기기 로그인 가능성이 높음
                 */
                if (refreshed === false)
                {

                    forceLogout();


                    return;
                }


                /*
                 * 네트워크 오류라면
                 * 사용자를 강제로 로그아웃시키지 않는다.
                 */
                if (refreshed === null)
                {
                    return;
                }


                currentAccessToken =
                    localStorage.getItem(
                        "accessToken"
                    );


                response =
                    await fetch(
                        "/auth/check",
                        {

                            method:
                                "GET",


                            headers: {

                                "Authorization":
                                    "Bearer "
                                    + currentAccessToken

                            },


                            cache:
                                "no-store"

                        }
                    );
            }


            if (!response.ok)
            {

                forceLogout();
            }

        }
        catch (error)
        {

            /*
             * 인터넷 끊김 / Spring 백엔드 잠깐 다운 등의
             * 상황에서 강제 로그아웃시키지는 않는다.
             */
            console.error(
                "[SESSION CHECK ERROR]",
                error
            );

        }
        finally
        {

            checking =
                false;
        }
    }


    // =========================================================
    // 감시 시작
    // =========================================================

    function startSessionGuard()
    {

        if (timerId)
        {
            return;
        }


        timerId =
            setInterval(
                checkCurrentSession,
                CHECK_INTERVAL_MS
            );
    }


    if (
        document.readyState
        === "loading"
    )
    {

        document.addEventListener(
            "DOMContentLoaded",
            startSessionGuard
        );

    }
    else
    {

        startSessionGuard();
    }

})();