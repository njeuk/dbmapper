/*
 * Copyright 2014 Nick Edwards and collaborators
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

/**
 * Provides query interpolation, similar to standard Scala string interpolation.
 *
 * Example
 * {{{
 *   val name = "Bruce"
 *   val bruce = DbAsync.exec[Person](q"select * from person where name = \$name")
 * }}}
 *
 * Lists are supported, as in:
 * {{{
 *   val names = List("Bruce", "Murray", "Charlene")
 *   val australians = DbAsync.exec[Person](q"select * from person where name in (\$names)")
 * }}}
 *
 * To perform string interpolation within the query, prefix the \$ with a ' as in:
 *
 * Lists are supported, as in:
 * {{{
 *   val table = "person"
 *   val people = DbAsync.exec[Person](q"select * from '\$table")
 * }}}
 *
 * For longer queries you can join the strings with +, as in:
 *
 * {{{
 *   val newZealanders = DbAsync.exec[Person](q"select * from person " +
 *    q"where smarter = 't' and " +
 *    q"good_looking = 't'");
 * }}}
 *
 */

object SqlInterpolation {

  // '$arg does string interpolation rather than argument
  implicit class SqlInterpolationHelper(val sc: StringContext) extends AnyVal {
    def q(args: Any*): SqlAndArgs = {

      var actualArgs:List[Any] = List()
      val parts = sc.parts.iterator.toList
      val inserts = args.iterator.toList

      val pi = parts.zip(inserts)
      val sql = pi.foldLeft("")((a:String, b:(String, Any)) => {
        if (b._1.endsWith("'")) {
          a + b._1.dropRight(1) + b._2
        }
        else {
          if (b._2.isInstanceOf[List[Any]]) {
            val list = b._2.asInstanceOf[List[Any]]
            actualArgs = list.reverse ++ actualArgs
            a + b._1 + ("?," * list.length).dropRight(1)
          }
          else {
            actualArgs = b._2 :: actualArgs
            a + b._1 + "?"
          }
        }
      })
      val extra = if (pi.length < parts.length) parts.reverse.head.toString else ""

      SqlAndArgs(sql + extra, actualArgs.reverse)
    }
  }
}

case class SqlAndArgs(sql: String, args: Seq[Any]) {
  def +(that: SqlAndArgs): SqlAndArgs = {
    SqlAndArgs(sql + " " + that.sql, args ++ that.args)
  }
}