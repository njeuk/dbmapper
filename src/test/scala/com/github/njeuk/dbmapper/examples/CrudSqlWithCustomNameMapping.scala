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
import com.github.njeuk.dbmapper._
import com.github.njeuk.dbmapper.macros._
import org.joda.time.LocalDate
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}
import com.github.njeuk.dbmapper.RowDataExtension.RowDataExtensionImplicit
import com.github.njeuk.dbmapper.SqlInterpolation._
import scala.annotation.meta.field
import scala.concurrent.Future
import scala.concurrent.duration.Duration


/** This sample shows CRUD operations where there are not standard mapping from
  * the Table to the Scala class.  
  */

class CrudSqlWithCustomNameMapping extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfter {
  implicit override val patienceConfig = new PatienceConfig(scaled(Span(5000, Millis)), scaled(Span(15, Millis)))
  implicit val dbAsyncConfig = DbAsyncConfig(URLParser.parse("jdbc:postgresql://localhost/dbmappersamples?user=postgres&password="), Duration("0 ms"))
  val villainAccess = new VillainAccess

  "dbmapper" should "load using name mappings defined via attibutes" in {
    val villain = villainAccess.load(2).futureValue
    villain should be(Villain(2, "Joker", false))

    an[Exception] should be thrownBy {
      val noVillain = villainAccess.load(123).futureValue
    }
  }

  it should "update using name from attributes" in {
    val riddler = villainAccess.load(1).futureValue
    villainAccess.update(riddler.copy(name = "The Riddler")).futureValue
    val theRiddler = villainAccess.load(1).futureValue
    theRiddler should be (Villain(1, "The Riddler", true))
  }

  it should "insert (using generated identity)" in {
    val poisonIvy = Villain(0, "Poison Ivy")
    val insertedPoisonIvy = villainAccess.insert(poisonIvy).futureValue

    val loadedPoisonIvy = villainAccess.load(insertedPoisonIvy.villain_reference).futureValue
    loadedPoisonIvy should be (Villain(insertedPoisonIvy.villain_reference, "Poison Ivy", false))
  }

  it should "delete" in {
    villainAccess.delete(1).futureValue
    val whereIsTheRiddler = villainAccess.get(1).futureValue
    whereIsTheRiddler should be (None)
  }

  before {
    DbAsync.execNonQuery("drop table if exists baddie").futureValue
    DbAsync.execNonQuery("create table baddie(baddiekey serial, tag text not null, misunderstood boolean not null)").futureValue
    DbAsync.execNonQuery("insert into baddie(baddiekey, tag, misunderstood) values " +
      "(1, 'Riddler', 't')," +
      "(2, 'Joker', 'f')," +
      "(3, 'Lex Luthor', 't')").futureValue
    DbAsync.execNonQuery("alter sequence baddie_baddiekey_seq restart with 1000").futureValue
  }
}

@TableName("baddie")
case class Villain (
  @(Identity @field)
  @(ColumnName @field)("baddiekey")
  villain_reference: Int = 0,

  @(ColumnName @field)("tag") name: String = "",
  misunderstood: Boolean = false
)

class VillainAccess extends TableAccess[Villain](
  () => DbCodeGenerator.codeToSql[Villain](),
  (r) => DbCodeGenerator.rowToClass[Villain](r),
  (u) => DbCodeGenerator.updateSql[Villain](u),
  (i) => DbCodeGenerator.insertSql[Villain](i),
  (i) => DbCodeGenerator.identityInsertSql[Villain](i),
  (t, i) => t.copy(villain_reference = i)) {

  def getViaName(name: String)(implicit config: DbAsyncConfig): Future[Option[Villain]] = {
    DbAsync.execOneOrNone[Villain](q"select * from baddie where tag = $name")
  }
}


