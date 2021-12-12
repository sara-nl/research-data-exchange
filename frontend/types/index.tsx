export type Share = {
  owner: string;
  path?: string;
  createdAt?: string;
  conditionsDocument: string;
  files: Array<string>;
};

export type Dataset = {
  title: string;
  description: string;
  conditionsUrl: string;
  conditionsUrlProxy: string;
  files: Array<string>;
};
