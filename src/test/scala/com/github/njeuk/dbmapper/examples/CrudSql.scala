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

import com.github.mauricio.async.db.postgresql.util.URLParser
import com.github.njeuk.dbmapper.macros._
import com.github.njeuk.dbmapper.{TableAccess, DbAsync, DbAsyncConfig}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}
import com.github.njeuk.dbmapper.RowDataExtension.RowDataExtensionImplicit
import com.github.njeuk.dbmapper.SqlInterpolation._
import scala.concurrent.Future
import scala.concurrent.duration.Duration

/** CrudSql sample shows CRUD style operations are using dbmapper.
  * In the sample a class SuperHeroAccess is used to perform the operations.
  * This contains scala macro generated code to map between Scala classes and the database table.
  *
  */

class CrudSql extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfter {
  implicit val dbAsyncConfig = DbAsyncConfig(URLParser.parse("jdbc:postgresql://localhost/dbmappersamples?user=test&password=test"), Duration("0 ms"))
  val superHeroAccess = new SuperHeroAccess

  "dbmapper" should "get via the id, the id may not exist" in {
      val hero = superHeroAccess.get(2).futureValue
      hero should be (Some(SuperHero(2, "Iron Man", false, None)))

      val noSuchHero = superHeroAccess.get(123).futureValue
      noSuchHero should be (None)
  }

  it should "load via the id, the id must exist" in {
    val hero = superHeroAccess.load(2).futureValue
    hero should be (SuperHero(2, "Iron Man", false, None))

    an[Exception] should be thrownBy {
      val noSuchHero = superHeroAccess.load(123).futureValue
    }
  }

  it should "update easily" in {
    val batman = superHeroAccess.load(1).futureValue
    superHeroAccess.update(batman.copy(partner = Some("Robin"))).futureValue
    val batmanWithRobin = superHeroAccess.load(1).futureValue
    batmanWithRobin should be (SuperHero(1, "Batman", true, Some("Robin")))
  }

  it should "insert (using generated identity)" in {
    val catwoman = SuperHero(0, "Catwoman")
    val insertedCatwoman = superHeroAccess.insert(catwoman).futureValue

    val loadedCatwoman = superHeroAccess.load(insertedCatwoman.superHeroId).futureValue
    loadedCatwoman should be (SuperHero(insertedCatwoman.superHeroId, "Catwoman", false, None))
  }

  it should "insert (using specified identity)" in {
    val wonderWoman = SuperHero(666, "Wonder Woman", true)
    superHeroAccess.identityInsert(wonderWoman).futureValue

    val loadedWonderWoman = superHeroAccess.load(666).futureValue
    loadedWonderWoman should be (SuperHero(666, "Wonder Woman", true, None))
  }

  it should "delete" in {
    superHeroAccess.delete(1).futureValue
    val whereIsBatman = superHeroAccess.get(1).futureValue
    whereIsBatman should be (None)
  }

  it should "allow custom queries" in {
    val ironMan = superHeroAccess.getViaName("Iron Man").futureValue
    ironMan should be (Some(SuperHero(2, "Iron Man", false, None)))
  }

  before {
    DbAsync.execNonQuery("drop table if exists super_hero").futureValue
    DbAsync.execNonQuery("create table super_hero(super_hero_id serial, name text not null, wears_tights boolean not null, partner text null)").futureValue
    DbAsync.execNonQuery("insert into super_hero(super_hero_id, name, wears_tights) values " +
      "(1, 'Batman', 't')," +
      "(2, 'Iron Man', 'f')," +
      "(3, 'Superman', 't')").futureValue
    DbAsync.execNonQuery("alter sequence super_hero_super_hero_id_seq restart with 1000").futureValue
  }
}

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
  (t, i) => t.copy(superHeroId = i)) {

  def getViaName(name: String)(implicit config: DbAsyncConfig): Future[Option[SuperHero]] = {
    DbAsync.execOneOrNone[SuperHero](q"select * from super_hero where name = $name")
  }
}
