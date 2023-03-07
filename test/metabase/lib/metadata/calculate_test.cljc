(ns metabase.lib.metadata.calculate-test
  (:require
   [clojure.test :refer [are deftest is testing]]
   [metabase.lib.metadata.calculate :as calculate]
   [metabase.lib.metadata.protocols :as lib.metadata.protocols]
   [metabase.lib.test-metadata :as meta]
   #?@(:cljs ([metabase.test-runner.assert-exprs.approximately-equal]))))

(def ^:private venues-query
  {:lib/type     :mbql/query
   :lib/metadata meta/metadata-provider
   :type         :pipeline
   :database     (meta/id)
   :stages       [{:lib/type     :mbql.stage/mbql
                   :lib/options  {:lib/uuid (str (random-uuid))}
                   :source-table (meta/id :venues)}]})

(defn- venues-query-with-last-stage [m]
  (update-in venues-query [:stages 0] merge m))

(defn- field-clause
  ([table field]
   (field-clause table field nil))
  ([table field options]
   [:field (merge {:lib/uuid (str (random-uuid))} options) (meta/id table field)]))

(deftest ^:parallel col-info-field-ids-test
  (testing "make sure columns are comming back the way we'd expect for :field clauses"
    (is (=? [(merge (meta/field-metadata :venues :price)
                    {:source    :fields
                     :field_ref [:field {:lib/uuid string?} (meta/id :venues :price)]})]
            (calculate/stage-metadata
             {:lib/type     :mbql/query
              :type         :pipeline
              :stages       [{:lib/type     :mbql.stage/mbql
                              :lib/options  {:lib/uuid "0311c049-4973-4c2a-8153-1e2c887767f9"}
                              :source-table (meta/id :venues)
                              :fields       [(field-clause :venues :price)]}]
              :database     (meta/id)
              :lib/metadata meta/metadata-provider})))))

;;; FIXME
#_(deftest ^:parallel col-info-implicit-join-test
  (testing (str "when a `:field` with `:source-field` (implicit join) is used, we should add in `:fk_field_id` "
                "info about the source Field")
    (is (=? [(merge (dissoc (meta/field-metadata :categories :name) :database_type)
                    {:fk_field_id (meta/id :venues :category-id)
                     :source      :fields
                     :field_ref   [:field {:fk-field-id (meta/id :venues :category-id)} (meta/id :categories :name)]})]
            (stage-metadata
             {:type  :query
              :query {:source-table (meta/id :venues)
                      :fields       [[:field (meta/id :categories :name) {:fk-field-id (meta/id :venues :category-id)}]]}})))))

(deftest ^:parallel col-info-explicit-join-test
  (testing (str "we should get `:fk_field_id` and information where possible when using joins; "
                "display_name should include the display name of the FK field (for IMPLICIT JOINS)")
    (is (=? [(merge (meta/field-metadata :categories :name)
                    {:display_name "Category → Name"
                     :source       :fields
                     :field_ref    [:field
                                    {:lib/uuid string?, :source-field (meta/id :venues :category-id)}
                                    (meta/id :categories :name)]
                     :fk_field_id  (meta/id :venues :category-id)
                     :source_alias "CATEGORIES__via__CATEGORY_ID"})]
            (calculate/stage-metadata
             {:lib/type     :mbql/query
              :type         :pipeline
              :stages       [{:lib/type     :mbql.stage/mbql
                              :lib/options  {:lib/uuid "fdcfaa06-8e65-471d-be5a-f1e821022482"}
                              :source-table (meta/id :venues)
                              :fields       [[:field
                                              {:join-alias "CATEGORIES__via__CATEGORY_ID", :lib/uuid "8704e09b-496e-4045-8148-1eef28e96b51"}
                                             (meta/id :categories :name)]]
                              :joins        [{:alias       "CATEGORIES__via__CATEGORY_ID"
                                              :condition   [:=
                                                            {:lib/uuid "cc5f6c43-1acb-49c2-aeb5-e3ff9c70541f"}
                                                            (field-clause :venues :category-id)
                                                            (field-clause :categories :id {:join-alias "CATEGORIES__via__CATEGORY_ID"})]
                                              :strategy    :left-join
                                              :fk-field-id (meta/id :venues :category-id)
                                              :lib/type    :mbql/join
                                              :stages      [{:lib/type     :mbql.stage/mbql
                                                             :lib/options  {:lib/uuid "bbbae500-c972-4550-b100-e0584eb72c4d"}
                                                             :source-table (meta/id :venues)}]
                                              :lib/options {:lib/uuid "490a5abb-54c2-4e62-9196-7e9e99e8d291"}}]}]
              :database     (meta/id)
              :lib/metadata meta/metadata-provider})))))

(deftest ^:parallel col-info-explicit-join-without-fk-field-id-test
  (testing (str "for EXPLICIT JOINS (which do not include an `:fk-field-id` in the Join info) the returned "
                "`:field_ref` should be have only `:join-alias`, and no `:source-field`")
    (is (=? [(merge (meta/field-metadata :categories :name)
                    {:display_name "Categories → Name"
                     :source       :fields
                     :field_ref    [:field {:join-alias "Categories"} (meta/id :categories :name)]
                     :source_alias "Categories"})]
            (calculate/stage-metadata
             {:lib/type     :mbql/query
              :type         :pipeline
              :stages       [{:lib/type     :mbql.stage/mbql
                              :lib/options  {:lib/uuid "e8a385bf-4a5b-4e44-bb0a-23095d4dac86"}
                              :source-table (meta/id :venues)
                              :fields       [(field-clause :categories :name {:join-alias "Categories"})]
                              :joins        [{:lib/type    :mbql/join
                                              :lib/options {:lib/uuid "783a1716-c450-4def-9422-798d59cb8785"}
                                              :alias       "Categories"
                                              :condition   [:=
                                                            {:lib/uuid "a0e8b12e-572e-4626-8040-250d25105e9c"}
                                                            (field-clause :venues :category-id)
                                                            (field-clause :categories :id {:join-alias "Categories"})]
                                              :strategy    :left-join
                                              :stages      [{:lib/type     :mbql.stage/mbql
                                                             :lib/options  {:lib/uuid "850f37a0-ba84-4c6b-b227-1aedfb058e41"}
                                                             :source-table (meta/id :venues)}]}]}]
              :database     (meta/id)
              :lib/metadata meta/metadata-provider})))))

(deftest ^:parallel col-info-for-field-with-temporal-unit-test
  (testing "when a `:field` with `:temporal-unit` is used, we should add in info about the `:unit`"
    (is (=? [(merge (meta/field-metadata :venues :price)
                    {:unit      :month
                     :source    :fields
                     :field_ref [:field {:temporal-unit :month} (meta/id :venues :price)]})]
            (calculate/stage-metadata
             {:lib/type     :mbql/query
              :type         :pipeline
              :stages       [{:lib/type     :mbql.stage/mbql
                              :lib/options  {:lib/uuid "ad7afe84-c51d-4a14-865e-501a61904bdf"}
                              :source-table (meta/id :venues)
                              :fields       [(field-clause :venues :price {:temporal-unit :month})]}]
              :database     (meta/id)
              :lib/metadata meta/metadata-provider})))))

(deftest ^:parallel col-info-for-field-with-temporal-unit-literal-test
  (testing "datetime unit should work on field literals too"
    (is (=? [{:name         "price"
              :base_type    :type/Number
              :display_name "Price"
              :unit         :month
              :source       :fields
              :field_ref    [:field {:base-type :type/Number, :temporal-unit :month} "price"]}]
            (calculate/stage-metadata
             {:lib/type     :mbql/query
              :lib/metadata meta/metadata-provider
              :database     (meta/id)
              :type         :pipeline
              :stages       [{:lib/type     :mbql.stage/mbql
                              :lib/options  {:lib/uuid "b75c696e-da02-4a46-979f-122884de352b"}
                              :source-table (meta/id :venues)
                              :fields       [[:field
                                              {:base-type     :type/Number
                                               :temporal-unit :month
                                               :lib/uuid      "74820cc4-4121-4c08-b234-36596b6ea99c"}
                                              "price"]]}]})))))

(deftest ^:parallel col-info-for-field-with-temporal-unit-correct-field-info-test
  (testing "should add the correct info if the Field originally comes from a nested query"
    (is (=? [{:name      "DATE"
              :unit      :month
              :field_ref [:field {:temporal-unit :default} (meta/id :checkins :date)]}
             {:name      "LAST_LOGIN"
              :unit      :month
              :field_ref [:field
                          {:temporal-unit :default
                           :join-alias    "USERS__via__USER_ID"}
                          (meta/id :users :last-login)]}]
            (calculate/stage-metadata
             {:lib/type     :mbql/query
              :lib/metadata meta/metadata-provider
              :database     (meta/id)
              :type         :pipeline
              :stages       [{:lib/type           :mbql.stage/mbql
                              :lib/options        {:lib/uuid "e076589f-04fb-44f6-bd3c-bc1c441766d2"}
                              :lib/stage-metadata [{:name      "DATE"
                                                    :id        (meta/id :checkins :date)
                                                    :unit      :month
                                                    :field_ref (field-clause :checkins :date {:temporal-unit :month})}
                                                   {:name      "LAST_LOGIN"
                                                    :id        (meta/id :users :last-login)
                                                    :unit      :month
                                                    :field_ref (field-clause :users :last-login {:temporal-unit :month
                                                                                                 :source-field  (meta/id :checkins :user-id)})}]
                              :source-table       (meta/id :checkins)
                              :breakout           [(field-clause :checkins :date {:temporal-unit :month})
                                                   (field-clause :users :last-login {:temporal-unit :month
                                                                                     :source-field  (meta/id :checkins :user-id)})]}
                             {:lib/type    :mbql.stage/mbql
                              :lib/options {:lib/uuid "4036700e-a3a8-4de6-b070-0f071dea6c50"}
                              :fields      [(field-clause :checkins :date {:temporal-unit :default})
                                            (field-clause :users :last-login {:temporal-unit :default
                                                                              :join-alias    "USERS__via__USER_ID"})]
                              :limit       1}]})))))

(deftest ^:parallel col-info-for-binning-strategy-test
  (testing "when binning strategy is used, include `:binning_info`"
    (is (=? [{:lib/type     :metadata/field
              :name         "price"
              :base_type    :type/Number
              :display_name "Price"
              :unit         :month
              :source       :fields
              :binning_info {:num_bins         10
                             :bin_width        5
                             :min_value        -100
                             :max_value        100
                             :binning_strategy :num-bins}
              :field_ref    [:field
                             {:lib/uuid      string?
                              :base-type     :type/Number
                              :temporal-unit :month
                              :binning       {:strategy  :num-bins
                                              :num-bins  10
                                              :bin-width 5
                                              :min-value -100
                                              :max-value 100}}
                             "price"]}]
            (calculate/stage-metadata
             {:lib/type     :mbql/query
              :lib/metadata meta/metadata-provider
              :database     (meta/id)
              :type         :pipeline
              :stages       [{:lib/type     :mbql.stage/mbql
                              :lib/options  {:lib/uuid "232612bd-6b8d-48ef-846f-108e4a6a8ae3"}
                              :source-table (meta/id :venues)
                              :fields       [[:field {:base-type     :type/Number
                                                      :temporal-unit :month
                                                      :binning       {:strategy  :num-bins
                                                                      :num-bins  10
                                                                      :bin-width 5
                                                                      :min-value -100
                                                                      :max-value 100}
                                                      :lib/uuid      "80c8e8e0-a93f-4a05-b811-1f788136fff7"}
                                              "price"]]}]})))))

(defn- grandparent-parent-child-id [field]
  (+ (meta/id :venues :id)
     (case field
       :grandparent 50
       :parent      60
       :child       70)))

(def ^:private grandparent-parent-child-metadata-provider
  "A DatabaseMetadataProvider for a Table that nested Fields: grandparent, parent, and child"
  (let [grandparent {:lib/type :metadata/field
                     :name     "grandparent"
                     :id       (grandparent-parent-child-id :grandparent)}
        parent      {:lib/type  :metadata/field
                     :name      "parent"
                     :parent_id (grandparent-parent-child-id :grandparent)
                     :id        (grandparent-parent-child-id :parent)}
        child       {:lib/type  :metadata/field
                     :name      "child"
                     :parent_id (grandparent-parent-child-id :parent)
                     :id        (grandparent-parent-child-id :child)}]
    (reify lib.metadata.protocols/DatabaseMetadataProvider
      (database [_this]
        (dissoc meta/metadata :tables))
      (tables [_this]
        [(dissoc (meta/table-metadata :venues) :fields)])
      (fields [_this table-id]
        (when (= table-id (meta/id :venues))
          (mapv (fn [field-metadata]
                  (merge (dissoc (meta/field-metadata :venues :id) :display_name)
                         field-metadata))
                [grandparent parent child]))))))

(deftest ^:parallel col-info-combine-parent-field-names-test
  (letfn [(col-info [a-field-clause]
            (#'calculate/col-info-for-field-clause
             {:lib/type     :mbql/query
              :lib/metadata grandparent-parent-child-metadata-provider
              :type         :pipeline
              :database     (meta/id)
              :stages       [{:lib/type     :mbql.stage/mbql
                              :lib/options  {:lib/uuid (str (random-uuid))}
                              :source-table (meta/id :venues)}]}
             -1
             a-field-clause))]
    (testing "For fields with parents we should return them with a combined name including parent's name"
      (is (=? {:table_id          (meta/id :venues)
               :name              "grandparent.parent"
               :field_ref         [:field {} (grandparent-parent-child-id :parent)]
               :parent_id         (grandparent-parent-child-id :grandparent)
               :id                (grandparent-parent-child-id :parent)
               :visibility_type   :normal}
              (col-info [:field {:lib/uuid (str (random-uuid))} (grandparent-parent-child-id :parent)]))))
    (testing "nested-nested fields should include grandparent name (etc)"
      (is (=? {:table_id          (meta/id :venues)
               :name              "grandparent.parent.child"
               :field_ref         [:field {} (grandparent-parent-child-id :child)]
               :parent_id         (grandparent-parent-child-id :parent)
               :id                (grandparent-parent-child-id :child)
               :visibility_type   :normal}
              (col-info [:field {:lib/uuid (str (random-uuid))} (grandparent-parent-child-id :child)]))))))

(deftest ^:parallel col-info-field-literals-test
  (testing "field literals should get the information from the matching `:source-metadata` if it was supplied"
    (is (=? {:name          "sum"
             :display_name  "sum of User ID"
             :base_type     :type/Integer
             :field_ref     [:field {:base-type :type/Integer} "sum"]
             :semantic_type :type/FK}
            (#'calculate/col-info-for-field-clause
             (venues-query-with-last-stage
              {:lib/stage-metadata
               {:lib/type :metadata/results
                :columns  [{:lib/type      :metadata/field
                            :name          "abc"
                            :display_name  "another Field"
                            :base_type     :type/Integer
                            :semantic_type :type/FK}
                           {:lib/type      :metadata/field
                            :name          "sum"
                            :display_name  "sum of User ID"
                            :base_type     :type/Integer
                            :semantic_type :type/FK}]}})
             -1
             [:field {:lib/uuid (str (random-uuid)), :base-type :type/Integer} "sum"])))))

(deftest ^:parallel col-info-expressions-test
  (testing "col info for an `expression` should work as expected"
    (is (=? {:base_type       :type/Float
             :name            "double-price"
             :display_name    "double-price"
             :expression_name "double-price"
             :field_ref       [:expression {} "double-price"]}
            (#'calculate/col-info-for-field-clause
             (venues-query-with-last-stage
              {:expressions {"double-price" [:* (field-clause :venues :price) 2]}})
             -1
             [:expression {:lib/uuid (str (random-uuid))} "double-price"])))))

(deftest ^:parallel col-info-for-convert-timezone-test
  (testing "col-info for convert-timezone should have a `converted_timezone` property"
    (are [expression] (=? {:converted_timezone "Asia/Ho_Chi_Minh"
                           :base_type          :type/DateTime
                           :name               "last-login-converted"
                           :display_name       "last-login-converted"
                           :expression_name    "last-login-converted"
                           :field_ref          [:expression {} "last-login-converted"]}
                          (#'calculate/col-info-for-field-clause
                           (venues-query-with-last-stage
                            {:expressions {"last-login-converted" expression}})
                           -1
                           [:expression {:lib/uuid (str (random-uuid))} "last-login-converted"]))
      [:convert-timezone (field-clause :users :last-login) "Asia/Ho_Chi_Minh" "UTC"]

      [:datetime-add
       [:convert-timezone (field-clause :users :last-login) "Asia/Ho_Chi_Minh" "UTC"]
       2
       :hour])))

(deftest ^:parallel col-info-for-expression-error-message-test
  (testing "if there is no matching expression it should give a meaningful error message"
    (letfn [(thunk []
              (#'calculate/col-info-for-field-clause
               (venues-query-with-last-stage
                {:expressions {"one-hundred" 100}})
               -1
               [:expression {:lib/uuid (str (random-uuid))} "double-price"]))]
      (is (thrown-with-msg?
           #?(:clj Throwable :cljs js/Error)
           #"No expression named \"double-price\""
           (thunk)))
      (try
        (thunk)
        (catch #?(:clj Throwable :cljs js/Error) e
          (is (= {:expression-name "double-price"
                  :found           #{"one-hundred"}}
                 (ex-data e))))))))

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                    (MBQL) Col info for Aggregation clauses                                     |
;;; +----------------------------------------------------------------------------------------------------------------+

;; test that added information about aggregations looks the way we'd expect
(defn- aggregation-names
  ([ag-clause]
   (aggregation-names venues-query -1 ag-clause))

  ([query stage-number ag-clause]
   {:name         (calculate/aggregation-name ag-clause)
    :display_name (calculate/aggregation-display-name query stage-number ag-clause)}))

(deftest ^:parallel aggregation-names-test
  (testing "basic aggregations"
    (testing ":count"
      (is (= {:name "count", :display_name "Count"}
             (aggregation-names [:count]))))

    (testing ":distinct"
      (is (= {:name "count", :display_name "Distinct values of ID"}
             (aggregation-names [:distinct (field-clause :venues :id)]))))

    (testing ":sum"
      (is (= {:name "sum", :display_name "Sum of ID"}
             (aggregation-names [:sum (field-clause :venues :id)])))))

  (testing "expressions"
    (testing "simple expression"
      (is (= {:name "expression", :display_name "Count + 1"}
             (aggregation-names [:+ [:count] 1]))))

    (testing "expression with nested expressions"
      (is (= {:name "expression", :display_name "Min of ID + (2 * Average of Price)"}
             (aggregation-names
              [:+
               [:min (field-clause :venues :id)]
               [:* 2 [:avg (field-clause :venues :price)]]]))))

    (testing "very complicated expression"
      (is (= {:name "expression", :display_name "Min of ID + (2 * Average of Price * 3 * (Max of Category ID - 4))"}
             (aggregation-names
              [:+
               [:min (field-clause :venues :id)]
               [:*
                2
                [:avg (field-clause :venues :price)]
                3
                [:- [:max (field-clause :venues :category-id)] 4]]])))))

  (testing "`aggregation-options`"
    (testing "`:name` and `:display-name`"
      (is (= {:name "generated_name", :display_name "User-specified Name"}
             (aggregation-names
              [:aggregation-options
               [:+ [:min (field-clause :venues :id)] [:* 2 [:avg (field-clause :venues :price)]]]
               {:name "generated_name", :display-name "User-specified Name"}]))))

    (testing "`:name` only"
      (is (= {:name "generated_name", :display_name "Min of ID + (2 * Average of Price)"}
             (aggregation-names
              [:aggregation-options
               [:+ [:min (field-clause :venues :id)] [:* 2 [:avg (field-clause :venues :price)]]]
               {:name "generated_name"}]))))

    (testing "`:display-name` only"
      (is (= {:name "expression", :display_name "User-specified Name"}
             (aggregation-names
              [:aggregation-options
               [:+ [:min (field-clause :venues :id)] [:* 2 [:avg (field-clause :venues :price)]]]
               {:display-name "User-specified Name"}]))))))

(defn- col-info-for-aggregation-clause
  ([clause]
   (col-info-for-aggregation-clause {} clause))

  ([stage clause]
   (#'calculate/col-info-for-aggregation-clause (venues-query-with-last-stage stage) -1 clause)))

(deftest ^:parallel col-info-for-aggregation-clause-test
  (testing "basic aggregation clauses"
    (testing "`:count` (no field)"
      (is (=? {:base_type :type/Float, :name "expression", :display_name "Count / 2"}
              (col-info-for-aggregation-clause [:/ [:count] 2]))))

    (testing "`:sum`"
      (is (=? {:base_type :type/Float, :name "sum", :display_name "Sum of Price + 1"}
              (col-info-for-aggregation-clause [:sum [:+ (field-clause :venues :price) 1]])))))

  (testing "`:aggregation-options`"
    (testing "`:name` and `:display-name`"
      (is (=? {:base_type     :type/Integer
               :semantic_type :type/Category
               :settings      nil
               :name          "sum_2"
               :display_name  "My custom name"}
              (col-info-for-aggregation-clause
               [:aggregation-options [:sum (field-clause :venues :price)] {:name "sum_2", :display-name "My custom name"}]))))

    (testing "`:name` only"
      (is (=? {:base_type     :type/Integer
               :semantic_type :type/Category
               :settings      nil
               :name          "sum_2"
               :display_name  "Sum of Price"}
              (col-info-for-aggregation-clause [:aggregation-options [:sum (field-clause :venues :price)] {:name "sum_2"}]))))

    (testing "`:display-name` only"
      (is (=? {:base_type     :type/Integer
               :semantic_type :type/Category
               :settings      nil
               :name          "sum"
               :display_name  "My Custom Name"}
              (col-info-for-aggregation-clause
               [:aggregation-options [:sum (field-clause :venues :price)] {:display-name "My Custom Name"}]))))))

(deftest ^:parallel col-info-named-aggregation-test
  (testing "col info for an `expression` aggregation w/ a named expression should work as expected"
    (is (=? {:base_type :type/Float, :name "sum", :display_name "Sum of double-price"}
            (col-info-for-aggregation-clause
             {:expressions {"double-price" [:* (field-clause :venues :price) 2]}}
             [:sum [:expression "double-price"]])))))

(defn- infer-all
  ([expr]
   (infer-all expr nil))

  ([expr last-stage]
   (-> (venues-query-with-last-stage
        (merge
         {:expressions {"expr" expr}
          :fields      [[:expression {:lib/uuid (str (random-uuid))} "expr"]]
          :limit       10}
         last-stage))
       calculate/stage-metadata)))

(defn- infer-first
  [expr]
  (first (infer-all expr)))

(defn- infered-col-type
  [expr]
  (select-keys (infer-first expr) [:base_type :semantic_type]))

(deftest ^:parallel infer-coalesce-test
  (testing "Coalesce"
    (testing "Uses the first clause"
      (testing "Gets the type information from the field"
        (is (= {:semantic_type     :type/Name
                :coercion_strategy nil
                :name              "expr"
                :expression_name   "expr"
                :source            :fields
                :field_ref         [:expression "expr"]
                :effective_type    :type/Text
                :display_name      "expr"
                :base_type         :type/Text}
               (infer-first [:coalesce (field-clause :venues :name) "bar"])))
        (testing "Does not contain a field id in its analysis (#18513)"
          (is (not (contains? (infer-first [:coalesce (field-clause :venues :name) "bar"])
                              :id)))))
      (testing "Gets the type information from the literal"
        (is (= {:base_type       :type/Text
                :name            "expr"
                :display_name    "expr"
                :expression_name "expr"
                :field_ref       [:expression "expr"]
                :source          :fields}
               (infer-first [:coalesce "bar" (field-clause :venues :name)])))))))

(deftest ^:parallel infer-case-test
  (testing "Case"
    (testing "Uses first available type information"
      (testing "From a field"
        (is (= {:semantic_type     :type/Name
                :coercion_strategy nil
                :name              "expr"
                :expression_name   "expr"
                :source            :fields
                :field_ref         [:expression "expr"]
                :effective_type    :type/Text
                :display_name      "expr"
                :base_type         :type/Text}
               (infer-first [:coalesce (field-clause :venues :name) "bar"])))
        (testing "does not contain a field id in its analysis (#17512)"
          (is (false?
               (contains? (infer-first [:coalesce (field-clause :venues :name) "bar"])
                          :id))))))
    (is (= {:base_type :type/Text}
           (infered-col-type [:case [[[:> (field-clause :venues :price) 2] "big"]]])))
    (is (= {:base_type :type/Float}
           (infered-col-type [:case [[[:> (field-clause :venues :price) 2]
                                      [:+ (field-clause :venues :price) 1]]]])))
    (testing "Make sure we skip nils when infering case return type"
      (is (= {:base_type :type/Number}
             (infered-col-type [:case [[[:<
                                         (field-clause :venues :price)
                                         10]
                                        [:value nil {:base_type :type/Number}]]
                                       [[:> (field-clause :venues :price) 2] 10]]]))))
    (is (= {:base_type :type/Float}
           (infered-col-type [:case [[[:>
                                       (field-clause :venues :price)
                                       2]
                                      [:+
                                       (field-clause :venues :price)
                                       1]]]])))))

(deftest ^:parallel datetime-arithmetics?-test
  (are [x] (#'calculate/datetime-arithmetics? x)
    [:interval -1 :month]
    [:field (meta/id :checkins :date) {:temporal-unit :month}])
  (are [x] (not (#'calculate/datetime-arithmetics? x))
    [:+ 1 [:temporal-extract
           [:+ [:field (meta/id :checkins :date) nil] [:interval -1 :month]]
           :year]]
    [:+ [:field (meta/id :checkins :date) nil] 3]))

(deftest ^:parallel temporal-extract-test
  (are [clause] (= {:base_type :type/DateTime}
                   (infered-col-type clause))
    [:datetime-add [:field (meta/id :checkins :date) nil] 2 :month]
    [:datetime-add [:field (meta/id :checkins :date) nil] 2 :hour]
    [:datetime-add [:field (meta/id :users :last-login) nil] 2 :month]))

(deftest ^:parallel test-string-extracts
  (are [clause expected] (= expected
                            ((infered-col-type clause)))
    [:trim "foo"]                                        {:base_type :type/Text}
    [:ltrim "foo"]                                       {:base_type :type/Text}
    [:rtrim "foo"]                                       {:base_type :type/Text}
    [:length "foo"]                                      {:base_type :type/BigInteger}
    [:upper "foo"]                                       {:base_type :type/Text}
    [:lower "foo"]                                       {:base_type :type/Text}
    [:substring "foo" 2]                                 {:base_type :type/Text}
    [:replace "foo" "f" "b"]                             {:base_type :type/Text}
    [:regex-match-first "foo" "f"]                       {:base_type :type/Text}
    [:concat "foo" "bar"]                                {:base_type :type/Text}
    [:coalesce "foo" "bar"]                              {:base_type :type/Text}
    [:coalesce (field-clause :venues :name) "bar"] {:base_type :type/Text, :semantic_type :type/Name}))

(deftest ^:parallel unique-name-key-test
  (testing "Make sure `:cols` always come back with a unique `:name` key (#8759)"
    (is (= [{:base_type      :type/Number
             :effective_type :type/Number
             :semantic_type  :type/Quantity
             :name           "count"
             :display_name   "count"
             :source         :aggregation
             :field_ref      [:aggregation 0]}
            {:source         :aggregation
             :name           "sum"
             :display_name   "sum"
             :base_type      :type/Number
             :effective_type :type/Number
             :field_ref      [:aggregation 1]}
            {:base_type      :type/Number
             :effective_type :type/Number
             :semantic_type  :type/Quantity
             :name           "count_2"
             :display_name   "count"
             :source         :aggregation
             :field_ref      [:aggregation 2]}
            {:base_type      :type/Number
             :effective_type :type/Number
             :semantic_type  :type/Quantity
             :name           "count_3"
             :display_name   "count_2"
             :source         :aggregation
             :field_ref      [:aggregation 3]}]
           (calculate/stage-metadata
            (venues-query-with-last-stage
             {:aggregation        [[:count]
                                   [:sum]
                                   [:count]
                                   [:aggregation-options [:count] {:display-name "count_2"}]]
              :lib/stage-metadata {:lib/type :metadata/results
                                   :columns  [{:lib/type     :metadata/field
                                               :name         "count"
                                               :display_name "count"
                                               :base_type    :type/Number}
                                              {:lib/type     :metadata/field
                                               :name         "sum"
                                               :display_name "sum"
                                               :base_type    :type/Number}
                                              {:lib/type     :metadata/field
                                               :name         "count"
                                               :display_name "count"
                                               :base_type    :type/Number}
                                              {:lib/type     :metadata/field
                                               :name         "count_2"
                                               :display_name "count_2"
                                               :base_type    :type/Number}]}}))))))

(deftest ^:parallel expressions-keys-test
  (testing "make sure expressions come back with the right set of keys, including `:expression_name` (#8854)"
    (is (=? [{}
             {:name            "discount_price"
              :display_name    "discount_price"
              :base_type       :type/Float
              :expression_name "discount_price"
              :source          :fields
              :field_ref       [:expression {:lib/uuid string?} "discount_price"]}]
            (calculate/stage-metadata
             (venues-query-with-last-stage
              {:expressions {"discount_price" [:* 0.9 (field-clause :venues :price)]}
               :fields      [(field-clause :venues :name)
                             [:expression {:lib/uuid (str (random-uuid))} "discount_price"]]
               :limit       10}))))))

(deftest ^:parallel deduplicate-expression-names-in-aggregations-test
  (testing "make sure multiple expressions come back with deduplicated names"
    (testing "expressions in aggregations"
      (is (=? [{:base_type    :type/Float
                :name         "expression"
                :display_name "0.9 * Average of Price"
                :source       :aggregation
                :field_ref    [:aggregation {:lib/uuid string?} 0]}
               {:base_type    :type/Float
                :name         "expression_2"
                :display_name "0.8 * Average of Price"
                :source       :aggregation
                :field_ref    [:aggregation {:lib/uuid string?} 1]}]
              (calculate/stage-metadata
               (venues-query-with-last-stage
                {:aggregation [[:*
                                0.9
                                [:avg (field-clause :venues :price)]]
                               [:*
                                0.8
                                [:avg (field-clause :venues :price)]]]
                 :limit       10})))))))

(deftest ^:parallel deduplicate-named-expressions-test
  (testing "make sure multiple expressions come back with deduplicated names"
    (testing "named :expressions"
      (is (=? [{:name            "prev_month"
                :display_name    "prev_month"
                :base_type       :type/DateTime
                :expression_name "prev_month"
                :source          :fields
                :field_ref       [:expression {:lib/uuid string?} "prev_month"]}]
              (calculate/stage-metadata
               (venues-query-with-last-stage
                {:expressions {"prev_month" [:+ (meta/id :users :last-login) [:interval -1 :month]]}
                 :fields      [[:expression {:lib/uuid (str (random-uuid))} "prev_month"]]
                 :limit       10})))))))
