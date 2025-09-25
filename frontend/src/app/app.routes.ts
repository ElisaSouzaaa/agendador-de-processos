import { Component } from '@angular/core';
import { Routes } from '@angular/router';
import { JobList } from './components/job-list/job-list';
import { JobDetails } from './components/job-details/job-details';
import { JobForm } from './components/job-form/job-form';

export const routes: Routes = [
  {
    path: '',
    component: JobList
  },
  {
    path: 'detalhes/:id',
    component: JobDetails
  },
  {
    path: 'novo',
    component: JobForm
  },
  {
    path: 'editar/:id',
    component: JobForm
  },
  {
    path: '**',
    redirectTo: ''
  }
];