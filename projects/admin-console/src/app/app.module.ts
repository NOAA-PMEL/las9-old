import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import {ReactiveFormsModule} from "@angular/forms";
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { JsonFormComponent } from './forms/json-form/json-form.component';
import { FormPropertyComponent } from './forms/form-property/form-property.component';
import { DatasetPickerComponent } from './dataset-picker/dataset-picker.component';
import {
  BreadcrumbModule,
  ButtonModule,
  CardModule,
  DialogModule, DynamicDialogModule, PickListModule, ProgressBarModule,
  ScrollPanelModule,
  SlideMenuModule,
  TabViewModule
} from "primeng";
import {AdminComponent} from "./admin/admin.component";
import { DatasetEditComponent } from './dataset-edit/dataset-edit.component';
import { DatasetAddComponent } from './dataset-add/dataset-add.component';
import { DatasetOrganizeComponent } from './dataset-organize/dataset-organize.component';
import { DatasetHideComponent } from './dataset-hide/dataset-hide.component';
import { DatasetDeleteComponent } from './dataset-delete/dataset-delete.component';
import { SiteEditComponent } from './site-edit/site-edit.component';


@NgModule({
  declarations: [
    AppComponent,
    JsonFormComponent,
    FormPropertyComponent,
    DatasetPickerComponent,
    AdminComponent,
    DatasetEditComponent,
    DatasetAddComponent,
    DatasetOrganizeComponent,
    DatasetHideComponent,
    DatasetDeleteComponent,
    SiteEditComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpClientModule,
    ButtonModule,
    DialogModule,
    DynamicDialogModule,
    CardModule,
    SlideMenuModule,
    BreadcrumbModule,
    ScrollPanelModule,
    TabViewModule,
    PickListModule,
    ProgressBarModule
  ],
  entryComponents: [
    DatasetPickerComponent
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
