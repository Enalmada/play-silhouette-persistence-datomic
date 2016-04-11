package utils

import forms.SignUpForm.SignUpData
import play.api.Play.current
import play.api.i18n.{ Messages, MessagesApi }
import play.api.i18n.Messages.Implicits._
import play.twirl.api.Html

import scala.language.implicitConversions

object Mailer {

  implicit def html2String(html: Html): String = html.toString

  def welcome(signUpData: SignUpData, link: String)(implicit mailService: MailService, messagesApi: MessagesApi) {
    mailService.sendEmailAsync(signUpData.email)(
      subject = messagesApi("mail.welcome.subject"),
      bodyHtml = link,
      bodyText = link
    )
  }

  def forgotPassword(email: String, link: String)(implicit mailService: MailService, messagesApi: MessagesApi) {
    mailService.sendEmailAsync(email)(
      subject = messagesApi("mail.forgotpwd.subject"),
      bodyHtml = link,
      bodyText = link
    )
  }

}
