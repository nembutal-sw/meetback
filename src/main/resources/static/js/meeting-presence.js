(function (global)
{

    let presenceClient =
        null;


    function connect(
        meetingId,
        onMessage
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

                heartbeatIncoming:
                    5000,

                heartbeatOutgoing:
                    5000,

                reconnectDelay:
                    1000,


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

                            function (message)
                            {
                                let data;

                                try
                                {
                                    data =
                                        JSON.parse(
                                            message.body
                                        );
                                }
                                catch (error)
                                {
                                    console.error(
                                        "[PRESENCE MESSAGE PARSE ERROR]",
                                        error
                                    );

                                    return;
                                }


                                if (
                                    typeof onMessage
                                    ===
                                    "function"
                                )
                                {
                                    Promise.resolve(
                                        onMessage(data)
                                    ).catch(
                                        function (error)
                                        {
                                            console.error(
                                                "[PRESENCE MESSAGE HANDLER ERROR]",
                                                error
                                            );
                                        }
                                    );
                                }
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
                    },


                onWebSocketClose:
                    function(event)
                    {
                        const closeCode =
                            Number(event?.code);

                        if (closeCode === 4002)
                        {
                            presenceClient.reconnectDelay = 0;

                            MeetBack
                                .handleMeetingAccessWebSocketClose(
                                    event
                                );

                            return;
                        }

                        if (closeCode === 4001)
                        {
                            presenceClient.reconnectDelay = 0;
                        }

                        MeetBack.handleAuthWebSocketClose(
                            event
                        );
                    }

            });


        presenceClient.activate();
    }

    async function disconnect()
    {
        const client =
            presenceClient;

        presenceClient =
            null;


        if (!client)
        {
            return;
        }


        /*
         * 강퇴 후 자동 재접속하지 않도록 막는다.
         */
        client.reconnectDelay =
            0;


        if (client.active)
        {
            await client.deactivate();
        }
    }


    global.MeetBackPresence =
        {
            connect:
            connect,

            disconnect:
            disconnect
        };

})(window);
