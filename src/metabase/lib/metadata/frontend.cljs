(ns metabase.lib.metadata.frontend
  "[[lib.metadata.protocols/DatabaseMetadataProvider]] implementation on top of the TypeScript `Metadata` class."
  (:require
    [metabase.lib.metadata.protocols :as lib.metadata.protocols]))

;;; ------------------------ Extending TS Database into a DatabaseMetadataProvider -------------------
(defn metadata-provider
  "Given an instance of the TypeScript `Metadata` class and the database ID, return a
  [[lib.metadata.protocols/DatabaseMetadataProvider]] for that database."
  [^js js-metadata database-id]
  (let [^js js-database (.database js-metadata database-id)]
    (reify
      lib.metadata.protocols/DatabaseMetadataProvider
      (database [_]
        {:lib/type :metadata/database
         :id       database-id})

      ;; NOTE: This is deliberately lazy. It's tempting to precompute the tables and fields in the `let` above,
      ;; but that's messy in practice - some FE tests pass impoverished metadata with eg. a made-up database ID that
      ;; doesn't actually appear in the `Metadata`.
      (tables [_]
        (for [^js js-table (.-tables js-database)]
          {:lib/type :metadata/table
           :id       (.-id js-table)
           :name     (.-name js-table)
           :schema   (.-schema_name js-table)}))

      (fields [_ table-id]
        (if-let [js-table (.table js-database table-id)]
          (for [^js js-field (.-fields js-table)]
            (merge {:lib/type :metadata/field
                    :name     (.-display_name js-field)}
                   (when-let [field-id (.-id js-field)]
                     {:id field-id})))
          (throw (ex-info "No such table in this Database" {:table-id table-id :database js-database})))))))
