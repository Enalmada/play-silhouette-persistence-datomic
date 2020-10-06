package utils

import forms.SignUpForm.SignUpData
import play.api.i18n.MessagesProvider
import play.twirl.api.Html

import scala.language.implicitConversions

object Mailer {

  implicit def html2String(html: Html): String = html.toString

  def welcome(signUpData: SignUpData, link: String)(implicit mailService: MailService, messagesProvider: MessagesProvider): Unit = {
    mailService.sendEmailAsync(signUpData.email)(
      subject = messagesProvider.messages("mail.welcome.subject"),
      bodyHtml = link,
      bodyText = link
    )
  }

  def forgotPassword(email: String, link: String)(implicit mailService: MailService, messagesProvider: MessagesProvider): Unit = {
    mailService.sendEmailAsync(email)(
      subject = messagesProvider.messages("mail.forgotpwd.subject"),
      bodyHtml = link,
      bodyText = link
    )
  }

}
