(ns dots.web.rooms)

;create a map to store all rooms with channels
;this is basically a pubsub, after this works, move to wamp protocol (!!!consider security!!!)
; rooms structure-
; room-id (equals game-id, but a keyword) to list of connected channels
; {
;   :1/*room-id*/ (ch1/*some channel*/ ch2 ch3)
;   :2 (ch100 ch145)
;   :3 (ch27 ch35)
;   :4 (ch12 ch67 ch20 ch47)
; }

; TODO:
; use rooms to broadcast updates and commands to all clients
; simple flow (for "3. Play" use case):
;   1. player1 sends update to server
;   2. server updates the game
;   3. server sends new version of game field to all clients
;   4. all clients except player1 update their game fields on canvas
;   5. server broadcasts a command like "player2's turn"
;   6. all clients except player2 discard the message
;   7. game field for player2 is enabled, he can put a dot
;   8. goto 1
;
; ways to improve:
;   do not just broadcast a message, but send it to specific channel
;   need some structure of a room then, to know which role each channel has
; (e.g. {:player1 channel, :player2 channel, :spectators (ch3 ch4 ch5)})

;TODO move multimap to a separate namespace,
; maybe try create a generic implementation as a separate project on github?

;managed synchronous storage for rooms
(def room2ch (ref {}))

;maps channels to rooms they participate in
(def ch2room (ref {}))

;my own multimap...
;add another key-item pair
(defn mm-put [map key item]
  (if (contains? map key)
    (assoc map key (cons item (get map key)))
    (assoc map key (list item))))
;remove a key from map
(defn mm-rem-key [map key]
  (if (contains? map key)
    (dissoc map key)
    map))
;remove a value from list by key
(defn mm-rem-val [map key item]
  (if (contains? map key)
    (let [values-list (get map key)
          result-values-list (remove #(= item %) values-list)]
      (if (= (count result-values-list) 0)
        (mm-rem-key map key)
        (assoc map key result-values-list)))
    map
    ))

;add a channel to a room by id
(defn add-channel-to-room
  [room-id channel]
  (let [room-id-kw (keyword (str room-id))]
    (dosync
      (ref-set room2ch (mm-put @room2ch room-id-kw channel))
      (ref-set ch2room (mm-put @ch2room channel room-id-kw))
      channel)))

;remove a room and all its associations to existing channels
(defn remove-room
  [room-id]
  (dosync
    (let [room-id-kw (keyword (str room-id))]
      (doseq [channel (get @room2ch room-id-kw)]
        (ref-set ch2room (mm-rem-val @ch2room channel room-id-kw)))
      (ref-set room2ch (mm-rem-key @room2ch room-id-kw)))))

;remove a channel from all rooms
(defn remove-channel
  [channel]
  (dosync
    (doseq [room-id-kw (get @ch2room channel)]
      (ref-set room2ch (mm-rem-val @room2ch room-id-kw channel))
      )
    (ref-set ch2room (mm-rem-key @ch2room channel))))
;end of map to store rooms