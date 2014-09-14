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

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.postgresql.util.URLParser
import scala.concurrent.duration._

/**
 * Configuration information needed to access the database, and control logging.
 *
 * Usually passed implicitly into dbmapper.
 *
 * To setup a default implicit DbAsyncConfig add 'import com.github.njeuk.dbmapper.Implicits._'.
 *
 * This will access the database connection settings from the application.conf properties in the style of the Play Framework.
 *
 * Or you can specify the connection information directly, for example
 * {{{
 *     implicit val dbAsyncConfig = DbAsyncConfig(URLParser.parse("jdbc:postgresql://localhost/dbmappersamples?user=test&password=test"), Duration("0 ms"))
 * }}}
 *
 * @param configuration Configuration information used to setup the db connections
 * @param logQueriesLongerThan Any queries which take longer than the specified duration will be logged.
 *                             Use to identify slow queries.
 */

@annotation.implicitNotFound(msg = "Cannot find an implicit DbAsyncConfig for the database connection.  Either 'import com.github.njeuk.dbmapper.Implicits._' to use settings from the application.conf file or create an implicit DbAsyncConfig in scope with the database connection information.")
case class DbAsyncConfig(
  configuration: Configuration,
  logQueriesLongerThan: Duration = Duration(500, MILLISECONDS)
)

object DbAsyncConfig {
  def fromSettings: DbAsyncConfig = {
    def getConnectionString(): String = {
      val connectionString = Config.getString("db.default.url")
        .getOrElse(throw new Exception("Don't know how to connect to the database, either provide an explicit DbAsyncConf or add a play framework (db.default.url) style database property to application.conf (or as a -Ddb.default.url argument to the command line)"))
      if (connectionString.contains('?'))
        connectionString
      else
        connectionString +
          Config.getString("db.default.user").map("?user=" + _).getOrElse("") +
          Config.getString("db.default.password").map("&password=" + _).getOrElse("")
    }

    DbAsyncConfig(URLParser.parse(getConnectionString()),
      Duration(Config.getString("dbmapper.logging.maxQueryTime").getOrElse("500 millis")))
  }
}








