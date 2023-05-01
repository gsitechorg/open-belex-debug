(ns belex-dbg.events
  (:require
   [re-frame.core :as re-frame]
   ;; [day8.re-frame.tracing :refer-macros [fn;-traced]]
   [dommy.core :as dommy :refer-macros [sel1]]
   [belex-dbg.config :as config]
   [belex-dbg.db :as db]
   [belex-dbg.diri :as belex]
   [belex-dbg.sockets :as sockets]))

;; NOTE: Event handlers defined as `fn;-traced` are available for use with
;; re-frame-10x. To use them, rename `fn;-traced` as `fn;-traced`.
;; NOTE: Event handlers defined as `fn;;-traced` are handlers that break
;; re-frame-10x.

(re-frame/reg-event-db
 ::initialize-db
 (fn;-traced
  [_ _]
  db/default-db))

(re-frame/reg-fx
 ::start-socket-fx
 (fn []
   (sockets/start!)))

(re-frame/reg-event-fx
 ::start-socket
 (fn;-traced
  [_ _]
  {:fx [[::start-socket-fx]]}))

(re-frame/reg-fx
 ::stop-socket-fx
 (fn []
   (sockets/stop!)))

(re-frame/reg-event-fx
 ::stop-socket
 (fn;-traced
  [_ _]
  {:fx [[::stop-socket-fx]]}))

(defn apuc-event? [[event-nym]]
  (.startsWith event-nym "diri::"))

(defn seu-event? [[event-nym]]
  (.startsWith event-nym "seu::"))

(defn fifo-event? [[event-nym]]
  (.startsWith event-nym "fifo::"))

(defn diri-event? [[event-nym]]
  (or
   (.startsWith event-nym "seu::")
   (.startsWith event-nym "fifo::")
   (.startsWith event-nym "diri::")))

(defn instr-event? [[event-nym]]
  (= event-nym "diri::batch"))

(defn file-load-event? [[event-nym]]
  (.endsWith event-nym "::enter"))

(re-frame/reg-fx
 ::poll-app-event-fx
 (fn []
   (sockets/poll-app-event)))

(re-frame/reg-event-fx
 ::poll-app-event
 (fn;-traced
   [{:keys [db]
     {:keys [event-buffer socket-state]} :db} _]
   (when (and
          (< (count event-buffer) config/event-buffer-capacity)
          (= socket-state :ready))
     {:db (assoc db :socket-state :pending)
      :fx [[::poll-app-event-fx]]})))

(re-frame/reg-event-db
 ::update-parameters
 (fn;-traced
   [db [_ parameters]]
   (assoc db :parameters parameters)))

(defmulti handle-app-event first)

(defmethod handle-app-event "stdout" [[_ line]]
  [[:dispatch [::write-line line]]])

(defmethod handle-app-event "stderr" [[_ line]]
  [[:dispatch [::write-line line]]])

(defmethod handle-app-event "fragment::enter"
  [[_ [file-path line-number parameters]]]
  [[:dispatch [::load-file file-path line-number]]
   [:dispatch [::update-parameters parameters]]])

(defmethod handle-app-event "multi_statement::enter"
  [[_ [file-path line-number]]]
  [[:dispatch [::load-file file-path line-number]]])

(defmethod handle-app-event "statement::enter"
  [[_ [file-path line-number]]]
  [[:dispatch [::load-file file-path line-number]]])

(defmethod handle-app-event "app::start" [_]
  [[:dispatch [::start-app]]])

(defmethod handle-app-event "app::stop" [_]
  [[:dispatch [::stop-app]]])

(defmethod handle-app-event :default [event]
  [[:dispatch [::update-diri event]]])

(re-frame/reg-event-fx
 ::pop-app-event
 (fn;-traced
   [{:keys [db]
     {:keys [event-buffer state prev-state]} :db}
    _]
   (when-let [event (peek event-buffer)]
     (cond-> {:db (assoc db :event-buffer (pop event-buffer))
              :fx (handle-app-event event)}
       (and (file-load-event? event)
            (some #{state} [:waiting :stepping-over]))
       (update :db assoc :state prev-state :prev-state nil)
       (or (not (file-load-event? event))
           (some #{:playing} [state prev-state]))
       (update :fx conj [:dispatch [::handle-app-event]])
       :true
       (update :fx conj [:dispatch [::poll-app-event]])))))

(re-frame/reg-event-fx
 ::handle-app-event
 (fn;-traced
  [{:keys [db]
    {:keys [event-buffer state prev-state next-diri]} :db}
   _]
  (when-let [event (peek event-buffer)]
    (cond-> {:db db :fx []}
      (file-load-event? event)
      (update :fx conj [:dispatch [::prefetch-file event]])
      (and (diri-event? event)
           (nil? next-diri))
      (update :fx conj [:dispatch [::prepare-diri event]])
      (or (not (instr-event? event))
          (some #{:playing :stepping-over} [state prev-state]))
      (update :fx conj [:dispatch [::pop-app-event]])))))

(re-frame/reg-event-fx
 ::push-app-event
 (fn;-traced
   [{:keys [db]
     {:keys [event-buffer]} :db}
    [_ event]]
   (cond-> {:db (assoc db
                       :event-buffer (conj event-buffer event)
                       :socket-state :ready)
            :fx []}
     (file-load-event? event)
     (update :fx conj [:dispatch [::prefetch-file event]])
     true
     (update :fx conj
             [:dispatch [::handle-app-event]]
             [:dispatch [::poll-app-event]]))))

(re-frame/reg-fx
 ::open-terminal-fx
 (fn [terminal]
   (let [terminal-el (sel1 :#terminal)]
     (.open terminal terminal-el))))

(re-frame/reg-event-fx
 ::open-terminal
 (fn;-traced
  [{{:keys [terminal]} :db} _]
  {::open-terminal-fx terminal}))

(re-frame/reg-fx
 ::write-line-fx
 (fn [[terminal line]]
   (.write terminal line)))

(re-frame/reg-event-fx
 ::write-line
 (fn;-traced
   [{{:keys [terminal]} :db} [_ line]]
   {:fx [[::write-line-fx [terminal line]]]}))

(re-frame/reg-event-fx
  ::navigate
  (fn;-traced
   [_ [_ handler]]
   {:navigate handler}))

(defmulti dispatch-diri-event
  (fn [_ event]
    (first event)))

(defmethod dispatch-diri-event "seu::sm_reg" [db [_ reg-id value]]
  (update db :next-diri belex/set-sm-reg reg-id value))

(defmethod dispatch-diri-event "seu::rn_reg" [db [_ reg-id value]]
  (update db :next-diri belex/set-rn-reg reg-id value))

(defmethod dispatch-diri-event "seu::re_reg" [db [_ reg-id value]]
  (update db :next-diri belex/set-re-reg reg-id value))

(defmethod dispatch-diri-event "seu::ewe_reg" [db [_ reg-id value]]
  (update db :next-diri belex/set-ewe-reg reg-id value))

(defmethod dispatch-diri-event "seu::l1_addr_reg" [db [_ reg-id value]]
  (update db :next-diri belex/set-l1-reg reg-id value))

(defmethod dispatch-diri-event "seu::l2_addr_reg" [db [_ reg-id value]]
  (update db :next-diri belex/set-l2-reg reg-id value))

(defmethod dispatch-diri-event "fifo::enqueue" [db [_ apc-id [rsp32k rsp2k] _]]
  (let [rsp-fifo-msg (belex/->rsp-fifo-msg rsp32k rsp2k)]
    (update db :next-diri belex/fifo->enqueue apc-id rsp-fifo-msg)))

(defmethod dispatch-diri-event "fifo::dequeue" [db [_ apc-id _]]
  (update db :next-diri belex/fifo->dequeue apc-id))

(defmethod dispatch-diri-event "diri::rw_inh_filter"
  [db [_ plats sections value]]
  (update db :next-diri belex/patch-rwinh plats sections value))

(defmethod dispatch-diri-event "diri::vr"
  [db [_ row-number plats sections value]]
  (update db :next-diri belex/patch-vr row-number plats sections value))

(defmethod dispatch-diri-event "diri::rl" [db [_ plats sections value]]
  (update db :next-diri belex/patch-rl plats sections value))

(defmethod dispatch-diri-event "diri::gl" [db [_ plats value]]
  (update db :next-diri belex/patch-gl plats value))

(defmethod dispatch-diri-event "diri::ggl" [db [_ plats groups value]]
  (update db :next-diri belex/patch-ggl plats groups value))

(defmethod dispatch-diri-event "diri::rsp16" [db [_ plats sections value]]
  (update db :next-diri belex/patch-rsp16 plats sections value))

(defmethod dispatch-diri-event "diri::rsp256" [db [_ plats sections value]]
  (update db :next-diri belex/patch-rsp256 plats sections value))

(defmethod dispatch-diri-event "diri::rsp2k" [db [_ plats sections value]]
  (update db :next-diri belex/patch-rsp2k plats sections value))

(defmethod dispatch-diri-event "diri::rsp32k" [db [_ plats sections value]]
  (update db :next-diri belex/patch-rsp32k plats sections value))

(defmethod dispatch-diri-event "diri::l1" [db [_ l1-addr plats sections value]]
  (update db :next-diri belex/patch-l1 l1-addr plats sections value))

(defmethod dispatch-diri-event "diri::l2" [db [_ l2-addr plats value]]
  (update db :next-diri belex/patch-l2 l2-addr plats value))

(defmethod dispatch-diri-event "diri::lgl" [db [_ plats value]]
  (update db :next-diri belex/patch-lgl plats value))

(defmethod dispatch-diri-event "diri::batch" [db [_ batch]]
  (reduce dispatch-diri-event db batch))

(defmethod dispatch-diri-event :default [db event]
  (println "WARNING :: Unknown event type:" (first event))
  (js/console.log event)
  (assoc db :state :terminated))

(re-frame/reg-event-fx
 ::prepare-diri
 (fn;-traced
  [{:keys [db]
    {:keys [diri]} :db}
   [_ event]]
  (let [{:keys [state] :as db}
        (dispatch-diri-event (assoc db :next-diri diri) event)]
    (cond-> {:db db :fx []}
      (= state :terminated)
      (update :fx conj [:dispatch [::stop-socket]])))))

(re-frame/reg-event-fx
 ::prepare-next-diri
 (fn;;-traced
   [{:keys [db]
     {:keys [event-buffer next-diri]} :db}
    _]
   (when (and (nil? next-diri) (seq event-buffer))
     (loop [event (peek event-buffer)
            event-buffer (pop event-buffer)]
       (cond
         (diri-event? event)
         {:fx [[:dispatch [::prepare-diri event]]]}
         (seq event-buffer)
         (recur
          (peek event-buffer)
          (pop event-buffer))
         :else nil)))))

(re-frame/reg-event-fx
 ::update-diri
 (fn;-traced
  [{:keys [db]
    {:keys [next-diri]} :db}
   _]
  (when next-diri
    {:db (assoc db :diri next-diri :next-diri nil)
     :fx [[:dispatch [::prepare-next-diri]]]})))

(re-frame/reg-fx
 ::load-file-fx
 (fn [file-path]
   (sockets/load-file-source file-path)))

(defn collect-line [term]
  (let [frag (js/DocumentFragment.)]
    (loop [term term]
      (if (or (nil? term) (= "A" (.-nodeName term)))
        frag
        (let [next-term (.-nextSibling term)]
          (.append frag term)
          (recur next-term))))))

(re-frame/reg-fx
 ::highlight-line-fx
 (fn [line-number]
   (doseq [old-line (js/document.getElementsByClassName "hll")]
     (let [unwrapped (js/DocumentFragment.)]
       (while (.hasChildNodes old-line)
         (.append unwrapped (.-firstChild old-line)))
       (.replaceWith old-line unwrapped)))
   (when-let [new-line-anchor (sel1 (str "#line-" line-number))]
     (let [preview-panel (sel1 "#preview-panel")
           new-line (js/document.createElement "span")]
       (.. new-line -classList (add "hll"))
       (.appendChild new-line
                     (collect-line
                      (.-nextSibling new-line-anchor)))
       (.after new-line-anchor new-line)
       (set! ;; center the preview-panel on the highlighted line
        (.-scrollTop preview-panel)
        (- (.-offsetTop new-line-anchor)
           (/ (.-clientHeight preview-panel) 2)))))))

(re-frame/reg-event-fx
 ::highlight-line
 (fn;-traced
  [{{:keys [preview-line]} :db} [_ file-line]]
  (when (or file-line preview-line)
    {::highlight-line-fx (or file-line preview-line)})))

(re-frame/reg-event-fx
 ::load-file
 (fn;;-traced
   [{:keys [db]
     {:keys [files loading-files]
      loading-file? :loading-files} :db}
    [_ file-path line-number]]
   (cond
     ;; Case 1: file has already been loaded
     (contains? files file-path)
     (cond-> {:db (assoc db :preview-path file-path
                         :preview-line line-number
                         :preview-doc (files file-path))
              :fx [[::highlight-line-fx line-number]]})
     ;; Case 2: file is being loaded
     (loading-file? file-path)
     {:db (assoc db :preview-path file-path
                 :preview-line line-number)}
     ;; Case 3: file needs to be loaded
     :else
     {:db (assoc db :preview-path file-path
                 :preview-line line-number
                 :loading-files (conj loading-files file-path))
      :fx [[::load-file-fx file-path]]})))

(re-frame/reg-event-fx
 ::prefetch-file
 (fn;;-traced
   [{:keys [db]
     {:keys [loading-files files]
      loading-file? :loading-files} :db}
    [_ [_ [file-path]]]]
   (when (and (not (contains? files file-path))
              (not (loading-file? file-path)))
     {:db (assoc db :loading-files (conj loading-files file-path))
      :fx [[::load-file-fx file-path]]})))

(re-frame/reg-event-fx
 ::store-file
 (fn;-traced
  [{:keys [db]
    {:keys [preview-path preview-line]} :db}
   [_ file-path file-html]]
  (cond-> {:db (-> db
                   (assoc-in [:files file-path] file-html)
                   (update :loading-files disj file-path))}
    (= file-path preview-path)
    (merge
     {:fx [[:dispatch [::load-file file-path preview-line]]]}))))

(re-frame/reg-event-db
 ::toggle-active
 (fn;-traced
  [db [_ toggle-id & params]]
  (let [active? (not (get-in db [:toggle-active? toggle-id]))]
    (-> db
        (assoc-in [:toggle-active? toggle-id] active?)
        (update :active-toggles
                #(if active?
                   (conj % params)
                   (vec (remove #{params} %))))))))

(defn clamp
  ([value upper]
   (clamp value 0 upper))
  ([value lower upper]
   (cond
     (js/isNaN value) lower
     (< value lower) lower
     (> value upper) upper
     :else value)))

(re-frame/reg-event-db
 ::h-scroll
 (fn;-traced
  [db [_ & {:keys [key val max]}]]
  (assoc-in db [:h-scroll key] (clamp val max))))

(re-frame/reg-event-db
 ::set-fifo-idx
 (fn;-traced
  [db [_ apc-id fifo-idx]]
  (assoc-in db [:fifo-idx apc-id] fifo-idx)))

;; see: https://gist.github.com/rotaliator/73daca2dc93c586122a0da57189ece13
(re-frame/reg-fx
 ::copy-to-clipboard-fx
 (fn [val]
   (let [el (js/document.createElement "textarea")]
     (set! (.-value el) val)
     (.appendChild js/document.body el)
     (.select el)
     (js/document.execCommand "copy")
     (.removeChild js/document.body el))))

(re-frame/reg-event-fx
 ::copy-to-clipboard
 (fn;-traced
  [_ [_ value]]
  {::copy-to-clipboard-fx value}))

(def ^:const toggle-state
  {:playing :paused
   :paused :playing
   :terminated :terminated
   :stopped :stopped})

(re-frame/reg-event-fx
 ::toggle-state
 (fn;-traced
  [{:keys [db]
    {:keys [state]
     {timestamp-ms :toggle-state} :timestamp-ms
     {debounce-ms :toggle-state
      :or {debounce-ms 200}} :debounce-ms} :db}
   _]
  (when (or (nil? timestamp-ms)
            (> (- (js/Date.now) timestamp-ms) debounce-ms))
    (cond-> {:db (assoc-in db [:timestamp-ms :toggle-state] (js/Date.now))
             :fx []}
      (not-any? #{state} [:waiting :stepping-over])
      (update-in [:db :state] toggle-state)
      (some #{state} [:waiting :stepping-over])
      (update-in [:db :prev-state] toggle-state)
      (= state :paused)
      (update :fx conj [:dispatch [::handle-app-event]])))))

(re-frame/reg-event-fx
 ::step-over-instr
 (fn;-traced
  [{:keys [db]
    {:keys [state]
     {timestamp-ms :step-over-instr} :timestamp-ms
     {debounce-ms :step-over-instr
      :or {debounce-ms 200}} :debounce-ms} :db}
   _]
  (when (and (= state :paused)
             (or (nil? timestamp-ms)
                 (> (- (js/Date.now) timestamp-ms) debounce-ms)))
    {:db (-> db
             (assoc :state :stepping-over :prev-state state)
             (assoc-in [:timestamp-ms :step-over-instr] (js/Date.now)))
     :fx [[:dispatch [::handle-app-event]]]})))

(re-frame/reg-fx
 ::restart-app-fx
 (fn []
   (sockets/restart-app)))

(re-frame/reg-event-fx
 ::restart-app
 (fn;-traced
  [_ _]
  {:fx [[::restart-app-fx]]}))

(re-frame/reg-event-db
 ::start-app
 (fn [{:keys [state prev-state] :as db} _]
   (assoc db
          :diri db/default-diri
          :next-diri nil
          :state :waiting
          :prev-state (or prev-state state))))

(re-frame/reg-event-db
 ::stop-app
 (fn [{:keys [state prev-state] :as db} _]
   (assoc db
          :prev-state (or prev-state state)
          :state :stopped)))
