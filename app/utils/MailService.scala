package utils

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer._

import scala.concurrent.duration._
import scala.language.postfixOps

trait MailService {
  def sendEmailAsync(recipients: String*)(subject: String, bodyHtml: String, bodyText: String = ""): Unit

  def sendEmail(recipients: String*)(subject: String, bodyHtml: String, bodyText: String = ""): Unit
}

class MailServiceImpl @Inject() (mailerClient: MailerClient, system: ActorSystem, configuration: play.api.Configuration) extends MailService {

  def from: String = configuration.getString("play.mailer.from").getOrElse("UNKNOWN")

  def sendEmailAsync(recipients: String*)(subject: String, bodyHtml: String, bodyText: String = ""): Unit = {
    system.scheduler.scheduleOnce(100 milliseconds) {
      sendEmail(recipients: _*)(subject, bodyHtml, bodyText)
    }
  }

  def sendEmail(recipients: String*)(subject: String, bodyHtml: String, bodyText: String = ""): Unit = {
    mailerClient.send(Email(
      subject,
      from,
      recipients,

      // sends text, HTML or both...
      Some(bodyText),
      Some(bodyHtml)
    ))
  }
}
