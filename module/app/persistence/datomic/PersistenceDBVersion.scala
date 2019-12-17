package persistence.datomic

import datomisca.DatomicMapping._
import datomisca._
import datomiscadao.DB

import scala.concurrent.ExecutionContext

case class PersistenceDBVersion(
  id: Long = -1,
  version: Int = 0)

object PersistenceDBVersion extends DB[PersistenceDBVersion] {

  object Schema {

    object ns {
      val persistenceDBVersion = new Namespace("persistenceDBVersion")
    }

    val version = Attribute(ns.persistenceDBVersion / "version", SchemaType.long, Cardinality.one).withDoc("Version number")

    val schema = Seq(version)

  }

  implicit val reader: EntityReader[PersistenceDBVersion] = (
    ID.read[Long] and
    Schema.version.read[Int])(PersistenceDBVersion.apply _)

  implicit val writer: PartialAddEntityWriter[PersistenceDBVersion] = (
    ID.write[Long] and
    Schema.version.write[Int])(unlift(PersistenceDBVersion.unapply))

  def create(dbVersion: PersistenceDBVersion)(implicit conn: Connection, ec: ExecutionContext): Long = {

    val newVersion = DatomicMapping.toEntity(DId(Partition.USER))(dbVersion)

    DB.transactAndWait(Seq(newVersion), newVersion.id)

  }

  def update(id: Long, dbVersion: PersistenceDBVersion)(implicit conn: Connection, ec: ExecutionContext): Unit = {
    implicit val primaryId = id
    val o = PersistenceDBVersion.get(id)

    val facts: TraversableOnce[TxData] = Seq(
      DB.factOrNone(o.version, dbVersion.version, Schema.version -> dbVersion.version)).flatten

    DB.transactAndWait(facts)

  }

  val queryAll = Query(
    """
    [
      :find ?e
      :where
        [?e :persistenceDBVersion/version]
    ]
    """)

  def getDbVersion()(implicit conn: Connection, ec: ExecutionContext): PersistenceDBVersion = {

    PersistenceDBVersion.headOption(Datomic.q(queryAll, Datomic.database)) match {
      case Some(dbVersion) => dbVersion
      case None => {
        val id = PersistenceDBVersion.create(PersistenceDBVersion())
        PersistenceDBVersion.get(id)
      }
    }

  }

  def updateVersion(dbVersion: PersistenceDBVersion)(implicit conn: Connection, ec: ExecutionContext) = {
    val copy = dbVersion.copy(version = dbVersion.version + 1)
    PersistenceDBVersion.update(dbVersion.id, copy)
  }

  def delete(id: Long)(implicit conn: Connection, ec: ExecutionContext) = PersistenceDBVersion.retractEntity(id)

}
