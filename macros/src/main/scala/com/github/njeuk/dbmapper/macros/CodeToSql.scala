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
 * Provides details about an Entity object used to guide the SQL generation
 * @param tableName the name of the table the Entity is stored in
 * @param identityColumn the column which uniquely identifies the entity, i.e. a primary key
 *
 * if you get the error:
 *  `not found: type CodeToSql
 *    () => DbCodeGenerator.codeToSql[T]()`
 *
 *  then you haven't added:
 *    `import com.github.njeuk.dbmapper.macros.CodeToSql`
 */
case class CodeToSql(tableName: String, identityColumn: String)