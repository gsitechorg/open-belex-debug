(ns belex-dbg.views
  (:require
   [cljs.core.match :refer-macros [match]]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [breaking-point.core :as bp]
   [belex-dbg.diri :as belex]
   [belex-dbg.events :as events]
   [belex-dbg.subs :as subs]))

(defn fmt-hex
  ([num]
   (fmt-hex num 0))
  ([num pad-size]
   (when (not (nil? num))
     (str "0x" (.. num (toString 16) toUpperCase (padStart pad-size "0"))))))

(defn fmt-dec
  ([num]
   (when (not (nil? num))
     (.toString num)))
  ([num _]
   (fmt-dec num)))

(defn toggle-cell
  ([toggle-id & {:keys [label params disabled?]
                 :or {disabled? false}}]
   (let [active? (re-frame/subscribe [::subs/toggle-active? toggle-id])]
     (into
      [:td.toggle.code.text-center
       (merge
        {:id toggle-id
         :class (cond
                  disabled? "disabled"
                  @active? "active"
                  :else "inactive")}
        (when-not disabled?
          {:on-click #(re-frame/dispatch
                       (into
                        [::events/toggle-active toggle-id]
                        params))}))]
      label))))

(defn toggle-reg
  ([toggle-key & toggle-label]
   (let [toggle-id (str (name toggle-key) "-toggle")]
     (toggle-cell toggle-id
                  :label toggle-label
                  :params [toggle-key]))))

(defn vr-toggle [row-number]
  (let [toggle-id (str "vr-" row-number "-toggle")]
    (toggle-cell toggle-id
                 :label ["VR" [:sub row-number]]
                 :params [:vr row-number])))

(defn vmr-toggle [vmr]
  (let [toggle-id (str "vmr-" vmr "-toggle")]
    (toggle-cell toggle-id
                 :label ["VMR" [:sub vmr]]
                 :params [:vmr vmr])))

(defn empty-toggle-cell [col-span]
  [:td.code.text-center.no-border {:col-span col-span}])

(defn apuc-toggle-table []
  [:table#apuc-toggle-table.toggle-table.table.table-bordered.no-bottom-margen
   [:tbody
    (into [:tr] (mapv vr-toggle (range 00 12)))
    (into [:tr] (mapv vr-toggle (range 12 24)))
    (into [:tr] (mapv vmr-toggle (range 00 12)))
    (into [:tr] (mapv vmr-toggle (range 12 24)))
    (into [:tr] (mapv vmr-toggle (range 24 36)))
    (into [:tr] (mapv vmr-toggle (range 36 48)))
    [:tr
     (toggle-reg :rl "RL")
     (toggle-reg :gl "GL")
     (toggle-reg :ggl "GGL")
     (toggle-reg :lgl "LGL")
     (toggle-reg :rsp16 "RSP16")
     (toggle-reg :rsp256 "RSP256")
     (toggle-reg :rsp2k "RSP2K")
     (toggle-reg :rsp32k "RSP32K")
     (toggle-reg :vior "VIOR")
     (toggle-reg :fifo "FIFO")
     (toggle-reg :rwinh "RWINH")
     (empty-toggle-cell 1)]]])

(defn l1-toggle-cell [l1-addr]
  (let [toggle-id (str "l1-" l1-addr "-toggle")]
    (toggle-cell toggle-id
                 :label ["L1" [:sub l1-addr]]
                 :params [:l1 l1-addr]
                 :disabled? (not (belex/valid-l1-addr? l1-addr)))))

(defn l1-toggle-row [l1-addrs]
  (into [:tr] (mapv l1-toggle-cell l1-addrs)))

(defn l1-toggle-table []
  [:table#l1-toggle-table.toggle-table.table.table-bordered.no-bottom-margen
   (into
    [:tbody]
    (mapv l1-toggle-row (partition-all 12 (range belex/NUM_L1_ROWS))))])

(defn l2-toggle-cell [l2-addr]
  (let [toggle-id (str "l2-" l2-addr "-toggle")]
    (toggle-cell toggle-id
                 :label ["L2" [:sub l2-addr]]
                 :params [:l2 l2-addr])))

(defn l2-toggle-row [l2-addrs]
  (into [:tr]
        (cond-> (mapv l2-toggle-cell l2-addrs)
          (< (count l2-addrs) 12)
          (conj (empty-toggle-cell (- 12 (count l2-addrs)))))))

(defn l2-toggle-table []
  [:table#l2-toggle-table.toggle-table.table.table-bordered.no-bottom-margen
   (into
    [:tbody]
    (mapv l2-toggle-row (partition-all 12 (range belex/NUM_L2_ROWS))))])

(defn toggle-tab [tab-id pane-id tab-label active?]
  [:li.nav-item.text-center
   {:role "presentation"}
   [:button.nav-link
    (merge
     {:id tab-id
      :data-bs-toggle "tab"
      :data-bs-target (str "#" (name pane-id))
      :type "button"
      :role "tab"
      :aria-controls pane-id
      :aria-selected active?}
     (when active?
       {:class "active"}))
    tab-label]])

(defn toggle-tabs []
  [:ul#toggle-tabs.nav.nav-tabs
   {:role "tablist"}
   [toggle-tab :apuc-toggle-tab :apuc-toggle-pane "APUC" true]
   [toggle-tab :l1-toggle-tab :l1-toggle-pane "L1" false]
   [toggle-tab :l2-toggle-tab :l2-toggle-pane "L2" false]])

(defn toggle-pane [tab-id pane-id toggle-table active?]
  [:div.tab-pane
   (merge
    {:id pane-id
     :role "tabpanel"
     :aria-labelledby tab-id
     :tabIndex 0}
    (when active?
      {:class "show active"}))
   [toggle-table]])

(defn toggle-panes []
  [:div#toggle-panes.tab-content
   [toggle-pane :apuc-toggle-tab :apuc-toggle-pane apuc-toggle-table true]
   [toggle-pane :l1-toggle-tab :l1-toggle-pane l1-toggle-table false]
   [toggle-pane :l2-toggle-tab :l2-toggle-pane l2-toggle-table false]])

(defn toggle-tables []
  [re-com/v-box
   :src (at)
   :gap "0"
   :children [[toggle-tabs]
              [toggle-panes]]])

(defn number-input [& {:keys [value width class on-change min max]
                       :or {width "5em"
                            class nil
                            min 0
                            max 32768}}]
  [:input
   {:type :number
    :min min
    :max max
    :value value
    :class class
    :width width
    :onChange on-change}])

(defn slider [& {:keys [value width class on-change min max]
                 :or {width "250px"
                      class nil}}]
  [re-com/v-box
   :src (at)
   :gap "0em"
   :min-width width
   :children [[re-com/gap
               :size "1"]
              [:input
               {:type :range
                :min min
                :max max
                :value value
                :width width
                :class class
                :onChange on-change}]
              [re-com/gap
               :size "1"]]])

(defn event->number [event]
  (js/parseInt (.. event -target -value) 10))

(defn number-range [& {:keys [value
                              max-value
                              num-visible
                              width
                              on-change]
                       :or {width "250px"}}]
  (let [lower-value value
        upper-value (dec (+ lower-value num-visible))]
    [re-com/h-box
     :src (at)
     :gap "0em"
     :class "number-range"
     :children
     [[number-input
       :value (str lower-value)
       :class "lower-value code"
       :min 0
       :max (- max-value num-visible)
       :on-change #(on-change (event->number %))
       ]
      [re-com/gap
       :size "1"]
      [slider
       :value lower-value
       :on-change #(on-change (event->number %))
       :width width
       :min 0
       :max (- max-value num-visible)]
      [re-com/gap
       :size "1"]
      [number-input
       :value (str upper-value)
       :class "upper-value code"
       :min (dec num-visible)
       :max (dec max-value)
       :on-change #(on-change (- (inc (event->number %)) num-visible))]]]))

(defn vr-panel [row-number]
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        vr @(re-frame/subscribe [::subs/vr row-number])
        lower-plat @(re-frame/subscribe [::subs/h-scroll [:vr row-number]])
        upper-plat (+ lower-plat num-visible-plats)]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "VR" [:sub row-number]]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section (range belex/NUM_SECTIONS)]
          (let [row (nth vr section)]
            (into
             [:tr.text-end
              [:th [:div.cell [:div.row-num section]]]]
             (for [plat (range lower-plat upper-plat)]
               [:td
                {:class (if (nth row plat) "on" "off")}
                [:div.cell]])))))]
      [number-range
       :value lower-plat
       :max-value belex/NUM_PLATS_PER_APUC
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key [:vr row-number]
                     :val %
                     :max (- belex/NUM_PLATS_PER_APUC
                             num-visible-plats)])]]]))

(defn apc-fifo-row
  ([fn]
   (apc-fifo-row map fn))
  ([map-fn fn]
   (into
    [:tr]
    (map-fn fn (range belex/NUM_APCS_PER_APUC)))))

(defn fifo-title-row []
  (apc-fifo-row
   (fn [apc-id]
     [:th.code.text-center
      {:col-span 16}
      "APC" [:sub apc-id]])))

(defn fifo-length-row []
  (apc-fifo-row
   mapcat
   (fn [apc-id]
     (let [apc-length @(re-frame/subscribe [::subs/apc-rsp-length apc-id])]
       [[:th.code.text-end
         {:col-span 8}
         "Length"]
        [:td.code.text-start
         {:col-span 8}
         apc-length]]))))

(defn fifo-buffer-row []
  (apc-fifo-row
   mapcat
   (fn [apc-id]
     (let [fifo-len @(re-frame/subscribe [::subs/apc-rsp-length apc-id])
           active-fifo-idx @(re-frame/subscribe [::subs/fifo-idx apc-id])]
       (mapv
        (fn [fifo-idx]
          (let [disabled? (>= fifo-idx fifo-len)]
            [:th.code.text-center.toggle.buffer-index
             (merge
              {:class (cond
                        disabled? "disabled"
                        (= fifo-idx active-fifo-idx) "active"
                        :else "inactive")}
              (when-not disabled?
                {:on-click #(re-frame/dispatch
                             [::events/set-fifo-idx apc-id fifo-idx])}))
             fifo-idx]))
        (range belex/FIFO_CAPACITY))))))

(defn fifo-rsp32k-row []
  (apc-fifo-row
   mapcat
   (fn [apc-id]
     (let [apc-rsp32k @(re-frame/subscribe [::subs/apc-rsp32k apc-id])]
       [[:th.code.text-end
         {:col-span 8}
         "RSP32K"]
        [:td.code.text-start
         {:col-span 8}
         (fmt-hex apc-rsp32k 4)]]))))

(defn fifo-rsp2k-rows
  ([]
   (concat
    (list
     (reduce
      into [:tr]
      (for [_ (range belex/NUM_APCS_PER_APUC)]
        [[:th.code.text-end
          {:col-span 6
           :row-span (inc belex/NUM_BANKS_PER_APC)}
          "RSP2K"]
         [:th.code.text-center
          {:col-span 5}
          "Bank ID"]
         [:th.code.text-start
          {:col-span 5}
          "Value"]])))
    (doall
     (map
      (fn [bank-id]
        (into
         [:tr]
         (mapcat
          (fn [apc-id]
            (let [apc-rsp2k @(re-frame/subscribe
                              [::subs/apc-rsp2k apc-id bank-id])]
              [[:th.code.text-center
                {:col-span 5}
                bank-id]
               [:td.code.text-start
                {:col-span 5}
                (fmt-hex apc-rsp2k 8)]]))
          (range belex/NUM_APCS_PER_APUC))))
      (range belex/NUM_BANKS_PER_APC))))))

(defn fifo-panel []
  [re-com/v-box
   :src (at)
   :gap "0em"
   :class "table-view"
   :children
   [[:div.table-name.code
     [:div "FIFO"]]
    [:table.table.table-bordered.table-view.no-bottom-margin
     (into
      [:tbody
       (fifo-title-row)
       (fifo-length-row)
       (fifo-buffer-row)
       (fifo-rsp32k-row)]
      (fifo-rsp2k-rows))]]])

(defn rwinh-panel []
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        rwinh-filter @(re-frame/subscribe [::subs/rwinh-filter])
        lower-plat @(re-frame/subscribe [::subs/h-scroll :rwinh-filter])
        upper-plat (+ lower-plat num-visible-plats)]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "RWINH"]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section (range belex/NUM_SECTIONS)]
          (let [row (nth rwinh-filter section)]
            (into
             [:tr.text-end
              [:th [:div.cell [:div.row-num section]]]]
             (for [plat (range lower-plat upper-plat)]
               [:td
                {:class (if (nth row plat) "on" "off")}
                [:div.cell]])))))]
      [number-range
       :value lower-plat
       :max-value belex/NUM_PLATS_PER_APUC
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key :rwinh-filter
                     :val %
                     :max (- belex/NUM_PLATS_PER_APUC
                             num-visible-plats)])]]]))

(defn rl-panel []
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        rl @(re-frame/subscribe [::subs/rl])
        lower-plat @(re-frame/subscribe [::subs/h-scroll :rl])
        upper-plat (+ lower-plat num-visible-plats)]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "RL"]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section (range belex/NUM_SECTIONS)]
          (let [row (nth rl section)]
            (into
             [:tr.text-end
              [:th [:div.cell [:div.row-num section]]]]
             (for [plat (range lower-plat upper-plat)]
               [:td
                {:class (if (nth row plat) "on" "off")}
                [:div.cell]])))))]
      [number-range
       :value lower-plat
       :max-value belex/NUM_PLATS_PER_APUC
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key :rl
                     :val %
                     :max (- belex/NUM_PLATS_PER_APUC
                             num-visible-plats)])]]]))

(defn gl-panel []
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        gl @(re-frame/subscribe [::subs/gl])
        lower-plat @(re-frame/subscribe [::subs/h-scroll :gl])
        upper-plat (+ lower-plat num-visible-plats)]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "GL"]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        [(into
          [:tr.text-end
           [:th [:div.cell [:div.row-num 0]]]]
          (for [plat (range lower-plat upper-plat)]
            [:td
             {:class (if (nth gl plat) "on" "off")}
             [:div.cell]]))])]
      [number-range
       :value lower-plat
       :max-value belex/NUM_PLATS_PER_APUC
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key :gl
                     :val %
                     :max (- belex/NUM_PLATS_PER_APUC
                             num-visible-plats)])]]]))

(defn ggl-panel []
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        ggl @(re-frame/subscribe [::subs/ggl])
        lower-plat @(re-frame/subscribe [::subs/h-scroll :ggl])
        upper-plat (+ lower-plat num-visible-plats)]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "GGL"]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section (range belex/NUM_GROUPS)]
          (let [row (nth ggl section)]
            (into
             [:tr.text-end
              [:th [:div.cell [:div.row-num section]]]]
             (for [plat (range lower-plat upper-plat)]
               [:td
                {:class (if (nth row plat) "on" "off")}
                [:div.cell]])))))]
      [number-range
       :value lower-plat
       :max-value belex/NUM_PLATS_PER_APUC
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key :ggl
                     :val %
                     :max (- belex/NUM_PLATS_PER_APUC
                             num-visible-plats)])]]]))

(defn lgl-panel []
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        lgl @(re-frame/subscribe [::subs/lgl])
        lower-plat @(re-frame/subscribe [::subs/h-scroll :lgl])
        upper-plat (+ lower-plat num-visible-plats)]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "LGL"]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        [(into
          [:tr.text-end
           [:th [:div.cell [:div.row-num 0]]]]
          (for [plat (range lower-plat upper-plat)]
            [:td
             {:class (if (nth lgl plat) "on" "off")}
             [:div.cell]]))])]
      [number-range
       :value lower-plat
       :max-value belex/NUM_LGL_PLATS
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key :lgl
                     :val %
                     :max (- belex/NUM_LGL_PLATS
                             num-visible-plats)])]]]))

(defn rsp16-panel []
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        rsp16 @(re-frame/subscribe [::subs/rsp16])
        lower-plat @(re-frame/subscribe [::subs/h-scroll :rsp16])
        upper-plat (+ lower-plat num-visible-plats)]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "RSP16"]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section (range belex/NUM_SECTIONS)]
          (let [row (nth rsp16 section)]
            (into
             [:tr.text-end
              [:th [:div.cell [:div.row-num section]]]]
             (for [plat (range lower-plat upper-plat)]
               [:td
                {:class (if (nth row plat) "on" "off")}
                [:div.cell]])))))]
      [number-range
       :value lower-plat
       :max-value belex/NUM_RSP16_PLATS
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key :rsp16
                     :val %
                     :max (- belex/NUM_RSP16_PLATS
                             num-visible-plats)])]]]))

(defn rsp256-panel []
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        rsp256 @(re-frame/subscribe [::subs/rsp256])
        lower-plat @(re-frame/subscribe [::subs/h-scroll :rsp256])
        upper-plat (+ lower-plat num-visible-plats)]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "RSP256"]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section (range belex/NUM_SECTIONS)]
          (let [row (nth rsp256 section)]
            (into
             [:tr.text-end
              [:th [:div.cell [:div.row-num section]]]]
             (for [plat (range lower-plat upper-plat)]
               [:td
                {:class (if (nth row plat) "on" "off")}
                [:div.cell]])))))]
      [number-range
       :value lower-plat
       :max-value belex/NUM_RSP256_PLATS
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key :rsp256
                     :val %
                     :max (- belex/NUM_RSP256_PLATS
                             num-visible-plats)])]]]))

(defn rsp2k-panel []
  (let [rsp2k @(re-frame/subscribe [::subs/rsp2k])
        lower-plat 0
        upper-plat belex/NUM_RSP2K_PLATS
        num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "RSP2K"]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (apply
           conj
           (into
            [:tr [:th [:div.cell]]]
            (for [plat (range lower-plat upper-plat)]
              [:th
               {:row-span 3}
               [:div.cell.vertical-rl.dec-margin-top-1em
                [:div.col-num plat]]]))
           (for [_ (range (- num-visible-plats
                             belex/NUM_RSP2K_PLATS))]
             [:th
              {:row-span 3}
              [:div.cell]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section (range belex/NUM_SECTIONS)]
          (let [row (nth rsp2k section)]
            (apply
             conj
             (into
              [:tr.text-end
               [:th [:div.cell [:div.row-num section]]]]
              (for [plat (range lower-plat upper-plat)]
                [:td
                 {:class (if (nth row plat) "on" "off")}
                 [:div.cell]]))
             (for [_ (range (- num-visible-plats
                               belex/NUM_RSP2K_PLATS))]
               [:th [:div.cell]])))))]]]))

(defn rsp32k-panel []
  (let [rsp32k @(re-frame/subscribe [::subs/rsp32k])
        lower-plat 0
        upper-plat belex/NUM_RSP32K_PLATS
        num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "RSP32K"]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (apply
           conj
           (into
            [:tr [:th [:div.cell]]]
            (for [plat (range lower-plat upper-plat)]
              [:th
               {:row-span 3}
               [:div.cell.vertical-rl.dec-margin-top-1em
                [:div.col-num plat]]]))
           (for [_ (range (- num-visible-plats
                             belex/NUM_RSP32K_PLATS))]
             [:th
              {:row-span 3}
              [:div.cell]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section (range belex/NUM_SECTIONS)]
          (let [row (nth rsp32k section)]
            (apply
             conj
             (into
              [:tr.text-end
               [:th [:div.cell [:div.row-num section]]]]
              (for [plat (range lower-plat upper-plat)]
                [:td
                 {:class (if (nth row plat) "on" "off")}
                 [:div.cell]]))
             (for [_ (range (- num-visible-plats
                               belex/NUM_RSP32K_PLATS))]
               [:th [:div.cell]])))))]]]))

(defn l1-panel [l1-addr]
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        l1 @(re-frame/subscribe [::subs/l1 l1-addr])
        lower-plat @(re-frame/subscribe [::subs/h-scroll [:l1 l1-addr]])
        upper-plat (+ lower-plat num-visible-plats)]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "L1" [:sub l1-addr]]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section (range belex/NUM_GROUPS)]
          (let [row (nth l1 section)]
            (into
             [:tr.text-end
              [:th [:div.cell [:div.row-num section]]]]
             (for [plat (range lower-plat upper-plat)]
               [:td
                {:class (if (nth row plat) "on" "off")}
                [:div.cell]])))))]
      [number-range
       :value lower-plat
       :max-value belex/NUM_PLATS_PER_APUC
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key [:l1 l1-addr]
                     :val %
                     :max (- belex/NUM_PLATS_PER_APUC
                             num-visible-plats)])]]]))

(defn l2-panel [l2-addr]
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        l2 @(re-frame/subscribe [::subs/l2 l2-addr])
        lower-plat @(re-frame/subscribe [::subs/h-scroll [:l2 l2-addr]])
        upper-plat (+ lower-plat num-visible-plats)]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "L2" [:sub l2-addr]]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        [(into
          [:tr.text-end
           [:th [:div.cell [:div.row-num 0]]]]
          (for [plat (range lower-plat upper-plat)]
            [:td
             {:class (if (nth l2 plat) "on" "off")}
             [:div.cell]]))])]
      [number-range
       :value lower-plat
       :max-value belex/NUM_L2_PLATS
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key [:l2 l2-addr]
                     :val %
                     :max (- belex/NUM_L2_PLATS
                             num-visible-plats)])]]]))

;; Involution between VR and VMR sections.
;; VMR sections are interleaves of the corresponding VR sections of size 4.
(def ^:const vr<->vmr
  [0 4 8 12 1 5 9 13 2 6 10 14 3 7 11 15])

(defn vmr-panel [vmr]
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        {:keys [l1-addr parity-addr parity-grp]} (belex/vmr->set-ext vmr)
        l1_0 @(re-frame/subscribe [::subs/l1 l1-addr])
        l1_1 @(re-frame/subscribe [::subs/l1 (+ l1-addr 1)])
        l1_2 @(re-frame/subscribe [::subs/l1 (+ l1-addr 2)])
        l1_3 @(re-frame/subscribe [::subs/l1 (+ l1-addr 3)])
        l1_parity @(re-frame/subscribe [::subs/l1 parity-addr])
        vr (vec (concat l1_0 l1_1 l1_2 l1_3))
        lower-parity-sec parity-grp
        upper-parity-sec (+ 2 lower-parity-sec)
        lower-parity-plat @(re-frame/subscribe
                            [::subs/h-scroll [:vmr-parity vmr]])
        upper-parity-plat (+ lower-parity-plat num-visible-plats)
        lower-plat @(re-frame/subscribe [::subs/h-scroll [:vmr vmr]])
        upper-plat (+ lower-plat num-visible-plats)]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "VMR" [:sub vmr]]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section (range belex/NUM_SECTIONS)]
          (let [row (nth vr (vr<->vmr section))]
            (into
             [:tr.text-end
              [:th [:div.cell [:div.row-num section]]]]
             (for [plat (range lower-plat upper-plat)]
               [:td
                {:class (if (nth row plat) "on" "off")}
                [:div.cell]])))))]
      [number-range
       :value lower-plat
       :max-value belex/NUM_PLATS_PER_APUC
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key [:vmr vmr]
                     :val %
                     :max (- belex/NUM_PLATS_PER_APUC
                             num-visible-plats)])]
      [:div.parity-bits.code
       [:div "Parity Bits (L1" [:sub parity-addr] ")"]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-parity-plat upper-parity-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section [lower-parity-sec upper-parity-sec]]
          (let [row (nth l1_parity section)]
            (into
             [:tr.text-end
              [:th [:div.cell [:div.row-num section]]]]
             (for [plat (range lower-parity-plat upper-parity-plat)]
               [:td
                {:class (if (nth row plat) "on" "off")}
                [:div.cell]])))))]
      [number-range
       :value lower-parity-plat
       :max-value belex/NUM_PLATS_PER_APUC
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key [:vmr-parity vmr]
                     :val %
                     :max (- belex/NUM_PLATS_PER_APUC
                             num-visible-plats)])]]]))

(def ^:const plat+sec->l2
  {[   0   0]   0
   [   0   4]   1
   [   0   8]   2
   [   0  12]   3
   [   0   1]   4
   [   0   5]   5
   [   0   9]   6
   [   0  13]   7
   [   0   2]  16
   [   0   6]  17
   [   0  10]  18
   [   0  14]  19
   [   0   3]  20
   [   0   7]  21
   [   0  11]  22
   [   0  15]  23
   [4096   0]  32
   [4096   4]  33
   [4096   8]  34
   [4096  12]  35
   [4096   1]  36
   [4096   5]  37
   [4096   9]  38
   [4096  13]  39
   [4096   2]  48
   [4096   6]  49
   [4096  10]  50
   [4096  14]  51
   [4096   3]  52
   [4096   7]  53
   [4096  11]  54
   [4096  15]  55
   [8192   0]  64
   [8192   4]  65
   [8192   8]  66
   [8192  12]  67
   [8192   1]  68
   [8192   5]  69
   [8192   9]  70
   [8192  13]  71
   [8192   2]  80
   [8192   6]  81
   [8192  10]  82
   [8192  14]  83
   [8192   3]  84
   [8192   7]  85
   [8192  11]  86
   [8192  15]  87
   [12288  0]  96
   [12288  4]  97
   [12288  8]  98
   [12288 12]  99
   [12288  1] 100
   [12288  5] 101
   [12288  9] 102
   [12288 13] 103
   [12288  2] 112
   [12288  6] 113
   [12288 10] 114
   [12288 14] 115
   [12288  3] 116
   [12288  7] 117
   [12288 11] 118
   [12288 15] 119})

(defn plat->bank [plat]
  (quot
   (cond-> plat
     (> plat belex/NUM_PLATS_PER_APC)
     (- belex/NUM_PLATS_PER_APC))
   belex/NUM_PLATS_PER_BANK))

(defn vr-plat->bank-plat
  ([bank vr-plat]
   (let [lower-bank-plat (* bank belex/NUM_PLATS_PER_BANK)
         bank-plat (- vr-plat lower-bank-plat)]
     (if (> bank-plat belex/NUM_PLATS_PER_BANK)
       (+ (- bank-plat belex/NUM_PLATS_PER_APC)
          belex/NUM_PLATS_PER_BANK)
       bank-plat))))

(defn vior-panel []
  (let [num-visible-plats @(re-frame/subscribe [::subs/num-visible-plats])
        lower-plat @(re-frame/subscribe [::subs/h-scroll :vior])
        upper-plat (+ lower-plat num-visible-plats)
        L2 @(re-frame/subscribe [::subs/l2])]
    [re-com/v-box
     :src (at)
     :gap "0em"
     :class "table-view"
     :children
     [[:div.table-name.code
       [:div "VIOR"]]
      [:table.table.table-bordered.table-view.no-bottom-margin
       (into
        (conj
         [:tbody
          (into
           [:tr [:th [:div.cell]]]
           (for [plat (range lower-plat upper-plat)]
             [:th
              {:row-span 3}
              [:div.cell.vertical-rl.dec-margin-top-1em
               [:div.col-num plat]]]))]
         [:tr [:th [:div.cell]]]
         [:tr [:th [:div.cell]]])
        (for [section (map vr<->vmr (range belex/NUM_SECTIONS))]
          (into
           [:tr.text-end
            [:th [:div.cell [:div.row-num section]]]]
           (for [vr-plat (range lower-plat upper-plat)]
             (let [bank (plat->bank vr-plat)
                   bank-plat (vr-plat->bank-plat bank vr-plat)
                   lower-bank-plat (* bank belex/NUM_PLATS_PER_BANK)
                   l2-addr (plat+sec->l2 [lower-bank-plat section])
                   l2 (nth L2 l2-addr)]
               [:td
                {:class (if (nth l2 bank-plat) "on" "off")}
                [:div.cell]])))))]
      [number-range
       :value lower-plat
       :max-value belex/NUM_PLATS_PER_APUC
       :num-visible num-visible-plats
       :width "50em"
       :on-change #(re-frame/dispatch
                    [::events/h-scroll
                     :key :vior
                     :val %
                     :max (- belex/NUM_PLATS_PER_APUC
                             num-visible-plats)])]]]))

(defn active-panels []
  (let [active-toggles (re-frame/subscribe [::subs/active-toggles])]
    [re-com/v-box
     :src (at)
     :gap "1em"
     :children
     (for [params @active-toggles]
       (match (vec params)
         [:vr row-number] [vr-panel row-number]
         [:rl] [rl-panel]
         [:gl] [gl-panel]
         [:ggl] [ggl-panel]
         [:lgl] [lgl-panel]
         [:rsp16] [rsp16-panel]
         [:rsp256] [rsp256-panel]
         [:rsp2k] [rsp2k-panel]
         [:rsp32k] [rsp32k-panel]
         [:l1 l1-addr] [l1-panel l1-addr]
         [:l2 l2-addr] [l2-panel l2-addr]
         [:vmr vmr] [vmr-panel vmr]
         [:vior] [vior-panel]
         [:fifo] [fifo-panel]
         [:rwinh] [rwinh-panel]
         :else (println "Unsupported active-panel params:" (vec params))))]))

(defn apuc-panel
  ([]
   (let [apuc-height @(re-frame/subscribe [::subs/preview-height])]
     [re-com/scroller
      :min-height (str apuc-height "px")
      :max-height (str apuc-height "px")
      ;; :min-width "65.5em"
      ;; :min-width "65.5em"
      :min-width "67em"
      :max-width "67em"
      :child
      [re-com/h-box
       :src (at)
       :gap "0em"
       :min-width "67em"
       :max-width "67em"
       :children
       [[re-com/gap
         :size "1"]
        [re-com/v-box
         :src (at)
         :gap "1em"
         :children [[toggle-tables]
                    [active-panels]]]
        [re-com/gap
         :size "1"]]]])))

(defn reg-panel
  ([fmt pad-size reg-key reg-pfx reg-id]
   (let [reg-val (re-frame/subscribe [reg-key reg-id])
         fmt-fn (condp = fmt :hex fmt-hex :dec fmt-dec)]
     [:tr
      [:th.code.text-end {:scope "row"} (str reg-pfx reg-id)]
      [:td.code.text-start (fmt-fn @reg-val pad-size)]])))

(defn accordion-panel
  ([& {:keys [accordion-id
              accordion-pfx
              begin-expanded
              title
              content]
       :or {begin-expanded false}}]
   (let [heading-id (str accordion-pfx "heading")
         collapsible-id (str accordion-pfx "collapsible")]
     [:div.accordion-item
      [:h2.accordion-header
       {:id heading-id}
       [:button.accordion-button
        {:class ["accordion-button" (when-not begin-expanded "collapsed")]
         :type "button"
         :data-bs-toggle "collapse"
         :data-bs-target (str "#" collapsible-id)
         :aria-expanded begin-expanded
         :aria-controls collapsible-id}
        title]]
      [:div
       {:id collapsible-id
        :class ["accordion-collapse" "collapse" (when begin-expanded "show")]
        :aria-labelledby heading-id
        :data-bs-parent (str "#" accordion-id)}
       content]])))

(defn regs-panel
  ([& {:keys [accordion-id
              title
              reg-key
              reg-pfx
              num-regs
              begin-expanded
              fmt
              pad-size]
       :or {begin-expanded false
            fmt :hex
            pad-size 4}}]
   (let [accordion-pfx (.toLowerCase reg-pfx)]
     (accordion-panel
      :accordion-id accordion-id
      :accordion-pfx accordion-pfx
      :begin-expanded begin-expanded
      :title title
      :content
      [:table
       {:class ["table"
                "table-bordered"
                "table-hover"
                "accordion-body"
                "no-bottom-margin"
                "table-fixed"]}
       (into
        [:tbody]
        (mapv (partial reg-panel fmt pad-size reg-key reg-pfx)
              (range num-regs)))]))))

(defn sm-regs-panel
  ([accordion-id]
   (regs-panel
    :accordion-id accordion-id
    :title "SM_REGS"
    :reg-key ::subs/sm-reg
    :reg-pfx "SM_REG_"
    :num-regs belex/NUM_SM_REGS
    :begin-expanded true)))

(defn rn-regs-panel
  ([accordion-id]
   (regs-panel
    :accordion-id accordion-id
    :title "RN_REGS"
    :reg-key ::subs/rn-reg
    :reg-pfx "RN_REG_"
    :num-regs belex/NUM_RN_REGS
    :fmt :dec)))

(defn re-regs-panel
  ([accordion-id]
   (regs-panel
    :accordion-id accordion-id
    :title "RE_REGS"
    :reg-key ::subs/re-reg
    :reg-pfx "RE_REG_"
    :num-regs belex/NUM_RE_REGS
    :pad-size 6)))

(defn ewe-regs-panel
  ([accordion-id]
   (regs-panel
    :accordion-id accordion-id
    :title "EWE_REGS"
    :reg-key ::subs/ewe-reg
    :reg-pfx "EWE_REG_"
    :num-regs belex/NUM_EWE_REGS
    :pad-size 3)))

(defn l1-regs-panel
  ([accordion-id]
   (regs-panel
    :accordion-id accordion-id
    :title "L1_REGS"
    :reg-key ::subs/l1-reg
    :reg-pfx "L1_ADDR_REG_"
    :num-regs belex/NUM_L1_REGS
    :fmt :dec)))

(defn l2-regs-panel
  ([accordion-id]
   (regs-panel
    :accordion-id accordion-id
    :title "L2_REGS"
    :reg-key ::subs/l2-reg
    :reg-pfx "L2_ADDR_REG_"
    :num-regs belex/NUM_L2_REGS
    :fmt :dec)))

(defn seu-layer-panel
  ([]
   (let [accordion-id "seu-layer-panel"]
     [:div.accordion.col-1
      {:id accordion-id}
      [sm-regs-panel accordion-id]
      [rn-regs-panel accordion-id]
      [re-regs-panel accordion-id]
      [ewe-regs-panel accordion-id]
      [l1-regs-panel accordion-id]
      [l2-regs-panel accordion-id]])))

(defn register-panel
  ([]
   [re-com/h-box
    :src (at)
    :gap "1em"
    :max-width "85.5em"
    :children [[apuc-panel]
               [seu-layer-panel]]]))

(defn format-path [file-path]
  (as-> file-path formatted-path
    (.substring formatted-path (- (count formatted-path) 60))
    (cond->> formatted-path
      (.includes formatted-path "/")
      (drop-while #(not= "/" %)))
    (apply str formatted-path)
    (.split formatted-path "/")
    (filter seq formatted-path)
    (map #(vector :span.file-nym %) formatted-path)
    (interpose
     [:i.bi.bi-chevron-compact-right.file-sep]
     formatted-path)))

(defn tooltip-btn [& {:keys [btn-id]}]
  (r/create-class
   {:display-name btn-id
    :component-did-mount
    (fn [this]
      (doseq [el (.querySelectorAll
                  (rdom/dom-node this)
                  "[data-bs-toggle='tooltip']")]
        (js/bootstrap.Tooltip. el)))
    :reagent-render
    (fn [& {:keys [btn-id
                   btn-class
                   icon-class
                   tooltip-text
                   on-click
                   disabled?
                   waiting?]
            :or {btn-class "btn-primary"
                 disabled? false
                 waiting? false}}]
      [re-com/v-box
       :src (at)
       :gap "0em"
       :children
       [[re-com/gap
         :size "1"]
        (cond->
         [re-com/button
          :label [:i.bi {:class icon-class}]
          :style {:color "#f8f8f2"}
          :class (str
                  btn-class
                  (when disabled? " disabled")
                  (when waiting? " busy"))
          :attr {:id btn-id
                 :data-bs-toggle "tooltip"
                 :data-bs-placement "bottom"
                 :data-bs-title tooltip-text}]
          (not (or disabled? waiting?))
          (conj :on-click on-click))
        [re-com/gap
         :size "1"]]])}))

(defn copy-path-to-clipboard-btn [preview-path]
  [tooltip-btn
   :btn-id "copy-path-to-clipboard-btn"
   :btn-class "btn-outline-secondary"
   :icon-class "bi-clipboard"
   :tooltip-text "Copy path to clipboard"
   :on-click #(re-frame/dispatch
               [::events/copy-to-clipboard preview-path])])

(defn preview-path-panel [preview-path]
  [re-com/h-box
   :src (at)
   :gap "0em"
   :attr {:id "preview-path"}
   :class "code"
   :children
   [[re-com/v-box
     :src (at)
     :gap "0em"
     :children
     [[re-com/gap
       :size "1"]
      (when preview-path
        [re-com/h-box
         :src (at)
         :gap "0.5em"
         :children
         (format-path preview-path)])
      [re-com/gap
       :size "1"]]]
    [re-com/gap
     :size "1"]
    [copy-path-to-clipboard-btn preview-path]]])

(def ^:const toggle-state-icon
  {:playing "bi-pause"
   :paused "bi-play"})

(defn toggle-state-btn []
  (let [state @(re-frame/subscribe [::subs/state])
        prev-state @(re-frame/subscribe [::subs/prev-state])]
    [tooltip-btn
     :btn-id "play-pause-btn"
     :icon-class (or (toggle-state-icon state)
                     (toggle-state-icon prev-state))
     :tooltip-text "Toggle executation"
     :on-click #(re-frame/dispatch [::events/toggle-state])
     :disabled? (= state :terminated)]))

(defn step-over-btn []
  (let [terminated? @(re-frame/subscribe [::subs/terminated?])
        waiting? @(re-frame/subscribe [::subs/waiting?])]
    [tooltip-btn
     :btn-id "step-over-btn"
     :icon-class "bi-arrow-90deg-right"
     :tooltip-text "Step over intstruction"
     :on-click #(re-frame/dispatch [::events/step-over-instr])
     :disabled? terminated?
     :waiting? waiting?]))

(defn restart-btn []
  (let [stopped? @(re-frame/subscribe [::subs/stopped?])
        terminated? @(re-frame/subscribe [::subs/terminated?])]
    [tooltip-btn
     :btn-id "restart-btn"
     :icon-class "bi-arrow-counterclockwise"
     :tooltip-text "Restart executation"
     :on-click #(re-frame/dispatch [::events/restart-app])
     :disabled? (not stopped?)
     :waiting? (not (or stopped? terminated?))]))

(defn control-panel []
  [re-com/h-box
   :src (at)
   :gap "1em"
   :attr {:id "control-panel"}
   :class "code"
   :children
   [[re-com/gap
     :size "1"]
    [toggle-state-btn]
    [step-over-btn]
    ;; [restart-btn]  ;; FIXME: Determine why this does not work
    [re-com/gap
     :size "1"]]])

(defn preview-panel
  ([]
   (r/create-class
    {:display-name "preview-panel"
     :component-did-update
     (fn [_]
       (re-frame/dispatch [::events/highlight-line]))
     :reagent-render
     (fn []
       (let [preview-doc @(re-frame/subscribe [::subs/preview-doc])
             preview-height @(re-frame/subscribe [::subs/preview-height])
             preview-path @(re-frame/subscribe [::subs/preview-path])]
         [re-com/v-box
          :src (at)
          :gap "0em"
          :min-width "60em"
          :max-width "60em"
          :min-height (str preview-height "px")
          :max-height (str preview-height "px")
          :class "preview-panel"
          :children
          [[preview-path-panel preview-path]
           [re-com/scroller
            :attr {:id "preview-panel"}
            :child
            [:div.code
             {:dangerouslySetInnerHTML {:__html preview-doc}}]]
           [control-panel]]]))})))

(defn console-panel
  ([]
   (let [terminal-height @(re-frame/subscribe [::subs/terminal-height])]
     [re-com/box
      :min-height (str terminal-height "px")
      :max-height (str terminal-height "px")
      :child
      [:div#terminal]])))

(defn main-panel []
  (let [screen-width @(re-frame/subscribe [::bp/screen-width])
        screen-height @(re-frame/subscribe [::bp/screen-height])]
    [re-com/h-box
     :src (at)
     :gap "1em"
     :min-width (str screen-width "px")
     :max-width (str screen-width "px")
     :min-height (str screen-height "px")
     :max-height (str screen-height "px")
     :children
     [[re-com/gap
       :size "1"]
      [re-com/v-box
       :src (at)
       :gap "1em"
       :attr {:id "main-panel"}
       :children
       [[re-com/h-box
         :src (at)
         :gap "1em"
         :children
         [[preview-panel]
          [register-panel]]]
        [console-panel]]]
      [re-com/gap
       :size "1"]]]))
