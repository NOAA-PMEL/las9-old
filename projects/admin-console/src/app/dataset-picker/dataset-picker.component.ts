import {Component, Input, OnInit} from '@angular/core';
import {Dataset} from "../json/Dataset";
import {DatasetService} from "../../dataset.service";
import {Site} from "../json/Site";
import {Subscription} from "rxjs";
import {ApplicationStateService} from "../application-state.service";
import {AddProperty} from "../../add-property";
import {AddRequest} from "../../add-request";
import {AdminService} from "../../admin.service";

@Component({
  selector: 'app-dataset-picker',
  templateUrl: './dataset-picker.component.html',
  styleUrls: ['./dataset-picker.component.css'],
})

export class DatasetPickerComponent implements OnInit {

  breadcrumbs;
  datasets;
  errorDialogMessage: string;
  error;

  current_type;
  current_id;

  @Input()
  header;
  @Input()
  subHeader;
  @Input()
  edit: boolean = false;
  @Input()
  move: boolean = false;
  @Input()
  hide: boolean = false;
  @Input()
  delete: boolean = false;

  @Input()
  side_by_side: boolean = false;

  secondary_breadcrumbs = [];
  secondary_datasets;
  current_secondary_type;



  stateChanges: Subscription;
  constructor(private datasetService: DatasetService,
              private adminService: AdminService,
              private applicationStateService: ApplicationStateService) { }

  ngOnInit() {
    this.breadcrumbs = [];
    this.secondary_breadcrumbs = [];

    this.stateChanges = this.applicationStateService.stateChanged.subscribe(state => {
      if (state) {
        this.current_type = state['parent_type'];
        this.current_id = state.parent.id;
        if (state.add_breadcrumb) this.addBreadcrumb(state.parent);
        this.doPick(state['parent']);
        if ( this.side_by_side ) {
          // Won't be there until the tab is revealed
          if ( state.secondary ) {
            this.current_secondary_type = state.secondary_type;
            if (state.add_secondary_breadcrumb) this.addSecondaryBreadcrumb(state.secondary);
            this.doSecondary(state['secondary']);
          }
        }
      }
    });
  }
  getDataset(dataset: Dataset) {
    if ( dataset.variableChildren ) {
      this.errorDialogMessage = "This data set contains variables, there are no more data sets below this point."
      this.error = true;
    } else {
      this.datasetService.getDataset(dataset.id).subscribe(indataset => {
        this.applicationStateService.setParent(indataset, 'dataset', true);
      });
    }
  }
  getSecondaryDataset(dataset: Dataset) {
    if ( dataset.variableChildren ) {
      this.errorDialogMessage = "This data set contains variables, there are no more data sets below this point."
      this.error = true;
    }
    this.datasetService.getDataset(dataset.id).subscribe(indataset => {
      this.applicationStateService.setSecondary(indataset, 'dataset', true);
    });
  }
  editDataset(dataset: Dataset) {
    this.datasetService.getDataset(dataset.id).subscribe(indataset => {
      this.applicationStateService.setDatasetToEdit(indataset);
    });
  }
  deleteDataset(dataset: Dataset) {
    this.adminService.deleteDataset(dataset.id).subscribe(indataset => {
      this.applicationStateService.setParent(indataset, this.current_type, false);
    });
  }
  moveDataset(from_dataset: Dataset) {
    const dataset = this.applicationStateService.getSecondary();
    const parent = this.applicationStateService.getParent()
    if ( parent.id === dataset.id && this.current_type === this.current_secondary_type ) {
      this.errorDialogMessage = "The source and destination appear to be the same. Nothing to move."
      this.error = true;
    } else {
      const addProperty1: AddProperty = {name: "move_from_id", value: from_dataset.id.toString()};
      const addProperty2: AddProperty = {name: "move_to_id", value: dataset.id.toString()};
      const addProperty3: AddProperty = {name: "move_to_type", value: this.current_secondary_type};
      const props = [addProperty1, addProperty2, addProperty3];
      const moveDataset: AddRequest = {
        addProperties: props,
        url: '',
        type: 'move'
      };
      this.adminService.moveDataset(moveDataset).subscribe(data => {
        const parent = data.origin;
        const dest = data.destination;
          this.applicationStateService.setParentAndSecondary(parent, this.current_type, dest, this.current_secondary_type, false, false)
        }
      )
    }
  }
  showDataset(from_dataset: Dataset) {
    const dataset =  this.applicationStateService.getParent();
    const addProperty1: AddProperty = {name: "move_from_id", value: from_dataset.id.toString()};
    const addProperty2: AddProperty = {name: "move_to_id", value: dataset.id.toString()};
    const addProperty3: AddProperty = {name: "move_to_type", value: this.current_type};
    const props = [addProperty1, addProperty2, addProperty3];
    const moveDataset: AddRequest = {
      addProperties: props,
      url: '',
      type: 'show'
    };
    this.adminService.moveDataset(moveDataset).subscribe(data => {
        const parent = data.origin;
        const dest = data.destination;
        this.applicationStateService.setParentAndSecondary(parent, this.current_type, dest, this.current_secondary_type, false, false)
      }
    )
  }
  doPick(indataset: any) {
    if (indataset != null ) {
      if ( indataset.status === "Ingest failed") {
        this.errorDialogMessage = indataset.message;
        this.error = true;
      } else {
        if ( indataset.datasets != null ) {
          this.datasets = indataset.datasets;
        }
      }
    }
  }
  doSecondary(dataset: any) {
    if ( dataset.datasets != null ) {
      this.secondary_datasets = dataset.datasets;
    }
  }
  addSecondaryBreadcrumb(container: any) {
    if ( this.current_secondary_type === 'site') {
      this.secondary_breadcrumbs = []
    }
    this.secondary_breadcrumbs.push({label: container.title, command: (event)=> {
        this.doSecondaryBreadcrumb(container.title, container.id)
      }})
  }
  addBreadcrumb(parent: any) {
    if ( this.current_type === 'site') {
      this.breadcrumbs = []
    }
    this.breadcrumbs.push({label: parent.title, command: (event)=> {
        this.doBreadcrumb(parent.title, parent.id)
      }})
  }
  doSecondaryBreadcrumb(title: string, id: number) {
    let index = -1;
    for (let i = 0; i < this.secondary_breadcrumbs.length; i++) {
      let bc = this.secondary_breadcrumbs[i];
      if ( bc.label === title) {
        index = i;
      }
    }
    if ( index === 0 ) {
      this.datasetService.getSite().subscribe(site =>{
        this.applicationStateService.setSecondary(site, 'site', true)
      });
    } else {
      this.datasetService.getDataset(id).subscribe(indataset => {
        this.secondary_breadcrumbs.splice(index + 1, this.secondary_breadcrumbs.length - (index + 1));
        this.applicationStateService.setSecondary(indataset, 'dataset', false);
      });
    }
  }
  doBreadcrumb(title: string, id: number) {
    let index = -1;
    for (let i = 0; i < this.breadcrumbs.length; i++) {
      let bc = this.breadcrumbs[i];
      if ( bc.label === title) {
        index = i;
      }
    }
    if ( index === 0 ) {
      this.datasetService.getSite().subscribe(site =>{
        this.applicationStateService.setParent(site, 'site', true);
      });
    } else {
      this.datasetService.getDataset(id).subscribe(indataset => {
        this.breadcrumbs.splice(index + 1, this.breadcrumbs.length - (index + 1));
        this.applicationStateService.setParent(indataset, 'dataset', false);
      });
    }
  }
  doSite(site: Site) {
    this.breadcrumbs = [];
    this.current_id = site.id;
    this.current_type = "site";
    this.breadcrumbs.push({label: site.title, command: ()=>{this.doBreadcrumb(site.title, site.id)}})
    this.datasets = site.datasets;
  }
}
