import { isValidCron } from 'cron-validator';
import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidatorFn,
  ValidationErrors,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobService } from '../../services/job-service';
import { AlertService } from '../../services/alert-service';
import { finalize } from 'rxjs';

export function cronExpressionValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) return { required: true };
    let valid = isValidCron(value, { seconds: true, allowBlankDay: true });
    if (!valid) valid = isValidCron(value, { seconds: false, allowBlankDay: true });
    return valid ? null : { invalidCron: true };
  };
}

@Component({
  selector: 'app-job-form',
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './job-form.html',
  styleUrls: ['./job-form.css'],
})
export class JobForm implements OnInit {
  form!: FormGroup;
  isEdit = false;
  jobId?: number;
  loading = false;
  formSubmitted = false;

  private fb = inject(FormBuilder);
  private jobService = inject(JobService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private alertService = inject(AlertService);

  constructor() {}

  ngOnInit(): void {
    this.form = this.fb.group({
      nome: ['', Validators.required],
      cronExpression: ['', [Validators.required, cronExpressionValidator()]],
    });

    this.jobId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.jobId) {
      this.isEdit = true;
      this.carregarJob(this.jobId);
    }
  }

  get nome() {
    return this.form.get('nome');
  }

  get cronExpression() {
    return this.form.get('cronExpression');
  }

  carregarJob(id: number) {
    this.loading = true;
    this.jobService
      .buscarJobId(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (job) => {
          this.form.patchValue({
            nome: job.nome,
            cronExpression: job.cronExpression,
          });
        },
        error: () => {
          this.alertService.show('Erro ao carregar agendamento');
        },
      });
  }

  salvar() {
    this.formSubmitted = true;
    if (this.form.invalid) {
      this.alertService.show('Por favor, preencha os campos corretamente');
      return;
    }

    this.loading = true;
    const jobData = this.form.value;

    if (this.isEdit && this.jobId) {
      this.jobService
        .atualizarJob(this.jobId, jobData)
        .pipe(finalize(() => (this.loading = false)))
        .subscribe({
          next: () => {
            this.alertService.show('Agendamento atualizado com sucesso!');
            this.router.navigate(['/']);
          },
          error: () => {
            this.alertService.show('Erro ao atualizar agendamento');
          },
        });
    } else {
      this.jobService
        .criarJob(jobData)
        .pipe(finalize(() => (this.loading = false)))
        .subscribe({
          next: () => {
            this.alertService.show('Agendamento criado com sucesso!');
            this.router.navigate(['/']);
          },
          error: () => {
            this.alertService.show('Erro ao criar agendamento');
          },
        });
    }
  }

  cancelar() {
    this.router.navigate(['/']);
  }
}
