;; PheroGraph configuration file
;;
;; This file is intended to load and provide access to
;; database connection parameters. In the future it might
;; also be used to support plugins, logging configuration,
;; experiment settings and other things which tend to be
;; lumped under the category of configuration.
;;
;; For configuration that is environment specific, such as
;; debug flags and hostnames, and for configuration which
;; is sensitive, like passwords and secrets, variables will
;; be loaded from the environment rather than tracked in
;; source.
(ns pherograph.config
  (require [environ.core :refer [env]]))



(def *database-url* (env :database-url))