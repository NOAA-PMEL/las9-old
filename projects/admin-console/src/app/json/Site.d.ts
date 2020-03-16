import {Dataset} from "./Dataset";

export interface Site {
  id: number;
  toast: boolean;
  siteProperties?: (null)[] | null;
  profile: number;
  total: number;
  trajectoryProfile: number;
  title: string;
  infoUrl: string;
  attributes: Attributes;
  grids: number;
  timeseries: number;
  point: number;
  discrete: number;
  trajectory: number;
  dashboard: boolean;
  datasets?: (Dataset)[] | null;
}
export interface Attributes {
}
