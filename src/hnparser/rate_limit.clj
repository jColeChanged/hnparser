;; When I talked to Paul Graham about writing a scraper for Hacker News
;; he told me that I could scrape the site, but only at 1 page request
;; every 5 seconds. So I made this.

;; Your allowed rate goes down by units of one, because each unit is an allowed
;; action. Your allowed rate goes up by the maximum rate multipled by the time
;; since you last called the function. This normalizes the units by cancelling
;; out the seconds. I think. I tried to make this into a function that will
;; let you create functions for a plethora of rate limiting tasks. I'm not sure
;; that I did a very good job, but I did learn a bit about clojure.

;; And before you point out that I could do this so much more simply by removing
;; support for x per y and coding for 1 per y, I know. I did it this way in part
;; because I felt it would be more reusable.

(ns hnparser.rate-limit)

(defn now
  []
  (/ (System/currentTimeMillis) 1000)) ; working in seconds seems simpler

(defn rate-limit-creator
  [rate per]
  (let [allowed (ref rate) last-access (ref (now))]
    (fn [& more]
      (dosync
       (let [current-time (now) time-passed (- current-time @last-access)]
	 (ref-set allowed
		  (let [allow (+ @allowed (* time-passed (/ rate per)))]
		    (if (> allow rate) ; make sure not to make the bucket
		      rate             ; larger than the allowed rate
		      allow)))
	 (ref-set last-access current-time)
	 (if (>= @allowed 1) ; I'm doing the rest outside the doysnc
	     (do             ; to avoid thunks and side effects
	       (ref-set allowed (dec @allowed))
	       [true]) ; about to estimate the time till were within capacity
	     [false (/ (* (- 1 @allowed) rate) per)]))))))

(defn rate-limit
  [func rate per retry]
  (let [rate-limiter (rate-limit-creator rate per)]
    (fn [& args]
      (let [throttler (rate-limiter)]
	(if (first throttler)
	  (apply func args)
	  (if retry
	    (do ; convert from seconds to milliseconds
	      (. Thread sleep (* 1000 (second throttler)))
	      (recur args))))))))

(defmacro defrlf ; must.. hide.. complexity..
  [name func rate per retry]
  `(def ~name (rate-limit ~func ~rate ~per ~retry)))