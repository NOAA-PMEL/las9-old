import { Component, OnInit } from '@angular/core';
import {FormGroup} from "@angular/forms";
import {DatasetService} from "../../dataset.service";
import {StringProperty} from "../forms/string-property";
import {JsonFormService} from "../../json-form.service";
import {AdminService} from "../../admin.service";

@Component({
  selector: 'app-site-edit',
  templateUrl: './site-edit.component.html',
  styleUrls: ['./site-edit.component.css']
})
export class SiteEditComponent implements OnInit {

  constructor(private datasetService:DatasetService,
              private formService:JsonFormService,
              private adminService:AdminService) { }
  site_properties = [];
  siteForm: FormGroup;
  ngOnInit() {
    this.datasetService.getSite().subscribe(site => {
      for (let prop in site) {
        if (site[prop]) {
          if (site[prop] instanceof String || typeof site[prop] === 'string') {
            let sp: StringProperty = new StringProperty({label: prop, value: site[prop], key: prop})
            this.site_properties.push(sp);
          }
        }
      }
      this.siteForm = this.formService.makeFormGroup(this.site_properties);
    })
  }
  save() {
    const dirty = this.getDirtyValues(this.siteForm);
    this.adminService.saveSite(dirty).subscribe(site => {
      this.site_properties = [];
      for (let prop in site) {
        if (site[prop]) {
          if (site[prop] instanceof String || typeof site[prop] === 'string') {
            let sp: StringProperty = new StringProperty({label: prop, value: site[prop], key: prop})
            this.site_properties.push(sp);
          }
        }
      }
      this.siteForm = this.formService.makeFormGroup(this.site_properties);
    });
  }
  getDirtyValues(cg: FormGroup) {
    const dirtyValues = {};
    Object.keys(cg.controls).forEach(c => {
      const currentControl = cg.get(c);

      if (currentControl.dirty) {
        dirtyValues[c] = currentControl.value;
      }
    });
    return dirtyValues;
  }
}
