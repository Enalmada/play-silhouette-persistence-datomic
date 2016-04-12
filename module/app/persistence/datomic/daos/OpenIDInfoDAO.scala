package persistence.datomic.daos

import com.mohiva.play.silhouette.impl.providers.OpenIDInfo

/**
 * The DAO to persist the OpenID information.
 *
 * Note: Not thread safe, demo only.
 */
class OpenIDInfoDAO extends DatomicAuthInfoDAO[OpenIDInfo]
