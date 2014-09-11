/*
 * Copyright 2014 Comdevelopment Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.njeuk.dbmapper.examples

import com.github.mauricio.async.db.RowData
import com.github.mauricio.async.db.postgresql.util.URLParser
import com.github.njeuk.dbmapper.{DbAsyncConfig, DbAsync}
import com.github.njeuk.dbmapper.macros.DbCodeGenerator
import com.github.njeuk.dbmapper.SqlInterpolation._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}
import org.joda.time.{Interval, LocalDate, LocalDateTime}
import com.github.njeuk.dbmapper.RowDataExtension.RowDataExtensionImplicit

import scala.concurrent.duration.Duration

/** This sample shows how to do straight forward SQL queries with dbmapper.
  * All queries are executed with either the function exec, execOne, execOneOrNone - returning
  * a list of results, exactly one result, or an Option result respectively.
  *
  */

class Queries extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfter {

  /** Results from the Query are mapped onto the function's generic Type, Book in this sample.
  * This performed by the implicit function rowToBook in the sample.
  * The code for rowToBook is generated via a Scala Macro, however this is optional and can be
  * hand crafted for special cases.
  */
  implicit def rowToBook: RowData => Book = (r) => DbCodeGenerator.rowToClass[Book](r)

  /** The implicit DbAsyncConfig is used to pass connection string configuration and other state
    * into the queries.  Here we initialise it with a jdbc url, there are other options to simply init
    * directly via PlayFramework application.config info.
    */
  implicit val dbAsyncConfig = DbAsyncConfig(URLParser.parse("jdbc:postgresql://localhost/dbmappersamples?user=postgres"), Duration("0 ms"))

  "dbmapper" should "do simple queries simply" in {
    val allBooks = DbAsync.exec[Book]("select * from book order by book_id").futureValue
    allBooks.length should equal (3)
    allBooks.head should equal (Book(1, "The Art Of Computer Programming", 134.41, new LocalDate(2011, 3, 3) ))
  }

  it should "support query parameters without lots of fuss" in {
    val maxPrice = 100
    val allBooks = DbAsync.exec[Book](q"select * from book where retail_price < $maxPrice order by book_id").futureValue
    allBooks.length should equal (2)
    allBooks.head should equal (Book(2, "Sql for Smarties", 29.73, new LocalDate(2010, 11, 1) ))
  }

  it should "let you specify if you only expect one row" in {
    val oneBook = DbAsync.execOne[Book]("select * from book where retail_price = 29.73").futureValue
    oneBook.title should equal ("Sql for Smarties")

    an[Exception] should be thrownBy {
      val tooMany = DbAsync.execOne[Book]("select * from book where retail_price > 1").futureValue
    }
    an[Exception] should be thrownBy {
      val tooFew = DbAsync.execOne[Book]("select * from book where retail_price > 500").futureValue
    }
  }

  it should "also let you specify you expect one or no rows" in {
    val noBook = DbAsync.execOneOrNone[Book]("select * from book where retail_price > 500").futureValue
    noBook should equal (None)
  }

  it should "allow you to specify the mapping from RowData to the Type" in {
    val allTitles = DbAsync.exec[String]("select title from book order by book_id")(r => r(0).toString, dbAsyncConfig).futureValue
    allTitles.length should equal (3)
    allTitles.head should equal ("The Art Of Computer Programming")
  }

  it should "allow interpolation of lists" in {
    val prices = List("35.99", "29.73")
    val books = DbAsync.exec[Book](q"select * from book where retail_price in ($prices) order by book_id").futureValue
    books.length should equal (2)
    books.head.title should equal ("Sql for Smarties")
  }

  it should "allow interpolation anywhere, not just arguments" in {
    val table = "book"
    val column = "retail_price"
    val price = "35.99"

    val aBook = DbAsync.execOne[Book](q"select * from '$table where '$column = $price").futureValue
    aBook.title should equal ("Functional Programming in Scala")
  }

  before {
    DbAsync.execNonQuery("drop table if exists book").futureValue
    DbAsync.execNonQuery("create table book(book_id serial, title text, retail_price numeric(10,2), publish_date date)").futureValue
    DbAsync.execNonQuery("insert into book(book_id, title, retail_price, publish_date) values " +
      "(1, 'The Art Of Computer Programming', 134.41, '3 Mar 2011')," +
      "(2, 'Sql for Smarties', 29.73, '1 Nov 2010')," +
      "(3, 'Functional Programming in Scala', 35.99, '1 Aug 2014')").futureValue
  }

}

case class Book(
  bookId: Int,
  title: String,
  retailPrice: BigDecimal,
  publishDate: LocalDate)
