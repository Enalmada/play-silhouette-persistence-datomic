package persistence.datomic.daos

import com.mohiva.play.silhouette.impl.providers.OAuth2Info

/**
 * The DAO to persist the OAuth2 information.
 *
 * Note: Not thread safe, demo only.
 */
class OAuth2InfoDAO extends DatomicAuthInfoDAO[OAuth2Info]
