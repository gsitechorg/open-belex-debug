(ns belex-dbg.diri
  (:require [belex-dbg.utils :refer [zip]]))

(def ^:const NUM_SM_REGS 16)
(def ^:const NUM_RN_REGS 16)
(def ^:const NUM_RE_REGS 4)
(def ^:const NUM_EWE_REGS 4)
(def ^:const NUM_L1_REGS 4)
(def ^:const NUM_L2_REGS 1)

(def ^:const NUM_VRS 24)
(def ^:const NUM_VMRS 48)
(def ^:const NUM_SECTIONS 16)
(def ^:const NUM_GROUPS 4)

(def ^:const NUM_PLATS_PER_HALF_BANK 2048)
(def ^:const NUM_PLATS_PER_BANK (* 2 NUM_PLATS_PER_HALF_BANK))

(def ^:const NUM_HALF_BANKS_PER_APC 8)
(def ^:const NUM_BANKS_PER_APC 4)
(def ^:const NUM_PLATS_PER_APC
  (* NUM_PLATS_PER_HALF_BANK NUM_HALF_BANKS_PER_APC))

(def ^:const NUM_APCS_PER_APUC 2)
(def ^:const NUM_PLATS_PER_APUC
  (* NUM_PLATS_PER_APC NUM_APCS_PER_APUC))

(def ^:const NUM_RSP16_PLATS (/ NUM_PLATS_PER_APUC 16))
(def ^:const NUM_RSP256_PLATS (/ NUM_PLATS_PER_APUC 256))
(def ^:const NUM_RSP2K_PLATS (/ NUM_PLATS_PER_APUC 2048))
(def ^:const NUM_RSP32K_PLATS (/ NUM_PLATS_PER_APUC 32768))

(def ^:const NUM_LGL_PLATS (* 2 NUM_PLATS_PER_BANK))

(def ^:const NUM_L1_ROWS 384)
(def ^:const NUM_L1_PLATS NUM_PLATS_PER_APUC)
(def ^:const NUM_L1_SECTIONS NUM_GROUPS)

(def ^:const VALID_L1_ROWS
  (mapv #(< (mod % 16) 9) (range NUM_L1_ROWS)))

(def ^:const NUM_L2_ROWS 128)
(def ^:const NUM_L2_PLATS NUM_LGL_PLATS)

(def ^:const GSI_APC_PARITY_SET_BITS 3)
(def ^:const GSI_L1_VA_SET_DATA_ADDR_BITS GSI_APC_PARITY_SET_BITS)
(def ^:const GSI_L1_VA_SET_PARITY_ADDR_BITS 1)
(def ^:const GSI_L1_VA_SET_ADDR_BITS
  (+ GSI_L1_VA_SET_DATA_ADDR_BITS GSI_L1_VA_SET_PARITY_ADDR_BITS))
(def ^:const GSI_L1_VA_SET_ADDR_ROWS
  (bit-shift-left 1 GSI_L1_VA_SET_ADDR_BITS))
(def ^:const APL_VM_ROWS_PER_U16 4)

(def ^:const FIFO_CAPACITY 16)

(defn valid-l1-addr? [l1-addr]
  (nth VALID_L1_ROWS l1-addr))

(defn ndarray
  ([shape value]
    (if (seq shape)
      (let [rest-shape (rest shape)]
        (loop [num-pending (first shape)
               arr []]
          (if (pos? num-pending)
            (recur
             (dec num-pending)
             (conj arr (ndarray rest-shape value)))
            arr)))
      value)))

(defrecord SEULayer [rn-regs sm-regs re-regs ewe-regs l1-regs l2-regs])

(defn ->seu-layer
  ([]
   (SEULayer.
    (ndarray [NUM_RN_REGS] 0)
    (ndarray [NUM_SM_REGS] 0)
    (ndarray [NUM_RE_REGS] 0)
    (ndarray [NUM_EWE_REGS] 0)
    (ndarray [NUM_L1_REGS] 0)
    (ndarray [NUM_L2_REGS] 0))))

(defrecord RspFifoMsg [rsp32k rsp2k])

(defn ->rsp-fifo-msg
  ([rsp32k rsp2k]
   (RspFifoMsg. rsp32k rsp2k)))

(defrecord ApcRspFifo [apc-id length cursor buffer])

(defn ->apc-rsp-fifo
  ([apc-id]
   (ApcRspFifo. apc-id 1 -1
    (vec (range FIFO_CAPACITY)))))

(defn apc-rsp-length [apc-rsp-fifo]
  (dec (:length apc-rsp-fifo)))

(defn apc-rsp-head [apc-rsp-fifo]
  (when (pos? (apc-rsp-length apc-rsp-fifo))
    (nth (:buffer apc-rsp-fifo)
         (max 0 (:cursor apc-rsp-fifo)))))

(defn rd-rsp2k-reg [apc-rsp-fifo bank-id]
  (let [rsp-fifo-msg (apc-rsp-head apc-rsp-fifo)
        lower-half-bank bank-id
        upper-half-bank lower-half-bank + 4
        rsp2k (:rsp2k rsp-fifo-msg)
        lower-val (nth rsp2k lower-half-bank)
        upper-val (nth rsp2k upper-half-bank)]
    (bit-or
     (bit-shift-left (js/BigInt upper-val) (js/BigInt 16))
     (js/BigInt lower-val))))

(defn rd-rsp32k-reg [apc-rsp-fifo]
  (let [rsp-fifo-msg (apc-rsp-head apc-rsp-fifo)]
    (:rsp32k rsp-fifo-msg)))

(defrecord ApucRspFifo [queues active])

(defn ->apuc-rsp-fifo
  ([]
   (ApucRspFifo.
    (mapv ->apc-rsp-fifo (range NUM_APCS_PER_APUC))
    nil)))

(defrecord Apuc [vrs rl gl ggl rsp16 rsp256 rsp2k rsp32k l1 l2 lgl
                 rwinh-filter])

(defn ->apuc
  ([]
   (Apuc.
    (ndarray [NUM_VRS NUM_SECTIONS NUM_PLATS_PER_APUC] false)
    (ndarray [NUM_SECTIONS NUM_PLATS_PER_APUC] false)
    (ndarray [NUM_PLATS_PER_APUC] false)
    (ndarray [NUM_GROUPS NUM_PLATS_PER_APUC] false)
    (ndarray [NUM_SECTIONS NUM_RSP16_PLATS] false)
    (ndarray [NUM_SECTIONS NUM_RSP256_PLATS] false)
    (ndarray [NUM_SECTIONS NUM_RSP2K_PLATS] false)
    (ndarray [NUM_SECTIONS NUM_RSP32K_PLATS] false)
    ;; NOTE: Skip invalid L1 addresses to save space ...
    ;; -------------------------------------------------
    ;; (ndarray [NUM_L1_ROWS NUM_L1_SECTIONS NUM_L1_PLATS] false)
    (mapv
     (fn [l1-addr]
       (when (valid-l1-addr? l1-addr)
         (ndarray [NUM_L1_SECTIONS NUM_L1_PLATS] false)))
     (range NUM_L1_ROWS))
    (ndarray [NUM_L2_ROWS NUM_L2_PLATS] false)
    (ndarray [NUM_LGL_PLATS] false)
    (ndarray [NUM_SECTIONS NUM_PLATS_PER_APUC] true))))

(defrecord DIRI [seu-layer apuc-rsp-fifo apuc])

(defn ->diri
  ([]
   (DIRI. (->seu-layer) (->apuc-rsp-fifo) (->apuc))))

(defn load-16->parity-msk [parity-grp]
  (bit-shift-left 0x0808 parity-grp))

(defn store-16->parity-msk [parity-grp]
  (bit-shift-left 0x0001 (* 4 parity-grp)))

(defn vmr->set-ext [vmr]
  (let [parity-set (bit-shift-right vmr 1)
        parity-grp (bit-and vmr 1)
        row (* parity-set GSI_L1_VA_SET_ADDR_ROWS)
        parity-row (+ row (* 2 APL_VM_ROWS_PER_U16))
        row (+ row (* APL_VM_ROWS_PER_U16 parity-grp))]
    {:l1-addr row
     :parity-addr parity-row
     :parity-set parity-set
     :parity-grp parity-grp
     :load-msk (load-16->parity-msk parity-grp)
     :store-msk (store-16->parity-msk parity-grp)}))

(defn vmr->l1-addr [vmr]
  (let [parity-set (bit-shift-right vmr 1)
        parity-grp (bit-and vmr 1)]
    (+ (* parity-set GSI_L1_VA_SET_ADDR_ROWS)
       (* APL_VM_ROWS_PER_U16 parity-grp))))

(defn set-sm-reg
  ([diri reg-id value]
   (assoc-in diri [:seu-layer :sm-regs reg-id] value)))

(defn set-rn-reg
  ([diri reg-id value]
   (assoc-in diri [:seu-layer :rn-regs reg-id] value)))

(defn set-re-reg
  ([diri reg-id value]
   (assoc-in diri [:seu-layer :re-regs reg-id] value)))

(defn set-ewe-reg
  ([diri reg-id value]
   (assoc-in diri [:seu-layer :ewe-regs reg-id] value)))

(defn set-l1-reg
  ([diri reg-id value]
   (assoc-in diri [:seu-layer :l1-regs reg-id] value)))

(defn set-l2-reg
  ([diri reg-id value]
   (assoc-in diri [:seu-layer :l2-regs reg-id] value)))

(defn fifo->enqueue
  ([diri apc-id rsp-fifo-msg]
   (let [apc-rsp-fifo (get-in diri [:apuc-rsp-fifo :queues apc-id])
         index (mod (+ (:cursor apc-rsp-fifo)
                       (:length apc-rsp-fifo))
                    FIFO_CAPACITY)
         length (inc (:length apc-rsp-fifo))
         buffer (assoc (:buffer apc-rsp-fifo) index rsp-fifo-msg)
         apc-rsp-fifo (assoc apc-rsp-fifo :length length :buffer buffer)]
     (-> diri
         (assoc-in [:apuc-rsp-fifo :active] apc-rsp-fifo)
         (assoc-in [:apuc-rsp-fifo :queues apc-id] apc-rsp-fifo)))))

(defn fifo->dequeue
  ([diri apc-id]
   (let [apc-rsp-fifo (get-in diri [:apuc-rsp-fifo :queues apc-id])
         length (dec (:length apc-rsp-fifo))
         cursor (mod (inc (:cursor apc-rsp-fifo)) FIFO_CAPACITY)
         apc-rsp-fifo (assoc apc-rsp-fifo :length length :cursor cursor)]
     (-> diri
         (assoc-in [:apuc-rsp-fifo :active] apc-rsp-fifo)
         (assoc-in [:apuc-rsp-fifo :queues apc-id] apc-rsp-fifo)))))

(defn apply-patch
  ([row patch col-idxs num-cols]
   (let [col-idxs (or col-idxs (range num-cols))]
     (reduce
      (fn [row [col-idx value]]
        (assoc row col-idx value))
      row
      (zip col-idxs patch))))
  ([array patch col-idxs num-cols row-idxs num-rows]
   (let [col-idxs (or col-idxs (range num-cols))
         row-idxs (or row-idxs (range num-rows))]
     (if (or (boolean? patch) (boolean? (first patch)))
       (let [patch (if (boolean? patch)
                     (repeat
                      (count row-idxs)
                      (repeat (count col-idxs) patch))
                     (repeat
                      (count row-idxs) patch))]
         (reduce
          (fn [array [row-idx row-patch]]
            (as-> (nth array row-idx) row
              (apply-patch row row-patch col-idxs num-cols)
              (assoc array row-idx row)))
          array
          (zip row-idxs patch)))
       (reduce
        (fn [array [col-idx col-patch]]
          (reduce
           (fn [array [row-idx value]]
             (assoc-in array [row-idx col-idx] value))
           array
           (zip row-idxs col-patch)))
        array
        (zip col-idxs patch))))))

(defn patch-rwinh
  ([diri plats sections value]
   (update-in diri [:apuc :rwinh-filter]
    apply-patch value plats NUM_PLATS_PER_APUC sections NUM_SECTIONS)))

(defn patch-vr
  ([diri row-number plats sections value]
   (update-in diri [:apuc :vrs row-number]
    apply-patch value plats NUM_PLATS_PER_APUC sections NUM_SECTIONS)))

(defn patch-rl
  ([diri plats sections value]
   (update-in diri [:apuc :rl]
    apply-patch value plats NUM_PLATS_PER_APUC sections NUM_SECTIONS)))

(defn patch-gl
  ([diri plats value]
   (update-in diri [:apuc :gl]
    apply-patch value plats NUM_PLATS_PER_APUC)))

(defn patch-ggl
  ([diri plats groups value]
   (update-in diri [:apuc :ggl]
    apply-patch value plats NUM_PLATS_PER_APUC groups NUM_GROUPS)))

(defn patch-rsp16
  ([diri plats sections value]
   (update-in diri [:apuc :rsp16]
    apply-patch value plats NUM_RSP16_PLATS sections NUM_SECTIONS)))

(defn patch-rsp256
  ([diri plats sections value]
   (update-in diri [:apuc :rsp256]
    apply-patch value plats NUM_RSP256_PLATS sections NUM_SECTIONS)))

(defn patch-rsp2k
  ([diri plats sections value]
   (update-in diri [:apuc :rsp2k]
    apply-patch value plats NUM_RSP2K_PLATS sections NUM_SECTIONS)))

(defn patch-rsp32k
  ([diri plats sections value]
   (update-in diri [:apuc :rsp32k]
    apply-patch value plats NUM_RSP32K_PLATS sections NUM_SECTIONS)))

(defn patch-l1
  ([diri l1-addr plats sections value]
   (update-in diri [:apuc :l1 l1-addr]
    apply-patch value plats NUM_L1_PLATS sections NUM_L1_SECTIONS)))

(defn patch-l2
  ([diri l2-addr plats value]
   (update-in diri [:apuc :l2 l2-addr]
    apply-patch value plats NUM_L2_PLATS)))

(defn patch-lgl
  ([diri plats value]
   (update-in diri [:apuc :lgl]
    apply-patch value plats NUM_LGL_PLATS)))
