import { faEye, faPen, faTrash } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeModule, FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { Component } from '@angular/core';

import { library } from '@fortawesome/fontawesome-svg-core';
import { RouterModule } from '@angular/router';
import { CustomAlert } from './components/custom-alert/custom-alert';
import { ConfirmationDialog } from './components/confirmation-dialog/confirmation-dialog';


@Component({
  selector: 'app-root',
  imports: [FontAwesomeModule, RouterModule, CustomAlert, ConfirmationDialog],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected title = 'agendador-frontend';

  constructor(library: FaIconLibrary) {
    library.addIcons(faPen, faEye, faTrash)
  }
}
