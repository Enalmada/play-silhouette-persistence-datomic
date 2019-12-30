package utils.persistence

import datomisca.{ Connection, Datomic }
import datomiscadao.DB
import javax.inject._
import models.User
import play.api.Logging
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ ExecutionContext, Future }

object DatomicService {
  implicit var connOpt: Option[Connection] = None

  implicit def conn() = connOpt.get
}

@Singleton
class DatomicService @Inject() (implicit env: play.Environment, config: play.api.Configuration, lifecycle: ApplicationLifecycle, ec: ExecutionContext) extends Logging {

  logger.debug("My Datomisca initialized.")

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
    logger.debug("My Datomisca shutdown.")
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
