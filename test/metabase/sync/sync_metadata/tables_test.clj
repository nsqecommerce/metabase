(ns metabase.sync.sync-metadata.tables-test
  "Test for the logic that syncs Table models with the metadata fetched from a DB."
  (:require
   [clojure.test :refer :all]
   [metabase.models :refer [Database Table]]
   [metabase.sync.sync-metadata.tables :as sync-tables]
   [metabase.test :as mt]
   [metabase.util :as u]
   [toucan.db :as db]))

(deftest crufty-tables-test
  (testing "south_migrationhistory, being a CRUFTY table, should still be synced, but marked as such"
    (mt/dataset :db-with-some-cruft
      (is (= #{{:name "SOUTH_MIGRATIONHISTORY", :visibility_type :cruft, :initial_sync_status "complete"}
               {:name "ACQUIRED_TOUCANS",       :visibility_type nil,    :initial_sync_status "complete"}}
             (set (for [table (db/select [Table :name :visibility_type :initial_sync_status], :db_id (mt/id))]
                    (into {} table))))))))

(deftest retire-tables-test
  (testing "`retire-tables!` should retire the Table(s) passed to it, not all Tables in the DB -- see #9593"
    (mt/with-temp* [Database [db]
                    Table    [table-1 {:name "Table 1", :db_id (u/the-id db)}]
                    Table    [_       {:name "Table 2", :db_id (u/the-id db)}]]
      (#'sync-tables/retire-tables! db #{{:name "Table 1", :schema (:schema table-1)}})
      (is (= {"Table 1" false, "Table 2" true}
             (db/select-field->field :name :active Table, :db_id (u/the-id db)))))))
