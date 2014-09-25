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

import com.typesafe.config.{ConfigException, ConfigFactory}

/**
 * Access config information which may or may not exist.
 */
object Config {
  lazy val config = ConfigFactory.load()

  def getInt(key: String) : Option[Int] = Optional(config.getInt(key))
  def getLong(key: String) : Option[Long] = Optional(config.getLong(key))
  def getBoolean(key: String) : Option[Boolean] = Optional(config.getBoolean(key))
  def getString(key: String) : Option[String] = Optional(config.getString(key))

  def Optional[A](f: => A): Option[A] = try {
    Some(f)
  } catch {
    case e: ConfigException.Missing =>
      None
  }
}
