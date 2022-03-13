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

import com.github.njeuk.dbmapper.SqlInterpolation._
import com.github.njeuk.dbmapper.RowDataStandardImplicitConversions._
import com.github.njeuk.dbmapper.macros.CodeToSql
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.mauricio.async.db.{QueryResult, RowData}
import scala.concurrent.Future
import scala.language.implicitConversions

/**
 * TableAccess lets you easily implement a 'Table Data Gateway' as per Fowler PoEAA
 *
 * Example:
 * {{{
 *   class SuperHeroAccess extends TableAccess[SuperHero](
 *      () => DbCodeGenerator.codeToSql[SuperHero](),
 *     (r) => DbCodeGenerator.rowToClass[SuperHero](r),
 *     (u) => DbCodeGenerator.updateSql[SuperHero](u),
 *     (i) => DbCodeGenerator.insertSql[SuperHero](i),
 *     (i) => DbCodeGenerator.identityInsertSql[SuperHero](i),
 *     (t, i) => t.copy(superHeroId = i)) {
 *
 *      def getViaName(name: String)(implicit config: DbAsyncConfig): Future[Option[SuperHero]] = {
 *      DbAsync.execOneOrNone[SuperHero](q"select * from super_hero where name = \$name")
 *    }
 *
 *    case class SuperHero(
 *      superHeroId: Int = 0,
 *      name: String = "",
 *      wearsTights: Boolean = false,
 *      partner: Option[String] = None
 *    )
 * }}}
 *
 * The above code creates a Table Data Gateway to access the SuperHeroes.
 *
 * Each SuperHero is represented by a row in the table 'super_hero'.
 *
 * The DbCodeGenerator is a Scala Macro based class which builds data mappers
 * based on the names of the Entity object SuperHero.  The generated code can be replaced
 * by hand crafted SQL where needed.
 *
 * Once you have the SuperHeroAccess you can used standard CRUD activities on the SuperHeroes.
 * For example:
 * {{{
 *    async {
 *      val hero = await( superHeroAccess.load(1) )
 *      superHeroAccess.update( hero.copy(wearsTights = true) )
 *    }
 * }}}
 * for a full example, see the sample in the test package of this project.
 *
 * Using a Table Data Gateway is great for testing, it makes it very simple to mock out
 * the database access.
 *
 * @param codeToSql an instance of the CodeToSql class, providing information about T to guide the SQL construction
 * @param rowToClass a data mapper to convert the RowData to the entity object (T)
 * @param updateSql the sql used for updating the table
 * @param insertSql the sql used for inserting new rows in the table, using the default identity (usually a primary key) (usually a generated from a postgres Sequence)
 * @param identityInsertSql the sql needed to insert rows when the inserted object is providing the identity
 * @param copy a function to produce a copy of the Entity (T) with the identity replaced
 * @tparam T the type of the Entity object the TableAccess is providing access to
 */

abstract class TableAccess[T](
  val codeToSql: () => CodeToSql,
  val rowToClass: RowData => T,
  val updateSql: T => (String, Seq[Any]),
  val insertSql: T => (String, Seq[Any]),
  val identityInsertSql: T => (String, Seq[Any]),
  val copy: (T, Int) => T
  )
{
  implicit def rowDataToTypeImplicit(r: RowData): T = rowToClass(r)

  /**
   * Gets the Entity (T) with the specified id.  If it doesn't exist None is returned.
   *
   * @param id the identity of the row containing the Entity (T)
   * @param config implicit configuration to use
   * @return returns Some(Entity) or None if it doesn't exist
   *
   * Example:
   * {{{
   *    async {
   *      val hero = await(superHeroAccess.get(2))
   *      println hero.get.name
   *    }
   * }}}
   */
  def get(id: Int)(implicit config: DbAsyncConfig) : Future[Option[T]] = {
    DbAsync.execOneOrNone(q"select * from '${codeToSql().tableName} where '${codeToSql().identityColumn} = $id")
  }

  /**
   * Loads the Entity from the underlying table.  The entity is expected to exist, if not
   * throws an exception.
   *
   * @param id the identity of the row containing the Entity (T)
   * @param config implicit configuration to use
   * @return the Entity
   *
   * Example:
   * {{{
   *    async {
   *      val hero = await(superHeroAccess.load(2))
   *      println hero.name
   *
   *      val BANG = await(superHeroAccess.load(-123)) // would throw exception assuming not -123 exists!
   *    }
   * }}}
   */
  def load(id: Int)(implicit config: DbAsyncConfig): Future[T] = {
    get(id).map(_.getOrElse(throw new Error(s"${codeToSql().tableName} ${codeToSql().identityColumn} $id not found")))
  }

  /**
   * Deletes the entity with the specified id
   * @param id the identity of the row containing the Entity (T)
   * @param config implicit configuration to use
   * @return returns a QueryResult, but really it is expected the result is used just
   *         to ensure the delete has been performed before continuing
   *
   * Example:
   * {{{
   *    async {
   *      await(superHeroAccess.delete(1))
   *      // ignoring return, but code after this point will be continued once delete is performed
   *    }
   * }}}
   */
  def delete(id: Int)(implicit config: DbAsyncConfig): Future[QueryResult] = {
    DbAsync.execNonQuery(q"delete from '${codeToSql().tableName} where '${codeToSql().identityColumn} = $id")
  }

  /**
   * Updates the specified entity
   *
   * @param u the entity to be updated
   * @param config implicit configuration info
   * @return the update entity, this isn't retrieved from the database, so any db calculated
   *         columns won't have been updated
   *
   * Example:
   * {{{
   *    async {
   *      val hero = await( superHeroAccess.load(1) )
   *      superHeroAccess.update( hero.copy(wearsTights = true) )
   *    }
   * }}}
   */
  def update(u: T)(implicit config: DbAsyncConfig): Future[T] = {
    val (sql, args) = updateSql(u)
    DbAsync.execNonQuery(sql, args)
      .map(c => u)     // does nothing just allows callers to wait on completion
  }

  /**
   * Inserts the entity into the database.  This uses the database default for the id
   * column, which is typically auto generated from some Postgres Sequence.
   *
   * @param u the entity to be updated, the value in the id is ignored
   * @param config the implicit configuration info
   * @return the entity with its new id.  The inserted row isn't retrieved from the
   *         database, so any db calculated values (apart from the identity) won't be changed
   *
   * Example:
   * {{{
   *    async {
   *      val catwoman = SuperHero(0, "Catwoman")
   *      val insertedCatwoman = await( superHeroAccess.insert(catwoman) )
   *      val loadedCatwoman = await( superHeroAccess.load(insertedCatwoman.superHeroId ))
   *    }
   * }}}
   */
  def insert(u: T)(implicit config: DbAsyncConfig): Future[T] = {
    val (sql, args) = insertSql(u)
    DbAsync.execOneOrNone[Int](sql, args)
      .map(c => copy(u, c.get))
  }

  /**
   * Inserts the entity into the database, where a specific value for the identity column is provided.
   *
   * @param u  the entity to be updated, the id column must not exist in the database yet
   * @param config info for configuration
   * @return the inserted entity. The inserted row isn't retrieved from the
   *         database, so any db calculated values won't have changed
   *
   * Example:
   * {{{
   *    val wonderWoman = SuperHero(666, "Wonder Woman", true)
   *    superHeroAccess.identityInsert(wonderWoman)
   * }}}
   */
  def identityInsert(u: T)(implicit config: DbAsyncConfig): Future[T] = {
    val (sql, args) = identityInsertSql(u)
    DbAsync.execOneOrNone[Int](sql, args)
      .map(c => copy(u, c.get))
  }

}
