import { Component, inject } from '@angular/core';
import { ConfirmationService } from '../../services/confirmation-service';

@Component({
  selector: 'app-confirmation-dialog',
  imports: [],
  templateUrl: './confirmation-dialog.html',
  styleUrl: './confirmation-dialog.css'
})
export class ConfirmationDialog {
  public confirmationService = inject(ConfirmationService);
}
