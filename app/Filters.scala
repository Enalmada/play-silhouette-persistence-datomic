import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.cors.CORSFilter
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter

class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter, corsFilter: CORSFilter, csrfFilter: CSRFFilter) extends HttpFilters {
  def filters = Seq(securityHeadersFilter, corsFilter, csrfFilter)
}