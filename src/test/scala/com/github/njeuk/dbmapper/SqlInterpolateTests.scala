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

package scala.com.github.njeuk.dbmapper

import com.github.njeuk.dbmapper.SqlInterpolation._
import com.github.njeuk.dbmapper.{SqlAndArgs, SqlInterpolation}
import org.scalatest._

class SqlInterpolateTests extends FlatSpec with MustMatchers {
  "SqlInterpolate" should
    "extract parts" in {
    val someValue = "test"
    val r = q"select * from x where v = $someValue" +
      q"and y = $someValue"
    r must be(SqlAndArgs("select * from x where v = ? and y = ?", Array("test", "test")))
  }

  it should "do standard insertion for escaped args" in {
    val someValue = "test"
    val table = "sometable"
    val r = q"select * from '$table where v = $someValue" +
      q"and y = $someValue"
    r must be (SqlAndArgs("select * from sometable where v = ? and y = ?", Array("test", "test")))
  }

  it should "do nothing when no insets" in {
    val someValue = "test"
    val table = "sometable"
    val r = q"select * from table where v = 123"
    r must be (SqlAndArgs("select * from table where v = 123", Seq[Any]()))
  }

  it should "expand out list of strings" in {
    val someValues = List("cat", "dog")
    val r = q"select * from table where v in ($someValues)"
    r must be (SqlAndArgs("select * from table where v in (?,?)", List("cat","dog")))
  }

  it should "expand out list of ints" in {
    val someValues = List(5,4,3)
    val r = q"select * from table where v in ($someValues)"
    r must be (SqlAndArgs("select * from table where v in (?,?,?)", List(5,4,3)))
  }

  it should "expand out empty list" in {
    val someValues = List()
    val r = q"select * from table where v in ($someValues)"
    r must be (SqlAndArgs("select * from table where v in ()", Seq[Any]()))
    r.args.length must be (0)
  }

}
