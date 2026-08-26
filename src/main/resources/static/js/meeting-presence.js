(function (global)
{

    let presenceClient =
        null;


    function connect(
        meetingId
    )
    {

        if (!meetingId)
        {

            console.warn(
                "[PRESENCE] meetingId가 없습니다."
            );


            return;
        }


        if (
            presenceClient
            &&
            presenceClient.active
        )
        {

            return;
        }


        const accessToken =
            MeetBack.getAccessToken();


        if (!accessToken)
        {

            console.warn(
                "[PRESENCE] Access Token이 없습니다."
            );


            return;
        }


        const protocol =
            location.protocol === "https:"
                ? "wss"
                : "ws";


        presenceClient =
            new StompJs.Client({

                brokerURL:
                    protocol
                    + "://"
                    + location.host
                    + "/ws",


                connectHeaders:
                    {

                        "Authorization":
                            "Bearer "
                            + accessToken

                    },


                reconnectDelay:
                    5000,


                onConnect:
                    function ()
                    {

                        console.log(
                            "[PRESENCE CONNECTED]",
                            meetingId
                        );


                        presenceClient.subscribe(

                            "/topic/meetings/"
                            + meetingId
                            + "/chat",

                            function ()
                            {
                                /*
                                 * Presence 유지용 구독.
                                 *
                                 * 이 페이지에서는
                                 * 채팅 메시지를 출력하지 않는다.
                                 */
                            }
                        );


                        console.log(
                            "[PRESENCE SUBSCRIBED]",
                            "/topic/meetings/"
                            + meetingId
                            + "/chat"
                        );
                    },


                onStompError:
                    function (frame)
                    {

                        console.error(
                            "[PRESENCE STOMP ERROR]",
                            frame
                        );
                    },


                onWebSocketError:
                    function (error)
                    {

                        console.error(
                            "[PRESENCE WS ERROR]",
                            error
                        );
                    }

            });


        presenceClient.activate();
    }


    global.MeetBackPresence =
        {
            connect:
            connect
        };

})(window);