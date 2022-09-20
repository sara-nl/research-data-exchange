package nl.surf.rdx.common.model.owncloud

object OwncloudShare {
  val itemTypeFolder = "folder"
  val itemTypeFile = "file"
  def canReshare(permissions: Int): Boolean = permissions >= 16
}

/**
  * This class represents a projection of a "share" as exposed via Owncloud API as is,
  * i.e. field names, values. Its purpose is to de-serialize OC's JSON object and store a
  * "shapshot" of OC share in the DB.
  * Because it's so much OC-specific, the business logic should depend on it as least as possible.
  * See other classes in [[nl.surf.rdx.common.model]].
  */
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
    file_source: Int,
    permissions: Byte
)
