import { Component, OnInit } from '@angular/core';
import {FormGroup} from "@angular/forms";
import {DatasetService} from "../../dataset.service";
import {StringProperty} from "../forms/string-property";
import {JsonFormService} from "../../json-form.service";
import {AdminService} from "../../admin.service";
import {Util} from "../util/Util";
import {FooterLink} from "../json/Site";
import {ApplicationStateService} from "../application-state.service";

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

  footerLinks: any;
  edit: boolean = false;

  footerLinkForm: FormGroup;
  footerLinkToEdit: FooterLink;
  edit_footerLink: boolean = false;
  linkProperties = [];

  ngOnInit() {
    this.datasetService.getSite().subscribe(site => {
      for (let prop in site) {
        if (site[prop]) {
          if (site[prop] instanceof String || typeof site[prop] === 'string') {
            let sp: StringProperty = new StringProperty({label: prop, value: site[prop], key: prop})
            this.site_properties.push(sp);
          } else if (Util.isArray(site[prop])) {
            if (prop === "footerLinks") {
              this.footerLinks = site[prop];
            }
          }
        }
      }
      this.siteForm = this.formService.makeFormGroup(this.site_properties);
    })
  }
  editLink(flink: FooterLink) {
    this.linkProperties = [];
    this.footerLinkToEdit = flink;
    for ( let prop in flink ) {
      if ( flink[prop] ) {
        if ( flink[prop] instanceof String || typeof flink[prop] === 'string') {
          let fp: StringProperty = new StringProperty({label: prop, value: flink[prop], key: prop})
          this.linkProperties.push(fp);
        } else if ( Util.isNumber(flink[prop]) || isNaN(flink[prop])) {
          let fp: StringProperty = new StringProperty({label: prop, value: flink[prop], key: prop})
          this.linkProperties.push(fp);
        }
      }
    }
    this.footerLinkForm = this.formService.makeFormGroup(this.linkProperties);
    this.edit_footerLink = true;
  }
  deleteLink(flink: FooterLink) {
    let id = flink.id;
    let remove = -1;
    for (let i = 0; i < this.footerLinks.length; i++) {
      let fl = this.footerLinks[i];
      if ( fl.id == id ) {
        remove = i;
      }
    }
    if ( remove >= 0 ) {
      this.footerLinks.splice(remove, 1);
    }
  }
  save() {
    const dirty = this.getDirtyValues(this.siteForm);
    dirty['footerLinks'] = this.footerLinks;
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
  doneLink() {
    const dirty = this.getDirtyValues(this.footerLinkForm);
    for (let prop in dirty ) {
      this.footerLinkToEdit[prop] = dirty[prop];
    }
    this.edit_footerLink = false;
  }
  add() {
    this.linkProperties = [];
    this.footerLinkToEdit = new class implements FooterLink {
      id: number;
      index: number;
      text: string;
      url: string;
    };
    this.footerLinkToEdit['url'] = "";
    this.footerLinkToEdit['test'] = "";
    this.footerLinkToEdit['index'] = 0;
    this.footerLinks.push(this.footerLinkToEdit);
    let fpu: StringProperty = new StringProperty({label: 'url', value: '', key: 'url'})
    this.linkProperties.push(fpu);
    let fpt: StringProperty = new StringProperty({label: 'text', value: '', key: 'text'})
    this.linkProperties.push(fpt);
    let fpi: StringProperty = new StringProperty({label: 'index', value: '', key: 'index'})
    this.linkProperties.push(fpi);
    this.footerLinkForm = this.formService.makeFormGroup(this.linkProperties);
    this.edit_footerLink = true;
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
