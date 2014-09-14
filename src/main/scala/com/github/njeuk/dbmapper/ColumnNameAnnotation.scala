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
 * Specifies that the column name that represents the annotated variable.
 * Used when the default Scala identifier to database column name conversion
 * is not correct for your table
 * @param name the column name to use in the database for this variable
 *
 * Example: {{{
 *
 *    // the identifier 'name' will be mapped to the column 'tag' in the book table
 *
 *    case class Book (
 *      bookId: Int
 *      \@(ColumnName @field)("tag") name: String
 *    )
 * }}}
 */
case class ColumnName(name: String) extends scala.annotation.StaticAnnotation

/**
 * Specifies that the TableName that represents the Scala Class (or entity).
 * Used when the default Scala identifier to database table name conversion
 * is not correct for you schema.
 *
 * @param name the name of the table in the database
 *
 * Example: {{{
 *
 *    // the entity 'Book' will be named 'novel' in the database
 *
 *    \@TableName("novel")
 *    case class Book (
 *      bookId: Int
 *      name: String
 *    )
 * }}}
 */
case class TableName(name: String) extends scala.annotation.StaticAnnotation

/**
 * Specifies that the value is the identifier for the entity.
 *
 * Use when the Scala to Database identifier conversion routine isn't correct for your schema.
 *
 * Example: {{{
 *
 *    // the table 'book' in the database will have a primary key 'book_reference' in the database
 *
 *    case class Book (
 *      @(Identity @field) bookReference: Int
 *      name: String
 *    )
 * }}}
 *
 * Only one identity can be specified, compound keys are not supported.
 *
 * The key must be an Int, although this restriction maybe lifted one day.
 *
 * Really, we expect the schema to have integer surrogate keys on all tables, and
 * although not text book database design, your life will be easier even if you don't
 * use dbmapper...
 */
case class Identity() extends scala.annotation.StaticAnnotation


