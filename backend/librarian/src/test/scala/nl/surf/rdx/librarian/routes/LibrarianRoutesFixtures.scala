package nl.surf.rdx.librarian.routes

import cats.Applicative
import cats.data.Kleisli
import cats.effect.Sync
import cats.implicits._
import com.minosiants.pencil.data.{Body, Email, Mailbox, Subject, To}
import com.minosiants.pencil.protocol.Replies
import io.lemonlabs.uri.RelativeUrl
import nl.surf.rdx.common.email.RdxEmail
import nl.surf.rdx.common.email.RdxEmail.{SendTemplate, Template, SendMail}
import nl.surf.rdx.common.email.conf.EmailConf
import nl.surf.rdx.common.model.access.RdxDownloadableDataset
import nl.surf.rdx.common.model.api.{ShareInfo, UserMetadata}
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.common.model.{RdxDataset, RdxShare}
import nl.surf.rdx.librarian.codecs.service.DatasetService
import nl.surf.rdx.librarian.email.DatasetAccessLinkEml
import org.mockito.{MockitoSugar, ArgumentMatchers => matchers}

import java.nio.file.{Paths, Path => JPath}
import java.time.OffsetDateTime
import java.util.UUID

abstract class LibrarianRoutesFixtures[F[_]: Applicative: Sync] extends MockitoSugar {

  sealed trait Flow
  object Flow {
    case object Happy extends Flow
  }

  def depsFor[B](flow: Flow): LibrarianRoutes.Deps[F] =
    flow match {
      case Flow.Happy =>
        val ocShare = OwncloudShare(
          "id1",
          "john@doe.com",
          "john.doe.com".some,
          "/dataset folder",
          OwncloudShare.itemTypeFolder,
          1,
          16
        )
        val ocShareInfo = ShareInfo(
          ocShare.path,
          ocShare.uid_owner,
          OffsetDateTime.now(),
          "conditions.pdf",
          List("conditions.pdf", "file.csv")
        )

        val rdxDataset = RdxDownloadableDataset(ocShare, "http://example.com")
        val dsMock = mock[DatasetService[F]]
        when(dsMock.fetchOCShare(matchers.any[RelativeUrl]))
          .thenReturn(none[RdxDownloadableDataset].pure[F])
        when(dsMock.fetchOCShare(matchers.eq(doiUrl)))
          .thenReturn(rdxDataset.some.pure[F])

        when(dsMock.fetchShare(matchers.any[UUID])).thenReturn(none[RdxShare].pure[F])
        when(dsMock.fetchShare(uuid)).thenReturn(shareToken.some.pure[F])
        when(dsMock.fetchShare(expiredUuid)).thenReturn(expiredShareToken.some.pure[F])

        when(dsMock.publishShare(matchers.eq(uuid), matchers.any[UserMetadata]))
          .thenReturn(Sync[F].unit)

        when(dsMock.fetchDataset(matchers.eq(doiUrl)))
          .thenReturn(publishedDataset.some.pure[F])

        val sendMailMock = mock[SendMail[F]]
        when(sendMailMock.apply(matchers.any[SendTemplate[F]])).thenReturn(Replies(Nil).pure[F])

        val mkPublicLinkMock = mock[JPath => F[String]]
        when(mkPublicLinkMock.apply(matchers.eq(Paths.get("/dataset folder"))))
          .thenReturn(publicLink.pure[F])

        val downloadConditions = mock[String => F[String]]
        when(downloadConditions.apply(matchers.any[String]))
          .thenReturn(attachmentPath.pure[F])

        val dummyMailTemplate = Kleisli.fromFunction[F, (EmailConf, DatasetAccessLinkEml.Vars[F])] {
          case (_, vars) =>
            RdxEmail(
              To(Mailbox.unsafeFromString(vars.contactEmail)),
              Subject(s"Your access to ${vars.ds.owncloudShare.path}"),
              Body.Ascii("https://example.com/dataset.zip")
            )
        }

        LibrarianRoutes.Deps(
          dsMock,
          _ => ocShareInfo.pure[F],
          sendMailMock,
          dummyMailTemplate,
          mkPublicLinkMock,
          downloadConditions
        )
    }

  def withDeps[B](flow: Flow)(thunk: LibrarianRoutes.Deps[F] => F[B]): F[B] = {
    thunk.apply(depsFor(flow))
  }
  def runWithDeps[B](flow: Flow)(thunk: Kleisli[F, LibrarianRoutes.Deps[F], B]): F[B] = {
    thunk.run(depsFor(flow))
  }

  val uuid: UUID = UUID.randomUUID()
  val expiredUuid: UUID = UUID.randomUUID()
  val doiUrlEncoded: String = "10.1000%2Fthe.last.part"
  val doiUrl: RelativeUrl = RelativeUrl.parse("10.1000/the.last.part")
  val from: Mailbox = Mailbox.unsafeFromString("rdx@surf.nl")
  val publicLink: String = "http://example.com"
  val attachmentPath = "librarian/src/test/resources/conditions.pdf"

  private val shareToken: RdxShare = RdxShare(
    OwncloudShare("id1", "user1", None, "/share1", "file", 10, 0),
    OffsetDateTime.now(),
    uuid.some,
    OffsetDateTime.now().plusDays(1),
    "sales@microsoft.com",
    List("conditions.pdf"),
    "http://example.com"
  )

  val expiredShareToken: RdxShare = RdxShare(
    OwncloudShare("id1", "user1", None, "/share1", "file", 10, 0),
    OffsetDateTime.now(),
    uuid.some,
    OffsetDateTime.now().minusDays(1),
    "sales@microsoft.com",
    List("conditions.pdf"),
    "http://example.com"
  )

  private[librarian] val publishedDataset: RdxDataset = RdxDataset(
    "title",
    "description",
    "http:/example.com/conditions.pdf",
    Seq("file.csv", "folder/")
  )

}
