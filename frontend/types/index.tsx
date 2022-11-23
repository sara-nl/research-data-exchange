import internal from "stream";

export type Dataset = {
  id?: number,
  rdx_share?: Share;
  conditions_url: string;
  // TODO: figure out why proxy is needed
  conditionsUrlProxy?: string;
  files: Array<string>;
  doi?: string;
  title?: string;
  authors?: string;
  description?: string;
  published?: boolean;
  published_at?: Date;
  access_license?: AccessLicense;
};

export enum AccessLicense {
  download = "sign+download",
  analyze = "sign+analyze",
}

export type Share = {
  // TODO: get owner variable from dataset.yml config
  uid_owner?: string;
  additional_info_owner?: string
  path?: string;
  share_time: Date;
};
