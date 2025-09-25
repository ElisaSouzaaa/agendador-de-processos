import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-upload-modal',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    ReactiveFormsModule,
    MatButtonModule,
  ],
  templateUrl: './upload-modal.html',
  styleUrls: ['./upload-modal.css'], 
})
export class UploadModal {

  private http = inject(HttpClient);
  private dialogRef = inject(MatDialogRef<UploadModal>);
  selectedFile: File | null = null;
  errorMsg = '';
  jobId: number;

  
  jobName: string;

  constructor() {
    const data = inject(MAT_DIALOG_DATA);
    this.jobId = data.jobId;
    this.jobName = data.jobName;
  }

  onFileChange(event: Event): void {
    this.errorMsg = '';
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length) {
      const file = input.files[0];
      if (!file.name.toLowerCase().endsWith('.txt')) {
        this.errorMsg = 'Apenas arquivos .txt são permitidos.';
        this.selectedFile = null;
      } else {
        this.selectedFile = file;
      }
    }
  }


  submit(): void {
    if (!this.selectedFile) return;

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.http
      .post(`http://localhost:8080/arquivos/upload/${this.jobId}`, formData)
      .subscribe({
        next: () => {
          this.dialogRef.close('Arquivo enviado com sucesso!');
        },
        error: () => {
          this.errorMsg = 'Erro ao enviar o arquivo!';
        },
      });
  }

  cancelar(): void {
    this.dialogRef.close();
  }
}