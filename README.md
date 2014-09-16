# Yesql - Clojure & SQL rethought.

Yesql is a Clojure library for _using_ SQL.

[![Build Status](https://travis-ci.org/krisajenkins/yesql.png?branch=travis)](https://travis-ci.org/krisajenkins/yesql)

## Installation

Add this to your [Leiningen](https://github.com/technomancy/leiningen) `:dependencies`:

``` clojure
[yesql "0.4.0"]
```

Plus you'll want a database driver. Here are some examples (but double
check, because there may be a newer version available):

|Database|`:dependencies` Entry|
|---|---|
|PostgreSQL|`[org.postgresql/postgresql "9.3-1102-jdbc41"]`|
|MySQL|`[mysql/mysql-connector-java "5.1.32"]`|
|Oracle|`[com.oracle/ojdbc14 "10.2.0.4.0"]`|
|SQLite|`[org.xerial/sqlite-jdbc "3.7.2"]`|
|Derby|`[org.apache.derby/derby "10.11.1.1"]`|

(If you know of others, please do submit a pull request for this document.)

## Rationale

You're writing Clojure. You need to write some SQL.

I think we're all agreed that this is a problem:

``` clojure
(query "SELECT * FROM users WHERE country_code = ?" "GB")
```

Unless these query strings are short, they quickly get hard to read
and hard to rewrite. Plus the lack of indentation & syntax
highlighting is horrible.

But something like this is not the solution:

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
SELECT *
FROM users
WHERE country_code = ?
```

...and then pull it in and use it as a regular Clojure function:

``` clojure
(defquery users-by-country "some/where/users_by_country.sql")
(users-by-country db-spec "GB")
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

Create an SQL query. Note we can supply named parameters and a comment string:

```sql
-- Counts the users in a given country.
SELECT count(*) AS count
FROM user
WHERE country_code = :country_code
```

Make sure it's on the classpath. For this example, it's in
`src/some/where/`. Now we can use it in our Clojure program.

```clojure
; Import the SQL query as a function.
(require '[yesql.core :refer [defquery]])
(defquery users-by-country "some/where/users_by_country.sql")
```

Lo! It has automatic, useful docstrings in the REPL:

```clojure
(clojure.repl/doc users-by-country)

;=> -------------------------
;=> user/users-by-country
;=> ([db country_code])
;=>   Counts the users in a given country.
```

Now we can use it:

```clojure
; Define a database connection spec. (This is standard clojure.java.jdbc.)
(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname "//localhost:5432/demo"
              :user "me"})

; Use it standalone. Note that the first argument is the db-spec.
(users-by-country db-spec "GB")
;=> ({:count 58})

; Use it in a clojure.java.jdbc transaction.
(require '[clojure.java.jdbc :as jdbc])
(jdbc/with-db-transaction [connection db-spec]
   {:limeys (users-by-country connection "GB")
    :yanks  (users-by-country connection "US")})
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
(defqueries "some/where/queryfile.sql")
```

`defqueries` returns a sequence of the vars it binds, which can be
useful feedback while developing.

As with `defquery`, each function will have a docstring based on the
comments, and a sensible argument list based on the query parameters.

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
(defqueries "some/where/queryfile.sql")
(find-users db-spec [1001 1003 1005] 18)
```

The query will be automatically expanded to `... IN (1001, 1003, 1005)
...` under the hood, and work as expected.

Just remember that some databases have a limit on the number of values
in an `IN`-list, and Yesql makes no effort to circumvent such limits.

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
(save-person! db-spec "Dave" 1)
;=> 1
```

A `!`-tagged function will return the number of rows affected.

`!` enables every statement type - not just `INSERT/UPDATE/DELETE` but
also `CREATE/DROP/ALTER/BEGIN/...` - anything your driver will
support.

#### Insert, Returning Autogenerated Keys

There's one more variant: when you want to insert data and get back a
database-generated primary key, the driver requires a special call, so
Yesql needs to be specially-informed. You can do an "insert returning
autogenerated key" with the `<!` suffix, like so:

```sql
-- name: create-person<!
INSERT INTO person ( name ) VALUES ( :name )
```

```clojure
(create-person<! db-spec "Dave")
;=> {:name "Dave" :id 5}
```

The exact return value will depend on your database driver. For
example PostgreSQL returns the whole row, whereas Derby returns just
`{:1 5M}`.

The `<!` suffix is intended to mirror `core.async`, so it should be easy to remember.

## Other Languages

Yesql has inspired ports to other languages:

|Language|Project|
|---|---|
|JavaScript|[Preql](https://github.com/NGPVAN/preql)|
|JavaScript|[sqlt](https://github.com/eugeneware/sqlt)|
|Python|[Anosql](https://github.com/honza/anosql)|

## Development & Testing

Yesql uses the marvellous
[expectations library](http://jayfields.com/expectations/index.html)
for tests. It's like clojure.test, but has lighter-weight syntax and
much better failure messages.

Call `lein test` to run the test suite.  
Call `lein test-all` to run the tests against all (supported) versions of Clojure.  
Call `lein autoexpect` to automatically re-run the tests as source files change.  

## Status

Ready to use. The API is subject to change. Feedback is welcomed.

## License

Copyright © 2013-2014 Kris Jenkins

Distributed under the Eclipse Public License, the same as Clojure.
