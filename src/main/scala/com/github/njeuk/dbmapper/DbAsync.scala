/*
 * Copyright 2022 Nick Edwards and collaborators
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

package com.github.njeuk.dbmapper

import com.github.mauricio.async.db._
import com.github.mauricio.async.db.pool.{ConnectionPool, PoolConfiguration}
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.mauricio.async.db.util.Log

import scala.async.Async._
import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * DbAsync is the main entry point for executing queries.
 *
 * All queries are executed asynchronously
 *
 * Example:
 *  {{{
 *
 *    import scala.async.Async._
 *    import com.github.njeuk.dbmapper.Implicits._
 *
 *    implicit def rowToBook: RowData => Book = (r) => DbCodeGenerator.rowToClass[Book](r)
 *    implicit def rowToAuthor: RowData => Author = (r) => DbCodeGenerator.rowToClass[Author](r)
 *
 *    def getBooks(authorName: String): Future[List[Book]] {
 *    async {
 *      val author = await(DbAsync.execOne[Author](q"select * author where name = \$authorName"))
 *      DbAsync.exec[Book](q"select * from book where author_id = \${author.authorId}")
 *    }
 *
 *  }}}
 *
 * The public methods expect two implicit parameters:
 * param f This is a function to convert from the returned database row, of type RowData to the entity object T.
 *          DbCodeGenerator.rowToClass will generate a function at compile time using a Scala Macros.
 *          Or you can hand craft the function.
 * param config This contains the configuration used by dbmapper to connect to the database, and details of what to log.
 *               import com.github.njeuk.dbmapper.Implicits._ will place a default config in scope, which reads the db
 *               connection string from the application.conf settings.  Or you can create your own specific settings
 *               as can be seen in the sample's attached to this project
 */
object DbAsync {

  /** Returns a list of T's created from executing the sql asynchronously
    *
    *  @param sql  the sql to execute
    *  @param values the arguments for the query
    *  @param f implicit function to transform the RowData to a T
    *  @param config implicit configuration settings
    *
    * Example:
    * {{{
    *  val books = exec[Book]("select * from book")
    *  val allTitles = DbAsync.exec[String]("select title from book order by book_id")(r => r(0).toString, dbAsyncConfig)
    * }}}
    */

  def exec[T](sql: String, values: Seq[Any] = List())(implicit f:(RowData) => T, config: DbAsyncConfig): Future[List[T]] = {
    val result = sendQuery(sql, values, config)
    result.map( qr =>
      qr.rows match {
        case Some(rowSet) => rowSet.map(f(_)).toList
        case None => List()
      }
    )
  }

  /** Returns a list of entity objects (T's) created from executing the sql asynchronously
    *
    *  @param q  the sql and arguments to execute, formed from SqlInterpolation
    *  @param f implicit function to transform the RowData to a T
    *  @param config implicit configuration settings
    *
    *  Example:
    *   {{{
    *          val price = 33.99
    *          val books = exec[Book](q"select * from book where retail_price > \$price")
    *          val aBook = execOne[Book](q"select * from '\$table where '\$column = \$price")
    *          val prices = List(33.99, 14.52)
    *          val moreBooks = exec[Book](q"select * from book where retail_price in (\$prices) order by book_id")
    *   }}}
    */

  def exec[T](q: SqlAndArgs)(implicit f:(RowData) => T, config: DbAsyncConfig): Future[List[T]] = {
    exec(q.sql, q.args)(f, config)
  }


  /** Asynchronously returns Option[T] containing Some single result from the query or None if there were no results.
    *
    * If more than one result then throws exception.
    *
    *  @param sql  the sql to execute
    *  @param values the arguments for the query
    *  @param f implicit function to convert sql RowData into the entity object T
    *  @param config implicit configuration settings
    *  @tparam T type of the entity objects expected to be returned
    *  @return Some(Entity Object) or None -- throws exception if more than one row is found
    *
    *   Example:
    *   {{{
    *     val book = execOneOrNone[Book]("select * from book where book_id = 123") // Some()
    *     val book = execOneOrNone[Book]("select * from book where book_id = -1") // None
    *     val book = execOneOrNone[Book]("select * from book") // Exception -- too many results
    *  }}}
    */

  def execOneOrNone[T](sql: String, values: Seq[Any] = List())(implicit f:(RowData) => T, config: DbAsyncConfig): Future[Option[T]] = {
    exec(sql, values)(f, config).map(c => {
      if (c.length > 1)
        throw new Error(s"execOneOrNone expects 1 or 0 rows, it got ${c.length} rows: ${displayableSql(sql, values)}")
      c.headOption
    })
  }

  /**
   * Returns one or zero results from executing the query.  Designed to be called from a queryInterpolated string
   *
   * @param q result from a querty Interpolated string i.e. q"some query"
   * @param f implicit function to convert sql RowData into the entity object T
   * @param config implicit configuration settings
   * @tparam T type of the entity objects expected to be returned
   * @return Some(Entity Object) or None -- throws exception if more than one row is found
   *
   *   Example:
   *   {{{
   *     val price = 33.44
   *     val book = execOneOrNone[Book](q"select * from book where retail_price = \$price") // Some()
   *     val book = execOneOrNone[Book](q"select * from book where book_id = -1") // None
   *     val book = execOneOrNone[Book](q"select * from book") // Exception -- too many results
   *  }}}
   */
  def execOneOrNone[T](q: SqlAndArgs)(implicit f:(RowData) => T, config: DbAsyncConfig): Future[Option[T]] = {
    execOneOrNone(q.sql, q.args)(f, config)
  }


  /** Asynchronously returns T containing single result from the query
    *
    *  If more than one result or no result then throws exception.
    *
    *  @param sql  the sql to execute
    *  @param values the arguments for the query
    *  @param f implicit function to transform the RowData to a T
    *  @param config implicit configuration settings
    *
    *   Example:
    *   {{{
    *     val book = execOne[Book]("select * from book where book_id = 123") // Book
    *     val book = execOne[Book]("select * from book where book_id = -1") // Exception -- no result
    *     val book = execOne[Book]("select * from book") // Exception -- too many results
    *   }}}
    */

  def execOne[T](sql: String, values: Seq[Any] = List())(implicit f:(RowData) => T, config: DbAsyncConfig): Future[T] = {
    exec(sql, values)(f, config).map(c => {
      if (c.length != 1)
        throw new Error(s"execOne expects exactly 1 row, it got ${c.length} rows: ${displayableSql(sql, values)}")
      c.head
    })
  }

  /** Version of execOne which expects a query generated from the query interpolation
    *
    * Example:
    * {{{
    *     val bookId = 123
    *     val noBookId = -1
    *     val book = execOne[Book](q"select * from book where book_id = \$bookId") // Book
    *     val book = execOne[Book](q"select * from book where book_id = \$noBookId") // Exception -- no result
    *     val book = execOne[Book](q"select * from book") // Exception -- too many results
    * }}}
    */

  def execOne[T](q: SqlAndArgs)(implicit f:(RowData) => T, config: DbAsyncConfig): Future[T] = {
    execOne(q.sql, q.args)(f, config)
  }


  /** Asynchronously executes query, no results are expected
    *
    * A Future[QueryResult] is returned, this is so that you can wait on the completion of the query if required.
    *
    *  @param sql  the sql to execute
    *  @param values the arguments for the query
    *  @param config implicit configuration settings
    *  @return returns a QueryResult, don't expect callers will use this apart from to wait on the result
    *          if they need to wait until the query has executed
    *
    *   Example:
    *   {{{
    *     execNonQuery[Book]("delete from book where book_id = 123")
    *
    *     // example of stalling until query executes
    *     async {
    *       await(execNonQuery[Book]("delete from book where book_id = 123"))
    *       // code here won't continue until delete has been executed
    *     }
    *
    *   }}}
    */

  def execNonQuery(sql: String, values: Seq[Any] = List())(implicit config: DbAsyncConfig): Future[QueryResult] = {
    sendQuery(sql, values, config)
  }

  /**
   * Version of execNonQuery which takes a query interpolation generated query
   *
   *  Example:
   *   {{{
   *     val id = 123
   *     execNonQuery[Book](q"delete from book where book_id = \$id")
   *   }}}
   */

  def execNonQuery(q: SqlAndArgs)(implicit config: DbAsyncConfig): Future[QueryResult] = {
    execNonQuery(q.sql, q.args)(config)
  }

  /** Explicitly close down the database connections, releasing any resources.
    *
    * Not a big deal to do, will happen automatically in most instances when app closes.
    */
  def shutdown: Unit = pools.foreach(cp => cp._2.close)

  ///////////////////////////////////////////////////////////

  private final val log = Log.getByName("dbmapper")

  private def sendQuery(sql: String, values: Seq[Any], config: DbAsyncConfig): Future[QueryResult] = {
    sendQuery(sql, values, cn(config), config)
  }

  private def sendQuery(sql: String, values: Seq[Any], cn: Connection, config: DbAsyncConfig): Future[QueryResult] = {
    val f = async {
      val start = System.currentTimeMillis()
      val result = await(cn.sendPreparedStatement(sql, values))
      val elapsed = System.currentTimeMillis() - start
      logSql(elapsed, sql, values, config)
      result
    }
    f.onFailure( {case e:Exception => logSqlError(e, sql, values) })
    f
  }

  val factories:  mutable.HashMap[DbAsyncConfig, PostgreSQLConnectionFactory] = mutable.HashMap.empty

  private def getFactory(config: DbAsyncConfig) : PostgreSQLConnectionFactory = {
    synchronized {
      factories.getOrElse(config, {
        val factory = new PostgreSQLConnectionFactory(config.configuration)
        factories += (config -> factory)
        factory
      })
    }
  }

  val pools: mutable.HashMap[DbAsyncConfig, ConnectionPool[_]] = mutable.HashMap.empty

  private def cn(config: DbAsyncConfig) : Connection = {
    synchronized {
      pools.get(config) match {
        case None =>
          log.debug(s"Creating new connection pool : ${config}")
          val pool = new ConnectionPool(getFactory(config), config.poolConfiguration)
          Await.result(pool.connect, 5 seconds)
          pools += (config -> pool)
          pool
        case Some(pool) =>
          if (pool.isClosed) {
            log.debug(s"Replacing closed connection pool : ${config}")
            val replacement = new ConnectionPool(getFactory(config), config.poolConfiguration)
            Await.result(replacement.connect, 5 seconds)
            pools.put(config, replacement)
            replacement
          }
          else
            pool
      }
    }
  }

  private def displayableSql(sql: String, values: Seq[Any]) : String = {
    val s = sql.split('?')
    val z = s.zip(values)
    val q = z.foldLeft("")((a,b) => a + s"${b._1} '${b._2}'")
    val e = if (s.length > z.length) s.reverse.head else ""
    q + e
  }

  private def logSql(elapsed: Long, sql: String, values: Seq[Any], config: DbAsyncConfig) {
    if (elapsed > config.logQueriesLongerThan.toMillis || config.logQueriesLongerThan.toMillis == 0)
      log.info(f"${elapsed}%3sms : ${displayableSql(sql, values)}")
  }

  private def logSqlError(e: Throwable, sql: String, values: Seq[Any]) {
    log.error(s"Failed query : ${displayableSql(sql, values)} - ${e.getMessage}")
  }

}
