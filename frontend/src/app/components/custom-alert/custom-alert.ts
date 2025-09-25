import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AlertService } from '../../services/alert-service';

@Component({
  selector: 'app-custom-alert',
  imports: [CommonModule],
  templateUrl: './custom-alert.html',
  styleUrl: './custom-alert.css'
})
export class CustomAlert {
  public alertService = inject(AlertService);
}
