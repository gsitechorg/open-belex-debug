(ns belex_dbg.core
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-font-face]]
            [garden.units :refer [em vh vw]]))

(defstyles screen
  [;; WORKAROUND: Since garden wants to reorder the background elements such
   ;; that ":background-size :cover" appears before ":background", I break them
   ;; into two definitions to ensure proper order. Otherwise, Chrome may not
   ;; render the background image correctly.
   [:html
    {:background ["linear-gradient(to right, rgba(39,40,34, 0.95) 0 100%)"
                  "url(/img/copperhead-background.png) no-repeat center center fixed"]}]
   [:html
    {:-webkit-background-size :cover
     :-moz-background-size :cover
     :-o-background-size :cover
     :background-size :cover}]

   [:html>:body
    {:font-family "'Helvetica Neue', Helvetica, Arial, sans-serif"}]

   [:body
    {:background-color :transparent}]

   [(at-font-face
     {:font-family "'Material Icons'"
      :font-style "normal"
      :font-weight 400
      :src ["url(/assets/material-icons/iconfont/MaterialIcons-Regular.eot)" ;; For IE6-8
            "local('Material Icons')"
            "local('MaterialIcons-Regular')"
            "url(/assets/material-icons/iconfont/MaterialIcons-Regular.woff2) format('woff2')"
            "url(/assets/material-icons/iconfont/MaterialIcons-Regular.woff) format('woff')"
            "url(/assets/material-icons/iconfont/MaterialIcons-Regular.ttf) format('truetype')"]})]

   [:.material-icons
    {:font-family "'Material Icons'"
     :font-weight "normal"
     :font-style "normal"
     :font-size (em 1)  ;; Preferred icon size
     :display "inline-block"
     :line-height 1
     :text-transform "none"
     :letter-spacing "normal"
     :word-wrap "normal"
     :white-space "nowrap"
     :direction "ltr"
     ;; Support for all WebKit browsers.
     :-webkit-font-smoothing "antialiased"
     ;; Support for Safari and Chrome.
     :text-rendering "optimizeLegibility"
     ;; Support for Firefox.
     :-moz-osx-font-smoothing "grayscale"}]

   [:#app
    {:padding-left 0
     :padding-right 0}]

   [:.heading
    {:font-weight :bold
     :font-size (em 1.2)}]

   [:.breadcrumb
    {:font-weight :bold
     :font-size (em 1.1)}]

   [:.code
    {:font-family "'DejaVu Sans Mono', monospace"}]

   [:.table-fixed
    {:table-layout :fixed}]

   [:.no-bottom-margin
    {:margin-bottom 0}]

   [:#toggle-tabs>li.nav-item>button
    {:min-width (em 5.45)
     :min-height (em 3.5)}]

   [:.number-range
    {:border "1px solid #686862"
     :border-top "none !important"}]

   [:div.table-view
    {:background-color "#383832"}
    [:div.table-name
     {:min-height (em 3)
      :color "#f8f8f2"
      :display :table-cell
      :vertical-align :middle
      :border "1px solid #686862"
      :border-bottom "none !important"
      :border-radius "0.3em 0.3em 0 0"}
     [:div
      {:margin-top (em 0.75)
       :margin-left (em 0.75)}]]
    [:div.parity-bits
     {:min-height (em 3)
      :color "#f8f8f2"
      :display :table-cell
      :vertical-align :middle
      :border "1px solid #686862"
      :border-top "none !important"
      :border-bottom "none !important"}
     [:div
      {:margin-top (em 0.75)
       :margin-left (em 0.75)}]]
    [:.text-start
     {:padding-left "7px"}]
    [:.text-end
     {:padding-right "7px"}]]

   [:.accordion
    {:--bs-accordion-border-color "#686862"
     :--bs-accordion-active-bg "#383832"
     :--bs-accordion-active-color "#f8f8f2"
     :--bs-accordion-bg "#383832"
     :--bs-accordion-color "#f8f8f2"
     :--bs-accordion-btn-color "#f8f8f2"
     :--bs-accordion-btn-icon "url(\"data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' fill='%23f8f8f2'%3e%3cpath fill-rule='evenodd' d='M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708z'/%3e%3c/svg%3e\")"
     :--bs-accordion-btn-active-icon "url(\"data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' fill='%23f8f8f2'%3e%3cpath fill-rule='evenodd' d='M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708z'/%3e%3c/svg%3e\")"}]

   [:div.accordion-item
    {:--bs-accordion-bg "#49483e"
     :--bs-accordion-color "#f8f8f2"
     :min-height (em 3.5)}]

   [:.lower-value, :.upper-value
    {:min-width (em 5)}]

   [:table.toggle-table
    {:border-color "#686862"}
    [:td
     {:min-width (em 5.45)}]]

   [:td.toggle
    {:color "#f8f8f2"}]

   ["td.toggle:not(.active)", "th.toggle:not(.active)"
    {:background-color "#49483e"}]

   [:td.toggle.active, :th.toggle.active
    {:background-color "#99988e"}]

   [:td.toggle.disabled, :th.toggle.disabled
    {:background-image "repeating-linear-gradient(-45deg, #89887e, #89887e 5px, #49483e 5px, #49483e 10px)"
     :color "#e8e8e2"
     :font-weight :bold
     :-webkit-text-stroke "black"
     :-webkit-text-fill-color "#e8e8e2"}]

   ["td.toggle.disabled:hover", "th.toggle.disabled:hover"
    {:cursor :not-allowed}]

   ["td.toggle:hover:not(.disabled)", "th.toggle:hover:not(.disabled)"
    {:background-color "#89887e"
     :cursor :pointer}]

   ["td.toggle:hover:active:not(.disabled)", "th.toggle:hover:active:not(.disabled)"
    {:background-color "#69685e"}]

   [:table.table-view
    {:background-color "#272822"}
    [:div.cell
     {:min-width (em 1)
      :max-width (em 1)
      :min-height (em 1)
      :max-height (em 1)}]
    [:td :th
     {:padding 0
      :margin 0}]
    [:th
     {:vertical-align :middle}
     [:div.row-num, :div.col-num
      {:font-size (em 0.8)}]]
    [:td.on
     {:background-color "#f8f8f2"}]
    [:td.off
     {:background-color "#272822"}]]

   [:.vertical-rl
    {:writing-mode :vertical-rl}]

   [:.dec-margin-top-1em
    {:margin-top "-1em"}]

   [:#seu-layer-panel
    {:min-width "18em"
     :max-width "18em"}]

   [:#preview-panel
    {:padding "0.5em 0"}]

   [:#preview-panel, :#terminal
    {:border "1px solid #686862"}]

   [:.preview-panel
    {:background-color "rgb(39, 40, 34)"}]

   [:.buffer-index
    {:min-width "1.3em"}]

   [:.nav-tabs
    {:--bs-nav-tabs-link-active-bg "#383832"
     :--bs-nav-link-hover-color "#75715e"
     :--bs-nav-tabs-link-active-border-color "#686862"
     :--bs-nav-tabs-link-hover-border-color "#686862"
     :border-bottom :none
     :border-color "#75715e"}]

   [:.nav-link
    {:color "#75715e"}]

   [".nav-tabs .nav-item.show .nav-link", ".nav-tabs .nav-link.active"
    {:color "#f8f8f2"}]

   [:.table
    {:--bs-table-border-color "#75715e"
     :--bs-table-color "#f8f8f2"
     :--bs-table-hover-bg "#89887e"
     :--bs-table-hover-color "#f8f8f2"}]

   [:#preview-path
    {:min-height "3.35em"
     :padding-left "0.5em"
     :padding-right "0.5em"
     :border "1px solid #686862"
     :border-bottom "none !important"
     :border-radius "0.3em 0.3em 0 0"
     :background-color "#383832"}]

   [:#control-panel
    {:min-height "3.35em"
     :border "1px solid #686862"
     :border-top "none !important"
     :background-color "#383832"}]

   [:.file-nym
    {:color "#f8f8f2"}]

   [:.file-sep
    {:color "#75715e"}]

   [:.btn-primary
    {:--bs-btn-border-color "#686862"
     :--bs-btn-bg "#49483e"
     :--bs-btn-color "#f8f8f2"
     :--bs-btn-hover-bg "#89887e"
     :--bs-btn-hover-color "#f8f8f2"
     :--bs-btn-hover-border-color "#a8a8a2"
     :--bs-btn-active-bg "#99988e"
     :--bs-btn-active-color "#f8f8f2"
     :--bs-btn-disabled-bg "#39382e"
     :--bs-btn-disabled-color "#f8f8f2"
     :--bs-btn-disabled-border-color "#686862"}]

   ["[type=button].disabled:not(.busy)",
    "[type=reset].disabled:not(.busy)",
    "[type=submit].disabled:not(.busy)",
    "button.disabled:not(.busy)"
    {:cursor :not-allowed}]

   ["[type=button].busy",
    "[type=reset].busy",
    "[type=submit].busy",
    "button.busy"
    {:cursor :wait}]

   [:.btn-outline-secondary
    {:--bs-btn-border-color "#686862"
     :--bs-btn-hover-bg "#585852"
     :--bs-btn-hover-color "#f8f8f2"
     :--bs-btn-color "#f8f8f2"
     :--bs-btn-active-bg "#484842"
     :--bs-btn-active-color "#f8f8f2"}]

   ["#step-over-btn>i.bi::before"
    {:transform "rotate(90deg)"}]])
