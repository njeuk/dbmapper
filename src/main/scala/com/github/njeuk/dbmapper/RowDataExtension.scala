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

import com.github.mauricio.async.db.RowData

/**
 * RowDataExtension is used by the macro generated sql code.
 *
 * It allows the generated code to easily extract values from the RowData columns.
 *
 * If a Option is expected then we check for a null value in the column before lifting it into the Option.
 */
object RowDataExtension {
  import com.github.njeuk.dbmapper.ColumnConversion.ColumnConvertible

  implicit class RowDataExtensionImplicit(val self: RowData) extends AnyVal {
    def maybe[T](column: String)(implicit ev: ColumnConvertible[T]): Option[T] = {
      if (self(column) == null) None
      else Some(ev.fromColumn(self(column)))
    }
    def maybe[T](columnNumber: Int)(implicit ev: ColumnConvertible[T]): Option[T] = {
      if (self(columnNumber) == null) None
      else Some(ev.fromColumn(self(columnNumber)))
    }
    def get[T](column: String)(implicit ev: ColumnConvertible[T]): T = ev.fromColumn(self(column))
    def get[T](columnNumber: Int)(implicit ev: ColumnConvertible[T]): T = ev.fromColumn(self(columnNumber))
  }
}
