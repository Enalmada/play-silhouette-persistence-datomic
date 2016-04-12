package utils.persistence.datomic

import javax.inject._

import datomisca.{ Connection, Datomic }
import datomiscadao.DB
import models.{ TokenUser, User }
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import utils.persistence.datomic.daos.{ LoginInfoImpl, OAuth1InfoImpl, OAuth2InfoImpl, PasswordInfoImpl }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

object DatomicService {
  implicit var connOpt: Option[Connection] = None

  implicit def conn() = connOpt.get
}

@Singleton
class DatomicService @Inject() (env: play.Environment, config: play.api.Configuration, lifecycle: ApplicationLifecycle) {

  Logger.debug("My Datomisca initialized.")

  var datomiscaPlayPlugin = new DatomiscaPlayPlugin(config)

  def connectionUrl(appKey: String) = datomiscaPlayPlugin.uri(appKey)

  implicit val conn: Connection = if (env.isTest) {
    Datomic.createDatabase(connectionUrl("test"))
    Datomic.connect(connectionUrl("test"))
  } else {
    Datomic.createDatabase(connectionUrl("prod"))
    Datomic.connect(connectionUrl("prod"))
  }

  DatomicService.connOpt = Some(conn)

  if (env.isTest) {
    loadSchema(check = false)
  } else {

    if (env.isDev) {
      //Datomic.deleteDatabase(connectionUrl("prod"))
    }

    loadSchema()

  }

  def testShutdown() = {
    Logger.debug("My Datomisca shutdown.")
    Datomic.deleteDatabase(connectionUrl("test"))
    Datomic.createDatabase(connectionUrl("test"))
    Datomic.connect(connectionUrl("test"))
  }

  if (env.isTest) {
    lifecycle.addStopHook { () =>
      Future.successful(testShutdown())
    }
  }

  def loadSchema(check: Boolean = true)(implicit conn: Connection) = {
    implicit val db = Datomic.database

    val combinedSchema = User.Schema.schema ++
      LoginInfoImpl.Schema.schema ++
      OAuth1InfoImpl.Schema.schema ++
      OAuth2InfoImpl.Schema.schema ++
      PasswordInfoImpl.Schema.schema ++
      TokenUser.Schema.schema

    val filteredSchema = if (check) combinedSchema.filterNot(s => DB.hasAttribute(s.ident)) else combinedSchema

    if (filteredSchema.nonEmpty) {
      val fut = Datomic.transact(filteredSchema) map { tx =>
        Logger.info(s"Loaded Schema: $filteredSchema")
      }

      Await.result(fut, Duration("3 seconds"))
    }

  }

}