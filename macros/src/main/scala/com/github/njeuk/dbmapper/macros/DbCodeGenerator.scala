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

import com.github.mauricio.async.db.RowData
import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros


class Impl(val c: Context) {

  def update_impl[T: c.WeakTypeTag, U](u:c.Expr[T]) = {
    import c.universe._

    val tableName = getTableName[T]()
    val valToColumn = buildValToColumnMap[T]()
    val identityValName = getIdentityValName[T]()

    val t = weakTypeOf[T]
    val valNames = t.members.filter(_.asTerm.isVal).map(_.name.toString.trim)
    val valsToUpdate = valNames.filter(_ != identityValName)

    val argsList = valsToUpdate.map(c => s"${valToColumn(c)} = ?").mkString(", ")
    val sql = s"update $tableName set $argsList where ${valToColumn(identityValName)} = ?"
    //println("sql:" + sql)

    val arg = q"List(..${valNames.map(x => q"$u.${TermName(x)}")})"
    q"($sql,$arg)"
  }

  def insert_impl[T: c.WeakTypeTag, U](u:c.Expr[T]) = {
    import c.universe._

    val tableName = getTableName[T]()
    val valToColumn = buildValToColumnMap[T]()
    val identityValName = getIdentityValName[T]()

    val t = weakTypeOf[T]
    val valNames = t.members.filter(_.asTerm.isVal).map(_.name.toString.trim).filter(_ != identityValName)
    val columnList = valNames.map(c => valToColumn(c)).mkString(", ")
    val sql = s"insert into $tableName (${columnList}) values (${valNames.map(_=>'?').mkString(",")}) returning ${valToColumn(identityValName)}"
    //println("sql:" + sql)
    val arg = q"List(..${valNames.map(x => q"$u.${TermName(x)}")})"
    q"($sql,$arg)"
  }

  def identityInsert_impl[T: c.WeakTypeTag, U](u:c.Expr[T]) = {
    import c.universe._

    val tableName = getTableName[T]()
    val valToColumn = buildValToColumnMap[T]()
    val identityValName = getIdentityValName[T]()

    val t = weakTypeOf[T]
    val valNames = t.members.filter(_.asTerm.isVal).map(_.name.toString.trim)
    val columnList = valNames.map(c => valToColumn(c)).mkString(", ")
    val sql = s"insert into $tableName (${columnList}) values (${valNames.map(_=>'?').mkString(",")}) returning ${valToColumn(identityValName)}"
    //println("sql:" + sql)
    val arg = q"List(..${valNames.map(x => q"$u.${TermName(x)}")})"
    q"($sql,$arg)"
  }

  def codeToSql_impl[T: c.WeakTypeTag, U]() = {
    import c.universe._

    val tableName = getTableName[T]()
    val valToColumn = buildValToColumnMap[T]()
    //println(s"valToColumn: $valToColumn")
    val identityValName = getIdentityValName[T]()
    //println(s"identityValName: '$identityValName'")
    val identityColumnName = valToColumn(identityValName)
    q"new CodeToSql($tableName, $identityColumnName)"
  }

  def getTableName[T: c.WeakTypeTag](): String = {
    import c.universe._

    val t = weakTypeOf[T]
    extractNameFromAnnotation(t.typeSymbol.annotations.toString, "TableName") match {
      case Some(a) => a
      case None => ScalaToSqlNameConversion.convert(t.toString.reverse.takeWhile(_ != '.').reverse)
    }
  }

  def buildValToColumnMap[T: c.WeakTypeTag](): Map[String, String] = {
    import c.universe._

    val t = weakTypeOf[T]
    val vals = t.members.filter(_.asTerm.isVal)
    vals.map(c => {
      val valName = c.name.toString.trim
      val columnName = extractNameFromAnnotation(c.annotations.toString, "ColumnName @scala.annotation.meta.field") match {
        case Some(a) => a
        case None => ScalaToSqlNameConversion.convert(valName)
      }
      (valName, columnName)}).toMap
  }

  private def extractNameFromAnnotation(annotationListAsString: String, annotationName: String): Option[String] = {
    val i = annotationListAsString.indexOf(annotationName + "(\"")
    if (i >= 0)
      Some(annotationListAsString.drop(i + annotationName.length + 2).takeWhile(_ != '"'))
    else
      None
  }

  def getIdentityValName[T: c.WeakTypeTag](): String = {
    import c.universe._

    val t = weakTypeOf[T]
    val annotatedIdentities = t.members.filter(_.asTerm.isVal).filter(_.annotations.toString.contains("Identity")).toList
    if (annotatedIdentities.length > 1)
      throw new IllegalArgumentException("More than one val fields for the class are marked as the Identity, you can only mark one field")
    if (annotatedIdentities.length == 1)
      annotatedIdentities.head.toString.reverse.takeWhile(c => !c.isSpaceChar).reverse
    else {
      val justTypeName = t.toString.reverse.takeWhile(_ != '.').reverse
      val identityValName = justTypeName.head.toLower + justTypeName.tail + "Id"
      if (!t.members.filter(_.asTerm.isVal).exists(_.name.toString.trim == identityValName))
        throw new IllegalArgumentException(s"Expecting to find a val field named $identityValName in ${t.toString}, use the @identity annotation to mark the identity val if it has a non standard name")
      identityValName
    }
  }

  def rowToClass_impl[T: c.WeakTypeTag](row: c.Expr[RowData]): c.Expr[T] = {
    import c.universe._

    val t = weakTypeOf[T]
    val vals = t.members.filter(_.asTerm.isVal)
    val valNames = vals.map(_.name.toString.trim)
    val valToColumn = buildValToColumnMap[T]()

    val assignments = vals.map(c => {
      val valName = c.name.toString.trim
      val columnName = valToColumn(valName)

      //println("name: " + name + " type: " + c.typeSignature.typeSymbol.name.toString)
      if (c.typeSignature.typeSymbol.name.toString.trim == "Option")
        q"`${TermName(valName)}` = $row.maybe[${c.typeSignature.typeArgs.head}]($columnName)"
      else
        q"`${TermName(valName)}` = $row.get[${c.typeSignature.resultType}]($columnName)"
    })
    val q = q"""new `$t`(..$assignments)"""
    //println(showRaw(q))
    c.Expr(q)
  }

}

object DbCodeGenerator {

  def rowToClass[T](row: RowData): T = macro Impl.rowToClass_impl[T]

  def updateSql[T](u: T): (String, Seq[Any]) = macro Impl.update_impl[T, String]

  def insertSql[T](u: T): (String, Seq[Any]) = macro Impl.insert_impl[T, String]

  def identityInsertSql[T](u: T): (String, Seq[Any]) = macro Impl.identityInsert_impl[T, String]

  def codeToSql[T](): CodeToSql = macro Impl.codeToSql_impl[T, String]
}

