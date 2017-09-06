(ns reading-game.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.reader :as reader]))

(enable-console-print!)

(println "This text is printed from src/reading-game/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload


(defn set-data [k v]
  ;(println "writing" k v)
  (.setItem (.-localStorage js/window) (str k) (pr-str v)))

(defn get-data
  ([k] (get-data k nil))
  ([k default]
   ;(println "reading" k)
   (or (doto (reader/read-string (or (.getItem (.-localStorage js/window) (str k)) "nil"))
         println)
       default)))

(defn empty-profile []
  {:draw    []
   :discard []})

(defn empty-state []
  {"default"
   (assoc (empty-profile)
     :draw ["cat" "sat" "on" "the" "mat"]
     :word "a")})

(defonce state
         (do
           (.addEventListener js/document "visibilitychange"
                              (fn [] (swap! state #(or % (get-data :state (empty-state))))))
           (add-watch (atom
                        (get-data :state (empty-state)))
                        :local-storage
                        (fn [_ _ _ new-state]
                          (set-data :state new-state)))))

(defonce profile (atom "default"))                          ; TODO: keep in local storage?

(defn print-profile []
  (println (get @state @profile)))


(defn pick-word [{:keys [draw word discard plays]}]
  (let [[next-word & next-draw] (shuffle draw)]
    {:draw    next-draw
     :word    next-word
     :discard (vec (remove nil? (conj discard word)))
     :plays   (if plays (cons :correct plays) [])}))

(defn pick-word-again [{:keys [draw word discard plays]}]
  (let [[next-word & next-draw] (concat (shuffle draw) [word])]
    {:draw    next-draw
     :word    next-word
     :discard discard
     :plays   (cons :skip plays)}))

(defn restack [{:keys [draw word discard]}]
  {:draw (vec (set (remove nil? (concat [word] draw discard))))})

(defn add-word [profile word]
  (pick-word {:draw    (conj (:discard profile) word)
              :discard []})
  {:draw    []
   :discard (conj (:discard profile) word)})

(defn finish [{:keys [draw word discard plays]}]
  {:discard (concat draw [word] discard)
   :plays plays})

(defn add-word! []
  (let [word (.-value (.getElementById js/document "new-word"))]
    (set! (.-value (.getElementById js/document "new-word")) "")
    (swap! state update @profile add-word word)))

(defn remove-word! [word]
  (swap! state update-in [@profile :discard] #(vec (remove #{word} %))))

(defn go! []
  (swap! state update @profile #(-> % restack pick-word))
  (print-profile))

(defn correct! []
  (swap! state update @profile pick-word)
  (print-profile))

(defn wrong! []
  (swap! state update @profile pick-word-again)
  (print-profile))

(defn finish! []
  (swap! state update @profile finish))

(defn set-profile! [profile-name]
  (reset! profile profile-name)
  (swap! state update profile-name #(or % (empty-profile))))

(defn pick-profile! [event]
  (let [selected-profile (.-value (.-target event))
        new-profile (if (= selected-profile "add-new")
                      (js/prompt "Enter your name:" "")
                      selected-profile)]
    (set-profile! new-profile)))

(defn profile-select []
  [:div
   [:select {:value     @profile
             :on-change pick-profile!}
    (cons
      [:option {:key "add-new" :value "add-new"} "Create new profile..."]
      (for [k (keys @state)]
        [:option {:key k :value k} k]))]
   " "
   [:input {:type     "button"
            :value    (str "Delete " @profile)
            :on-click #(do
                         (swap! state dissoc @profile)
                         (set-profile! "default"))}]
   ])

(defn hello-world []

  (let [{:keys [word draw discard plays]} (get @state @profile)]

    (println @profile)
    (println @state)

    (println word draw discard)

    [:div
     [:h1 {:class "white"} "Reading Game"]

     [profile-select] [:br]

     (if word
       [:div {:id "the-word-holder"}
        [:div {:id "the-word"} word]
        [:input {:key "correct" :class "big green" :type "button" :value "Correct!" :on-click correct!}]
        [:input {:key "wrong" :class "big red" :type "button" :value "Skip" :on-click wrong!}]

        [:div {:class "plays"}
         (for [[idx play] (map-indexed vector (reverse plays))]

           (if (= play :correct)
             [:span {:key   idx
                     :class "play"} "\u2B50"]
             [:span {:key   idx
                     :class "play"} "\u2639" ])

           )]

        [:div
         [:input {:key "wrong" :class "red" :type "button" :value "Skip to End" :on-click finish!}]]

        ]



       [:div {:id "the-input-holder"}

        [:input {:key "new-word" :id "new-word" :type "text"}]
        [:input {:key "add-button" :class "medium green" :type "button" :value "Add word" :on-click add-word!}]

        [:br]
        [:input {:key "add-button" :class "big green" :type "button" :value "Go!" :on-click go!}]

        [:br]
        [:span {:class "white"}
         "Remove: "

         (for [word discard]

           [:span {:key      (str "remove-" word)
                   :class    "remove-word"
                   :on-click #(remove-word! word)} word]

           )

         ]]


       )


     ])

  )

(reagent/render-component [hello-world]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
