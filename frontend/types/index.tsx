export type Share = {
  owner: string;
  path?: string;
  createdAt?: string;
  conditionsDocument: string;
  files: Array<string>;
};

export type Dataset = {
  owner: string;
  title: string;
  description: string;
  conditionsUrl: string;
  files: Array<string>;
};
