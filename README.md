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
  implicit def rowToBook: RowData => Book = (r) => DbCodeGenerator.rowToClass\[Book\](r)
  
  // query returning future list of books, safe query interpolation, converting maxPrice to a query argument  
  val maxPrice = 11.99
  val allBooksFuture = DbAsync.exec\[Book\](q"select * from book where retail_price < $maxPrice")        
  
  val oneBook: Future\[Book\] = DbAsync.execOne\[Book\](q"select * from book where book_id = 2")
 
  // returns Future\[Option\[\]\]     
  val maybeOneBook: Future\[Option\[Book\]\] = DbAsync.execOneOrNone\[Book\](q"select * from book where book_id = -123")
      
```

CRUD via Data Access object (Data Table Gateway)
------------------------------------------------

```scala

case class SuperHero(
  superHeroId: Int = 0,
  name: String = "",
  wearsTights: Boolean = false,
  partner: Option\[String\] = None
)

class SuperHeroAccess extends TableAccess[SuperHero](
  () => DbCodeGenerator.codeToSql[SuperHero](),
  (r) => DbCodeGenerator.rowToClass[SuperHero](r),
  (u) => DbCodeGenerator.updateSql[SuperHero](u),
  (i) => DbCodeGenerator.insertSql[SuperHero](i),
  (i) => DbCodeGenerator.identityInsertSql[SuperHero](i),
  (t, i) => t.copy(superHeroId = i)) {}

// get SuperHero with id 2, all results are Future\[\] 
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

[Best Starting Point](https://njeuk.github.io/dbmapper/latest/api/#com.github.njeuk.dbmapper.DbAsync$)


Dependencies
------------

The artifacts are hosted in JCenter on Bintray.

If you are using SBT >= 0.13.5 then the Bintray resolver is already known, just add the following to your build.sbt:
 
```
"com.github.njeuk" %% "dbmapper" % "2.3.19"
```

If you are using SBT < 0.13.5, then you need to add a resolver for BinTray, see here: https://github.com/softprops/bintray-sbt

License
-------
This project is freely available under the Apache 2 licence, have fun....












