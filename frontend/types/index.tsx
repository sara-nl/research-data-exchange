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
  access_license_id?: number;
};

export type Metadata = {
  doi?: string;
  title?: string;
  authors?: string;
  description?: string;
}

export enum AccessLicense {
  download = 1,
  analyze_blind_with_output_check = 2,
  analyze_blind_no_output_check = 3,
  analyze_tinker_with_output_check = 4,
  analyze_tinker_no_output_check = 5,
}

export namespace AccessLicenseUtil {
  export function toString(accessLicense: number): string {
    switch (accessLicense) {
      case AccessLicense.download: {
        return "sign+download";
      }
      case AccessLicense.analyze_blind_with_output_check: {
        return "sign+analyze (blind, with output check)";
      }
      case AccessLicense.analyze_blind_no_output_check: {
        return "sign+analyze (blind, without output check)";
      }
      case AccessLicense.analyze_tinker_with_output_check: {
        return "sign+analyze (tinker, with output check)";
      }
      case AccessLicense.analyze_tinker_no_output_check: {
        return "sign+analyze (tinker, without output check)";
      }
    }
  }
}

export type Share = {
  // TODO: get owner variable from dataset.yml config
  uid_owner?: string;
  additional_info_owner?: string
  path?: string;
  share_time: Date;
};

export type Job = {
  id: number;
  script_location: string;
  status: string;
  results_url?: string
  analyst_name?: string
  analyst_email?: string
}

export type DatasetsPerPolicy = {
  access_license_id: number;
  total: number;
}

export type DatasetStats = {
  id: number;
  doi: string;
  title: string;
  access_license_id: number;
  signed: number;
  analyzed: number;
  owncloud_private_link: string;
}
