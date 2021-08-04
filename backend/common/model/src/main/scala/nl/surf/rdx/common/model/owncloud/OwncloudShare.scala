package nl.surf.rdx.common.model.owncloud

object OwncloudShare {
  val itemTypeFolder = "folder"
}

case class OwncloudShare(
    /**
      * Internal ID of the storage provider?
      */
    id: String,
    /**
      * Username of the share owner
      */
    uid_owner: String,
    /**
      * Email address of the share owner
      */
    additional_info_owner: Option[String],
    /**
      * How is it different from file_target?
      */
    path: String,
    /**
      * "folder" or "file"
      */
    item_type: String,
    file_source: Int
)
