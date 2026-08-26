(function(global)
{

    let presenceClient = null;

    function connect(meetingId)
    {
        if(!meetingId)
        {
            return;
        }

        if(presenceClient && presenceClient.active)
        {
            return;
        }

        const accessToken = MeetBack.getAccessToken();

        if(!accessToken)
        {
            return;
        }

        const protocol =
            location.protocol === "https:"
                ? "wss"
                : "ws";

        presenceClient = new StompJs.Client({

            brokerURL:
                protocol
                + "://"
                + location.host
                + "/ws",

            connectHeaders: {
                "Authorization":
                    "Bearer "
                    + accessToken
            },

            reconnectDelay:
                5000,

            onConnect:
                function()
                {
                    console.log(
                        "[PRESENCE CONNECTED]",
                        meetingId
                    );
                    /*
                     * 이 구독 때문에
                     * MeetingPresenceEventListener가
                     * 해당 사용자를 ONLINE으로 판단한다.
                     *
                     * 메시지 내용 자체는 이 페이지에서
                     * 사용할 필요가 없으므로 callback은 비워둔다.
                     */
                    presenceClient.subscribe(
                        "/topic/meetings/"
                        + meetingId
                        + "/chat",

                        function()
                        {

                        }
                    );
                }

        });

        presenceClient.active();
    }

    global.MeetBackPresence =
        {
            connect: connect
        };

})(window);