package nl.surf.rdx.sharer.owncloud

import better.files.StringInterpolations
import cats.Applicative
import cats.data.Kleisli
import cats.effect.{Resource, Sync}
import cats.implicits.catsSyntaxApplicativeId
import com.github.sardine.impl.SardineException
import com.github.sardine.{DavResource, Sardine}
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.common.owncloud.conf.OwncloudConf.WebdavBase
import org.http4s.implicits.http4sLiteralsSyntax
import org.mockito.MockitoSugar

import java.nio.file.{Path => JPath}
import java.util
import scala.jdk.CollectionConverters._

abstract class OwncloudSharesObserverFixtures[F[_]: Applicative: Sync] extends MockitoSugar {

  private type Deps = OwncloudSharesObserver.Deps[F]

  sealed trait Flow
  object Flow {
    case object Happy extends Flow
    case class FolderName(name: String) extends Flow
    case object ShareIsNotFolder extends Flow
    case object NoCondtionsFile extends Flow
  }

  def depsFor[B](flow: Flow): Deps =
    flow match {
      case Flow.Happy =>
        depsFor(Flow.FolderName(helpers.defaultDatasetName))
      case Flow.FolderName(name) =>
        OwncloudSharesObserver.Deps[F](
          Resource.pure[F, Sardine](
            helpers.sardineWithPaths(
              name,
              fileSet1(name)
            )
          ),
          List(shareFolder(name)).pure[F],
          helpers.webdavBase,
          helpers.conditionsFile
        )

      case Flow.ShareIsNotFolder =>
        depsFor(Flow.Happy).copy(getShares =
          List(
            shareFolder(helpers.defaultDatasetName).copy(item_type = OwncloudShare.itemTypeFile)
          ).pure[F]
        )
      case Flow.NoCondtionsFile =>
        depsFor(Flow.Happy).copy(conditionsFileName = "wontbethere.pdf")
    }

  def withDeps[B](flow: Flow)(thunk: Deps => F[B]): F[B] = {
    thunk.apply(depsFor(flow))
  }
  def runWithDeps[B](flow: Flow)(thunk: Kleisli[F, Deps, B]): F[B] = {
    thunk.run(depsFor(flow))
  }

  def shareFolder(name: String): OwncloudShare =
    OwncloudShare("id1", "uid1", None, s"/$name", OwncloudShare.itemTypeFolder, 0, 0)

  def fileSet1(name: String) = {
    fileSetNoConditions(name) :+ (helpers.prefix / name / "conditions.pdf").path
  }

  def fileSetNoConditions(name: String) = {

    List(
      helpers.prefix / name,
      helpers.prefix / name / "1.csv",
      helpers.prefix / name / "2.csv"
    ).map(_.path)
  }

  object helpers {

    val defaultDatasetName = "dataset1"
    val conditionsFile = "conditions.pdf"

    val prefix = file"/remote.php/nonshib-webdav"

    def webdavApiURL =
      uri"https://owncloud.server/remote.php/nonshib-webdav" / _

    def resources(files: List[JPath]): util.List[DavResource] =
      files
        .map(path => when(mock[DavResource].getPath).thenReturn(path.toString).getMock[DavResource])
        .asJava

    def sardineWithPaths(userPath: String, internals: List[JPath]): Sardine = {
      val rr = resources(internals)
      val url = webdavApiURL(userPath)
      when(mock[Sardine].list(url.toString(), 1))
        .thenReturn(rr)
        .getMock[Sardine]
    }

    def sardineFailFirstWithPaths(userPath: String, internals: List[JPath]): Sardine = {
      val rr = resources(internals)
      when(mock[Sardine].list(webdavApiURL(userPath).toString(), 1))
        .thenThrow(new SardineException("Oh no!", 502, "Server failed to respond"))
        .andThen(rr)
        .getMock[Sardine]
    }

    def sardineFail(userPath: String, e: Throwable): Sardine = {
      when(mock[Sardine].list(webdavApiURL(userPath).toString(), 1))
        .thenThrow(e)
        .getMock[Sardine]
    }

    val webdavBase: WebdavBase =
      WebdavBase("https://owncloud.server", "/remote.php/nonshib-webdav/")
  }

}
