package persistence.datomic

import datomisca.{ Connection, Datomic }
import datomiscadao.DB
import javax.inject._
import persistence.datomic.daos._
import play.api.{ Logger, Logging }
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ ExecutionContext, Future }

object DatomicAuthService {
  implicit var connOpt: Option[Connection] = None

  implicit def conn() = connOpt.get
}

@Singleton
class DatomicAuthService @Inject() (implicit env: play.Environment, config: play.api.Configuration, lifecycle: ApplicationLifecycle, ec: ExecutionContext) extends Logging {

  Logger.debug("DatomicAuthService initialized.")

  var datomiscaPlayPlugin = new DatomiscaPlayPlugin(config)

  def connectionUrl(appKey: String) = datomiscaPlayPlugin.uri(appKey)

  implicit val conn: Connection = if (env.isTest) {
    Datomic.createDatabase(connectionUrl("test"))
    Datomic.connect(connectionUrl("test"))
  } else {
    Datomic.createDatabase(connectionUrl("prod"))
    Datomic.connect(connectionUrl("prod"))
  }

  DatomicAuthService.connOpt = Some(conn)

  if (env.isTest) {
    loadSchema(check = false)
  } else {

    if (env.isDev) {
      //Datomic.deleteDatabase(connectionUrl("prod"))
    }

    loadSchema()
    postMigrations()
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
  /*  I still get some errors...gonna leave it
   else {
    lifecycle.addStopHook { () =>
      conn.release()
      Datomic.shutdown(false)
      Future.successful(true)
    }
  }
  */

  def loadSchema(check: Boolean = true)(implicit conn: Connection) = {

    val combinedSchema =
      PersistenceDBVersion.Schema.schema ++
        LoginInfoImpl.Schema.schema ++
        OAuth1InfoImpl.Schema.schema ++
        OAuth2InfoImpl.Schema.schema ++
        OpenIDInfoImpl.Schema.schema ++
        PasswordInfoImpl.Schema.schema ++
        TokenUser.Schema.schema

    DB.loadSchema(combinedSchema)

  }

  def postMigrations() = {
    val dbVersion = PersistenceDBVersion.getDbVersion
    Logger.info(s"PersistenceDBVersion: ${dbVersion.version}")
  }

}
