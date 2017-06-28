package utils.persistence

import javax.inject._

import datomisca.{ Connection, Datomic }
import datomiscadao.DB
import models.User
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
  } else {
    lifecycle.addStopHook { () =>
      conn.release()
      Datomic.shutdown(false)
      Future.successful(true)
    }
  }

  def loadSchema(check: Boolean = true)(implicit conn: Connection) = {
    // implicit val db = Datomic.database

    val combinedSchema = User.Schema.schema
    DB.loadSchema(combinedSchema, check)

  }

}