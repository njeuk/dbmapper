dbmapper - simple asynchronous Postgres db access for Scala  
===========================================================

[![Build Status](https://travis-ci.org/njeuk/dbmapper.svg?branch=master)](https://travis-ci.org/njeuk/dbmapper)
[![Coverage Status](https://coveralls.io/repos/njeuk/dbmapper/badge.png?branch=master)](https://coveralls.io/r/njeuk/dbmapper?branch=master)

Features
--------
* Wraps [postgresql-async](https://github.com/mauricio/postgresql-async) to provide a simple asynchronous access to Postgres
* SQL Queries with interpolation
* Macro generated mapping layer from Scala type to Database Table
* Provides a Table Data Gateway model [P of EAA](http://martinfowler.com/eaaCatalog/tableDataGateway.html)  

Not Features
------------
* No new Domain Specific Language to access data.  You know a pretty good database access DSL, it is called SQL. This is not [Typesafe's Slick](https://github.com/slick/slick]Slick)
* No ORM.  No Hibernate magic.

Queries with interpolation for query arguments
----------------------------------------------

```scala
  
  // Scala class that maps to the book table, with columns to match the class members
  case class Book(
    bookId: Int,
    title: String,
    retailPrice: BigDecimal,
    publishDate: LocalDate)
    
  // mapping function from table row to Book class, auto generated at compile time by Scala Macro  
  implicit def rowToBook: RowData => Book = (r) => DbCodeGenerator.rowToClass[Book](r)
  
  // query returning future list of books, safe query interpolation, converting maxPrice to a query argument  
  val maxPrice = 11.99
  val allBooksFuture = DbAsync.exec[Book](q"select * from book where retail_price < $maxPrice")        
  
  val oneBook: Future[Book] = DbAsync.execOne[Book](q"select * from book where book_id = 2")
 
  // returns Future[Option[]]     
  val maybeOneBook: Future[Option[Book]] = DbAsync.execOneOrNone[Book](q"select * from book where book_id = -123")
      
```

CRUD via Data Access object (Data Table Gateway)
------------------------------------------------

```scala

case class SuperHero(
  superHeroId: Int = 0,
  name: String = "",
  wearsTights: Boolean = false,
  partner: Option[String] = None
)

class SuperHeroAccess extends TableAccess[SuperHero](
  () => DbCodeGenerator.codeToSql[SuperHero](),
  (r) => DbCodeGenerator.rowToClass[SuperHero](r),
  (u) => DbCodeGenerator.updateSql[SuperHero](u),
  (i) => DbCodeGenerator.insertSql[SuperHero](i),
  (i) => DbCodeGenerator.identityInsertSql[SuperHero](i),
  (t, i) => t.copy(superHeroId = i)) {}

// get SuperHero with id 2, all results are Future[] 
val hero = superHeroAccess.get(2)

// update Batman
val batman = await( superHeroAccess.load(1) )
superHeroAccess.update(batman.copy(partner = Some("Robin")))

// insert Catwoman
val catwoman = SuperHero(0, "Catwoman")
superHeroAccess.insert(catwoman)

// delete Batman
superHeroAccess.delete(1)

```

Full Code Samples
-----------------

[Executable Code Samples](https://github.com/njeuk/dbmapper/tree/master/src/test/scala/com/github/njeuk/dbmapper/examples)

The default connection string is for the samples is:

`jdbc:postgresql://localhost/dbmappersamples?user=postgres&password=`

Either change the connection string for your environment, or create a database named *dbmappersamples*, accessible by the postgres user with no password.


Full ScalaDocs
--------------

[ScalaDocs](https://njeuk.github.io/dbmapper/latest/api) on GitHub

[Best Starting Point](https://njeuk.github.io/dbmapper/latest/api/#com.github.njeuk.dbmapper.DbAsync$) in the docs


Dependencies
------------

The artifacts are hosted in JCenter on Bintray.

If you are using SBT >= 0.13.5 then the Bintray resolver is already known, just add the following to your build.sbt:
 
```
"com.github.njeuk" %% "dbmapper" % "2.3.19"
```

If you are using SBT < 0.13.5, then you need to add a resolver for BinTray, see here: https://github.com/softprops/bintray-sbt

Quick start
----------

1. Place the dependency in you libDependencies of build.sbt  

```
    libraryDependencies ++= Seq(
      ...
      "org.scalatestplus" % "play_2.11" % "1.1.0" % "test",
      "com.github.njeuk" %% "dbmapper" % "2.3.19"
    )
```

2. Create a Case Class to represent the table in the database.
  For example, assume there is a table called books:
  
```sql
  create table book(
    book_id serial, 
    title text not null, 
    retail_price numeric(10,2) not null, 
    publish_date date not null)
```

The corresponding Case Class would look like:
 
 ```scala 
  case class Book(
    bookId: Int,
    title: String,
    retailPrice: BigDecimal,
    publishDate: LocalDate)
```

What happens if you don't like the name mapping conventions between from Scala and the DB?
  
 You can override them with @attributes, see [this sample](https://github.com/njeuk/dbmapper/blob/master/src/test/scala/com/github/njeuk/dbmapper/examples/CrudSqlWithCustomNameMapping.scala)
  
3. Ensure that the implicit object DbAsyncConfig is in scope.  

You can setup the config explicitly like this:

```implicit val dbAsyncConfig = DbAsyncConfig(URLParser.parse("jdbc:postgresql://localhost/dbmappersamples?user=postgres&password="), Duration("500 ms"))```

Or more simply just import:

```import com.github.njeuk.dbmapper.Implicits._```

which will create a config that pulls [PlayFramework style database settings](https://www.playframework.com/documentation/2.3.x/SettingsJDBC) from your application.conf details.  We only look at username/password/url.

4. Ensure there is an implicit function in scope to map a the database rowData to the Scala object.
 
```implicit def rowToBook: RowData => Book = (r) => DbCodeGenerator.rowToClass[Book](r)```

This one will generate the code based on the names in the Book class.

5. Write your queries:

```
async {
  val allBooks = await( DbAsync.exec[Book]("select * from book") )
  val titles = allBooks.map(b => b.title)
  println(s"Title where $titles")
}
```

How to use
----------

### Async code
dbmapper is asynchronous.  We have used this codebase to implement completely asynchronous PlayFramework websites.
We find using the Scala Async/Await mechanism the most straight forward to deal with Future results.

For example:

```scala
import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.njeuk.dbmapper.Implicits._

...

async {
  val batmanFuture = superHeroAccess.load(1)
  
  val batman = await(batmanFuture)
  // code here continues after batmanFuture succeeds
    
  // now we have found batman, update his partner
  await(superHeroAccess.update(batman.copy(partner = Some("Robin"))))
  
  // load updated Batman,  note await in the line above stalls this code until the update has completed 
  val batmanWithRobin = superHeroAccess.load(1)
  batmanWithRobin.partner should be (Some("Robin"))
}

...

```
### Name mapping conventions / db schema assumptions

dbmapper needs to map from the scala identifier to the database identifier.  To do this it makes
certain assumptions about the identifier names.  These assumptions can be changed via attributes 
in your code, see the example [CrudSqlWithCustomNameMapping](https://github.com/njeuk/dbmapper/blob/master/src/test/scala/com/github/njeuk/dbmapper/examples/CrudSqlWithCustomNameMapping.scala)
                                 
The default assumptions are:

Scala identifiers are in [camelCase](http://en.wikipedia.org/wiki/CamelCase) or PascalCase, these are represented in the database
by identifiers in lowercase [snake_case](http://en.wikipedia.org/wiki/Snake_case).

The table name is not pluralised, but is a straight conversion from the class name.
 
Each table has an Integer [surrogate key](http://en.wikipedia.org/wiki/Surrogate_key).  
The surrogate key is named \<class\>Id (e.g. bookId) in Scala and \<table\>_id (e.g. book_id) in the database.

If a variable is defined as an Option[] then dbmapper will convert null column values to None.
Otherwise null column values will cause an exception.

See [ScalaToSqlNameConversion](https://github.com/njeuk/dbmapper-macros/blob/master/src/main/scala/com/github/njeuk/dbmapper/macros/ScalaToSqlNameConversion.scala)

### DB Connections
dbmapper needs to pass the data base connection information on to postgresql-async to access the database.

All the of the public dbmapper functions take an implicit arguments of type DbAsyncConfig.  
This contains the connection information and details about what logging is wanted.

So prior to calling into dbmapper there needs to be a DbAsyncConfig in scope.

You have a number of ways to do this.

The easiest method is to:
```import com.github.njeuk.dbmapper.Implicits._```

This uses a DbAsyncConfig which gets the connection information from config file settings / jvm properties using [com.typesafe.config](https://github.com/typesafehub/config).
DbAsyncConfig expects PlayFramework style db configurations.
Read their documents for the details of locations looked, some like this in application.conf works:

```
db.default.url="jdbc:postgresql://some-domain-name-here/some-db-name-here"
db.default.user=your-db-user-here
db.default.password=your-password-here
```

You can also construct a DbAsyncConfig directly:

```implicit val dbAsyncConfig = DbAsyncConfig(URLParser.parse("jdbc:postgresql://localhost/dbmappersamples?user=postgres&password="), Duration("500 ms"))```

### Code Generators

dbmapper uses Scala Macros to build code to map between the database row data and the scala class.

The Macros run at compile time, of course, and exist in the project [dbmapper-macros](https://github.com/njeuk/dbmapper-macros).
The project is split to help with testing of dbmapper.  The macros project is a dbmapper dependency, and thus automatically obtained from JCenter, you don't need to be aware of it.

If you have specific requirements, you could handcraft the code the macros generate.

See [DbCodeGenerator](https://github.com/njeuk/dbmapper-macros/blob/master/src/main/scala/com/github/njeuk/dbmapper/macros/DbCodeGenerator.scala#L164)

### Query interpolation

To make SQL queries simpler dbmapper provide query interpolation.

This is like string interpolation, but the embed variable references are converted to argument for the database call.

This provides a clean coding experience which is also protected from SQL Injection.
The string is prefixed with a 'q' to signal Query Interpolation.

For example:

```scala
val name = "Bruce"
val bruce = DbAsync.exec[Person](q"select * from person where name = $name")
```

Lists and direct string interpolation are also supported, see the [scaladocs](http://njeuk.github.io/dbmapper/latest/api/#com.github.njeuk.dbmapper.SqlInterpolation$) for more examples.

### Data access objects / Table Data Gateway

Using the Macro generated code and the table mappings, you can easily build a Data Access object.
The full example of this is shown in [CrudSql.scala](https://github.com/njeuk/dbmapper/blob/master/src/test/scala/com/github/njeuk/dbmapper/examples/CrudSql.scala)

### Logging

By default, dbmapper will log any query that takes longer than 500ms.  This is controlled via a setting in DbAsyncConfig.
Setting this to zero will cause dbmapper to log all SQL that it executes with the execution lapsed time.

### Errors you might get

A common compile error with dbmapper is:

   `not found: type CodeToSql
     () => DbCodeGenerator.codeToSql[T]()`
 
you need:
     ```import com.github.njeuk.dbmapper.macros.CodeToSql```

Limitations
-----------

### Postgresql only.  
Extending to MySql would be simple, but not implemented because I have no need for it.  (You should probably think very
 carefully about implementing a new project in MySql anyway ;-) )
   
Sql Server and Oracle would be a lot harder, you need a replacement for [postgresql-async](https://github.com/mauricio/postgresql-async), not impossible,
just not a straightforward job to do.

### Hard coded identifier mappings.  
The code generation assumes that you use camelCase in Code and snake_case in the database.  It also assumes that table names 
are not pluralized unlike Rails ActiveRecord.  

You can override theses assumptions on a case by case basis using @attributes. But if you do this a lot your code is going to look pretty horrendous.  
 Maybe we could pass the algorithm in to the code generators and evaluate them using [Twitters Eval](https://github.com/twitter/util/blob/master/util-eval/src/main/scala/com/twitter/util/Eval.scala).
 Not sure how bad this will impact compile speeds.

### Code looks more like Java than Haskell 
The code isn't really massively idiomatic functional Scala.  On the plus side, there are no loops in the code :-)

### Only built for Scala 2.11.2 and above
Haven't built or tested for other older Scala versions.  It probably will work, but no idea, I don't use those version any more.


License
-------
This project is freely available under the Apache 2 licence, have fun....












