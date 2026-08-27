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

    let lastAuthError =
        null;

    // Refresh Token으로 Access Token을 한 번 갱신한다.
    async function refreshTokens()
    {
        const refreshToken =
            localStorage.getItem(
                "refreshToken"
            );

        if (!refreshToken)
        {
            lastAuthError = null;
            return false;
        }

        try
        {
            const response =
                await fetch(
                    "/auth/refresh",
                    {
                        method: "POST",
                        headers: {
                            "Content-Type":
                                "application/json"
                        },
                        body: JSON.stringify(
                            {
                                refreshToken:
                                    refreshToken
                            }
                        ),
                        cache: "no-store"
                    }
                );

            if ([400, 401, 403].includes(response.status))
            {
                lastAuthError =
                    await readAuthError(
                        response
                    );

                return false;
            }

            // 서버 오류와 요청 제한은 다음 검사 주기에 다시 시도한다.
            if (!response.ok)
            {
                lastAuthError = null;
                return null;
            }

            const data =
                await response.json();

            if (!data.accessToken
                || !data.refreshToken)
            {
                lastAuthError = null;
                return false;
            }

            localStorage.setItem(
                "accessToken",
                data.accessToken
            );

            localStorage.setItem(
                "refreshToken",
                data.refreshToken
            );

            lastAuthError = null;
            return true;
        }
        catch (error)
        {
            console.error(
                "[TOKEN REFRESH ERROR]",
                error
            );

            return null;
        }
    }

    async function readAuthError(response)
    {
        try
        {
            return await response.clone().json();
        }
        catch (error)
        {
            return null;
        }
    }

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
    let forceLogoutInProgress =
        false;

    // =========================================================
// 강제 로그아웃
// =========================================================

    function forceLogout(authError)
    {

        /*
         * setInterval 때문에
         * forceLogout()이 여러 번 실행되는 것 방지
         */
        if (forceLogoutInProgress)
        {
            return;
        }


        forceLogoutInProgress =
            true;


        const suspended =
            authError
            && authError.code ===
            "ACCOUNT_SUSPENDED";

        const notice = suspended
            ? "이용이 정지된 계정입니다. 관리자에게 문의해주세요."
            : "다른 기기에서 로그인되어 연결이 종료되었습니다.";

        const title = suspended
            ? "계정 이용이 정지되었습니다."
            : "연결이 종료되었습니다.";

        const detail = suspended
            ? "정지 상태에서는 MeetBack을 이용할 수 없습니다."
            : "다른 기기에서 동일한 계정으로 로그인했습니다.";

        console.log(
            suspended
                ? "[ACCOUNT SUSPENDED]"
                : "[SESSION INVALIDATED]"
        );


        // =====================================================
        // 로그인 페이지에서도 이유를 보여줄 수 있도록 저장
        // =====================================================

        sessionStorage.setItem(
            "authNotice",
            notice
        );


        // =====================================================
        // 더 이상 세션 검사하지 않음
        // =====================================================

        if (timerId)
        {

            clearInterval(
                timerId
            );


            timerId =
                null;
        }


        // =====================================================
        // 현재 페이지에 강제 로그아웃 안내 표시
        // =====================================================

        const overlay =
            document.createElement(
                "div"
            );


        overlay.style.position =
            "fixed";

        overlay.style.top =
            "0";

        overlay.style.left =
            "0";

        overlay.style.width =
            "100%";

        overlay.style.height =
            "100%";

        overlay.style.background =
            "rgba(0, 0, 0, 0.45)";

        overlay.style.display =
            "flex";

        overlay.style.alignItems =
            "center";

        overlay.style.justifyContent =
            "center";

        overlay.style.zIndex =
            "999999";


        const messageBox =
            document.createElement(
                "div"
            );


        messageBox.style.background =
            "#ffffff";

        messageBox.style.padding =
            "28px 36px";

        messageBox.style.borderRadius =
            "14px";

        messageBox.style.textAlign =
            "center";

        messageBox.style.boxShadow =
            "0 10px 30px rgba(0, 0, 0, 0.25)";

        messageBox.style.fontSize =
            "16px";

        messageBox.style.fontWeight =
            "600";


        messageBox.innerHTML =
            `
        <div
            style="
                font-size: 20px;
                margin-bottom: 12px;
            "
        >
            ${title}
        </div>

        <div
            style="
                color: #666;
                font-size: 14px;
                font-weight: 400;
            "
        >
            ${detail}
        </div>

        <div
            style="
                color: #999;
                font-size: 13px;
                margin-top: 12px;
                font-weight: 400;
            "
        >
            잠시 후 로그인 화면으로 이동합니다.
        </div>
        `;


        overlay.appendChild(
            messageBox
        );


        document.body.appendChild(
            overlay
        );


        // =====================================================
        // 3초 동안 메시지를 보여준 후 로그아웃
        // =====================================================

        setTimeout(
            () =>
            {

                clearLoginStorage();


                window.location.replace(
                    "/login"
                );

            },
            3000
        );
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

                    forceLogout(
                        lastAuthError
                    );


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

            if (response.status === 403)
            {
                const authError =
                    await readAuthError(
                        response
                    );

                if (authError
                    && authError.code ===
                    "ACCOUNT_SUSPENDED")
                {
                    forceLogout(
                        authError
                    );

                    return;
                }
            }


            if (response.status === 401
                || response.status === 403)
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

                    forceLogout(
                        lastAuthError
                    );


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

                forceLogout(
                    await readAuthError(
                        response
                    )
                );
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
