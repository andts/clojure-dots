(ns dots.core.invites
  (:require [dots.core.player :as player]
            [dots.field :as field]
            [dots.util :as util]
            [dots.db :as db]
            [clojure.data]))

(def invites
  "invite structure
  {
    :invite-id     123
    :name          \"super-game\"
    :width         10
    :height        10
    :player1-id    123 ;author's id
    :player1-color #{:red :blue}
    :player2-id    123 ;opponent's id
    :state         #{:open :full :starting :closed}
    :game-id       123 ;after both players ready and invite is closed
    :search-filter {:player-id 123 ;if we want to play with exact player
                    :skill     5 ;if we wanr to play with player of some skill level
                    :region    \"ukr\" ;if we want to play with player from some country (http://en.wikipedia.org/wiki/ISO_3166-1_alpha-3)
                    }
    }"
  (ref {}))

(defn save-invite-inmemory
  [invite]
  (util/save-inmemory invites :invite-id invite)
  invite)

(defn create-invite
  [player-id name width height color filter]
  (dosync
    (let [invite {:invite-id     (util/get-next-index @invites)
                  :name          name
                  :width         width
                  :height        height
                  :player1-id    player-id
                  :player1-color color
                  :search-filter filter
                  :state         :open}]
      (save-invite-inmemory invite))))

(defn filter-matches? [filter player]
  "Check if search filter matches player"
  (let [conditions (keys filter)
        comparison (clojure.data/diff filter player)
        matching (nthnext comparison 2)]
    (and (= (count conditions) (count matching)))))

(defn find-invites
  "Find invites with search-filter matching current players skill or region
  (or other possible conditions in future) and :state :open"
  [player]
  (dosync
    (filter (fn [invite] (and (filter-matches? (invite :search-filter) player) (= (invite :state) :open)))
            @invites)))

(defn join-invite
  "Try to join an invite. If it's :state is :open - joins and returns the invite,
  otherwise - returns false."
  [invite-id player-id]
  (dosync
    (when-let [invite (get @invites invite-id)]
      (when (= (invite :state) :open)
        (do
          (save-invite-inmemory (assoc invite :player2-id player-id :state :full)))))))

(defn leave-invite
  "Leave invite. If player is the author - remove invite entirely,
  if player is opponent - clear the field and change :state back to :open."
  [invite-id player-id]
  (dosync
    ;...
    ))

(defn set-player-ready
  "Change invite :state to :starting"
  [invite-id player-id]
  (dosync
    (when-let [invite (get @invites invite-id)]
      (when (and (= (invite :state) :open) (or (= (invite :player1-id) player-id) (= (invite :player2-id) player-id)))
        (do
          (save-invite-inmemory (assoc invite :state :starting)))))))
