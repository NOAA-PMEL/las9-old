import {Component, OnInit, ViewChild} from '@angular/core';
import {DatasetPickerComponent} from "../dataset-picker/dataset-picker.component";
import {StringProperty} from "../forms/string-property";
import {FormControl, Validators} from "@angular/forms";
import {AddProperty} from "../../add-property";
import {AddRequest} from "../../add-request";
import {AdminService} from "../../admin.service";
import {ApplicationStateService} from "../application-state.service";

@Component({
  selector: 'app-dataset-add',
  templateUrl: './dataset-add.component.html',
  styleUrls: ['./dataset-add.component.css']
})
export class DatasetAddComponent implements OnInit {

  griddedSingleFormControl = new FormControl('', [
    Validators.required,
  ]);
  dsgSingleFormControl = new FormControl('', [
    Validators.required,
  ]);
  threddsFormControl = new FormControl('', [
    Validators.required,
  ]);
  erddapFormControl = new FormControl('', [
    Validators.required,
  ]);
  addEmptyFormControl = new FormControl('', [
    Validators.required
  ]);

  @ViewChild(DatasetPickerComponent, {static: false}) picker: DatasetPickerComponent;
  header = "The dataset will be added to this list.";
  sub_header = "If you want it further down in the hierarchy, select a data set from the list to navigate to where the new data set should appear.";

  constructor(private addDataService: AdminService,
              private applicationStateService: ApplicationStateService) { }

  ngOnInit() {
  }
  submitNetcdf() {
    const addProperty1:AddProperty = {name: "parent_id", value: this.picker.current_id};
    const addProperty2:AddProperty = {name: "parent_type", value: this.picker.current_type};
    const props = [addProperty1, addProperty2];
    const addnetdf: AddRequest = {
      addProperties: props,
      url: this.griddedSingleFormControl.value,
      type: 'netcdf'
    };
    this.addDataService.addDataset(addnetdf).subscribe(data=>{
      this.applicationStateService.setParent(data, 'dataset', false);
      }
    )
  }
  submitThredds() {
    const addProperty1:AddProperty = {name: "parent_id", value: this.picker.current_id};
    const addProperty2:AddProperty = {name: "parent_type", value: this.picker.current_type};
    const props = [addProperty1, addProperty2];
    const addthredds: AddRequest = {
      addProperties: props,
      url: this.threddsFormControl.value,
      type: 'thredds'
    };
    this.addDataService.addDataset(addthredds).subscribe(data=>{
        // This should be return the parent and re-show the current list.
        console.log("Returned from add data method.");
        (data)
      }
    )
  }
  submitTabledap() {
    const addProperty1: AddProperty = {name: "parent_id", value: this.picker.current_id}
    const addProperty2: AddProperty = {name: "parent_type", value: this.picker.current_type}
    const props = [addProperty1, addProperty2];
    const addtabledap: AddRequest = {
      addProperties: props,
      url: this.erddapFormControl.value,
      type: 'tabledap'
    };
    this.addDataService.addDataset(addtabledap).subscribe(data=>{
        console.log("Returned from add data method.");
      this.applicationStateService.setParent(data, 'dataset', false);
      }
    )
  }
  submitDsg() {
    const addProperty1:AddProperty = {name: "parent_id", value: this.picker.current_id};
    const addProperty2:AddProperty = {name: "parent_type", value: this.picker.current_type};
    const props = [addProperty1, addProperty2];
    const adddsg: AddRequest = {
      addProperties: props,
      url: this.dsgSingleFormControl.value,
      type: 'dsg'
    };
    this.addDataService.addDataset(adddsg).subscribe(data=>{
      this.applicationStateService.setParent(data, 'dataset', false);
      }
    )
  }
  addEmpty() {
    const addProperty1:AddProperty = {name: "parent_id", value: this.picker.current_id};
    const addProperty2:AddProperty = {name: "parent_type", value: this.picker.current_type};
    const addProperty3:AddProperty = {name: "name", value: this.addEmptyFormControl.value};
    const props = [addProperty1, addProperty2, addProperty3];
    const addEmpty: AddRequest = {
      addProperties: props,
      url: null,
      type: 'empty'
    };
    this.addDataService.addDataset(addEmpty).subscribe(data=>{
      this.applicationStateService.setParent(data, this.picker.current_type, false);
    });
  }
}
