(ns belex-dbg.subs
  (:require
   [re-frame.core :as re-frame]
   [dommy.core :as dommy :refer-macros [sel1]]
   [breaking-point.core :as bp]
   [belex-dbg.diri :as belex]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::diri
 (fn [db _]
   (:diri db)))

(re-frame/reg-sub
 ::seu-layer
 (fn [db _]
   (get-in db [:diri :seu-layer])))

(re-frame/reg-sub
 ::apuc-rsp-fifo
 (fn [db _]
   (get-in db [:diri :apuc-rsp-fifo])))

(re-frame/reg-sub
 ::apuc
 (fn [db _]
   (get-in db [:diri :apuc])))

(re-frame/reg-sub
 ::sm-reg
 (fn [db [_ reg-id]]
   (get-in db [:diri :seu-layer :sm-regs reg-id])))

(re-frame/reg-sub
 ::rn-reg
 (fn [db [_ reg-id]]
   (get-in db [:diri :seu-layer :rn-regs reg-id])))

(re-frame/reg-sub
 ::re-reg
 (fn [db [_ reg-id]]
   (get-in db [:diri :seu-layer :re-regs reg-id])))

(re-frame/reg-sub
 ::ewe-reg
 (fn [db [_ reg-id]]
   (get-in db [:diri :seu-layer :ewe-regs reg-id])))

(re-frame/reg-sub
 ::l1-reg
 (fn [db [_ reg-id]]
   (get-in db [:diri :seu-layer :l1-regs reg-id])))

(re-frame/reg-sub
 ::l2-reg
 (fn [db [_ reg-id]]
   (get-in db [:diri :seu-layer :l2-regs reg-id])))

(re-frame/reg-sub
 ::active-rsp-fifo
 (fn [db [_ apc-id]]
   (get-in db [:diri :apuc-rsp-fifo :active])))

(re-frame/reg-sub
 ::rsp-fifo
 (fn [db [_ apc-id]]
   (get-in db [:diri :apuc-rsp-fifo :queues apc-id])))

(re-frame/reg-sub
 ::apc-rsp-length
 (fn [[_ apc-id]]
   (re-frame/subscribe [::rsp-fifo apc-id]))
 (fn [apc-rsp-fifo _]
   (belex/apc-rsp-length apc-rsp-fifo)))

(re-frame/reg-sub
 ::apc-rsp32k
 (fn [[_ apc-id]]
   (re-frame/subscribe [::rsp-fifo apc-id]))
 (fn [apc-rsp-fifo _]
   (when (pos? (belex/apc-rsp-length apc-rsp-fifo))
     (belex/rd-rsp32k-reg apc-rsp-fifo))))

(re-frame/reg-sub
 ::apc-rsp2k
 (fn [[_ apc-id _]]
   (re-frame/subscribe [::rsp-fifo apc-id]))
 (fn [apc-rsp-fifo [_ _ bank-id]]
   (when (pos? (belex/apc-rsp-length apc-rsp-fifo))
     (belex/rd-rsp2k-reg apc-rsp-fifo bank-id))))

(re-frame/reg-sub
 ::rwin-filter
 (fn [db _]
   (get-in db [:diri :apuc :rwinh-filter])))

(re-frame/reg-sub
 ::vr
 (fn [db [_ row-number]]
   (get-in db [:diri :apuc :vrs row-number])))

(re-frame/reg-sub
 ::rwinh-filter
 (fn [db _]
   (get-in db [:diri :apuc :rwinh-filter])))

(re-frame/reg-sub
 ::rl
 (fn [db _]
   (get-in db [:diri :apuc :rl])))

(re-frame/reg-sub
 ::gl
 (fn [db _]
   (get-in db [:diri :apuc :gl])))

(re-frame/reg-sub
 ::ggl
 (fn [db _]
   (get-in db [:diri :apuc :ggl])))

(re-frame/reg-sub
 ::rsp16
 (fn [db _]
   (get-in db [:diri :apuc :rsp16])))

(re-frame/reg-sub
 ::rsp256
 (fn [db _]
   (get-in db [:diri :apuc :rsp256])))

(re-frame/reg-sub
 ::rsp2k
 (fn [db _]
   (get-in db [:diri :apuc :rsp2k])))

(re-frame/reg-sub
 ::rsp32k
 (fn [db _]
   (get-in db [:diri :apuc :rsp32k])))

(re-frame/reg-sub
 ::lgl
 (fn [db _]
   (get-in db [:diri :apuc :lgl])))

(re-frame/reg-sub
 ::l1
 (fn [db [_ l1-addr]]
   (if (nil? l1-addr)
     (get-in db [:diri :apuc :l1])
     (get-in db [:diri :apuc :l1 l1-addr]))))

(re-frame/reg-sub
 ::l2
 (fn [db [_ l2-addr]]
   (if (nil? l2-addr)
     (get-in db [:diri :apuc :l2])
     (get-in db [:diri :apuc :l2 l2-addr]))))

(re-frame/reg-sub
 ::toggle-active?
 (fn [db [_ toggle-id]]
   (get-in db [:toggle-active? toggle-id])))

(re-frame/reg-sub
 ::active-toggles
 (fn [db _]
   (:active-toggles db)))

(re-frame/reg-sub
 ::h-scroll
 (fn [db [_ key]]
   (or (get-in db [:h-scroll key]) 0)))

(re-frame/reg-sub
 ::num-visible-plats
 (fn [db _]
   (:num-visible-plats db)))

(re-frame/reg-sub
 ::preview-doc
 (fn [db _]
   (:preview-doc db)))

(re-frame/reg-sub
 ::preview-path
 (fn [db _]
   (:preview-path db)))

(defn em->px []
  (-> (sel1 "body")
      js/getComputedStyle
      .-fontSize
      (js/parseInt 10)))

(re-frame/reg-sub
 ::terminal-height
 ;; :<- [::bp/screen-height]
 (fn [db _]
   ;; (min 408 (* screen-height 0.25))
   408))

(re-frame/reg-sub
 ::preview-height
 :<- [::bp/screen-height]
 :<- [::terminal-height]
 (fn [[screen-height terminal-height]]
   (let [num-v-elems 2]
     (- screen-height
        terminal-height
        (* (em->px) (dec num-v-elems))
        (* 2 num-v-elems)))))

(re-frame/reg-sub
 ::fifo-idx
 (fn [db [_ apc-id]]
   (or (get-in db [:fifo-idx apc-id]) 0)))

(re-frame/reg-sub
 ::state
 (fn [db _]
   (:state db)))

(re-frame/reg-sub
 ::prev-state
 (fn [db _]
   (:prev-state db)))

(re-frame/reg-sub
 ::loading-files?
 (fn [db _]
   (seq (:loading-files db))))

(re-frame/reg-sub
 ::terminated?
 (fn [{:keys [state]} _]
   (= state :terminated)))

(re-frame/reg-sub
 ::stopped?
 (fn [{:keys [state]} _]
   (= state :stopped)))

(re-frame/reg-sub
 ::waiting?
 (fn [{:keys [state]} _]
   (some #{state} [:waiting :stepping-over :playing])))

(re-frame/reg-sub
 ::parameters
 (fn [{:keys [parameters]} _]
   parameters))
