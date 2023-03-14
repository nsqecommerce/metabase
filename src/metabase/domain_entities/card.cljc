(ns metabase.domain-entities.card
  "Functions for building, updating and querying Cards (that is, Metabase questions)."
  (:require
   [metabase.domain-entities.malli :as de]
   [metabase.lib.equality :as lib.equality]
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

(def LocalFieldReference
  "Malli schema for a *legacy* field clause for a local field."
  [:tuple
   [:= :field]
   number?
   [:maybe [:map-of string? :any]]])

(def ForeignFieldReference
  "Malli schema for a *legacy* field clause for a foreign field."
  [:tuple
   [:= :field]
   [:or number? string?]
   [:map [:source-field {:js/prop "source-field"} [:or number? string?]]]])

(def VariableTarget
  "Malli schema for a parameter that references a template variable."
  [:tuple [:= :template-tag] string?])

(def ParameterTarget
  "Malli schema for a parameter's target field."
  [:orn
   [:variable [:tuple [:= :variable] VariableTarget]]
   [:dimension [:tuple
                [:= :dimension]
                [:or LocalFieldReference ForeignFieldReference VariableTarget]]]])

(def Parameter
  "Malli schema for each Card.parameters value."
  [:map
   [:id string?]
   [:name string?]
   [:display-name {:optional true :js/prop "display-name"} string?]
   [:slug string?]
   [:type string?]
   [:sectionId {:optional true :js-prep "sectionId"} string?]
   [:default {:optional true} :any]
   [:filtering-parameters {:optional true :js/prop "filteringParameters"} [:vector string?]]
   [:is-multi-select {:optional true :js/prop "isMultiSelect"} boolean?]
   [:required {:optional true} boolean?]
   [:target {:optional true} ParameterTarget]
   [:value {:optional true} :any]
   [:values_query_type {:optional true} :any]
   [:values_source_config {:optional true} :any]
   [:values_source_type {:optional true} :any]])

(def Scalar
  "JSON scalar values - the set of values a question parameter can take on."
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
  [:schema
   [:map
    [:archived {:optional true} boolean?]
    [:average_query_time {:optional true} number?]
    [:cache-ttl {:optional true} [:maybe number?]]
    [:can_write {:optional true} boolean?]
    [:collection {:optional true} :any]
    [:collection_id [:maybe number?]]
    [:collection_position [:maybe number?]]
    [:collection_preview boolean?]
    [:created_at [:maybe string?]] ;; TODO: Date regex?
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
    [:dashboard_count {:optional true} number?]
    [:dashboard-id {:optional true :js/prop "dashboardId"} number?]
    [:dashcard-id  {:optional true :js/prop "dashcardId"}  number?]
    [:database_id {:optional true} number?]
    [:dataset {:optional true} boolean?]
    [:dataset_query {:decode/js incoming-query
                     :encode/js outgoing-query}
     ::lib.schema/query]
    [:description [:maybe string?]]
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
    [:made_public_by_id {:optional true} [:maybe number?]]
    [:moderation_reviews {:optional true} [:vector :any]]
    [:name {:optional true} string?]
    [:original_card_id {:optional true} number?]
    [:parameter_mappings {:optional true} [:vector :any]]
    [:parameter_usage_count {:optional true} number?]
    [:parameters {:optional true} [:sequential Parameter]]
    [:persisted {:optional true} boolean?]
    [:public_uuid {:optional true} [:maybe string?]]
    [:query_type {:optional true} string?] ;; TODO: Probably an enum
    [:result_metadata {:optional true} :any]
    [:table_id {:optional true} [:maybe number?]]
    [:updated_at {:optional true} string?] ;; TODO: Date regex
    [:visualization_settings VisualizationSettings]]])

;;; ---------------------------------------- Exported API ----------------------------------------
(de/define-getters-and-setters Card
  ;; NOTE: Do not use define-getters-and-setters for `dataset-query`; it needs special handling.
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
   (def ^:export query-from-js
     "Converter for a plain JS object query to a CLJS map.
     Needs special handling because of the metadata."
     (let [->DatasetQuery (converters/incoming ::lib.schema/query)]
       (fn [^js js-dataset-query js-metadata]
         (let [database-id (.-database js-dataset-query)]
           (binding [*metadata-provider* (lib.metadata.frontend/metadata-provider js-metadata database-id)]
             (-> js-dataset-query incoming-query ->DatasetQuery)))))))

#?(:cljs
   (def ^:export to-js
     "Converter from CLJS maps to plain JS objects."
     (converters/outgoing Card)))

(mu/defn ^:export with-dataset-query :- Card
  "Attaches a `:dataset_query` to a `Card`.

  This can't be generated by [[de/define-getters-and-setters]] because the conversion of queries requires special
  handling."
  [card :- Card dataset-query :- ::lib.schema/query]
  (when-not (map? card)
    (throw (ex-info "with-dataset-query does not auto-convert; call from-js first" {})))
  (when-not (map? dataset-query)
    (throw (ex-info "with-dataset-query does not auto-convert; call query-from-js first" {})))
  (assoc card :dataset_query dataset-query))

(defn- set-like [inner-schema]
  [:schema [:or [:set inner-schema] [:sequential inner-schema]]])

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
   (def ^:export parameter-values-from-js
     "Converts a ParameterValues object into CLJS data."
     (converters/incoming ParameterValues)))

(defn- trim-card-for-dirty-check [card]
  (-> card
      (select-keys [:collection_id :creation-type :dashboard-id :dashcard-id :dataset
                    :description :display :name :parameters :visualization_settings])
      (update :parameters #(or % []))))

;; TODO: This inverted "is dirty" logic is a bit confusing - this would feel more natural as an equality check.
;; However it seems more convenient for the transition to keep the names and logic the same.
(mu/defn ^:export is-dirty-compared-to :- boolean?
  "Given two cards, compare a subset of their properties to see if they're different.

  Two cards are considered dirty if:
  - Their `parameter-values` are different; OR
  - Their queries are not [[lib.equality/=]]; OR
  - Their values for a certain subset of fields is different.

  Note that the comparison is symmetric - it doesn't matter which is `card` and which `original-card`."
  [card          :- Card param-values :- ParameterValues
   original-card :- Card original-param-values :- ParameterValues]
  (or (not= param-values original-param-values)
      ;; Queries require a specialized =, since they contain eg. `:lib/uuid`s that should be ignored.
      (not (lib.equality/= (:dataset_query card) (:dataset_query original-card)))
      (not= (trim-card-for-dirty-check card)
            (trim-card-for-dirty-check original-card))))
