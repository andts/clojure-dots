(function ($) {
    var invitesStub1 =
        [
            {
                id: 557678,
                name: "Test server 1",
                author: "And",
                status: "Open"
            },
            {
                id: 557679,
                name: "Test server 2",
                author: "Art",
                status: "Open"
            },
            {
                id: 557680,
                name: "Test server 3",
                author: "Ant",
                status: "Playing"
            },
            {
                id: 557681,
                name: "Test server 4",
                author: "Yeg",
                status: "Playing"
            },
            {
                id: 557682,
                name: "Test server 5",
                author: "And",
                status: "Open"
            },
            {
                id: 557683,
                name: "Test server 6",
                author: "Art",
                status: "Playing"
            }
        ];

// Class to represent an invite
    function Invite(id, name, author, status) {
        var self = this;
        self.id = id;
        self.name = name;
        self.author = author;
        self.status = ko.observable(status);
        self.selected = ko.observable(false);
        self.inviteStyle = ko.computed(function () {
            if (self.selected()) {
                return "selected";
            }
            else {
                return "";
            }
        });

        self.toggleSelected = function () {
            self.selected(!self.selected());
        }
    }

// Overall viewmodel for invites page
    function InvitesViewModel() {
        var self = this;
        self.selected = ko.observable(null);

        self.invites = ko.observableArray([]);

        self.selectInvite = function (invite) {
            if (self.selected()) {
                self.selected().toggleSelected();
            }
            invite.toggleSelected();
            self.selected(invite);
        };

        self.joinGame = function () {
            if (self.selected()) {
                console.info("Will join game " + self.selected().id);
                window.location.href = "play?id=" + self.selected().id;
            }
        };

        self.inviteSelected = ko.computed(function () {
            return !!self.selected();
        });
    }


    $(function () {
        var pageModel = new InvitesViewModel();

        var firstMessage = true;

        function initSocket() {
            if (!window.WebSocket) {
                window.WebSocket = window.MozWebSocket;
            }

            if (window.WebSocket) {
                var socket = new WebSocket("ws://host.domic.us:8080/websocket");

                socket.onmessage = function (event) {
                    if (!firstMessage) {
                        var data = JSON.parse(event.data);
                        var invitesStubJson = JSON.stringify(data[2][0]);
                        ko.mapping.fromJSON(invitesStubJson, inviteMapping, pageModel.invites);
                    }
                    else {
                        firstMessage = false;
                    }
                };

                socket.onopen = function () {
                    console.info("Socket opened!");
                };

                socket.onclose = function () {
                    console.info("Socket closed!");
                };

                return socket;
            } else {
                return null;
            }
        }

        var socket = initSocket();

        var inviteMapping = {
            key: function (invite) {
                return ko.utils.unwrapObservable(invite.id);
            },
            create: function (element) {
                return new Invite(element.data.id, element.data.name, element.data.author, element.data.status);
            }
        };

        $("#stub-invites").click(function () {
            var message = '[2, "9000", "http://host.domic.us/echo", ' + JSON.stringify(invitesStub1) + ']';
            send(message);
        });

        function send(message) {
            if (!window.WebSocket) {
                return;
            }
            if (socket.readyState == WebSocket.OPEN) {
                socket.send(message);
            } else {
                alert("The socket is not open.");
            }
        }

        ko.applyBindings(pageModel);
    });
})(jQuery);