import { Injectable } from '@angular/core';
import {AddRequest} from "./add-request";
import {Observable} from "rxjs";
import {HttpClient} from "@angular/common/http";
import {Changes, UpdateSpec} from "./app/application-state.service";

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  constructor(private httpClient: HttpClient) { }

  addDataset(addRequest: AddRequest): Observable<any> {
    return this.httpClient.post<any>('/las/admin/addDataset', addRequest);
  }
  saveDataset(changes: Changes): Observable<any> {
    return this.httpClient.put<Map<string, Map<number, string>>>('/las/admin/saveDataset', changes);
  }
  saveDatasetUpdateSpec(updateSpec: UpdateSpec): Observable<any> {
    return this.httpClient.post<any>('/las/admin/saveDatasetUpdateSpec', updateSpec);
  }
  saveSite(changes): Observable<any> {
    return this.httpClient.put<any>('/las/admin/saveSite', changes);
  }
  moveDataset(moveRequest: AddRequest): Observable<any> {
    return this.httpClient.post<any>('/las/admin/moveDataset', moveRequest);
  }
  deleteDataset(id: number): Observable<any> {
    return this.httpClient.delete<any>('/las/admin/deleteDataset/' + id);
  }
}
