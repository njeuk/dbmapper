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

package com.github.njeuk.dbmapper.macros

/**
 * ScalaToSqlNameConversion provides the mapping from the Scala identifier to the Database identifier
 *
 * @note Really need to make this more flexible for different naming schemes.
 *       Maybe use Twitter's Scala Eval code to make pass in the algorithm, not sure
 *       on the impact on compile speed.  Don't want to invent a DSL for it.  Don't think
 *       Scala's RegEx is able to do it either.
 *
 */
object ScalaToSqlNameConversion {

  /**
   * Converts from a Scala identifier to an identifier in the database.
   * Read the code for the algorithm, everything goes to lowercase snake_case, samples are:
   *
   *  `camelCase -> camel_case
   *  PascalCase -> pascal_case
   *  ID -> id`
   *
   * @param scalaName the scala identifier
   * @return the identifier in the database
   */
  def convert(scalaName: String): String = {
    if (scalaName.length == 0) ""
    else if (scalaName.length == 1) scalaName.toLowerCase
    else (scalaName.foldLeft("")((a: String, b: Char) => a + (if (b.isUpper && a.length > 1) "_" + b else b))).toLowerCase
  }

}
