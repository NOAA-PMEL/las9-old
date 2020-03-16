import {Component, OnInit} from '@angular/core';
import {AdminService} from "../../admin.service";
import {DatasetService} from "../../dataset.service";
import {ApplicationStateService} from "../application-state.service";

/** Error when invalid control is dirty, touched, or submitted. */

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css']
})
export class AdminComponent implements OnInit {
  constructor(private datasetService: DatasetService,
              private addDataService: AdminService,
              private applicationStateService: ApplicationStateService,
              ) { }

  ngOnInit() {
    this.datasetService.getSite().subscribe(site => {
      this.applicationStateService.setParent(site, 'site', true);
    });
  }
  loadSecondary(event) {
    const index = event.index;
    if ( index === 3 ) {
      this.datasetService.getSite().subscribe(site => {
        this.applicationStateService.setSecondary(site, 'site', true);
      });
    } else if ( index === 4 ) {
      this.datasetService.getPrivate().subscribe(site => {
        this.applicationStateService.setSecondary(site, 'site', true);
      });
    }
  }
}
