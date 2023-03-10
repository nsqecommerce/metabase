(ns metabase.domain-entities.card
  "Functions for building, updating and querying Cards (that is, Metabase questions)."
  (:require
   [metabase.domain-entities.malli :as de]
   [metabase.lib.schema :as lib.schema]
   [metabase.util.malli :as mu]
   #?@(:cljs ([metabase.domain-entities.converters :as converters]
              [metabase.lib.metadata.frontend :as lib.metadata.frontend]
              [metabase.lib.query :as lib.query]
              [metabase.lib.util :as lib.util]
              [metabase.mbql.normalize :as mbql.normalize]))))

;;; ------------------------------- JS<->CLJS conversion helpers ---------------------------------
#?(:cljs
   (def ^:private ^:dynamic *metadata-provider* nil))

(def ^:private incoming-query
  #?(:cljs #(->> (if (object? %) (js->clj %) %)
                 js->clj
                 mbql.normalize/normalize
                 (lib.query/query *metadata-provider*))
     :clj  identity))

(def ^:private outgoing-query
  #?(:cljs #(-> % lib.util/depipeline clj->js)
     :clj  identity))

;;; ----------------------------------- Schemas for Card -----------------------------------------
(def VisualizationSettings
  "Malli schema for `visualization_settings` - a map of strings to opaque JS values."
  [:map-of string? :any])

(def Parameter
  "Malli schema for each Card.parameters value."
  [:map
   [:id string?]
   [:name string?]
   [:display-name {:optional true :js/prop "display-name"} string?]
   [:type string?]
   [:slug string?]
   [:sectionId {:optional true :js-prep "sectionId"} string?]
   [:default {:optional true} :any]
   [:required {:optional true} boolean?]
   [:filtering-parameters {:optional true :js/prop "filteringParameters"} [:vector string?]]
   [:is-multi-select {:optional true :js/prop "isMultiSelect"} boolean?]
   [:value {:optional true} :any]])

(def Scalar
  [:or number? string? boolean? nil?])

(def ParameterValue
  "Malli schema for a single parameter's value. Part of [[ParameterValues]].
  Yes, this is a one-element tuple."
  [:tuple Scalar])

(def ParameterValues
  "Malli schema for the set of parameter values for a Card. This is not a key on Card; it's stored separately."
  [:map
   [:values [:sequential ParameterValue]]
   [:has_more_values {:optional true} boolean?]])

(def Card
  "Malli schema for a possibly-saved Card."
  [:map
   [:archived {:optional true} boolean?]
   [:average_query_time {:optional true} number?]
   [:cache-ttl {:optional true} [:maybe number?]]
   [:can_write {:optional true} boolean?]
   [:collection {:optional true} :any]
   [:collection_id {:optional true} [:maybe number?]]
   [:collection_position {:optional true} [:maybe number?]]
   [:collection_preview {:optional true} boolean?]
   [:created_at {:optional true} string?] ;; TODO: Date regex?
   [:creator {:optional true}
    [:map
     [:id number?]
     [:common_name string?]
     [:date_joined string?] ;; TODO: Date regex
     [:email string?]
     [:first_name  [:maybe string?]]
     [:is_qbnewb boolean?]
     [:is_superuser boolean?]
     [:last_login string?] ;; TODO: Date regex
     [:last_name   [:maybe string?]]]]
   [:creator_id {:optional true} number?]
   [:creation-type {:optional true :js/prop "creationType"} string?]
   [:dashboard-count {:optional true} number?]
   [:dashboard-id {:optional true :js/prop "dashboardId"} number?]
   [:dashcard-id  {:optional true :js/prop "dashcardId"}  number?]
   [:database_id {:optional true} number?]
   [:dataset {:optional true} boolean?]
   [:dataset_query
    ;; Custom encoding and decoding for :dataset-query to convert JS legacy MBQL <-> CLJS pMBQL.
    {:decode/js incoming-query
     :encode/js outgoing-query}
    ::lib.schema/query]
   [:description {:optional true} [:maybe string?]]
   ;; TODO: Display is really an enum but I don't know all its values.
   ;; Known values: table, scalar, gauge, map, area, bar, line. There are more missing for sure.
   [:display string?]
   [:display-is-locked {:optional true :js/prop "displayIsLocked"} boolean?]
   [:embedding_params {:optional true} :any]
   [:enable_embedding {:optional true} boolean?]
   [:entity_id {:optional true} string?]
   [:id {:optional true} [:or number? string?]]
   [:last-edit-info {:optional true :js/prop "last-edit-info"} :any]
   [:last-query-start {:optional true} :any]
   [:made_public_by_id {:optional true} number?]
   [:moderation_reviews {:optional true} [:vector :any]]
   [:name {:optional true} string?]
   [:original_card_id {:optional true} number?]
   [:parameter_mappings {:optional true} [:vector :any]]
   [:parameter_usage_count {:optional true} number?]
   [:parameters {:optional true} [:sequential Parameter]]
   [:persisted {:optional true} boolean?]
   [:public_uuid {:optional true} string?]
   [:query_type {:optional true} string?] ;; TODO: Probably an enum
   [:result_metadata {:optional true} :any]
   [:table_id {:optional true} number?]
   [:updated_at {:optional true} string?] ;; TODO: Date regex
   [:visualization_settings VisualizationSettings]])

;;; ---------------------------------------- Exported API ----------------------------------------
(de/define-getters-and-setters Card
  dataset-query     [:dataset_query]
  display           [:display]
  display-is-locked [:display-is-locked])

#?(:cljs
   (def ^:export from-js
     "Converter from plain JS objects to CLJS maps.
     You should pass this a JS `Card` and an instance of `Metadata`."
     (let [->Card (converters/incoming Card)]
       (fn [^js js-card js-metadata]
         (let [database-id (.-database (.-dataset_query js-card))]
           (binding [*metadata-provider* (lib.metadata.frontend/metadata-provider js-metadata database-id)]
             (->Card js-card)))))))

#?(:cljs
   (def ^:export to-js
     "Converter from CLJS maps to plain JS objects."
     (converters/outgoing Card)))

(defn- set-like [inner-schema]
  [:or [:set inner-schema] [:sequential inner-schema]])

(mu/defn ^:export maybe-unlock-display :- Card
  "Given current and previous sets of \"sensible\" display settings, check which of them the current `:display` setting
  is in.
  - If it's in the previous set, or the previous set is not defined, consider `:display` formerly sensible.
  - If `:display` is in current set, consider `:display` currently sensible.

  The `:display` should be unlocked if:
  - it was formerly sensible, AND
  - it is not currently sensible, AND
  - the display is currently locked.
  If the display is not currently locked, this never locks it."
  ([card :- Card
    current-sensible-displays :- (set-like string?)]
   (maybe-unlock-display card current-sensible-displays nil))

  ([card :- Card
    current-sensible-displays  :- (set-like string?)
    previous-sensible-displays :- [:maybe (set-like string?)]]
   (let [dsp                 (display card)
         previous            (set previous-sensible-displays)
         current             (set current-sensible-displays)
         formerly-sensible?  (or (empty? previous) (previous dsp)) ; An empty previous set is always sensible.
         currently-sensible? (current dsp)
         should-unlock?      (and formerly-sensible? (not currently-sensible?))]
     (with-display-is-locked card (and (display-is-locked card)
                                       (not should-unlock?))))))

#?(:cljs
   (def ^:export parameter_values_from_js
     "Converts a ParameterValues object into CLJS data."
     (converters/incoming ParameterValues)))


(mu/defn ^:export is-dirty-compared-to :- boolean?
  "Given two cards, compare a subset of their properties to see if they're different."
  [card          :- Card param-values :- ParameterValues
   original-card :- Card original-param-values :- ParameterValues]
  (or (not= param-values original-param-values)
      (apply not= (for [c [card original-card]]
                    (select-keys c [:name :description :collection_id :dashboard_id :dashcard_id
                                    :dataset :dataset_query :display :parameters :visualization_settings])))))
