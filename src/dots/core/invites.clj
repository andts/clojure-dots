(ns dots.core.invites
  (:require [dots.core.player :as player]
            [dots.core.game :as game]
            [dots.field :as field]
            [dots.util :as util]
            [dots.db :as db]
            [clojure.data]))

(def invites
  "invite structure
  {
    :inviteId     123
    :gameInfo     {
                    :name          \" super-game \"
                    :width         10
                    :height        10
                    :player1       {:id          123
                                    :name        \"John\"
                                    :avatar      \"ololo.jpg\"
                                    :skill       5
                                    :region      \"ukr\"
                                    :facebookUid \"aaa.324\"
                                    }
                    :player2       {;same as player1}
                    :player1Color  #{:RED :BLUE}
                    }
    :state        #{:OPEN :FULL :CLOSED}
    :playerState  {player1-id :JOINED
                   player2-id :READY}
    :gameId       123 ;after both players ready and invite is closed
    :searchFilter {:playerId 123 ;if we want to play with exact player
                   :skill    5 ;if we wanr to play with player of some skill level
                   :region   \"ukr\" ;if we want to play with player from some country (http://en.wikipedia.org/wiki/ISO_3166-1_alpha-3)
                   }
    }"
  (ref {}))

(defn save-invite-inmemory
  [invite]
  (util/save-inmemory invites :inviteId invite)
  invite)

(defn create-invite
  [gameInfo filter player1-info]
  (dosync
    (let [invite {:inviteId     (util/get-next-index @invites)
                  :gameInfo     (assoc gameInfo :player1 player1-info)
                  :searchFilter filter
                  :state        :OPEN
                  :playerState  {(:id player1-info) :JOINED}}]
      (save-invite-inmemory invite))))

(defn filter-matches? [filter player]
  "Check if search filter matches player "
  (let [conditions (keys filter)
        comparison (clojure.data/diff filter player)
        matching (nthnext comparison 2)]
    (and (= (count conditions) (count matching)))))

(defn find-invites
  "Find invites with search-filter matching current players skill or region
  (or other possible conditions in future) and :state :OPEN "
  [player]
  (dosync
    (filter (fn [invite] (and (filter-matches? (invite :search-filter) player) (= (invite :state) :OPEN)))
            @invites)))

(defn join-invite
  "Try to join an invite. If it's :state is :OPEN - joins and returns the invite,
  otherwise - returns false."
  [invite-id player-id]
  (dosync
    (when-let [invite (get @invites invite-id)]
      (when (= (invite :state) :OPEN)
        (let [player2-info (get @player/players player-id)
              invite-w-player-info (assoc-in invite [:gameInfo :player2] player2-info)
              invite-w-player-state (assoc-in invite-w-player-info [:playerState (:id player2-info)] :JOINED)]
          (save-invite-inmemory (assoc invite-w-player-state :state :FULL)))))))

(defn leave-invite
  "Leave invite. If player is the author - remove invite entirely,
  if player is opponent - clear the field and change :state back to :OPEN. "
  [invite-id player-id]
  (dosync
    (when-let [invite (get @invites invite-id)]
      (cond
        (= player-id (get-in invite [:gameInfo :player1 :id]))
        (
          ;player1 disconnects, destroy the invite
          )
        (= player-id (get-in invite [:gameInfo :player2 :id]))
        (
          ;player2 disconnects, remove him from invite, update invite
          )
        ))))

(defn- update-invite-ready [invite-id player-id]
  (dosync
    (when-let [invite (get @invites invite-id)]
      (when (= (get-in invite [:playerState player-id]) :JOINED)
        (let [updated-invite (assoc-in invite [:playerState player-id] :READY)
              player-states (distinct (vals (updated-invite :playerState)))]
          (if (and (= (count player-states) 1) (= (first player-states) :READY))
            (save-invite-inmemory (assoc updated-invite :state :CLOSED))
            (save-invite-inmemory updated-invite)))))))

(defn set-player-ready
  "Set player state to :READY, create game, change game state to :CLOSED"
  [invite-id player-id]
  (let [updated-invite (update-invite-ready invite-id player-id)]
    (if (= (updated-invite :state) :CLOSED)
      (let [new-game (game/create-game updated-invite)
            invite-w-game (assoc updated-invite :game-id (new-game :game-id))]
        (save-invite-inmemory invite-w-game))
      updated-invite)))

(defn set-player-not-ready
  "Set player state to :JOINED and invite state to :FULL"
  [invite-id player-id]
  (dosync
    (when-let [invite (get @invites invite-id)]
      (when (= (get-in invite [:playerState player-id]) :READY)
        (let [updated-invite (assoc-in invite [:playerState player-id] :JOINED)]
          (save-invite-inmemory (assoc updated-invite :state :FULL)))))))
