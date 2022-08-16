(ns donut.dbdna.generate.malli
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.string :as str]
   [donut.dbdna.generate :as ddg]))

(def column-types
  {:integer    'pos-int?
   :integer-pk 'nat-int?
   :text       'string?
   :varchar    'string?})

(defn column-spec
  [dna table-name column-name]
  (let [{:keys [column-type primary-key? nullable? refers-to]} (get-in dna [table-name :columns column-name])]
    (prn "column type column name" column-name column-type)
    (or [column-name
         {:optional? (boolean nullable?)}

         (cond
           refers-to
           (last (column-spec  dna (first refers-to) (second refers-to)))

           (and (= :integer column-type) primary-key?)
           (:integer-pk column-types)

           :else
           (column-type column-types))]
        (throw (ex-info "unknown column-type" {:column-type column-type})))))

(defn generate
  [dna]
  (reduce (fn [specs table-name]
            (let [table-dna (table-name dna)]
              (->> (reduce (fn [spec column-name]
                             (conj spec (column-spec dna table-name column-name)))
                           [:map]
                           (:column-order table-dna))
                   (conj ['def (-> table-name
                                   name
                                   (str/replace #"s$" "")
                                   csk/->PascalCaseSymbol)])
                   seq
                   (conj specs))))
          []
          (ddg/table-order dna)))
