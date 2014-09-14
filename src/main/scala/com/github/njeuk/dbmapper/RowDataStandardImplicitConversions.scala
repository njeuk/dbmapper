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

import com.github.mauricio.async.db.RowData
import org.joda.time.{LocalDate, LocalDateTime}
import com.github.njeuk.dbmapper.RowDataExtension._
import scala.language.implicitConversions

/**
 * RowDataStandardImplicitConversions provides convenience conversions to extract
 * data from RowData.
 *
 * Example:
 * {{{
 *   import com.github.njeuk.dbmapper.RowDataStandardImplicitConversions._
 *
 *   // this will use the implicit rowDataToString function
 *   val titles = DbAsync.exec[String]("select title from book order by book_id")
 *
 *   // or you could just specify the conversion directly
 *   val titles = DbAsync.exec[String]("select title from book order by book_id")(r => r(0).toString, dbAsyncConfig)
 *
 * }}}
 */

object RowDataStandardImplicitConversions {
  implicit def rowDataToInt(r: RowData): Int = r.get[Int](0)
  implicit def rowDataToFloat(r: RowData): Float = r.get[Float](0)
  implicit def rowDataToDouble(r: RowData): Double = r.get[Double](0)
  implicit def rowDataToBoolean(r: RowData): Boolean = r.get[Boolean](0)
  implicit def rowDataToString(r: RowData): String = r.get[String](0)
  implicit def rowDataTo2StringTuple(r: RowData): (String, String) = (r.get[String](0), r.get[String](1))
  implicit def rowDataToLocalDateTimeTuple(r: RowData): (LocalDateTime, LocalDateTime) = (r.get[LocalDateTime](0), r.get[LocalDateTime](1))
  implicit def rowDataToLocalDateTimeDoubleTuple(r: RowData):(LocalDateTime, Double) = (r.get[LocalDateTime](0), r.get[Double](1))
  implicit def rowDataToLocalDateTimeFloatTuple(r: RowData):(LocalDateTime, Float) = (r.get[LocalDateTime](0), r.get[Float](1))
  implicit def rowDataToLocalDateTuple(r: RowData): (LocalDate, LocalDate) = (r.get[LocalDate](0), r.get[LocalDate](1))
  implicit def rowDataToLocalDateDoubleTuple(r: RowData):(LocalDate, Double) = (r.get[LocalDate](0), r.get[Double](1))
  implicit def rowDataToLocalDateFloatTuple(r: RowData):(LocalDate, Float) = (r.get[LocalDate](0), r.get[Float](1))
}
