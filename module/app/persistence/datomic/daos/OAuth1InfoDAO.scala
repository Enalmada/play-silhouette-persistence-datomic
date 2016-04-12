package persistence.datomic.daos

import com.mohiva.play.silhouette.impl.providers.OAuth1Info

/**
 * The DAO to persist the OAuth1 information.
 *
 * Note: Not thread safe, demo only.
 */
class OAuth1InfoDAO extends DatomicAuthInfoDAO[OAuth1Info]
