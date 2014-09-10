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

import com.github.njeuk.dbmapper.macros.ScalaToSqlNameConversion
import org.scalatest._

class ScalaToSqlNameConversionTests extends FlatSpec with Matchers {

  "convert" should "convert camel case to sql" in {
      ScalaToSqlNameConversion.convert("asd") should be ("asd")
      ScalaToSqlNameConversion.convert("") should be ("")
      ScalaToSqlNameConversion.convert("nickEdwards") should be ("nick_edwards")
      ScalaToSqlNameConversion.convert("nickE") should be ("nick_e")
      ScalaToSqlNameConversion.convert("doesThisWork") should be ("does_this_work")
      ScalaToSqlNameConversion.convert("d") should be ("d")
      ScalaToSqlNameConversion.convert("D") should be ("d")
      ScalaToSqlNameConversion.convert("ID") should be ("id")
      ScalaToSqlNameConversion.convert("Dummy") should be ("dummy")
  }

}
