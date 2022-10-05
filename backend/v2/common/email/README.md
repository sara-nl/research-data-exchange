# Email

The email library implements a mail client that can be used in other components to send emails. It expects the mail server to use TLS.
It uses the credentials and settings specified in the `.env` file.

## Packages used

* Email modules from Python's standard library: email, smtplib, ssl
* [html2text](https://pypi.org/project/html2text/) for making an html body suitable for plain text email

## Example

```{python}
from common.email.mail_client import MailClient

receiver = "foo@example.nl"
message = f"""{MailClient.BODY_OPEN}
        <p>Dear Sir/Madam,</p>

        <p>A new dataset, has just been shared with RDX.</p>

        <p>You can now add the required metadata and publish it. That will make it available for download upon agreement to the associated use conditions.</p>

        <p>Kind regards, RDX</p>
    {MailClient.BODY_CLOSE}"""

mail_client = MailClient(receiver=receiver, subject="Test", message=message)
mail_client.mail()
```
