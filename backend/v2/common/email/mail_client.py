import smtplib
import ssl
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from typing import ClassVar

import html2text
from pydantic import BaseModel, EmailStr

from .settings import email_settings


class MailClient(BaseModel):

    receiver: EmailStr
    subject: str
    message: str

    BODY_OPEN: ClassVar = """
        <body style="font-size: 16px;
        font-family: Arial,
        sans-serif, 'Open Sans'">
    """
    BODY_CLOSE: ClassVar = "</body>"
    SENDER: ClassVar = email_settings.sender

    def mail(self):
        print("Preparing email")
        message = MIMEMultipart("alternative")
        message["Subject"] = self.subject
        message["From"] = email_settings.sender
        message["To"] = self.receiver

        text_part = MIMEText(html2text.html2text(self.message), "plain")
        html_part = MIMEText(self.message, "html")

        message.attach(text_part)
        message.attach(html_part)
        message = message.as_string()

        with smtplib.SMTP(email_settings.host, email_settings.port) as server:
            try:
                print("Setting up TLS with mail server")
                context = ssl.create_default_context()
                server.starttls(context=context)
            except Exception as error:
                print(f"Failed to login to mail server: {error}")
                raise error
            try:
                print("Sending email")
                server.sendmail(email_settings.sender, self.receiver, message)
            except Exception as error:
                print(f"Failed to send mail: {error}")
                raise error
