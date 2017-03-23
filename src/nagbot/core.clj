(ns nagbot.core
  (:require [slack-rtm.core :as rtm]
            [clj-slack.channels :as channels]
            [clj-time.periodic :as p]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [chime :as chime]))

(def token
  "token")

(defonce conn
  (atom nil))

(defonce id-counter (atom 0))

(defn msg-id []
  (swap! id-counter inc))

(def users
  ["srihari" "shafeeq"])

(defn init []
  (reset! conn (rtm/connect token))
  nil)

(defn parse-date-time [dt-str]
  (tf/parse (tf/formatter "yyyyMMddHHmm") dt-str))

(defn month-seq [start-time]
  (p/periodic-seq start-time (t/months 1)))



#_(chime/chime-at (take 5 (p/periodic-seq (t/now) (t/seconds 5)))
                             (fn [time]
                               (println "Chiming at" time)))


(defn find-channel-by-name [channel-name]
  (->> (get-in @conn [:start :channels])
       (filter #(= channel-name (:name_normalized %)))
       first))

(defn find-user-by-name [user-name]
  (->> (get-in @conn [:start :users])
       (filter #(= user-name (:name %)))
       first))

(defn send-message-to-channel [id channel-name text]
  (let [channel-id (:id (find-channel-by-name channel-name))]
    (rtm/send-event (:dispatcher @conn)
                    {:type "message"
                     :channel channel-id
                     :id id
                     :text text})))

(defn mention [user-name]
  (let [user-id (:id (find-user-by-name user-name))]
    (format "<@%s>" user-id)))

(def timesheet-reminders
  {:first-time "Fill up your timesheet today, okay?"
   :ask "Have you filled up your timesheet?"
   :confirm "You've filled up your timesheet, ya? Invoice is ready to send?"})

;; make a list of billable people who need to fill timesheets
;; remind them on the 30th of every month
;; ask (nag) them if they've finished it.
;; if they say yes, confirm;
;; - then stop nagging, and do it again next month
;; if they say no, nag again after a while


(defn tell [user severity]
  (let [msg (format "Hey %s. %s"
                     (mention user)
                     (get timesheet-reminders severity))]
    (send-message-to-channel (msg-id) "bot-test" msg)))
