# Yesql - Clojure & SQL rethought.

Yesql is a Clojure library for _using_ SQL.

## Status

Thawing. Assistance maintaining sought.

Tested with Clojure 1.7-1.12.0.

[Kris Jenkins](https://twitter.com/krisajenkins) originally wrote and
maintained this project, and [Mike Schaeffer](https://mschaef.com)
is taking on forward maintenance. Please contact Mike with issues,
suggestions, and questions.

The primary focus moving forward will be to keep versions and tests up
to date, and potentially add a few new features where they are most
likely to be helpful. The goal is for yesql to continue as a stable
and composable library for integrating SQL easily into Clojure
projects.

You might also consider using [hugsql](https://www.hugsql.org/) which is
philosophically similar and more actively developed.

## Installation

Add this to your [Leiningen](https://github.com/technomancy/leiningen) `:dependencies`:

[![Clojars Project](https://img.shields.io/clojars/v/yesql.svg)](https://clojars.org/yesql)

### Driver
Plus you'll want a database driver. Here are some examples (but double
check, because there may be a newer version available):

|Database|`:dependencies` Entry|
|---|---|
|Derby|`[org.apache.derby/derby "10.16.1.1"]`|
|H2|`[com.h2database/h2 "2.1.212"]`|
|HyperSQL|`[org.hsqldb/hsqldb "2.6.1"]`|
|MySQL|`[mysql/mysql-connector-java "8.0.29"]`|
|Oracle|`[com.oracle.database.jdbc/ojdbc10 "19.14.0.0"]]`|
|PostgreSQL|`[org.postgresql/postgresql "42.3.5"]`|
|SQLite|`[org.xerial/sqlite-jdbc "3.36.0.3"]`|

(Any database with a JDBC driver should work. If you know of a driver
that's not listed here, please open a pull request to update this
section.)

### Migrating From Previous Versions

See the [Migration Guide](https://github.com/krisajenkins/yesql/wiki/Migration).

## Rationale

You're writing Clojure. You need to write some SQL.

I think we're all agreed that this is a problem:

``` clojure
(query "SELECT * FROM users WHERE country_code = ?" "GB")
```

Unless these query strings are short, they quickly get hard to read
and hard to rewrite. Plus the lack of indentation & syntax
highlighting is horrible.

But something like this **is not the solution**:

``` clojure
(select :*
        (from :users)
        (where (= :country_code "GB")))
```

Clojure is a great language for writing DSLs, but we don't need a new
one. SQL is already a mature DSL.  And s-expressions are great, but
here they're not adding anything. This is parens-for-parens sake.
(Don't agree? Wait until this extra syntax layer breaks down and you
start wrestling with a `(raw-sql)` function.)

So what's the solution? Keep the SQL as SQL. Have one file with your
query:

``` sql
-- name: users-by-country
SELECT *
FROM users
WHERE country_code = :country_code
```

...and then read that file to turn it into a regular Clojure function:

``` clojure
(defqueries "some/where/users_by_country.sql"
   {:connection db-spec})

;;; A function with the name `users-by-country` has been created.
;;; Let's use it:
(users-by-country {:country_code "GB"})
;=> ({:name "Kris" :country_code "GB" ...} ...)
```

By keeping the SQL and Clojure separate you get:

- No syntactic surprises. Your database doesn't stick to the SQL
  standard - none of them do - but Yesql doesn't care. You will
  never spend time hunting for "the equivalent sexp syntax". You will
  never need to fall back to a `(raw-sql "some('funky'::SYNTAX)")` function.
- Better editor support. Your editor probably already has great SQL
  support. By keeping the SQL as SQL, you get to use it.
- Team interoperability. Your DBAs can read and write the SQL you
  use in your Clojure project.
- Easier performance tuning. Need to `EXPLAIN` that query plan? It's
  much easier when your query is ordinary SQL.
- Query reuse. Drop the same SQL files into other projects, because
  they're just plain ol' SQL. Share them as a submodule.

### When Should I Not Use Yesql?

When you need your SQL to work with many different kinds of
database at once. If you want one complex query to be transparently
translated into different dialects for MySQL, Oracle, Postgres etc.,
then you genuinely do need an abstraction layer on top of SQL.

## Usage
### One File, One Query

Create an SQL query. Note we can supply named parameters ([in
`snake_case`](https://github.com/krisajenkins/yesql/issues/1))
and a comment string:

```sql
-- Counts the users in a given country.
SELECT count(*) AS count
FROM user
WHERE country_code = :country_code
```

Make sure it's on the classpath. For this example, it's in
`src/some/where/`. Now we can use it in our Clojure program.

```clojure
(require '[yesql.core :refer [defquery]])

; Define a database connection spec. (This is standard clojure.java.jdbc.)
(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname "//localhost:5432/demo"
              :user "me"})

; Import the SQL query as a function.
(defquery users-by-country "some/where/users_by_country.sql"
   {:connection db-spec})
```

Lo! The function has been created, with automatic, useful docstrings
in the REPL:

```clojure
(clojure.repl/doc users-by-country)

;=> -------------------------
;=> user/users-by-country
;=> ([{:keys [country_code]}]
;=>  [{:keys [country_code]} {:keys [connection]}])
;=>
;=>   Counts the users in a given country.
```

Now we can use it:

```clojure
; Use it standalone.
(users-by-country {:country_code "GB"})
;=> ({:count 58})

; Use it in a clojure.java.jdbc transaction.
(require '[clojure.java.jdbc :as jdbc])

(jdbc/with-db-transaction [tx db-spec]
   {:limeys (users-by-country {:country_code "GB"} {:connection tx})
    :yanks  (users-by-country {:country_code "US"} {:connection tx})})
```

### One File, Many Queries

As an alternative to the above, you can have many SQL statements in a
single SQL file. The file format is: `(<name tag> [docstring comments]
<the query>)*`, like so:

``` sql
-- name: users-by-country
-- Counts the users in a given country.
SELECT count(*) AS count
FROM user
WHERE country_code = :country_code

-- name: user-count
-- Counts all the users.
SELECT count(*) AS count
FROM user
```

Then read the file in like so:

```clojure
(require '[yesql.core :refer [defqueries]])
(defqueries "some/where/queryfile.sql"
   {:connection db-spec})
```

`defqueries` returns a sequence of the vars it binds, which can be
useful feedback while developing.

As with `defquery`, each function will have a docstring based on the
comments, and a parameter map based on the SQL parameters.

### ? Parameters

Yesql supports named parameters, and `?`-style positional
parameters. Here's an example:

```sql
-- name: young-users-by-country
SELECT *
FROM user
WHERE (
  country_code = ?
  OR
  country_code = ?
)
AND age < :max_age
```

Supply the `?` parameters as a vector under the `:?` key, like so:

```clojure
(young-users-by-country {:? ["GB" "US"]
                         :max_age 18})
```

#### Selectively import queries

Similarly to `defqueries`, `require-sql` lets you create a number of
query functions at a time, but with a syntax more like
`clojure.core/require`.

Using the `queryfile.sql` from the previous example:

```clojure
(require '[yesql.core :refer [require-sql]])

; Use :as to alias the entire namespace, and :refer to refer functions
; into the current namespace. Use one or both.
(require-sql ["some/where/queryfile.sql" :as user :refer [user-count])

(user-count)
;=> ({:count 132})

(user/users-by-country db-spec "GB")
;=> ({:count 58})
```

### IN-list Queries

Yesql supports `IN`-style queries. Define your query with a
single-element in the `IN` list, like so:

```sql
-- name: find-users
-- Find the users with the given ID(s).
SELECT *
FROM user
WHERE user_id IN (:id)
AND age > :min_age
```

And then supply the `IN`-list as a vector, like so:

```clojure
(defqueries "some/where/queryfile.sql"
   {:connection db-spec})

(find-users {:id [1001 1003 1005]
             :min_age 18})
```

The query will be automatically expanded to `... IN (1001, 1003, 1005)
...` under the hood, and work as expected.

Just remember that some databases have a limit on the number of values
in an `IN`-list, and Yesql makes no effort to circumvent such limits.

### Row And Result Processors

Like `clojure.java.jdbc`, Yesql accepts functions to pre-process each
row, and the final result, like so:

```sql
-- name: current-time
-- Selects the current time, according to the database.
SELECT sysdate
FROM dual;
```

```clojure
(defqueries "/some/where/queryfile.sql"
  {:connection db-spec})

;;; Without processors, this query returns a list with one element,
;;;   containing a map with one key:
(current-time)
;=> ({:sysdate #inst "2014-09-30T07:30:06.764000000-00:00"})

;;; With processors we just get the value we want:
(current-time {} {:result-set-fn first
                  :row-fn :sysdate
                  :identifiers identity})
;=> #inst "2014-09-30T07:30:06.764000000-00:00"
```

As with `clojure.java.jdbc` the default `:result-set-fn` is `doall`,
the default `:row-fn` is `identity`, and the default `:identifiers` is
`clojure.string/lower-case`.

_A note of caution_: Remember you're often better off doing your
processing directly in SQL. For example, if you're counting a million
rows, you can do it with `{:result-set-fn count}` or
`SELECT count(*) ...`. Both wil give the same answer, but the
SQL-version will avoid sending a million rows over the wire to do it.

### Insert/Update/Delete and More

To do `INSERT/UPDATE/DELETE` statements, you just need to add an `!`
to the end of the function name, and Yesql will execute the function
appropriately. For example:

```sql
-- name: save-person!
UPDATE person
SET name = :name
WHERE id = :id
```

```clojure
(save-person! {:id 1
               :name "Dave"})
;=> 1
```

A `!`-tagged function will return the number of rows affected.

`!` enables every statement type - not just `INSERT/UPDATE/DELETE` but
also `CREATE/DROP/ALTER/BEGIN/...` - anything your driver will
support.

### Insert, Returning Autogenerated Keys

There's one more variant: when you want to insert data and get back a
database-generated primary key, the driver requires a special call, so
Yesql needs to be specially-informed. You can do an "insert returning
autogenerated key" with the `<!` suffix, like so:

```sql
-- name: create-person<!
INSERT INTO person (name) VALUES (:name)
```

```clojure
(create-person<! {:name "Dave"})
;=> {:name "Dave" :id 5}
```

The exact return value will depend on your database driver. For
example PostgreSQL returns the whole row, whereas Derby returns just
`{:1 5M}`.

The `<!` suffix is intended to mirror `core.async`, so it should be easy to remember.

## Query Middlware (Experimental)

***Note: This section covers an experimental feature that is under
development. The interface is subject to review (by you!) and may
or may not continue to be supported its current form. Please
check it out and report back how it works and might be improved.***

Yesql now has support for middleware in a style modeled after
[Ring](https://github.com/ring-clojure/ring/wiki/Concepts#middleware).
Yesql middleware are higher-order functions that wrap queries,
allowing inspection and modification of both arguments and
results. The intent is for middlware to allowa single definition for
cross cutting concerns that apply to all of the queries defined within
a file. Possible uses include query logging, caching, alternative
sources for database connections, and various forms of result
post-processing.

Middleware is specified via a new `:middleware` option to `defquery`,
`defquery*`, and `defqueries`.

```clojure
(defquery current-time-query-middleware
  "yesql/sample_files/current_time.sql"
  {:middleware log-query-middleware})
```

The middleware itself is a higher order function over a query
function. (Note that the query is passed as a new value under
`call-options`, keyed by `:query`.)

```clojure
(def log-query-middleware
  (fn [ query-fn ]
    (fn [args call-options]
      (let [ query-name (get-in call-options [:query :name]) ]
        (println [ :begin query-name ])
        (let [ result (query-fn args call-options) ]
          (println [ :end query-name])
          result)))))
```

To allow middleware to be paramaterized per-query, the query
definition format has been extended to allow `info` attributes
to be specified, binding EDN values to specific names.

``` sql
-- name: users-by-country
-- info-value-1: { :edn :value-1 }
-- info-value-2: { :edn :value-2 }
SELECT *
FROM users
WHERE country_code = :country_code
```

The `info` values are accessible within the middleware through the
`:info` field of the query. (`(get-in call-options [:query :info :value-1])`)

## Development & Testing

Yesql uses the marvellous
[expectations library](http://jayfields.com/expectations/index.html)
for tests. It's like clojure.test, but has lighter-weight syntax and
much better failure messages.

Call `lein test` to run the test suite.
Call `lein test-all` to run the tests against all (supported) versions of Clojure.
Call `lein autoexpect` to automatically re-run the tests as source files change.

## Other Languages

Yesql has inspired ports to other languages:

|Language|Project|
|---|---|
|JavaScript|[jsyesql](https://github.com/fanatid/jsyesql)|
|JavaScript|[Preql](https://github.com/NGPVAN/preql)|
|JavaScript|[sqlt](https://github.com/eugeneware/sqlt)|
|Python|[aiosql](https://github.com/nackjicholson/aiosql)|
|Go|[DotSql](https://github.com/gchaincl/dotsql)|
|Go|[goyesql](https://github.com/nleof/goyesql)|
|C#|[JaSql](https://bitbucket.org/rick/jasql)|
|Ruby|[yayql](https://github.com/gnarmis/yayql)|
|Erlang|[eql](https://github.com/artemeff/eql)|
|Clojure|[YeSPARQL](https://github.com/joelkuiper/yesparql)|
|PHP|[YepSQL](https://github.com/LionsHead/YepSQL)|
|Haskell|[YeshQL](https://hackage.haskell.org/package/yeshql)|

## License

Copyright © 2013-2016 Kris Jenkins
Copyright © 2022-2024 Mike Schaeffer

Distributed under the Eclipse Public License, the same as Clojure.

## PS - Is Yesql An ORM?

No. There are no Objects here, only Values. Yesql is a VRM. This is
better because it's pronounced, "Vroom!"
