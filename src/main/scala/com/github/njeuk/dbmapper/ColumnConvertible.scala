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

package com.github.njeuk.dbmapper

import org.joda.time.{LocalDateTime, LocalDate}

/**
 * ColumnConversion provides implicit conversions from the types in the RowData column
 * from the database to the associated Scala type.
 *
 * In most cases RowData contains reasonable types for the various Postgres types.
 *
 * They are documented in com.github.mauricio.async.db.postgresql.column.PostgreSQLColumnDecoderRegistry
 *
 * If the type isn't know, for example the Postgres money type, then it will be represented as a string in the RowData column.
 * If you wanted to support money as a type, and have it converted to a BigDecimal then you
 * would need to implement that conversion here.  (It would be tricky to do, the Postgres money
 * type converted to a string is quite variable depending on the currency)
 */
object ColumnConversion {

  @annotation.implicitNotFound(msg = "No member of type class ColumnConvertible in scope for ${T}, you may have to implement the specific TypeClass")
  trait ColumnConvertible[T] {
    def fromColumn(column: Any): T
  }

  object ColumnConvertible {

    implicit object ColumnConvertibleString extends ColumnConvertible[String] {
      override def fromColumn(column: Any): String = column.asInstanceOf[String]
    }
    implicit object ColumnConvertibleFloat extends ColumnConvertible[Float] {
      override def fromColumn(column: Any): Float = column.asInstanceOf[Float]
    }
    implicit object ColumnConvertibleDouble extends ColumnConvertible[Double] {
      override def fromColumn(column: Any): Double = column.asInstanceOf[Double]
    }
    implicit object ColumnConvertibleInt extends ColumnConvertible[Int] {
      override def fromColumn(column: Any): Int = column.asInstanceOf[Int]
    }
    implicit object ColumnConvertibleLong extends ColumnConvertible[Long] {
      override def fromColumn(column: Any): Long = column.asInstanceOf[Long]
    }
    implicit object ColumnConvertibleLocalDate extends ColumnConvertible[LocalDate] {
      override def fromColumn(column: Any): LocalDate = column.asInstanceOf[LocalDate]
    }
    implicit object ColumnConvertibleLocalDateTime extends ColumnConvertible[LocalDateTime] {
      override def fromColumn(column: Any): LocalDateTime = column.asInstanceOf[LocalDateTime]
    }
    implicit object ColumnConvertibleBigDecimal extends ColumnConvertible[BigDecimal] {
      override def fromColumn(column: Any): BigDecimal = column.asInstanceOf[BigDecimal]
    }
    implicit object ColumnConvertibleBoolean extends ColumnConvertible[Boolean] {
      override def fromColumn(column: Any): Boolean = column.asInstanceOf[Boolean]
    }

  }

}

