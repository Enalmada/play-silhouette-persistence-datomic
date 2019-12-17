import akka.stream.Materializer
import javax.inject.Inject
import play.api.Mode
import play.api.http.HttpFilters
import play.api.mvc.{ Filter, RequestHeader, Result, Results }
import play.filters.cors.CORSFilter
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter

import scala.concurrent.{ ExecutionContext, Future }

class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter, corsFilter: CORSFilter, csrfFilter: CSRFFilter, tlsFilter: TLSFilter) extends HttpFilters {
  def filters = Seq(securityHeadersFilter, corsFilter, csrfFilter, tlsFilter)
}

class TLSFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext, env: play.api.Environment, config: play.api.Configuration) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    if (!requestHeader.secure && env.mode != Mode.Test)
      Future.successful(Results.MovedPermanently("https://" + requestHeader.host.replace(config.getString("http.port").getOrElse("9000"), config.getString("https.port").getOrElse("9443")) + requestHeader.uri))
    else
      nextFilter(requestHeader).map(_.withHeaders("Strict-Transport-Security" -> "max-age=31536000; includeSubDomains; preload"))
  }

}
